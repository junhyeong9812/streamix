# [P1-12] MediaType.parseMediaType — 사용자 입력 직접 사용 시 500 발생

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — 입력 검증 / 500 응답** |
| 카테고리 | 견고성 (robustness) |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixApiController.java` |

## 문제 분석

### 현재 동작
```java
// StreamixApiController.java:222-225
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.parseMediaType(streamableFile.getContentType()));
headers.setContentLength(streamableFile.contentLength());
headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
```

`streamableFile.getContentType()`는 `FileMetadata.contentType`이고, 이는 업로드 시 사용자가 보낸 `MultipartFile.getContentType()`을 그대로 저장한 값.

### 위협
사용자가 잘못된 contentType으로 업로드:
- `multipart/form-data; boundary="`  (따옴표 미종결)
- `image/jpeg; charset="; xss=1`
- 빈 문자열 또는 null
- `not-a-mime-type`

→ `MediaType.parseMediaType()`이 `InvalidMediaTypeException` 발생
→ Spring 기본 `ResponseEntityExceptionHandler`도 처리 안 함 (Streamix의 catch-all `Exception` handler가 잡아 500 반환)

### 증상
```bash
$ curl -X POST -F "file=@x.jpg;type=invalid;mime" http://localhost:8080/api/streamix/files
# 업로드: 201 Created
# 그 다음:
$ curl -i http://localhost:8080/api/streamix/files/{id}/stream
HTTP/1.1 500 Internal Server Error
```

### 원인 분석
- `FileUploadService`는 contentType 검증 안 함 (FileTypeDetector로 type 추론은 하지만 contentType 자체 형식 검증 안 됨)
- 컨트롤러는 저장된 contentType을 신뢰
- `MediaType.parseMediaType`은 lenient하지 않음 — 형식 에러 시 예외

### 영향 범위
- 정상 사용자에겐 영향 없음 (브라우저가 정확한 contentType 보냄)
- 의도적 공격 또는 잘못된 클라이언트(스크립트, curl)에서 500 유발 가능
- DoS는 아니지만 신뢰성 저하

## 변경 프로세스

### 옵션 비교
| 옵션 | 전략 | 장점 | 단점 |
|------|------|------|------|
| A. 업로드 시점 검증 + 안전한 default | 입력 sanitization | 저장 데이터 깨끗 | 모든 사용자에 영향 |
| B. 응답 시점 안전 처리 | parseMediaType 실패 시 application/octet-stream fallback | 기존 데이터 호환 | 표면적 처리 |
| C. 둘 다 | defense in depth | 가장 안전 | 코드 분량 ↑ |

### 채택: 옵션 C (둘 다)
이유:
1. 기존 데이터 호환 + 신규 데이터 깨끗
2. 보안 default fallback은 프로덕션에 항상 필요

### Step 1: 업로드 시점 — FileUploadService에 contentType 정규화
```java
// FileUploadService.upload(UploadCommand command) 내부
String normalizedContentType = normalizeContentType(command.contentType(), command.originalName());
FileType fileType = fileTypeDetector.detect(command.originalName(), normalizedContentType);
// ...
FileMetadata metadata = new FileMetadata(
    fileId,
    command.originalName(),
    fileType,
    normalizedContentType,           // ⭐ 정규화된 값 저장
    command.size(),
    storagePath,
    null,
    LocalDateTime.now(),
    LocalDateTime.now()
);

private String normalizeContentType(String contentType, String fileName) {
    if (contentType == null || contentType.isBlank()) {
        return fileTypeDetector.getContentType(fileTypeDetector.extractExtension(fileName));
    }
    try {
        // Spring MediaType으로 파싱 시도 — 형식 검증
        org.springframework.http.MediaType.parseMediaType(contentType);
        return contentType;
    } catch (org.springframework.util.InvalidMimeTypeException e) {
        log.warn("Invalid contentType '{}', falling back to extension-based", contentType);
        return fileTypeDetector.getContentType(fileTypeDetector.extractExtension(fileName));
    }
}
```

