# [P1-11] FileStreamService.parseRange — RFC 7233 미준수 + NumberFormatException

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — RFC 미준수 / 500 에러** |
| 카테고리 | HTTP / 표준 준수 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `FileStreamService.java`, `StreamixApiController.java`, `GlobalExceptionHandler.java` |

## 문제 분석

### 현재 동작
```java
// FileStreamService.java:120-155
private Range parseRange(String rangeHeader, long fileSize) {
    if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
        return new Range(0, fileSize - 1);
    }

    String rangeValue = rangeHeader.substring(6).trim();
    String[] parts = rangeValue.split("-");

    long start;
    long end;

    if (rangeValue.startsWith("-")) {
        // bytes=-500
        long suffix = Long.parseLong(parts[1]);   // ⚠️ ArrayIndexOutOfBoundsException 가능
        start = Math.max(0, fileSize - suffix);
        end = fileSize - 1;
    } else if (parts.length == 1 || parts[1].isEmpty()) {
        // bytes=1024-
        start = Long.parseLong(parts[0]);          // ⚠️ NumberFormatException 가능
        end = fileSize - 1;
    } else {
        start = Long.parseLong(parts[0]);          // ⚠️ NumberFormatException 가능
        end = Long.parseLong(parts[1]);
    }

    start = Math.max(0, start);
    end = Math.min(end, fileSize - 1);

    if (start > end) {
        throw new IllegalArgumentException("Invalid range: " + rangeHeader);
    }

    return new Range(start, end);
}
```

### 발견된 결함

#### 결함 1: NumberFormatException 처리 누락
입력: `Range: bytes=abc-def`
→ `Long.parseLong("abc")` → `NumberFormatException`
→ `GlobalExceptionHandler`의 `handleGenericException`에 의해 500 Internal Server Error
→ **올바른 응답: 400 Bad Request**

#### 결함 2: ArrayIndexOutOfBoundsException
입력: `Range: bytes=-`
→ `rangeValue = "-"` → `split("-")` = `["", ""]`
→ `rangeValue.startsWith("-")` true → `parts[1]` = `""` → `Long.parseLong("")` → NumberFormatException
또는 `Range: bytes=`
→ `rangeValue = ""` → `split("-")` = `[]` (Java 8+ 동작) 또는 `[""]`
→ 분기 없음 → NPE 가능

#### 결함 3: HTTP 416 미반환 (RFC 7233 §4.4)
입력: `Range: bytes=99999-99999` (fileSize=1024)
- 현재: `start = 99999`, `end = min(99999, 1023) = 1023` → `start > end` → IllegalArgumentException → 400
- RFC 7233 권장: **416 Range Not Satisfiable + Content-Range: bytes \*/{fileSize}**

#### 결함 4: 빈 Range 헤더
입력: `Range: ` (값 없음) — 보통 클라이언트가 보내지 않지만 가능
- 현재: `rangeHeader.startsWith("bytes=")` false → 전체 파일 반환 (200 OK)
- RFC: bytes 단위가 아닌 Range는 무시 OK ✓

#### 결함 5: multi-range 미지원
입력: `Range: bytes=0-100,200-300`
- 현재: 파싱 실패 (NumberFormatException)
- RFC 7233 §4.1: multi-range는 multipart/byteranges로 응답
- 단순화 위해 미지원 OK이지만, 명시적으로 거부(416 또는 200 전체)해야 함

### 기대 동작 (RFC 7233 준수)
| 입력 | 응답 |
|------|------|
| `bytes=0-1023` | 206 Partial Content + 0~1023 byte |
| `bytes=1024-` | 206 Partial Content + 1024~끝 |
| `bytes=-500` | 206 Partial Content + 마지막 500바이트 |
| `bytes=99999-99999` (range > fileSize) | 416 Range Not Satisfiable |
| `bytes=abc-def` | 400 Bad Request 또는 200 (Range 무시) |
| `bytes=-` 또는 `bytes=` | 400 Bad Request 또는 200 (Range 무시) |
| `bytes=0-100,200-300` (multi-range) | 200 OK 전체 (단순화) |
| `Range: items=0-10` (bytes 외 단위) | 200 OK 전체 (RFC 7233 §3.1) |

### 원인 분석
- 행복한 경로(happy path)만 작성, 입력 검증 누락
- RFC 7233 416 응답 패턴을 알고 있지만 구현 안 함
- 컨트롤러/서비스 레이어 책임 분리 — 서비스는 IllegalArgumentException 던지고 컨트롤러가 어떻게 응답할지 결정해야 함

## 변경 프로세스

### Step 1: 새 예외 타입 — `RangeNotSatisfiableException`
RFC 7233 §4.4에 대응하는 의미적 예외:
```java
// streamix-core/.../domain/exception/RangeNotSatisfiableException.java
package io.github.junhyeong9812.streamix.core.domain.exception;

public final class RangeNotSatisfiableException extends StreamixException {
    private final long fileSize;
    public RangeNotSatisfiableException(long fileSize) {
        super("Requested range not satisfiable for file size " + fileSize);
        this.fileSize = fileSize;
    }
    public long getFileSize() { return fileSize; }
}
```

