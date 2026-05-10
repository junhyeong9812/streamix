# [P2-23] FileDeleteService 멱등성 부재

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — REST 시맨틱** |
| 카테고리 | API 설계 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `FileDeleteService.java`, `DeleteFileUseCase.java` |

## 문제 분석

### 현재 동작
```java
// FileDeleteService.delete(UUID fileId)
@Override
public void delete(UUID fileId) {
    log.info("Deleting file: {}", fileId);
    
    // 1. 메타데이터 조회 (존재 확인)
    FileMetadata metadata = metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));   // ⚠️ 두 번째 호출 시 404

    // 2~4. 삭제 로직
    deleteFileQuietly(metadata.storagePath(), "file");
    if (metadata.hasThumbnail()) {
        deleteFileQuietly(metadata.thumbnailPath(), "thumbnail");
    }
    metadataRepository.deleteById(fileId);
}
```

### REST DELETE의 멱등성 (RFC 7231 §4.2.2)
> The PUT, DELETE, and safe request methods are idempotent.

> A request method is considered "idempotent" if the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request.

→ 같은 DELETE 요청을 여러 번 보내도 서버 상태가 같아야 함 + 응답 코드도 일관되어야 함 (이상적).

### 응답 코드 옵션
RFC 7231은 명확한 답을 주지 않지만 일반적 패턴:
- **204 No Content**: 삭제 성공 또는 이미 없음 (멱등성 강조)
- **404 Not Found**: 처음부터 없는 리소스 (현재 동작)
- **200 OK + body**: 추가 정보 반환

### 현재 동작
1. 첫 호출: 200/204 + 삭제됨
2. 두 번째 호출: **404** (FileNotFoundException → handleFileNotFound)

→ 멱등성 시맨틱 위반은 아니지만(서버 상태는 같음) 응답 코드 불일치.

### 영향 범위
- 클라이언트가 retry 로직을 가질 때:
  - 첫 호출 응답이 네트워크 오류로 안 도착 → retry → 두 번째는 404
  - 클라이언트는 "삭제 실패"로 잘못 인식 가능
- mobile/flaky network에서 더 흔함

### 그래도 404가 정당한 경우
- "사용자가 잘못된 ID 입력" → 알려야 함
- API 명세에 따라 다름

## 변경 프로세스

### 옵션 비교
| 옵션 | 동작 | 장점 | 단점 |
|------|------|------|------|
| A. 현재 유지 (404 throw) | 명시적 | 잘못된 ID 알림 | retry 시 일관성 ↓ |
| B. 멱등 모드 — 없으면 silent return | 항상 204 | retry 안전 | 잘못된 ID 사일런트 |
| C. 옵션으로 선택 가능 | `delete(id, idempotent)` 오버로드 | 유연 | API 복잡 |

### 채택: 옵션 A 유지 + 컨트롤러 수준 보강
이유:
1. 도메인 서비스는 정확하게 동작 (없는 ID는 예외)
2. REST 시맨틱은 컨트롤러/HTTP 계층에서 처리
3. UseCase 사용자에게 정확한 정보 제공

### Step 1: FileDeleteService 변경 — 메서드 추가 (옵션 B용)
```java
public interface DeleteFileUseCase {
    void delete(UUID fileId);                  // 기존 — 없으면 예외
    
    /**
     * 파일을 멱등하게 삭제합니다. 존재하지 않아도 예외 없음.
     * 
     * @param fileId 삭제할 파일 ID
     * @return 실제로 삭제 동작이 발생했으면 true (메타데이터가 있었음)
     */
    boolean deleteIdempotent(UUID fileId);     // 신규
}
```

### Step 2: FileDeleteService 구현
```java
@Override
public boolean deleteIdempotent(UUID fileId) {
    Optional<FileMetadata> opt = metadataRepository.findById(fileId);
    if (opt.isEmpty()) {
        log.debug("Idempotent delete: file not found, no-op: {}", fileId);
        return false;
    }
    FileMetadata metadata = opt.get();
    deleteFileQuietly(metadata.storagePath(), "file");
    if (metadata.hasThumbnail()) {
        deleteFileQuietly(metadata.thumbnailPath(), "thumbnail");
    }
    metadataRepository.deleteById(fileId);
    return true;
}

@Override
public void delete(UUID fileId) {
    if (!deleteIdempotent(fileId)) {
        throw new FileNotFoundException(fileId);
    }
}
```

→ delete()는 deleteIdempotent를 호출 후 false면 예외. 내부 코드 일치.

### Step 3: REST 컨트롤러는 `delete()` 그대로 사용
404 시맨틱 유지:
```java
// StreamixApiController.deleteFile (변경 없음)
@DeleteMapping(...)
public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
    deleteFileUseCase.delete(id);   // 없으면 FileNotFoundException → 404
    return ResponseEntity.noContent().build();
}
```

### Step 4: 대시보드는 멱등 모드 사용 (UX 개선)
```java
// StreamixDashboardController.deleteFile
@PostMapping("${streamix.dashboard.path:/streamix}/files/{id}/delete")
public String deleteFile(UUID id, RedirectAttributes redirectAttributes) {
    try {
        boolean removed = deleteFileUseCase.deleteIdempotent(id);
        if (removed) {
            redirectAttributes.addFlashAttribute("successMessage", "파일이 삭제되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "파일이 이미 존재하지 않습니다.");
        }
    } catch (StreamixException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "파일 삭제 실패: " + e.getMessage());
    }
    return "redirect:" + properties.dashboard().path() + "/files";
}
```

