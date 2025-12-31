# Streamix Spring Boot 4.0 Migration & Bug Fix History

Streamix 라이브러리의 Spring Boot 4.0 호환성 작업 및 버그 수정 이력을 정리한 문서입니다.

---

## 📋 버전 변경 이력

| 버전 | 주요 변경 | 관련 파일 |
|------|----------|-----------|
| 1.0.0 → 1.0.1 | Javadoc 경고 수정, 테스트 실패 해결 | 전체 |
| 1.0.1 → 1.0.2 | ThumbnailService Bean 충돌 해결 | `StreamixThumbnailConfiguration`, `StreamixDashboardConfiguration` |
| 1.0.2 → 1.0.3 | Spring Boot 4.0 `@RequestParam`/`@PathVariable` name 필수 | Controller 전체, `StreamixRepositoryConfiguration` |
| 1.0.3 → 1.0.4 | `FileMetadataPort` Bean 생성 실패 | `StreamixRepositoryConfiguration`, `EnableStreamix` |
| 1.0.4 → 1.0.5 | `streamixProperties` Bean 미등록 | `StreamixRepositoryConfiguration` |
| 1.0.5 → 1.0.6 | Bean 충돌 + `avgDurationFormatted` 누락 | `StreamixRepositoryConfiguration`, `StreamingMonitoringService` |
| 1.0.6 → 1.0.7 | Phase 1: FileType 6개 확장, FileSizeExceededException | `FileType`, `FileMetadata`, Core 전체 |
| 1.0.7 → 2.0.0 | Phase 2: 서비스 SRP 분리, 파일 검증, 대시보드 확장 | Core 서비스, Starter, 템플릿 전체 |

---

## 🔧 상세 변경 내역

### 1.0.0 → 1.0.1: Javadoc 경고 및 테스트 수정

**문제:**
- Javadoc 빌드 시 경고 발생
- 일부 테스트 실패

**해결:**
- Javadoc 주석 형식 수정
- 테스트 코드 수정

---

### 1.0.1 → 1.0.2: ThumbnailService Bean 충돌

**에러:**
```
Parameter 0 of method thumbnailService in StreamixThumbnailConfiguration 
required a single bean, but 2 were found:
- imageThumbnailGenerator
- videoThumbnailGenerator
```

**원인:**
`ThumbnailGenerator` 타입의 Bean이 2개 존재하여 단일 주입 실패

**해결:**
```java
// Before
public ThumbnailService thumbnailService(ThumbnailGenerator generator) { ... }

// After - List 주입 방식
public ThumbnailService thumbnailService(List<ThumbnailGenerator> generators) {
    return new CompositeThumbnailService(generators);
}
```

**추가 작업:**
- `StreamixDashboardConfiguration` 분리 생성

---

### 1.0.2 → 1.0.3: Spring Boot 4.0 Parameter Name 필수

**에러:**
```
Name for argument of type [java.lang.String] not specified, 
and parameter name information not available via reflection.
```

**원인:**
Spring Boot 4.0 (Spring Framework 7.0)부터 `-parameters` 컴파일 옵션 없이는 파라미터 이름을 추론하지 않음

**해결:**
모든 Controller의 `@RequestParam`, `@PathVariable`에 `name` 속성 명시

```java
// Before
@GetMapping("/files/{id}")
public FileResponse getFile(@PathVariable UUID id) { ... }

// After
@GetMapping("/files/{id}")
public FileResponse getFile(@PathVariable(name = "id") UUID id) { ... }
```

**수정 대상 Controller:**
- `StreamixFileController`
- `StreamixStreamController`
- `StreamixDashboardController`

**추가 작업:**
- `StreamixRepositoryConfiguration` 생성 (JPA Repository 스캔 문제 해결)
- `@EntityScan`, `@EnableJpaRepositories` 추가

---

### 1.0.3 → 1.0.4: FileMetadataPort Bean 생성 실패

**에러:**
```
No qualifying bean of type 'FileMetadataPort' available
```

**원인:**
`@ConditionalOnBean(DataSource.class)` 조건 때문에 `@Import` 시점에 DataSource가 준비되지 않아 Bean 미생성

**해결:**
```java
// Before
@Configuration
@ConditionalOnBean(DataSource.class)  // ← 문제
public class StreamixRepositoryConfiguration { ... }

// After
@Configuration
// @ConditionalOnBean 제거
public class StreamixRepositoryConfiguration { ... }
```

**추가 수정:**
- `EnableStreamix.java`에서 `StreamixJpaConfiguration.class` Import 제거 (중복 방지)

---

### 1.0.4 → 1.0.5: streamixProperties Bean 미등록

**에러:**
```
No bean named 'streamixProperties' available
Exception evaluating SpringEL expression: "@streamixProperties.api.basePath"
```

**원인:**
Thymeleaf 템플릿에서 `@streamixProperties`로 Bean에 접근하는데, `@EnableConfigurationProperties`로 등록된 Bean은 내부 명명 규칙을 따라 이름이 다름

**해결:**
```java
@Configuration
@EnableConfigurationProperties(StreamixProperties.class)
public class StreamixRepositoryConfiguration {
    // Properties를 등록하긴 하지만 Bean 이름이 복잡함
}
```