`StreamixException` sealed permit 추가:
```java
public sealed class StreamixException extends RuntimeException
    permits FileNotFoundException,
            InvalidFileTypeException,
            StorageException,
            ThumbnailGenerationException,
            FileSizeExceededException,
            RangeNotSatisfiableException { ... }
```

### Step 2: parseRange 재작성
```java
private Range parseRange(String rangeHeader, long fileSize) {
    if (rangeHeader == null || rangeHeader.isBlank()) {
        return new Range(0, fileSize - 1);
    }

    // RFC 7233: bytes 단위만 지원, 그 외는 헤더 무시 (전체 응답)
    if (!rangeHeader.startsWith("bytes=")) {
        log.debug("Unsupported range unit, returning full content: {}", rangeHeader);
        return new Range(0, fileSize - 1);
    }

    String rangeValue = rangeHeader.substring(6).trim();

    // multi-range 미지원 — 전체 응답으로 fallback
    if (rangeValue.contains(",")) {
        log.debug("Multi-range not supported, returning full content: {}", rangeHeader);
        return new Range(0, fileSize - 1);
    }

    int dashIdx = rangeValue.indexOf('-');
    if (dashIdx < 0) {
        throw new IllegalArgumentException("Invalid range header: missing '-': " + rangeHeader);
    }

    String startStr = rangeValue.substring(0, dashIdx).trim();
    String endStr = rangeValue.substring(dashIdx + 1).trim();

    long start;
    long end;

    try {
        if (startStr.isEmpty()) {
            // bytes=-N: 마지막 N 바이트
            if (endStr.isEmpty()) {
                throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
            }
            long suffixLen = Long.parseLong(endStr);
            if (suffixLen <= 0) {
                throw new IllegalArgumentException("Invalid suffix length: " + rangeHeader);
            }
            start = Math.max(0, fileSize - suffixLen);
            end = fileSize - 1;
        } else if (endStr.isEmpty()) {
            // bytes=N-
            start = Long.parseLong(startStr);
            end = fileSize - 1;
        } else {
            // bytes=N-M
            start = Long.parseLong(startStr);
            end = Long.parseLong(endStr);
        }
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid range numbers in: " + rangeHeader, e);
    }

    // RFC 7233 §4.4 — 416 Range Not Satisfiable
    if (start < 0 || start >= fileSize) {
        throw new RangeNotSatisfiableException(fileSize);
    }

    if (end >= fileSize) {
        end = fileSize - 1;
    }
    if (start > end) {
        throw new RangeNotSatisfiableException(fileSize);
    }

    return new Range(start, end);
}
```

### Step 3: GlobalExceptionHandler에 RangeNotSatisfiableException 핸들러
```java
@ExceptionHandler(RangeNotSatisfiableException.class)
public ResponseEntity<ErrorResponse> handleRangeNotSatisfiable(
    RangeNotSatisfiableException ex,
    HttpServletRequest request
) {
    log.warn("Range not satisfiable: {}", ex.getMessage());
    ErrorResponse response = ErrorResponse.of(
        416,
        "Range Not Satisfiable",
        "RANGE_NOT_SATISFIABLE",
        ex.getMessage(),
        request.getRequestURI()
    );
    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + ex.getFileSize())
        .body(response);
}
```

### Step 4: StreamixException sealed permit 갱신
```java
public sealed class StreamixException extends RuntimeException
    permits FileNotFoundException,
            InvalidFileTypeException,
            StorageException,
            ThumbnailGenerationException,
            FileSizeExceededException,
            RangeNotSatisfiableException { ... }
```

### Step 5: 테스트 추가
`FileStreamServiceTest`:
```java
@Test
@DisplayName("bytes=abc-def 잘못된 형식은 IllegalArgumentException")
void invalidRangeFormat() {
    StreamCommand cmd = StreamCommand.withRange(fileId, "bytes=abc-def");
    assertThatThrownBy(() -> service.stream(cmd))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
@DisplayName("Range가 파일 크기 초과 시 RangeNotSatisfiableException")
void rangeBeyondFileSize() {
    given(metadataRepository.findById(fileId))
        .willReturn(Optional.of(createMetadata(fileId, 100L)));
    StreamCommand cmd = StreamCommand.withRange(fileId, "bytes=200-300");
    assertThatThrownBy(() -> service.stream(cmd))
        .isInstanceOf(RangeNotSatisfiableException.class);
}

@Test
@DisplayName("multi-range는 전체 응답으로 fallback")
void multiRangeFallsBackToFull() {
    given(metadataRepository.findById(fileId))
        .willReturn(Optional.of(createMetadata(fileId, 1000L)));
    given(storage.load(anyString())).willReturn(new ByteArrayInputStream(new byte[1000]));
    StreamCommand cmd = StreamCommand.withRange(fileId, "bytes=0-100,200-300");
    StreamableFile result = service.stream(cmd);
    assertThat(result.isPartialContent()).isFalse();
}

@Test
@DisplayName("bytes=- 단독은 IllegalArgumentException")
void emptyRange() {
    given(metadataRepository.findById(fileId))
        .willReturn(Optional.of(createMetadata(fileId, 100L)));
    StreamCommand cmd = StreamCommand.withRange(fileId, "bytes=-");
    assertThatThrownBy(() -> service.stream(cmd))
        .isInstanceOf(IllegalArgumentException.class);
}
```