(layout.html은 successMessage, errorMessage만 처리하니 infoMessage 추가 또는 successMessage로 통합 — 단순성 위해 successMessage 통합)

```java
String message = removed ? "파일이 삭제되었습니다." : "이미 삭제된 파일입니다.";
redirectAttributes.addFlashAttribute("successMessage", message);
```

### Step 5: 테스트 추가
```java
@Test
void deleteIdempotent_returnsFalseWhenNotFound() {
    UUID fileId = UUID.randomUUID();
    given(metadataRepository.findById(fileId)).willReturn(Optional.empty());
    
    boolean result = deleteService.deleteIdempotent(fileId);
    
    assertThat(result).isFalse();
    verify(storage, never()).delete(anyString());
    verify(metadataRepository, never()).deleteById(any());
}

@Test
void deleteIdempotent_returnsTrueWhenDeleted() {
    UUID fileId = UUID.randomUUID();
    given(metadataRepository.findById(fileId))
        .willReturn(Optional.of(createMetadata(fileId)));
    
    boolean result = deleteService.deleteIdempotent(fileId);
    
    assertThat(result).isTrue();
    verify(metadataRepository).deleteById(fileId);
}

@Test
void delete_throwsWhenNotFound() {
    UUID fileId = UUID.randomUUID();
    given(metadataRepository.findById(fileId)).willReturn(Optional.empty());
    
    assertThatThrownBy(() -> deleteService.delete(fileId))
        .isInstanceOf(FileNotFoundException.class);
}
```

## Before / After

### Before — DeleteFileUseCase
```java
public interface DeleteFileUseCase {
    void delete(UUID fileId);
}
```

### After — DeleteFileUseCase
```java
public interface DeleteFileUseCase {
    /**
     * 파일을 삭제합니다.
     * 
     * @param fileId 삭제할 파일 ID
     * @throws FileNotFoundException 파일이 존재하지 않는 경우
     */
    void delete(UUID fileId);

    /**
     * 파일을 멱등하게 삭제합니다.
     * 
     * <p>존재하지 않아도 예외 없이 false를 반환합니다.
     * 같은 ID를 여러 번 호출해도 안전합니다 (REST DELETE 멱등성).</p>
     * 
     * @param fileId 삭제할 파일 ID
     * @return 실제 삭제 동작이 발생했으면 {@code true}
     * @since 2.0.1
     */
    boolean deleteIdempotent(UUID fileId);
}
```

### Before — FileDeleteService.delete (구현)
```java
@Override
public void delete(UUID fileId) {
    log.info("Deleting file: {}", fileId);
    FileMetadata metadata = metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));
    deleteFileQuietly(metadata.storagePath(), "file");
    if (metadata.hasThumbnail()) {
        deleteFileQuietly(metadata.thumbnailPath(), "thumbnail");
    }
    metadataRepository.deleteById(fileId);
    log.info("File deleted successfully: {}", fileId);
}
```

### After — FileDeleteService
```java
@Override
public boolean deleteIdempotent(UUID fileId) {
    log.info("Deleting file (idempotent): {}", fileId);
    Optional<FileMetadata> opt = metadataRepository.findById(fileId);
    if (opt.isEmpty()) {
        log.debug("File not found, idempotent no-op: {}", fileId);
        return false;
    }
    FileMetadata metadata = opt.get();
    deleteFileQuietly(metadata.storagePath(), "file");
    if (metadata.hasThumbnail()) {
        deleteFileQuietly(metadata.thumbnailPath(), "thumbnail");
    }
    metadataRepository.deleteById(fileId);
    log.info("File deleted: {}", fileId);
    return true;
}

@Override
public void delete(UUID fileId) {
    if (!deleteIdempotent(fileId)) {
        throw new FileNotFoundException(fileId);
    }
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-core:compileJava
```

### 2. 단위 테스트
```bash
./gradlew :streamix-core:test --tests FileDeleteServiceTest
```

### 3. 통합 — REST API
```bash
# 첫 삭제: 204
curl -i -X DELETE http://localhost:8080/api/streamix/files/{id}
# 두 번째 삭제: 404
curl -i -X DELETE http://localhost:8080/api/streamix/files/{id}
```

### 4. 대시보드
- 대시보드에서 삭제 → "파일이 삭제되었습니다"
- 다시 같은 파일 (캐시된 페이지에서 다시 클릭) → "이미 삭제된 파일입니다" (오류 아님)

## 관련 파일
- `streamix-core/src/main/java/.../core/application/port/in/DeleteFileUseCase.java`
- `streamix-core/src/main/java/.../core/application/service/FileDeleteService.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/dashboard/StreamixDashboardController.java`
- `streamix-core/src/test/java/.../core/application/service/FileDeleteServiceTest.java`

## 참고
- RFC 7231 §4.2.2 Idempotent Methods
- REST API design — Idempotent DELETE patterns
- HTTP DELETE returning 204 (멱등) vs 404 (정확) — 설계 선택
