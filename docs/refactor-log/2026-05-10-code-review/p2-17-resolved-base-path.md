# [P2-17] Storage.getResolvedBasePath — 절대경로 판별 + Path API 사용

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 정확성 / 이식성** |
| 카테고리 | 버그 가능성 / Cross-platform |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixProperties.java` |

## 문제 분석

### 현재 동작
```java
// StreamixProperties.Storage.getResolvedBasePath():151-156
public String getResolvedBasePath() {
    if (basePath.startsWith("/") || basePath.contains(":")) {
        return basePath;
    }
    return System.getProperty("user.dir") + "/" + basePath;
}
```

### 결함

#### 결함 1: `contains(":")`로 Windows drive letter 판별 부정확
의도: `C:\data` 같은 Windows 절대 경로 인식
실제: `:`가 포함된 모든 문자열을 절대 경로로 판단

오판 예시:
- `data:image/jpeg` (data URI 같은 문자열) → 절대 경로로 잘못 판단
- `streamix-data:v1` (의도하지 않은 콜론) → 절대 경로
- `./relative:weird` → 절대 경로

#### 결함 2: 경로 결합에 `+ "/"` 사용
- Windows에서 `"\"`이 표준 경로 구분자
- Java NIO는 `/`도 받아들이지만 일관성 없음

#### 결함 3: `System.getProperty("user.dir")`이 컨테이너 환경에서 의외
- Docker/Kubernetes에서 working directory가 `/`인 경우
- spring-boot fat jar 실행 시 user.dir이 jar의 디렉토리 아닌 호출 위치

### 기대 동작
- `Path.of(basePath).isAbsolute()`로 정확한 절대경로 판별
- Path API로 OS 독립적 결합
- 결합 결과는 normalize

### 영향 범위
- 일반 사용 케이스(`./uploads`, `/var/lib/streamix`)는 동작 OK
- 엣지 케이스(콜론 포함 경로, Windows)에서 미묘한 버그
- `LocalFileStorageAdapter`가 이 결과를 받아 basePath로 사용 → path traversal 검증 시 비교 실패 가능 ([P1-06](p1-06-path-traversal.md)와 시너지 위험)

## 변경 프로세스

### Step 1: `Path.isAbsolute()` 활용
```java
public String getResolvedBasePath() {
    Path path = java.nio.file.Path.of(basePath);
    if (path.isAbsolute()) {
        return path.normalize().toString();
    }
    return java.nio.file.Path.of(System.getProperty("user.dir"))
        .resolve(path)
        .normalize()
        .toString();
}
```

`Path.isAbsolute()`는 OS별 정확한 판별:
- Linux/macOS: `/`로 시작
- Windows: `C:\`, `D:\`, `\\server\share` 등

### Step 2: import 추가
```java
import java.nio.file.Path;
```

### Step 3: 단위 테스트 추가
```java
// StreamixPropertiesTest (신규 파일)
@DisplayName("StreamixProperties.Storage 테스트")
class StoragePropertiesTest {

    @Test
    @DisplayName("절대 경로 (Unix)는 그대로 반환")
    void absolutePathUnix() {
        var s = new StreamixProperties.Storage("/var/lib/streamix", 0, null);
        assertThat(s.getResolvedBasePath()).isEqualTo("/var/lib/streamix");
    }

    @Test
    @DisplayName("상대 경로는 user.dir 기준으로 결합")
    void relativePath() {
        var s = new StreamixProperties.Storage("./uploads", 0, null);
        Path expected = Path.of(System.getProperty("user.dir"), "uploads").normalize();
        assertThat(s.getResolvedBasePath()).isEqualTo(expected.toString());
    }

    @Test
    @DisplayName("콜론 포함 상대 경로는 상대 경로로 처리")
    void colonContainingRelative() {
        var s = new StreamixProperties.Storage("./data:weird", 0, null);
        // Path.isAbsolute()가 콜론을 false로 판단 (Linux/macOS)
        // Windows에서 가능한 결과는 별도 검증
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;  // skip on Windows
        }
        Path expected = Path.of(System.getProperty("user.dir"), "data:weird").normalize();
        assertThat(s.getResolvedBasePath()).isEqualTo(expected.toString());
    }
}
```

## Before / After

### Before
```java
public String getResolvedBasePath() {
    // 이미 절대 경로이거나 Windows 드라이브 문자 포함 시 그대로 반환
    if (basePath.startsWith("/") || basePath.contains(":")) {
        return basePath;
    }
    return System.getProperty("user.dir") + "/" + basePath;
}
```

### After
```java
import java.nio.file.Path;

/**
 * 절대 경로로 변환된 저장소 경로를 반환합니다.
 *
 * <p>{@link Path#isAbsolute()}로 절대/상대 여부를 판별하여 OS별로 정확히 처리합니다:</p>
 * <ul>
 *   <li>Unix/macOS: {@code /}로 시작하는 경로</li>
 *   <li>Windows: {@code C:\}, {@code \\server\share} 등</li>
 * </ul>
 *
 * <p>상대 경로는 {@code System.getProperty("user.dir")} 기준으로 결합 후 normalize됩니다.</p>
 *
 * @return 절대 경로 (normalized)
 */
public String getResolvedBasePath() {
    Path path = Path.of(basePath);
    if (path.isAbsolute()) {
        return path.normalize().toString();
    }
    return Path.of(System.getProperty("user.dir"))
        .resolve(path)
        .normalize()
        .toString();
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 단위 테스트
```bash
./gradlew :streamix-spring-boot-starter:test --tests StoragePropertiesTest
```

### 3. 회귀
```bash
./gradlew test
```

### 4. 통합 — LocalFileStorageAdapter와의 연동
P1-06 작업 후 통합 테스트:
- 사용자가 `./streamix-data` 설정 → resolvedBasePath = `/현재디렉토리/streamix-data`
- LocalFileStorageAdapter가 그 경로로 정상 초기화
- `save("test.jpg", ...)` → `/현재디렉토리/streamix-data/test.jpg` 저장

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/properties/StreamixProperties.java`
- `streamix-spring-boot-starter/src/test/java/.../starter/properties/StoragePropertiesTest.java` (신규)

## 참고
- `java.nio.file.Path.isAbsolute()` API
- Cross-platform Java path handling: Unix uses `/`, Windows uses `\` or `/`
- `System.getProperty("user.dir")` vs `System.getenv("PWD")`: user.dir은 JVM 시작 시점, PWD는 셸 변수
