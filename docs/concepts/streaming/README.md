# HTTP 스트리밍

파일 스트리밍 구현에 필요한 HTTP 관련 개념들입니다.

---

## 1. HTTP Range 요청

### 개념

큰 파일을 한 번에 전송하지 않고, 특정 범위만 요청하여 받을 수 있는 HTTP 기능입니다.
동영상 재생 시 특정 위치로 이동(Seek)할 때 필수적입니다.

### 요청 헤더

```http
GET /video.mp4 HTTP/1.1
Range: bytes=0-1023
```

- `bytes=0-1023`: 0번째 바이트부터 1023번째 바이트까지 요청
- `bytes=1024-`: 1024번째 바이트부터 끝까지 요청
- `bytes=-500`: 마지막 500바이트 요청

### 응답

**일부 콘텐츠 (206 Partial Content)**
```http
HTTP/1.1 206 Partial Content
Content-Range: bytes 0-1023/15728640
Content-Length: 1024
Accept-Ranges: bytes
```

**전체 콘텐츠 (200 OK)**
```http
HTTP/1.1 200 OK
Content-Length: 15728640
Accept-Ranges: bytes
```

### Java 구현 예시

```java
public ResponseEntity<StreamingResponseBody> stream(
        @PathVariable UUID id,
        @RequestHeader(value = "Range", required = false) String rangeHeader) {
    
    FileMetadata metadata = findById(id);
    long fileSize = metadata.getSize();
    
    // Range 헤더 파싱
    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
        String[] ranges = rangeHeader.substring(6).split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty() 
            ? Long.parseLong(ranges[1]) 
            : fileSize - 1;
        
        long contentLength = end - start + 1;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Range", String.format("bytes %d-%d/%d", start, end, fileSize));
        headers.set("Accept-Ranges", "bytes");
        headers.setContentLength(contentLength);
        
        return ResponseEntity
            .status(HttpStatus.PARTIAL_CONTENT)
            .headers(headers)
            .body(outputStream -> {
                // start ~ end 범위만 전송
            });
    }
    
    // 전체 파일 전송
    return ResponseEntity.ok()
        .contentLength(fileSize)
        .body(outputStream -> {
            // 전체 파일 전송
        });
}
```

---

## 2. StreamingResponseBody

### 개념

Spring에서 제공하는 인터페이스로, 응답을 스트리밍 방식으로 전송할 때 사용합니다.
비동기로 OutputStream에 직접 쓰기 때문에 메모리 효율적입니다.

### 일반 방식 vs 스트리밍 방식

**일반 방식 (메모리에 전체 로드)**
```java
@GetMapping("/file/{id}")
public byte[] getFile(@PathVariable UUID id) {
    // 파일 전체를 메모리에 로드 - 큰 파일에서 OOM 발생 가능
    return Files.readAllBytes(path);
}
```

**스트리밍 방식 (청크 단위 전송)**
```java
@GetMapping("/file/{id}")
public StreamingResponseBody getFile(@PathVariable UUID id) {
    return outputStream -> {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; // 8KB 버퍼
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        }
    };
}
```

### ResponseEntity와 함께 사용

```java
@GetMapping("/stream/{id}")
public ResponseEntity<StreamingResponseBody> stream(@PathVariable UUID id) {
    FileMetadata metadata = findById(id);
    
    StreamingResponseBody body = outputStream -> {
        try (InputStream is = storageService.load(metadata.getStoragePath())) {
            is.transferTo(outputStream); // Java 9+
        }
    };
    
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.getContentType()))
        .contentLength(metadata.getSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, 
                "inline; filename=\"" + metadata.getOriginalName() + "\"")
        .body(body);
}
```

---

## 3. Content-Type과 Content-Disposition

### Content-Type

파일의 MIME 타입을 지정합니다.

| 확장자 | Content-Type |
|--------|--------------|
| .mp4 | video/mp4 |
| .webm | video/webm |
| .avi | video/x-msvideo |
| .jpg | image/jpeg |
| .png | image/png |
| .gif | image/gif |

### Content-Disposition

파일을 브라우저에서 어떻게 처리할지 지정합니다.

```http
# 브라우저에서 직접 재생/표시
Content-Disposition: inline; filename="video.mp4"

# 다운로드 유도
Content-Disposition: attachment; filename="video.mp4"
```

### 한글 파일명 처리

```java
String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
    .replaceAll("\\+", "%20");

headers.set(HttpHeaders.CONTENT_DISPOSITION, 
    "inline; filename*=UTF-8''" + encodedFilename);
```

---

## 4. 버퍼 크기와 성능

### 버퍼 크기 선택

| 버퍼 크기 | 특징 |
|-----------|------|
| 4KB | 작은 파일에 적합, 메모리 효율적 |
| 8KB | 일반적인 선택 |
| 64KB | 큰 파일 전송 시 I/O 횟수 감소 |
| 1MB | 고속 네트워크에서 대용량 파일 |

### 구현 예시

```java
public class StreamingConfig {
    private int bufferSize = 8192;        // 8KB
    private int chunkSize = 1048576;      // 1MB
}

// 스트리밍
byte[] buffer = new byte[config.getBufferSize()];
int bytesRead;
long totalSent = 0;

while ((bytesRead = inputStream.read(buffer)) != -1 && totalSent < chunkSize) {
    outputStream.write(buffer, 0, bytesRead);
    totalSent += bytesRead;
}
```

---

## 5. 참고 자료

- [RFC 7233 - HTTP Range Requests](https://tools.ietf.org/html/rfc7233)
- [MDN - Range Requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests)
- [Spring StreamingResponseBody](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/StreamingResponseBody.html)