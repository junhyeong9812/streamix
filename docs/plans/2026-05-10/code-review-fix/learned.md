# 학습 기록: 2026-05-10 code-review-fix

> 24개 이슈(P0 5 / P1 7 / P2 12) 일괄 수정 작업의 학습 기록입니다.
> 변경된 모든 파일을 다시 열어 읽고 확인한 내용을 기반으로 작성했습니다.

---

## 1. 사용된 라이브러리

| 라이브러리 | 버전 | 모듈 | 용도 | 비고 |
|-----------|------|------|------|------|
| Spring Boot | 4.0.0 | starter | 자동 설정 | 4.0에서 `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration` 등 패키지 재구성 |
| Spring Boot Hibernate | 4.0.x | starter (transitive) | `HibernateJpaAutoConfiguration` 새 위치 | `spring-boot-hibernate` 별도 모듈로 분리됨 |
| Spring Data JPA | 4.0.0 | starter | `@Modifying` 어노테이션 | `clearAutomatically=true`로 영속성 컨텍스트 자동 clear |
| Java | 25 | 전체 | `Executors.newVirtualThreadPerTaskExecutor()` | Virtual Threads로 ffmpeg stdout/stderr 동시 drain |
| WebJars Bootstrap | 5.3.2 | starter | CDN 의존 제거 | `org.webjars:bootstrap` |
| WebJars Bootstrap Icons | 1.11.1 | starter | CDN 의존 제거 | `org.webjars.npm:bootstrap-icons` |
| WebJars Locator Lite | 1.0.1 | starter | URL에서 버전 생략 가능 | Spring Boot 3+ 권장 |
| AssertJ | 3.26.3 | test | assertion | `assertThatThrownBy`, `isInstanceOf`, `hasMessageContaining` |
| Mockito | 5.14.2 | test | mock | strict mode에서 `anyString()` matcher와 stub 미적용 이슈 발견 → 정확 인자 사용 |

## 2. 핵심 함수/메서드

### 2.1 `ByteSizeFormatter.format(long)` (신규)

```java
public static String format(long bytes) {
  if (bytes < 0) return "0 B";
  if (bytes < KB) return bytes + " B";
  if (bytes < MB) return String.format("%.1f KB", bytes / (double) KB);
  if (bytes < GB) return String.format("%.1f MB", bytes / (double) MB);
  if (bytes < TB) return String.format("%.1f GB", bytes / (double) GB);
  return String.format("%.1f TB", bytes / (double) TB);
}
```

**역할**: 바이트 → "1.5 GB" 등 사람이 읽기 쉬운 문자열 변환. 6곳에 중복되어 있던 로직을 통합. 정밀도 1자리로 통일 (이전엔 GB가 2자리였음).

### 2.2 `RangeNotSatisfiableException` (신규)

```java
public final class RangeNotSatisfiableException extends StreamixException {
  private final long fileSize;
  public RangeNotSatisfiableException(long fileSize) {
    super("Requested range not satisfiable for file size " + fileSize);
    this.fileSize = fileSize;
  }
  public long getFileSize() { return fileSize; }
}
```

**역할**: RFC 7233 §4.4 - 416 Range Not Satisfiable 응답에 대응하는 도메인 예외. `StreamixException`의 sealed permit에 추가되어 switch exhaustive 보장.

### 2.3 `FileStreamService.parseRange()` (개정)