---

### 1.0.5 → 1.0.6: Bean 충돌 + 메서드 누락

#### 문제 1: StreamixProperties Bean 충돌

**에러 (IDE 경고):**
```
자동 주입을 할 수 없습니다. 'StreamixProperties' 타입의 bean이 두 개 이상 있습니다.
Beans:
- streamix-io.github.junhyeong9812.streamix.starter.properties.StreamixProperties
- streamixProperties
```

**원인:**
- `@EnableConfigurationProperties`가 자동으로 Bean 등록 (내부 명명 규칙)
- alias Bean도 등록하려 했으나 `@ConditionalOnMissingBean`이 있어서 문제

**해결:**
```java
// @ConditionalOnMissingBean 제거, @Primary 추가
@Bean("streamixProperties")
@Primary
public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
    return properties;
}
```

#### 문제 2: avgDurationFormatted 메서드 누락

**에러:**
```
Property or field 'avgDurationFormatted' cannot be found on object of type 
'StreamingMonitoringService$DashboardStats'
```

**원인:**
Thymeleaf 템플릿에서 사용하는 `avgDurationFormatted` 메서드가 `DashboardStats` record에 없음

**해결:**
`StreamingMonitoringService.java`의 `DashboardStats` record에 메서드 추가:

```java
public record DashboardStats(...) {
    
    // 기존 메서드
    public String todayBytesFormatted() { ... }
    public String totalBytesFormatted() { ... }
    
    // 추가된 메서드
    public String monthBytesFormatted() {
        return formatBytes(monthBytes);
    }
    
    public String avgDurationFormatted() {
        return formatDuration(avgDurationMs);
    }
    
    private String formatDuration(double durationMs) {
        if (durationMs <= 0) return "0초";
        
        long totalSeconds = (long) (durationMs / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d시간 %d분 %d초", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else {
            return String.format("%d초", seconds);
        }
    }
}
```

---

### 1.0.6 → 1.0.7: Phase 1 - FileType 확장

**변경 사항:**

#### 1. FileType 확장 (2개 → 6개)

```java
// Before
public enum FileType {
    IMAGE, VIDEO
}

// After
public enum FileType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER
}
```

#### 2. FileMetadata 메서드 추가

```java
public record FileMetadata(...) {
    // 신규 메서드
    public boolean isStreamable() { ... }
    public boolean isPreviewable() { ... }
    public boolean isDownloadOnly() { ... }
}
```

#### 3. FileSizeExceededException 신규 예외

```java
public class FileSizeExceededException extends StreamixException {
    private final String fileName;
    private final long actualSize;
    private final long maxSize;
}
```

#### 4. ThumbnailGeneratorPort 확장

```java
public interface ThumbnailGeneratorPort {
    // 기존 메서드
    boolean supports(FileType fileType);
    byte[] generate(InputStream source, int width, int height);
    byte[] generateFromPath(String path, int width, int height);
    
    // v1.0.7 신규
    default int getOrder() { return 500; }
    default String getName() { return getClass().getSimpleName(); }
}
```

---

### 1.0.7 → 2.0.0: Phase 2 - 서비스 분리 및 대시보드 확장

**변경 사항:**

#### 1. 서비스 역할 분리 (SRP)

| 서비스 | v1.x 역할 | v2.0 역할 |
|--------|----------|----------|
| `FileUploadService` | 업로드 | 업로드 + 크기/타입 검증 |
| `FileStreamService` | 스트리밍 + 썸네일 조회 | 스트리밍만 |
| `FileMetadataService` | 메타데이터 조회 + 삭제 | 메타데이터 조회만 |
| `FileDeleteService` | - | **신규**: 파일/썸네일/메타데이터 삭제 |
| `ThumbnailService` | 썸네일 생성 | 썸네일 조회 + 생성 (다중 Generator) |

#### 2. 신규 설정 옵션

```yaml
streamix:
  storage:
    max-file-size: 104857600    # 최대 파일 크기 (바이트)
    allowed-types: IMAGE,VIDEO  # 허용 파일 타입 (빈 값 = 전체 허용)
```

#### 3. StreamixProperties 확장

```java
public record Storage(
    String basePath,
    long maxFileSize,           // 신규
    Set<String> allowedTypes    // 신규
) {
    public boolean isAllTypesAllowed() {
        return allowedTypes.isEmpty();
    }
}
```

#### 4. StreamixAutoConfiguration Bean 변경

```java
// ThumbnailService - List 주입
@Bean
public ThumbnailService thumbnailService(
    List<ThumbnailGeneratorPort> generators,
    FileStoragePort fileStoragePort,
    FileMetadataPort fileMetadataPort
) { ... }

// FileMetadataService - StoragePort 제거
@Bean
public FileMetadataService fileMetadataService(
    FileMetadataPort fileMetadataPort
) { ... }

// FileDeleteService - 신규
@Bean
public FileDeleteService fileDeleteService(
    FileMetadataPort fileMetadataPort,
    FileStoragePort fileStoragePort
) { ... }

// FileUploadService - 검증 파라미터 추가
@Bean
public FileUploadService fileUploadService(...,
    long maxFileSize,
    Set<FileType> allowedTypes
) { ... }
```

