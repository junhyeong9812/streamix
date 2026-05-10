# [P2-13] formatSize 6곳 중복 — ByteSizeFormatter 유틸리티로 통합

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — DRY 위반 / 일관성** |
| 카테고리 | 리팩토링 |
| 발견 위치 | core, starter, dashboard.js |
| 영향 파일 | 6개 (Java 5개 + JS 1개) |

## 문제 분석

### 현재 상황 — `formatSize`/`formatBytes`가 6곳에 중복

#### 1. `FileMetadata.getFormattedSize()` (streamix-core)
```java
public String getFormattedSize() {
    if (size < 1024) return size + " B";
    else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
    else if (size < 1024L * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
    else return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
}
```

#### 2. `FileSizeExceededException.formatSize(long)` (private static)
1번과 동일

#### 3. `FileUploadService.formatSize(long)` (private static)
1번과 동일

#### 4. `StreamixAutoConfiguration.formatSize(long)` (private static)
1번과 동일

#### 5. `GlobalExceptionHandler.formatSize(long)` (private static)
1번과 동일

#### 6. `StreamingMonitoringService.DashboardStats.formatBytes(long)` + `FileStreamingStats.formatBytes(long)`
1~5와 약간 다름:
- TB 단위 추가
- `String.format("%.1f GB", ...)`로 GB 정밀도가 1자리 (1~5는 2자리)

#### 7. JS `dashboard.js formatFileSize(bytes)` — 같은 의미, 다른 구현

### 문제점
- **DRY 위반**: 동일 로직 6번 중복 → 유지보수 시 6곳 동시 변경
- **불일치**: GB 정밀도(1자리 vs 2자리), TB 지원 여부 다름
- **버그 위험**: 한 곳 고치면 다른 곳 까먹음

### 기대
- 단일 유틸리티 클래스 `ByteSizeFormatter`
- 모든 호출자가 공유
- 정책(정밀도, 단위 thresholds) 한 곳에서 결정

## 변경 프로세스

### Step 1: streamix-core에 유틸리티 클래스 신설
```java
// streamix-core/src/main/java/.../core/domain/util/ByteSizeFormatter.java
package io.github.junhyeong9812.streamix.core.domain.util;

/**
 * 바이트 크기를 사람이 읽기 쉬운 문자열로 변환합니다.
 *
 * <p>1024 단위(IEC binary prefix)를 사용합니다. 단위는 B/KB/MB/GB/TB까지 지원합니다.</p>
 */
public final class ByteSizeFormatter {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    private ByteSizeFormatter() {}

    /**
     * 바이트 크기를 단위 문자열로 변환합니다.
     *
     * <pre>
     *   500       → "500 B"
     *   2048      → "2.0 KB"
     *   1572864   → "1.5 MB"
     *   2147483648 → "2.0 GB"
     *   1099511627776 → "1.0 TB"
     * </pre>
     *
     * @param bytes 바이트 크기 (음수면 "0 B")
     * @return 포맷된 문자열
     */
    public static String format(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < KB) return bytes + " B";
        if (bytes < MB) return String.format("%.1f KB", bytes / (double) KB);
        if (bytes < GB) return String.format("%.1f MB", bytes / (double) MB);
        if (bytes < TB) return String.format("%.1f GB", bytes / (double) GB);
        return String.format("%.1f TB", bytes / (double) TB);
    }
}
```

GB 정밀도: 6번(MonitoringService) 패턴 따라 **1자리로 통일**. 외부 사용자에게 일관됨.

### Step 2: 6곳 모두 ByteSizeFormatter.format() 호출로 변경

#### 2.1. FileMetadata
```java
public String getFormattedSize() {
    return ByteSizeFormatter.format(size);
}
```

#### 2.2. FileSizeExceededException
```java
public FileSizeExceededException(long actualSize, long maxSize) {
    super(String.format("File size %s exceeds maximum allowed size %s",
        ByteSizeFormatter.format(actualSize),
        ByteSizeFormatter.format(maxSize)));
    // ...
}
// formatSize private 메서드 삭제
```

#### 2.3. FileUploadService
```java
log.info("Uploading file: {}, size: {}", command.originalName(), ByteSizeFormatter.format(command.size()));
// formatSize private 메서드 삭제
```

#### 2.4. StreamixAutoConfiguration
```java
log.info("  Max file size: {}", ByteSizeFormatter.format(properties.storage().maxFileSize()));
// formatSize private 메서드 삭제
```

#### 2.5. GlobalExceptionHandler
```java
log.warn("File size exceeded: {} (actual={}, max={})",
    ex.getFileName(),
    ByteSizeFormatter.format(ex.getActualSize()),
    ByteSizeFormatter.format(ex.getMaxSize()));
// formatSize private 메서드 삭제
```

#### 2.6. DashboardStats / FileStreamingStats
```java
public String getTodayBytesFormatted() {
    return ByteSizeFormatter.format(todayBytes);
}
// formatBytes private 메서드 삭제 (DashboardStats, FileStreamingStats 둘 다)
```