기존 `StreamixExceptionTest.allExceptionsExtendStreamixException` 갱신:
```java
assertThat(new RangeNotSatisfiableException(1024L))
    .isInstanceOf(StreamixException.class);
```

## Before / After

### Before — parseRange 핵심
```java
private Range parseRange(String rangeHeader, long fileSize) {
    if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
        return new Range(0, fileSize - 1);
    }
    String rangeValue = rangeHeader.substring(6).trim();
    String[] parts = rangeValue.split("-");
    long start;
    long end;
    if (rangeValue.startsWith("-")) {
        long suffix = Long.parseLong(parts[1]);
        start = Math.max(0, fileSize - suffix);
        end = fileSize - 1;
    } else if (parts.length == 1 || parts[1].isEmpty()) {
        start = Long.parseLong(parts[0]);
        end = fileSize - 1;
    } else {
        start = Long.parseLong(parts[0]);
        end = Long.parseLong(parts[1]);
    }
    start = Math.max(0, start);
    end = Math.min(end, fileSize - 1);
    if (start > end) {
        throw new IllegalArgumentException("Invalid range: " + rangeHeader);
    }
    return new Range(start, end);
}
```

### After — parseRange 핵심
```java
private Range parseRange(String rangeHeader, long fileSize) {
    if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
        return new Range(0, fileSize - 1);
    }
    String rangeValue = rangeHeader.substring(6).trim();
    if (rangeValue.contains(",")) {
        log.debug("Multi-range not supported, full content fallback: {}", rangeHeader);
        return new Range(0, fileSize - 1);
    }
    int dashIdx = rangeValue.indexOf('-');
    if (dashIdx < 0) {
        throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
    }
    String startStr = rangeValue.substring(0, dashIdx).trim();
    String endStr = rangeValue.substring(dashIdx + 1).trim();
    long start, end;
    try {
        if (startStr.isEmpty()) {
            if (endStr.isEmpty()) throw new IllegalArgumentException("Invalid range: " + rangeHeader);
            long suffixLen = Long.parseLong(endStr);
            if (suffixLen <= 0) throw new IllegalArgumentException("Invalid suffix: " + rangeHeader);
            start = Math.max(0, fileSize - suffixLen);
            end = fileSize - 1;
        } else if (endStr.isEmpty()) {
            start = Long.parseLong(startStr);
            end = fileSize - 1;
        } else {
            start = Long.parseLong(startStr);
            end = Long.parseLong(endStr);
        }
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid range numbers in: " + rangeHeader, e);
    }
    if (start < 0 || start >= fileSize) {
        throw new RangeNotSatisfiableException(fileSize);
    }
    if (end >= fileSize) end = fileSize - 1;
    if (start > end) throw new RangeNotSatisfiableException(fileSize);
    return new Range(start, end);
}
```

## 검증 방법

### 1. 단위 테스트
```bash
./gradlew :streamix-core:test --tests FileStreamServiceTest
./gradlew :streamix-core:test --tests StreamixExceptionTest
```

### 2. 수동 HTTP 검증
```bash
# 정상 Range
curl -i -H "Range: bytes=0-1023" http://localhost:8080/api/streamix/files/{id}/stream
# 기대: 206 Partial Content + Content-Range

# 잘못된 형식
curl -i -H "Range: bytes=abc-def" http://localhost:8080/api/streamix/files/{id}/stream
# 기대: 400 Bad Request

# 파일 크기 초과
curl -i -H "Range: bytes=99999999-99999999" http://localhost:8080/api/streamix/files/{id}/stream
# 기대: 416 Range Not Satisfiable + Content-Range: bytes */fileSize

# multi-range
curl -i -H "Range: bytes=0-100,200-300" http://localhost:8080/api/streamix/files/{id}/stream
# 기대: 200 OK 전체
```

### 3. 회귀 테스트
```bash
./gradlew :streamix-core:test
```

## 관련 파일
- `streamix-core/src/main/java/.../core/domain/exception/StreamixException.java` (sealed permit 추가)
- `streamix-core/src/main/java/.../core/domain/exception/RangeNotSatisfiableException.java` (신규)
- `streamix-core/src/main/java/.../core/application/service/FileStreamService.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/GlobalExceptionHandler.java`
- `streamix-core/src/test/java/.../core/application/service/FileStreamServiceTest.java`
- `streamix-core/src/test/java/.../core/domain/exception/StreamixExceptionTest.java`

## 참고
- [RFC 7233 — HTTP/1.1 Range Requests](https://datatracker.ietf.org/doc/html/rfc7233)
  - §2.1 Byte Ranges
  - §4.1 200 OK / 206 Partial Content
  - §4.4 416 Range Not Satisfiable
- HTTP 416 + `Content-Range: bytes */{total}` 헤더가 클라이언트(특히 progressive HTML5 video player)에 정확한 정보 제공