#### 5. GlobalExceptionHandler 확장

```java
@ExceptionHandler(FileSizeExceededException.class)
public ResponseEntity<ErrorResponse> handleFileSizeExceeded(...) {
    // HTTP 413 Payload Too Large
}
```

#### 6. 대시보드 템플릿 확장

| 파일 | 변경 내용 |
|------|----------|
| `layout.html` | v2.0.0 버전 표시, allowedTypes data 속성 |
| `dashboard.html` | 6개 FileType 아이콘/badge 지원 |
| `files.html` | 6개 FileType 아이콘/badge, 업로드 타입 안내 |
| `file-detail.html` | 6개 FileType 미리보기 (VIDEO/IMAGE/AUDIO 플레이어, DOCUMENT/ARCHIVE/OTHER 다운로드) |
| `dashboard.css` | FileType별 색상 및 스타일 |
| `dashboard.js` | FileType 검증 로직, MIME 매핑 |

#### 7. Javadoc 수정

```java
// Before (Java 25 오류)
* <h3>권장 우선순위</h3>

// After
* <p><strong>권장 우선순위:</strong></p>
```

---

## 📁 최종 수정 파일 목록 (2.0.0 기준)

### streamix-core

```
src/main/java/io/github/junhyeong9812/streamix/core/
├── domain/
│   ├── model/
│   │   ├── FileType.java              # 6개 타입
│   │   └── FileMetadata.java          # 헬퍼 메서드 추가
│   └── exception/
│       └── FileSizeExceededException.java  # 신규
├── application/
│   ├── port/
│   │   └── out/
│   │       └── ThumbnailGeneratorPort.java  # getOrder(), getName()
│   └── service/
│       ├── FileUploadService.java     # 검증 로직 추가
│       ├── FileMetadataService.java   # 조회 전용
│       ├── FileDeleteService.java     # 신규
│       └── ThumbnailService.java      # List<Generator> 관리
```

### streamix-spring-boot-starter

```
src/main/java/io/github/junhyeong9812/streamix/starter/
├── properties/
│   └── StreamixProperties.java        # allowedTypes, maxFileSize
├── autoconfigure/
│   ├── StreamixAutoConfiguration.java # Bean 생성자 변경
│   └── GlobalExceptionHandler.java    # FileSizeExceededException
├── adapter/
│   └── out/
│       └── FFmpegThumbnailAdapter.java  # getOrder(), getName()

src/main/resources/
├── templates/streamix/
│   ├── layout.html                    # v2.0.0, data 속성
│   ├── dashboard.html                 # 6개 타입 지원
│   ├── files.html                     # 6개 타입 지원
│   └── file-detail.html               # 6개 타입 미리보기
└── static/streamix/
    ├── css/dashboard.css              # FileType 스타일
    └── js/dashboard.js                # FileType 검증
```

---

## 🚀 Spring Boot 4.0 마이그레이션 체크리스트

Spring Boot 4.0으로 마이그레이션 시 확인해야 할 사항:

### 1. Parameter Name 처리
```java
// 모든 @RequestParam, @PathVariable에 name 속성 필수
@GetMapping("/files/{id}")
public Response method(@PathVariable(name = "id") UUID id) { ... }
```

### 2. EntityScan 패키지 변경
```java
// Before (Spring Boot 3.x)
import org.springframework.boot.autoconfigure.domain.EntityScan;

// After (Spring Boot 4.0)
import org.springframework.boot.persistence.autoconfigure.EntityScan;
```

### 3. Conditional Bean 주의
- `@ConditionalOnBean`은 Bean 등록 순서에 민감
- `@Import`로 가져오는 Configuration에서는 예상대로 동작하지 않을 수 있음

### 4. Properties Bean 이름
- `@EnableConfigurationProperties`로 등록된 Bean은 복잡한 내부 명명 규칙 사용
- Thymeleaf 등에서 직접 접근 시 alias Bean + `@Primary` 필요

### 5. Javadoc 헤딩 레벨 (Java 25)
- `<h3>` 연속 사용 금지
- `<p><strong>...</strong></p>` 패턴으로 대체

---

## 📝 권장 사항

### AutoConfiguration.imports 삭제
`@EnableStreamix` 어노테이션으로 모든 Configuration을 Import하는 경우, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일 삭제 권장

**이유:**
- `@EnableStreamix`와 AutoConfiguration.imports가 동시에 동작하면 Bean 중복 등록 가능
- 명시적 Import가 더 명확하고 제어 가능

### 사용법
```java
@SpringBootApplication
@EnableStreamix  // 이것 하나로 모든 설정 완료
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

## 🔗 참고 자료

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Migration Guide](https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-7.x)
- [Thymeleaf Spring Integration](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)

---

## 📌 버전 정보

- **Streamix**: 2.0.0
- **Spring Boot**: 4.0.0
- **Spring Framework**: 7.0.1
- **Java**: 25
- **Thymeleaf**: 3.1.3

---

*Last Updated: 2025-12-31*