```java
private Range parseRange(String rangeHeader, long fileSize) {
  if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
    return new Range(0, fileSize - 1);
  }
  String rangeValue = rangeHeader.substring(6).trim();
  int dashIdx = rangeValue.indexOf('-');
  if (dashIdx < 0) {
    throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
  }
  String startStr = rangeValue.substring(0, dashIdx).trim();
  String endStr = rangeValue.substring(dashIdx + 1).trim();
  long start, end;
  try {
    if (startStr.isEmpty()) {
      if (endStr.isEmpty()) throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
      long suffixLen = Long.parseLong(endStr);
      if (suffixLen <= 0) throw new IllegalArgumentException("Invalid suffix length: " + rangeHeader);
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

**역할**: HTTP Range 헤더를 RFC 7233에 맞게 파싱. 잘못된 형식은 IllegalArgumentException(400), 범위 초과는 RangeNotSatisfiableException(416). multi-range는 stream() 진입 시점에 차단됨.

### 2.4 `FFmpegThumbnailAdapter.drainStream(InputStream)` (신규 헬퍼)

```java
private static byte[] drainStream(InputStream in) throws IOException {
  try (in; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    in.transferTo(baos);
    return baos.toByteArray();
  }
}
```

**역할**: Virtual Thread Executor에서 호출되어 stdout/stderr를 동시에 drain. pipe deadlock 방지 핵심.

### 2.5 `LocalFileStorageAdapter.resolveAndValidatePath(String)` (확장)

```java
private Path resolveAndValidatePath(String path) {
  if (path == null || path.isBlank()) {
    throw new IllegalArgumentException("Invalid storage path");
  }
  Path resolved = isAbsolutePath(path)
      ? Path.of(path).normalize()
      : basePath.resolve(path).normalize();
  if (!resolved.startsWith(basePath)) {
    log.warn("Rejected path outside base directory: {}", path);
    throw new IllegalArgumentException("Invalid storage path");
  }
  return resolved;
}

private static boolean isAbsolutePath(String path) {
  if (path.startsWith("/")) return true;
  return path.length() >= 3
      && Character.isLetter(path.charAt(0))
      && path.charAt(1) == ':'
      && (path.charAt(2) == '/' || path.charAt(2) == '\\');
}
```

**역할**: `save()`에만 적용되던 검증을 `load`/`loadPartial`/`delete`/`exists`/`getSize` 모두에 확장. Path Traversal + 메타데이터 변조를 통한 임의 파일 접근 방어.

### 2.6 `FileTypeDetector.safeContentType(String, String)` (신규)

```java
public String safeContentType(String contentType, String fileName) {
  if (isValidContentType(contentType)) return contentType.trim();
  String ext = extractExtension(fileName);
  return getContentType(ext);
}

private static final Pattern MIME_TYPE_PATTERN =
    Pattern.compile("^[\\w.+-]+/[\\w.+-]+(;.*)?$");

public boolean isValidContentType(String contentType) {
  return contentType != null
      && !contentType.isBlank()
      && MIME_TYPE_PATTERN.matcher(contentType.trim()).matches();
}
```

**역할**: 사용자 입력 contentType이 RFC 6838 형식이 아니면 확장자 기반 fallback. Spring `MediaType.parseMediaType`이 잘못된 형식에서 InvalidMediaTypeException → 500 던지는 것을 방어.

### 2.7 `StreamixApiController.safeMediaType(String)` (신규 private)

```java
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

**역할**: 출력 단계에서도 잘못된 contentType 방어 (defense in depth).

### 2.8 `FileDeleteService.deleteIdempotent(UUID)` (신규)