⚠ 단, FileUploadService는 streamix-core 모듈이고 spring 의존성이 없음. → core에서 parseMediaType 의존 불가. 다른 방식 필요:

### Step 1' (수정): regex 기반 lenient 검증을 core에 추가
```java
// FileTypeDetector.java
private static final java.util.regex.Pattern MIME_TYPE_PATTERN =
    java.util.regex.Pattern.compile("^[\\w.+-]+/[\\w.+-]+(;.*)?$");

public boolean isValidContentType(String contentType) {
    return contentType != null
        && !contentType.isBlank()
        && MIME_TYPE_PATTERN.matcher(contentType.trim()).matches();
}

public String safeContentType(String contentType, String fileName) {
    if (isValidContentType(contentType)) return contentType.trim();
    String ext = extractExtension(fileName);
    return getContentType(ext);  // 확장자 기반 fallback
}
```

### Step 2: FileUploadService에서 사용
```java
// FileUploadService.upload() 내부 변경
String safeContentType = fileTypeDetector.safeContentType(command.contentType(), command.originalName());
FileType fileType = fileTypeDetector.detect(command.originalName(), safeContentType);
validateFileSize(command.originalName(), command.size());
validateFileType(command.originalName(), fileType);
// ...
FileMetadata metadata = new FileMetadata(
    fileId,
    command.originalName(),
    fileType,
    safeContentType,         // ⭐
    command.size(),
    storagePath,
    null,
    LocalDateTime.now(),
    LocalDateTime.now()
);
```

### Step 3: 응답 시점 — StreamixApiController defense
```java
private static MediaType safeMediaType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
        return MediaType.APPLICATION_OCTET_STREAM;
    }
    try {
        return MediaType.parseMediaType(contentType);
    } catch (org.springframework.util.InvalidMimeTypeException e) {
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}

// 사용:
headers.setContentType(safeMediaType(streamableFile.getContentType()));
```

(getThumbnail은 항상 image/jpeg 고정이라 영향 없음)

### Step 4: UploadCommand의 contentType nullable 허용
현재:
```java
public UploadCommand {
    if (contentType == null || contentType.isBlank()) {
        throw new IllegalArgumentException("contentType must not be blank");
    }
    // ...
}
```

브라우저가 contentType을 안 보내는 경우(특히 octet-stream으로 fallback 안 하는 클라이언트)도 정상 처리되도록 변경 검토 — 현재는 차단. 일단 유지하되 서비스 레이어가 fallback 처리.

## Before / After

### Before — StreamixApiController.streamFile
```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.parseMediaType(streamableFile.getContentType()));
headers.setContentLength(streamableFile.contentLength());
```

### After — StreamixApiController.streamFile
```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(safeMediaType(streamableFile.getContentType()));
headers.setContentLength(streamableFile.contentLength());

// (private static method 추가)
private static MediaType safeMediaType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
        return MediaType.APPLICATION_OCTET_STREAM;
    }
    try {
        return MediaType.parseMediaType(contentType);
    } catch (org.springframework.util.InvalidMimeTypeException e) {
        log.warn("Invalid stored contentType '{}', falling back to octet-stream", contentType);
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
```

### Before — FileUploadService.upload (관련 부분)
```java
FileType fileType = fileTypeDetector.detect(command.originalName(), command.contentType());
// ...
FileMetadata metadata = new FileMetadata(
    fileId,
    command.originalName(),
    fileType,
    command.contentType(),   // 사용자 입력 그대로
    ...
);
```

### After — FileUploadService.upload (관련 부분)
```java
String safeContentType = fileTypeDetector.safeContentType(command.contentType(), command.originalName());
FileType fileType = fileTypeDetector.detect(command.originalName(), safeContentType);
// ...
FileMetadata metadata = new FileMetadata(
    fileId,
    command.originalName(),
    fileType,
    safeContentType,         // 검증/정규화된 값
    ...
);
```