#### 2.7. JS dashboard.js (별도 — JS는 통합 안 함)
JS는 별도 언어이므로 그대로 유지. `formatFileSize` JS 함수는 1자리 정밀도로 통일.

### Step 3: 단위 테스트
```java
// streamix-core/src/test/java/.../core/domain/util/ByteSizeFormatterTest.java
@DisplayName("ByteSizeFormatter 테스트")
class ByteSizeFormatterTest {

    @Test
    @DisplayName("바이트 단위")
    void bytes() {
        assertThat(ByteSizeFormatter.format(0)).isEqualTo("0 B");
        assertThat(ByteSizeFormatter.format(500)).isEqualTo("500 B");
        assertThat(ByteSizeFormatter.format(1023)).isEqualTo("1023 B");
    }

    @Test
    @DisplayName("KB 단위")
    void kilobytes() {
        assertThat(ByteSizeFormatter.format(1024)).isEqualTo("1.0 KB");
        assertThat(ByteSizeFormatter.format(2048)).isEqualTo("2.0 KB");
    }

    @Test
    @DisplayName("MB 단위")
    void megabytes() {
        assertThat(ByteSizeFormatter.format(1572864)).isEqualTo("1.5 MB");
        assertThat(ByteSizeFormatter.format(15_728_640L)).isEqualTo("15.0 MB");
    }

    @Test
    @DisplayName("GB 단위")
    void gigabytes() {
        assertThat(ByteSizeFormatter.format(2_147_483_648L)).isEqualTo("2.0 GB");
    }

    @Test
    @DisplayName("TB 단위")
    void terabytes() {
        assertThat(ByteSizeFormatter.format(1_099_511_627_776L)).isEqualTo("1.0 TB");
    }

    @Test
    @DisplayName("음수는 0 B")
    void negative() {
        assertThat(ByteSizeFormatter.format(-1L)).isEqualTo("0 B");
    }
}
```

### Step 4: 기존 테스트 회귀 확인
`FileMetadataTest.GetFormattedSizeTest`:
- `formatsBytes` — 500 B 기대 ✓
- `formatsKilobytes` — 2.0 KB ✓
- `formatsMegabytes` — 15.0 MB ✓
- `formatsGigabytes` — **"2.00 GB" 기대** → 정밀도 변경으로 "2.0 GB"가 됨 → 테스트 갱신 필요

```java
@Test
@DisplayName("GB 단위로 표시한다")
void formatsGigabytes() {
    FileMetadata metadata = createMetadataWithSize(2_147_483_648L);
    assertThat(metadata.getFormattedSize()).isEqualTo("2.0 GB");  // 변경: 2.00 → 2.0
}
```

`StreamixExceptionTest.FileSizeExceededExceptionTest.formatsSizeInMessage`:
- `bytesEx.getMessage()).contains("500 B")` ✓
- `kbEx.getMessage()).contains("KB")` ✓
- `mbEx.getMessage()).contains("MB")` ✓
- `gbEx.getMessage()).contains("GB")` ✓
- → 모두 `contains` 패턴이라 정밀도 변경 영향 없음 ✓

## Before / After

### Before — 6곳에 동일 로직
(상기 1~6 참조)

### After — 1곳 정의 + 5곳 호출
```java
// 신규: streamix-core/.../core/domain/util/ByteSizeFormatter.java
public final class ByteSizeFormatter { /* ... */ }

// 5개 Java 호출처
ByteSizeFormatter.format(bytes)
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew compileJava
```

### 2. 신규 테스트 + 기존 테스트
```bash
./gradlew test --tests ByteSizeFormatterTest
./gradlew test --tests FileMetadataTest
./gradlew test --tests StreamixExceptionTest
```

### 3. 중복 코드 제거 확인
```bash
grep -rn 'formatSize\|formatBytes' streamix-core/src/main/ streamix-spring-boot-starter/src/main/
# 결과: ByteSizeFormatter.format 호출만 남아야 함 (또는 ByteSizeFormatter.java 자체)
```

## 관련 파일
- `streamix-core/src/main/java/.../core/domain/util/ByteSizeFormatter.java` (신규)
- `streamix-core/src/main/java/.../core/domain/model/FileMetadata.java`
- `streamix-core/src/main/java/.../core/domain/exception/FileSizeExceededException.java`
- `streamix-core/src/main/java/.../core/application/service/FileUploadService.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixAutoConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/GlobalExceptionHandler.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/service/StreamingMonitoringService.java`
- `streamix-core/src/test/java/.../core/domain/util/ByteSizeFormatterTest.java` (신규)
- `streamix-core/src/test/java/.../core/domain/model/FileMetadataTest.java` (한 줄 수정)

## 참고
- DRY 원칙 (The Pragmatic Programmer)
- IEC 60027-2 Binary Prefixes (KiB/MiB는 1024, KB/MB는 1000) — 본 라이브러리는 1024 단위지만 KB 표기 (관용 표기 우선)