```java
@Override
public boolean deleteIdempotent(UUID fileId) {
  log.info("Deleting file (idempotent): {}", fileId);
  java.util.Optional<FileMetadata> opt = metadataRepository.findById(fileId);
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

**역할**: REST DELETE 멱등성(RFC 7231 §4.2.2)을 위한 새 메서드. `delete()`는 deleteIdempotent를 호출 후 false면 예외. 대시보드는 멱등 모드 사용.

## 3. 어노테이션/데코레이터

### 3.1 Spring 자동 설정

| 어노테이션 | 역할 | 본 작업에서 적용 위치 |
|-----------|------|-------------------|
| `@Configuration(proxyBeanMethods = false)` | starter Bean 등록의 표준 — CGLIB proxy 회피 | 6개 *Configuration 클래스 |
| `@AutoConfigureAfter({...})` | Bean 등록 순서 hint | 5개 *Configuration (의존성 그래프) |
| `@EnableConfigurationProperties` | `@ConfigurationProperties` Bean 활성화 | StreamixAutoConfiguration, StreamixRepositoryConfiguration |
| `@ConditionalOnMissingBean` | 사용자가 직접 정의하면 자동 Bean 생략 | 모든 Bean 메서드 |
| `@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnProperty` / `@ConditionalOnWebApplication` | 조건부 활성화 | 각 Configuration의 진입 조건 |

### 3.2 Spring Data JPA

| 어노테이션 | 역할 |
|-----------|------|
| `@Modifying(clearAutomatically = true)` | DML 쿼리 (DELETE/UPDATE)에 필수 + 영속성 컨텍스트 자동 clear |
| `@Query("DELETE ...")` | 명시적 JPQL 쿼리 |
| `@Param` | named parameter binding |

### 3.3 Spring MVC

| 어노테이션 | 역할 |
|-----------|------|
| `@RestControllerAdvice(basePackages = ...)` | API 패키지에만 ExceptionHandler 적용 (사용자 advice와 격리) |
| `@ExceptionHandler(...)` | 특정 예외 처리 |
| `@Controller` / `@RestController` | MVC 컨트롤러 |
| `@GetMapping` / `@PostMapping` / `@DeleteMapping` | HTTP 메서드 매핑 |

## 4. 수정 전/후 코드 비교 (대표)

### 4.1 FileType.isPreviewable() 삭제 + FileMetadata 정확 구현 (P1-10)

**Before (FileType.java)** — 항상 true 버그
```java
public boolean isPreviewable() {
  return this == IMAGE || this == VIDEO || this == AUDIO ||
      (this == DOCUMENT && extensions.contains("pdf"));   // DOCUMENT.extensions에 "pdf" 항상 포함 → 모든 DOCUMENT가 true
}
```

**After (FileMetadata.java)** — contentType 기반 정확 판별
```java
public boolean isPreviewable() {
  if (type == FileType.IMAGE || type == FileType.VIDEO || type == FileType.AUDIO) {
    return true;
  }
  return type == FileType.DOCUMENT && "application/pdf".equalsIgnoreCase(contentType);
}
```

**변경 이유**: enum은 instance 단위 정보(contentType)를 모르므로 PDF 판별 불가. `FileMetadata`로 책임 이전.

### 4.2 FFmpegThumbnailAdapter — stdout/stderr 동시 drain (P1-09)

**Before** — pipe deadlock 위험
```java
processBuilder.redirectErrorStream(false);
Process process = processBuilder.start();
byte[] thumbnailData;
try (InputStream stdout = process.getInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
  // stdout만 읽음 → stderr 버퍼 가득 차면 ffmpeg block → deadlock
  byte[] buffer = new byte[8192];
  int bytesRead;
  while ((bytesRead = stdout.read(buffer)) != -1) {
    baos.write(buffer, 0, bytesRead);
  }
  thumbnailData = baos.toByteArray();
}
boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
```

**After** — Virtual Threads로 동시 drain
```java
processBuilder.redirectErrorStream(false);
Process process = null;
try {
  process = processBuilder.start();
  try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Process p = process;
    Future<byte[]> stdoutFuture = executor.submit(() -> drainStream(p.getInputStream()));
    Future<byte[]> stderrFuture = executor.submit(() -> drainStream(p.getErrorStream()));
    boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
      throw new ThumbnailGenerationException("FFmpeg process timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
    }
    byte[] thumbnailData = stdoutFuture.get(5, TimeUnit.SECONDS);
    byte[] errorBytes = stderrFuture.get(5, TimeUnit.SECONDS);
    // ... exit code 검증
  }
}
```

### 4.3 dashboard.js showToast — XSS 방지 (P1-07)

**Before** — innerHTML로 XSS
```javascript
toastEl.innerHTML =
    '<div class="d-flex">' +
    '<div class="toast-body">' +
    '<i class="bi ' + (type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle') + ' me-2"></i>' +
    message +     // 사용자 입력 → XSS
    '</div>' +
    '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
    '</div>';
```

**After** — DOM API + textContent
```javascript
var dFlex = document.createElement('div');
dFlex.className = 'd-flex';
var body = document.createElement('div');
body.className = 'toast-body';
var icon = document.createElement('i');
icon.className = 'bi me-2 ' + (isError ? 'bi-exclamation-circle' : 'bi-check-circle');
body.appendChild(icon);
// 사용자 입력은 textContent로 안전하게 처리 (XSS 방지)
body.appendChild(document.createTextNode(' ' + (message || '')));
```

### 4.4 build.gradle — 버전 동적화 (P0-05)

**Before**
```groovy
allprojects {
    group = 'io.github.junhyeong9812'
    version = '2.0.0'
    repositories { mavenCentral() }
}
```

**After**
```groovy
allprojects {
    group = 'io.github.junhyeong9812'
    // CI/CD에서 -PreleaseVersion=x.y.z 또는 RELEASE_VERSION 환경변수로 주입
    version = providers.gradleProperty('releaseVersion')
        .orElse(providers.environmentVariable('RELEASE_VERSION'))
        .getOrElse('2.0.1-SNAPSHOT')
    repositories { mavenCentral() }
}
```

## 5. 동작 구조 (변경된 흐름)

### 5.1 자동 설정 의존성 그래프 (P0-03 + P2-18 적용 후)

```
DataSourceAutoConfiguration (boot)
   ↓
HibernateJpaAutoConfiguration (boot)
   ↓
StreamixRepositoryConfiguration ─────┐
   ↓                                  ↓
StreamixMonitoringConfiguration   StreamixThumbnailConfiguration
   ↓                                  ↓
StreamixDashboardConfiguration    StreamixAutoConfiguration
                                      ↓
                                  StreamixWebConfiguration
```

모두 `@Configuration(proxyBeanMethods = false)` + `@AutoConfigureAfter` 어노테이션으로 명시적 순서.

### 5.2 파일 업로드 흐름 (P1-12 + P2-22 적용 후)

```
StreamixApiController.upload(MultipartFile)
   ↓
UploadCommand 생성
   ↓
FileUploadService.upload(command)
   ↓
1. fileTypeDetector.safeContentType()  ← 잘못된 형식 fallback
   ↓
2. fileTypeDetector.detect()
   ↓
3. validateFileSize() / validateFileType()
   ↓
4. UUID 기반 파일명 생성
   ↓
5. storage.save(filename, inputStream, size)
   ↓
6. metadata 생성 (safeContentType 저장)
   ↓
7. thumbnailService.generate() 시도
   ├─ ThumbnailGenerationException / StorageException → 무시 (silent fail)
   └─ 다른 RuntimeException → 위로 전파 (버그 표면화)
   ↓
8. metadataRepository.save(metadata)
   ↓
UploadResult 반환
```

### 5.3 Range 스트리밍 흐름 (P1-11 적용 후)

```
StreamixApiController.streamFile(id, range)
   ↓
StreamCommand.withRange(id, range)
   ↓
FileStreamService.stream(command)
   ↓
findMetadataOrThrow()
   ↓
isMultiRange(range)? ── YES → streamFull(metadata)
   ↓ NO
hasRange()? ── NO → streamFull(metadata)
   ↓ YES
streamPartial(metadata, rangeHeader)
   ↓
parseRange(rangeHeader, fileSize)
   ├─ 빈/bytes 외 → (0, fileSize-1) 전체
   ├─ 잘못된 형식 → IllegalArgumentException → 400
   └─ start ≥ fileSize → RangeNotSatisfiableException → 416
   ↓
storage.loadPartial(path, start, end)
   ↓
StreamableFile.partial(metadata, stream, start, end)
   ↓
Controller — safeMediaType(contentType) + Content-Range 헤더 + 206 Partial Content
```

### 5.4 FFmpeg 썸네일 생성 흐름 (P1-09 적용 후)

```
ThumbnailService.generate(VIDEO, path, w, h)
   ↓
FFmpegThumbnailAdapter.generateFromPath(path, w, h)
   ↓
ProcessBuilder(ffmpeg ... ) start
   ↓
Executors.newVirtualThreadPerTaskExecutor() ─── try-with-resources
   ├─ Future<byte[]> stdoutFuture = drainStream(stdout)  ← 가상 스레드 1
   └─ Future<byte[]> stderrFuture = drainStream(stderr)  ← 가상 스레드 2
   ↓
process.waitFor(30s)
   ├─ timeout → destroyForcibly + ThumbnailGenerationException
   └─ ok →
       stdoutFuture.get(5s) → thumbnailData
       stderrFuture.get(5s) → errorBytes
       exitCode 검증
       byte[] 반환
```

## 6. 디자인 패턴

| 패턴 | 적용 위치 | 본 작업에서 강화된 점 |
|------|----------|--------------------|
| Hexagonal (Ports & Adapters) | Core ↔ Adapter 전체 | port 인터페이스에 `deleteIdempotent` 추가 |
| Template Method | StreamixException sealed class | `RangeNotSatisfiableException`을 sealed permit에 추가 |
| Factory Method | `RangeNotSatisfiableException(fileSize)` | static factory 대신 직접 생성자 |
| Defense in Depth | safeContentType (입력) + safeMediaType (출력) | P1-12에서 양쪽 모두 방어 |
| Idempotent API | `deleteIdempotent` | RFC 7231 멱등성 |
| Utility Class | `ByteSizeFormatter` (static + private constructor + AssertionError) | Effective Java Item 4 패턴 |

## 7. 설정/컨벤션

### 7.1 Spring Boot 4 패키지 변경 (학습)

Spring Boot 3.x → 4.0에서 자동 설정 클래스들이 모듈 분리 + 패키지 재구성됨:

| 클래스 | 3.x | 4.0 |
|--------|-----|-----|
| `DataSourceAutoConfiguration` | `org.springframework.boot.autoconfigure.jdbc` | `org.springframework.boot.jdbc.autoconfigure` |
| `HibernateJpaAutoConfiguration` | `org.springframework.boot.autoconfigure.orm.jpa` | `org.springframework.boot.hibernate.autoconfigure` (별도 `spring-boot-hibernate` 모듈) |
| `JpaBaseConfiguration` | 동일 위치 | `org.springframework.boot.jpa.autoconfigure` (`spring-boot-jpa`) |

→ 본 작업에서 `StreamixRepositoryConfiguration`의 `@AutoConfigureAfter` import 정정.

### 7.2 starter 라이브러리 모범 사례

| 규칙 | 본 작업에서 적용 |
|------|----------------|
| starter는 `application.yml` 포함 금지 | `streamix-spring-boot-starter/src/main/resources/application.yml` 삭제 (P1-08) |
| 기본값은 `@ConfigurationProperties` compact constructor에 | StreamixProperties는 이미 적용 |
| Bean 등록 순서는 `@AutoConfigureAfter`로 명시 | P2-18 |
| `@Configuration(proxyBeanMethods = false)` | starter Bean에 표준 |
| 라이브러리 의존성은 webjars 등으로 자체 호스팅 | P2-24 |

### 7.3 멀티모듈 Gradle — 동적 버전

```groovy
version = providers.gradleProperty('releaseVersion')
    .orElse(providers.environmentVariable('RELEASE_VERSION'))
    .getOrElse('2.0.1-SNAPSHOT')
```

- `providers.gradleProperty()` → `-PreleaseVersion=x` CLI
- `providers.environmentVariable()` → `RELEASE_VERSION=x` env
- fallback chain으로 우선순위 지정

## 8. 테스트 관련 학습

### 8.1 사용된 테스트 라이브러리

| 라이브러리 | 용도 |
|-----------|------|
| JUnit 5 | `@Test`, `@Nested`, `@DisplayName`, `@BeforeEach`, `@ParameterizedTest`, `@ValueSource`, `@CsvSource` |
| AssertJ | `assertThat(...)`, `assertThatThrownBy(...)`, `isInstanceOf`, `hasMessageContaining`, `isEqualTo`, `isTrue/isFalse` |
| Mockito | `@Mock`, `@ExtendWith(MockitoExtension.class)`, `given(...).willReturn(...)`, `verify(...)`, `doThrow(...).when(...)` |
| Mockito BDDMockito | `given(...)` BDD 스타일 |
| MockitoSettings | `@MockitoSettings(strictness = Strictness.LENIENT)` (ThumbnailServiceTest) |
| ByteBuddy | Java 25 호환 (1.17.5) |
| ApplicationContextRunner | Spring Boot Configuration 단위 테스트 |
| TempDir | `@TempDir Path tempDir` — 임시 디렉토리 자동 cleanup |

### 8.2 Mockito strict mode 학습

`anyString()` matcher와 정확한 인자 매칭이 충돌 시 stub 미적용 → mock이 default null 반환 → NPE.

```java
// ❌ 다른 stub과 충돌 가능
given(storage.load(anyString())).willReturn(...);

// ✅ 정확한 인자
given(storage.load(metadata.storagePath())).willReturn(...);
```

### 8.3 Path 검증 강화 후 테스트 갱신 패턴

```java
// Before — 임의 절대 경로
adapter.load("/nonexistent/file.txt")  // → IllegalArgumentException (basePath 외부)

// After — basePath 내부의 비존재 경로
Path nonExistent = tempDir.resolve("nonexistent.txt");
adapter.load(nonExistent.toString())  // → FileNotFoundException
```

`@TempDir`이 basePath를 제공하므로 그 내부에서만 비존재 경로 생성.

### 8.4 신규 테스트 추가

| 테스트 | 케이스 |
|--------|--------|
| `ByteSizeFormatterTest` | bytes/KB/MB/GB/TB + 음수 + 인스턴스화 차단 |
| `FileMetadataTest.IsPreviewableTest` | IMAGE/VIDEO/AUDIO/PDF/non-PDF DOCUMENT/ARCHIVE/OTHER |
| `FileStreamServiceTest` 추가 | 잘못된 형식, 416, multi-range fallback, 빈 range |
| `LocalFileStorageAdapterTest` 추가 | load/loadPartial/delete/exists/getSize의 path traversal 방어 |
| `FileDeleteServiceTest.DeleteIdempotentTest` | 존재 X false / 존재 O true |
| `StreamixExceptionTest` | RangeNotSatisfiableException 검증 |

## 9. 새로 알게 된 것

1. **Spring Boot 4의 자동 설정 모듈 분리**: `spring-boot-autoconfigure`에서 jdbc/jpa/hibernate가 별도 jar로 분리됨. `*Configuration` 클래스의 패키지가 변경됨. 의존성 분석 시 직접 jar 안을 unzip해서 클래스 위치 확인하는 게 빠름.

2. **`@AutoConfiguration` vs `@Configuration`**: 전자는 `META-INF/spring/...AutoConfiguration.imports` 파일 등록 시점에서만 의미. 그 파일이 없으면 사실상 `@Configuration`처럼만 동작. 본 라이브러리는 `@EnableStreamix` 명시 활성화 패턴이라 `@Configuration(proxyBeanMethods = false)`가 더 적합.

3. **Java 25 Virtual Threads + try-with-resources**: `Executors.newVirtualThreadPerTaskExecutor()`도 `AutoCloseable`이라 try-with-resources로 자동 shutdown. close()는 모든 task 완료 대기.

4. **`@Modifying(clearAutomatically = true)`**: `@Query("DELETE/UPDATE")`에 필수. clearAutomatically로 영속성 컨텍스트 자동 clear → 같은 트랜잭션에서 stale entity 조회 방지.

5. **Path Traversal 방어 — Java NIO**: `Path.normalize()` 후 `startsWith(basePath)` 검사. `..`이 정리된 후 검사해야 우회 차단.

6. **Mockito strict mode와 anyString() matcher**: Mockito 5+의 strict mode는 stub 인자 모호성에 엄격. `anyString()`이 다른 정확한 stub과 충돌하면 미적용되어 NPE. 가능하면 정확한 인자 사용.

7. **starter 모듈에 `application.yml` 포함의 위험**: Spring Boot가 사용자 application.yml과 병합. 사용자가 의도하지 않은 default 강제 적용 → 라이브러리는 코드 default만 사용.

8. **WebJars Locator Lite**: Spring Boot 3+ 권장. URL에서 버전 생략 가능 (`/webjars/bootstrap/css/bootstrap.min.css` → 자동 5.3.2 해석). starter JAR 안에 webjar 포함 안 되지만 runtime classpath에 transitively 추가됨.

9. **RFC 7233 Range 응답 패턴**:
   - 200 OK = 전체
   - 206 Partial Content = 부분
   - 416 Range Not Satisfiable = 범위 초과 + `Content-Range: bytes */{fileSize}` 헤더 (현재 크기 알림)

10. **Java Record + Thymeleaf SpEL**: record는 component accessor (`field()`)만 자동 생성. JavaBean 스타일 (`getField()`)은 Thymeleaf SpEL이 우선 찾는 방식. 따라서 `getXxx()` 명시 메서드 추가하면 `${obj.xxx}` 접근 가능.

## 10. 더 공부할 것

1. **Spring Boot 4의 모듈 재구성 전체 매핑** — 다른 자동 설정 클래스들도 패키지 변경됐을 가능성
2. **`@ControllerAdvice` + `@ModelAttribute`로 streamixProperties alias 대체** (v3 검토)
3. **Virtual Threads의 OS 리소스 제어** — Process 동시 실행 시 fd 한도, JVM heap 영향
4. **WebJars Locator Lite vs WebJars Locator** — 새로운 라이브러리, 정확한 trade-off
5. **Mockito 5+ strict mode 회피 패턴** — anyString matcher 사용 시점
6. **JaCoCo 커버리지 도입** — 테스트 커버리지 측정 자동화
7. **Spring Boot integration test (`@SpringBootTest`) — `@EnableStreamix` 통합 검증** — autoconfig 시점 검증
8. **Sonatype Central Publishing 정책** — `2.0.1-SNAPSHOT` 같은 SNAPSHOT 배포 검증

## 11. 작업 통계

- **이슈 수**: 24 (P0 5 / P1 7 / P2 12)
- **신규 파일**: 5 (ByteSizeFormatter, RangeNotSatisfiableException, ByteSizeFormatterTest, dashboard 디렉토리, LICENSE 본문)
- **삭제 파일**: 2 (StreamixJpaConfiguration, application.yml)
- **이동 파일**: 1 (StreamixDashboardController web → dashboard)
- **수정 파일**: 약 35 (Java/HTML/JS/Gradle/YAML)
- **테스트 추가/수정**: 6 파일
- **Phase 단계**: 4 (A 신규 유틸 → B P0 → C P1 → D P2)
- **빌드 검증 횟수**: 5회 (각 Phase + 최종)
- **최종 테스트 결과**: BUILD SUCCESSFUL (304+ tests)
- **결과 JAR**: `streamix-spring-boot-starter-2.0.1-SNAPSHOT.jar` (동적 버전 적용 확인)
