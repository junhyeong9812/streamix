# [P2-22] FileUploadService.generateThumbnail — Exception catch 좁히기

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 예외 처리 정확성** |
| 카테고리 | 견고성 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `FileUploadService.java`, `FileDeleteService.java` |

## 문제 분석

### 현재 동작 — FileUploadService
```java
// FileUploadService.java:289
private FileMetadata generateThumbnail(...) {
    try {
        byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);
        String thumbnailFileName = fileId + "_thumb.jpg";
        String thumbnailPath = storage.save(thumbnailFileName, new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length);
        log.debug("Thumbnail generated: {}", thumbnailPath);
        return metadata.withThumbnailPath(thumbnailPath);
    } catch (Exception e) {                                 // ⚠️ 너무 광범위
        log.warn("Failed to generate thumbnail for file: {}", fileId, e);
        return metadata;
    }
}
```

### 현재 동작 — FileDeleteService
```java
// FileDeleteService.java:91-98
private void deleteFileQuietly(String path, String type) {
    try {
        storage.delete(path);
        log.debug("Deleted {}: {}", type, path);
    } catch (Exception e) {                                  // ⚠️ 너무 광범위
        log.warn("Failed to delete {}: {}", type, path, e);
    }
}
```

### 문제

#### 결함 1: `Error`와 `RuntimeException`을 구분 못 함
- `OutOfMemoryError`, `StackOverflowError` 같은 `Error`도 `Exception` catch가 잡지 않지만 (Error는 Throwable의 다른 분기)
- `Exception`은 `RuntimeException`의 부모 → 모든 RuntimeException 흡수
- 이상적: `ThumbnailGenerationException` + `StorageException`만 catch — 그 외는 던지기

#### 결함 2: `InterruptedException` 등 의도치 않은 catch
- 현재 thumbnailService와 storage는 checked exception을 던지지 않으므로 `Exception` catch는 RuntimeException만 잡음 (안전한 편)
- 하지만 향후 thumbnail 구현이 InterruptedException을 던지도록 확장되면 silent swallow

#### 결함 3: 디버깅 어려움
`Exception e`로 잡으면 NullPointerException, IllegalStateException 같은 프로그래밍 에러도 흡수 → 버그 isolated 어려움

### 기대 동작
구체적 예외만 catch:
- `ThumbnailGenerationException` — 썸네일 도메인 예외
- `StorageException` — 저장소 도메인 예외
- 그 외 (RuntimeException, NullPointerException 등) → 위로 전파

## 변경 프로세스

### Step 1: FileUploadService.generateThumbnail
```java
private FileMetadata generateThumbnail(FileMetadata metadata, FileType fileType,
                                        String storagePath, UUID fileId) {
    try {
        byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);
        String thumbnailFileName = fileId + "_thumb.jpg";
        String thumbnailPath = storage.save(
            thumbnailFileName,
            new ByteArrayInputStream(thumbnailBytes),
            thumbnailBytes.length
        );
        log.debug("Thumbnail generated: {}", thumbnailPath);
        return metadata.withThumbnailPath(thumbnailPath);

    } catch (ThumbnailGenerationException | StorageException e) {
        // 썸네일/저장소 도메인 예외만 silent fail (업로드 자체는 성공시킴)
        log.warn("Failed to generate thumbnail for file: {}, type={}", fileId, e.getClass().getSimpleName(), e);
        return metadata;
    }
}
```

### Step 2: FileDeleteService.deleteFileQuietly
```java
private void deleteFileQuietly(String path, String type) {
    try {
        storage.delete(path);
        log.debug("Deleted {}: {}", type, path);
    } catch (StorageException e) {
        // 저장소 예외만 silent fail (메타데이터 삭제는 진행)
        log.warn("Failed to delete {}: {}", type, path, e);
    }
}
```

### Step 3: 테스트 갱신
`FileUploadServiceTest.uploadSucceedsEvenIfThumbnailFails`:
```java
// Before
given(thumbnailService.generate(any(), anyString(), anyInt(), anyInt()))
    .willThrow(new RuntimeException("Thumbnail generation failed"));

// After — 명시적인 ThumbnailGenerationException
given(thumbnailService.generate(any(), anyString(), anyInt(), anyInt()))
    .willThrow(new ThumbnailGenerationException("Test failure"));
```