### Before — FileTypeDetector
```java
public String getContentType(String extension) {
    if (extension == null) return "application/octet-stream";
    return EXTENSION_TO_CONTENT_TYPE.getOrDefault(extension.toLowerCase(), "application/octet-stream");
}
```

### After — FileTypeDetector (추가)
```java
private static final java.util.regex.Pattern MIME_TYPE_PATTERN =
    java.util.regex.Pattern.compile("^[\\w.+-]+/[\\w.+-]+(;.*)?$");

/**
 * Content-Type 형식이 RFC 6838 호환인지 검증합니다.
 *
 * @param contentType 검증할 contentType
 * @return 형식이 유효하면 true
 */
public boolean isValidContentType(String contentType) {
    return contentType != null
        && !contentType.isBlank()
        && MIME_TYPE_PATTERN.matcher(contentType.trim()).matches();
}

/**
 * 안전한 contentType을 반환합니다.
 *
 * <p>입력 contentType이 유효하면 그대로, 아니면 파일명 확장자 기반 fallback,
 * 그것도 안 되면 application/octet-stream</p>
 *
 * @param contentType 사용자 입력 contentType
 * @param fileName    파일명 (확장자 기반 fallback용)
 * @return 안전한 contentType
 */
public String safeContentType(String contentType, String fileName) {
    if (isValidContentType(contentType)) return contentType.trim();
    String ext = extractExtension(fileName);
    return getContentType(ext);
}
```

## 검증 방법

### 1. 단위 테스트
```java
// FileTypeDetectorTest 추가
@Test
@DisplayName("유효한 MIME 타입은 그대로 반환")
void safeContentType_validReturnsAsIs() {
    assertThat(detector.safeContentType("image/jpeg", "x.jpg")).isEqualTo("image/jpeg");
    assertThat(detector.safeContentType("application/pdf; charset=UTF-8", "x.pdf"))
        .isEqualTo("application/pdf; charset=UTF-8");
}

@Test
@DisplayName("잘못된 MIME 타입은 확장자 기반 fallback")
void safeContentType_invalidFallsBack() {
    assertThat(detector.safeContentType("not-a-mime-type", "x.jpg")).isEqualTo("image/jpeg");
    assertThat(detector.safeContentType("multipart/; boundary=\"", "x.png")).isEqualTo("image/png");
}

@Test
@DisplayName("null/blank contentType은 확장자 fallback")
void safeContentType_nullFallsBack() {
    assertThat(detector.safeContentType(null, "x.mp4")).isEqualTo("video/mp4");
    assertThat(detector.safeContentType("  ", "x.mp4")).isEqualTo("video/mp4");
}

@Test
@DisplayName("확장자도 없으면 octet-stream")
void safeContentType_noExtension() {
    assertThat(detector.safeContentType(null, "noextension")).isEqualTo("application/octet-stream");
}
```

### 2. 통합 테스트 — 잘못된 contentType으로 업로드 후 stream
```java
@Test
void uploadWithInvalidContentType_streamsSafely() {
    // given - 잘못된 contentType으로 업로드
    UploadCommand cmd = new UploadCommand("photo.jpg", "totally-invalid", 1024L, ...);
    UploadResult result = uploadService.upload(cmd);
    
    // 메타데이터에는 image/jpeg가 저장되어 있어야 함 (확장자 fallback)
    FileMetadata stored = metadataRepository.findById(result.id()).orElseThrow();
    assertThat(stored.contentType()).isEqualTo("image/jpeg");
}
```

### 3. 회귀 테스트
```bash
./gradlew test
```

## 관련 파일
- `streamix-core/src/main/java/.../core/domain/service/FileTypeDetector.java`
- `streamix-core/src/main/java/.../core/application/service/FileUploadService.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixApiController.java`
- `streamix-core/src/test/java/.../core/domain/service/FileTypeDetectorTest.java`

## 참고
- RFC 6838 — Media Type Specifications and Registration Procedures
- Spring `MediaType.parseMediaType()` 문서: throws `InvalidMediaTypeException`
- Defense in depth — 입력 검증 + 출력 안전 처리 두 층