`FileUploadServiceTest.savesMetadataCorrectly` (line 234) — 동일하게 `RuntimeException` → `ThumbnailGenerationException`로 변경

`FileDeleteServiceTest.deletesMetadataEvenIfStorageFails` (line 101):
```java
// Before
doThrow(new RuntimeException("Storage error")).when(storage).delete(anyString());

// After
doThrow(new StorageException("Test failure")).when(storage).delete(anyString());
```

→ `deletesMetadataEvenIfThumbnailDeleteFails`, `deletesMetadataEvenIfAllStorageDeletesFail`도 동일 변경

### Step 4: 만약 다른 RuntimeException이 발생하면 어떻게 할지
`ThumbnailGenerationException` / `StorageException` 외의 RuntimeException(예: NPE) → 호출자 `upload()` 메서드까지 전파 → `GlobalExceptionHandler.handleGenericException`이 500 응답 반환

이는 의도된 동작 — **버그성 예외는 표면화**되어야 함.

## Before / After

### Before — FileUploadService.generateThumbnail
```java
private FileMetadata generateThumbnail(
    FileMetadata metadata, FileType fileType, String storagePath, UUID fileId
) {
    try {
        byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);
        String thumbnailFileName = fileId + "_thumb.jpg";
        String thumbnailPath = storage.save(thumbnailFileName,
            new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length);
        log.debug("Thumbnail generated: {}", thumbnailPath);
        return metadata.withThumbnailPath(thumbnailPath);
    } catch (Exception e) {
        log.warn("Failed to generate thumbnail for file: {}", fileId, e);
        return metadata;
    }
}
```

### After — FileUploadService.generateThumbnail
```java
private FileMetadata generateThumbnail(
    FileMetadata metadata, FileType fileType, String storagePath, UUID fileId
) {
    try {
        byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);
        String thumbnailFileName = fileId + "_thumb.jpg";
        String thumbnailPath = storage.save(thumbnailFileName,
            new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length);
        log.debug("Thumbnail generated: {}", thumbnailPath);
        return metadata.withThumbnailPath(thumbnailPath);
    } catch (ThumbnailGenerationException | StorageException e) {
        log.warn("Failed to generate thumbnail for file {} ({}): {}",
            fileId, e.getClass().getSimpleName(), e.getMessage(), e);
        return metadata;
    }
}
```

### Before — FileDeleteService.deleteFileQuietly
```java
private void deleteFileQuietly(String path, String type) {
    try {
        storage.delete(path);
        log.debug("Deleted {}: {}", type, path);
    } catch (Exception e) {
        log.warn("Failed to delete {}: {}", type, path, e);
    }
}
```

### After — FileDeleteService.deleteFileQuietly
```java
private void deleteFileQuietly(String path, String type) {
    try {
        storage.delete(path);
        log.debug("Deleted {}: {}", type, path);
    } catch (StorageException e) {
        log.warn("Failed to delete {} at {}: {}", type, path, e.getMessage(), e);
    }
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-core:compileJava
```

### 2. 테스트 갱신 후 실행
```bash
./gradlew :streamix-core:test --tests FileUploadServiceTest
./gradlew :streamix-core:test --tests FileDeleteServiceTest
```

### 3. 비도메인 예외 표면화 검증 (수동)
- 의도적으로 NullPointerException 발생시키는 mock thumbnailService 주입 시 → 업로드 실패 (500)
- 도메인 예외(ThumbnailGenerationException) 발생 시 → 업로드 성공, thumbnail null

## 관련 파일
- `streamix-core/src/main/java/.../core/application/service/FileUploadService.java`
- `streamix-core/src/main/java/.../core/application/service/FileDeleteService.java`
- `streamix-core/src/test/java/.../core/application/service/FileUploadServiceTest.java`
- `streamix-core/src/test/java/.../core/application/service/FileDeleteServiceTest.java`

## 참고
- "Catch the most specific exception possible" — Java best practice
- Effective Java Item 73: "Throw exceptions appropriate to the abstraction"
- silent fail은 도메인 예외에만 적용 — 프로그래밍 에러는 표면화
