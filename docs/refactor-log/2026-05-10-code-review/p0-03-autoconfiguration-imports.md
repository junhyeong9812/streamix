# [P0-03] AutoConfiguration.imports 부재 + StreamixJpaConfiguration 데드 코드

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P0 — 자동 구성 미동작 / 데드 코드** |
| 카테고리 | Spring Boot 자동 설정 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | 6개 `*Configuration.java`, `EnableStreamix.java`, `META-INF/spring/` |

## 문제 분석

### 현재 동작 (3가지 모순)

#### 모순 1: `@AutoConfiguration`이지만 imports 파일 없음
```java
// 6개 클래스 모두
@AutoConfiguration
public class StreamixAutoConfiguration { ... }
@AutoConfiguration
public class StreamixJpaConfiguration { ... }
@AutoConfiguration
public class StreamixWebConfiguration { ... }
@AutoConfiguration
public class StreamixThumbnailConfiguration { ... }
@AutoConfiguration
public class StreamixMonitoringConfiguration { ... }
@AutoConfiguration
public class StreamixDashboardConfiguration { ... }
```

`@AutoConfiguration`(Spring Boot 2.7+)은 **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** 파일에 클래스 FQN을 한 줄씩 등록해야 자동 로드됨.

검증:
```bash
$ find streamix-spring-boot-starter/src/main/resources -name "*.imports"
# (결과 없음)
$ find streamix-spring-boot-starter/src/main/resources -path "*META-INF*"
# (결과 없음)
```

→ **자동 구성 메커니즘이 작동하지 않음**

#### 모순 2: `@EnableStreamix`로 우회되어 일부만 동작
```java
// EnableStreamix.java:100-107
@Import({
    StreamixRepositoryConfiguration.class,
    StreamixAutoConfiguration.class,
    StreamixWebConfiguration.class,
    StreamixThumbnailConfiguration.class,
    StreamixMonitoringConfiguration.class,
    StreamixDashboardConfiguration.class
})
public @interface EnableStreamix { }
```

**`StreamixJpaConfiguration`이 빠져 있음** → 명시적으로 import되지 않으니 데드 코드.

#### 모순 3: 같은 Bean을 두 곳에서 등록
- `StreamixJpaConfiguration.fileMetadataPort()` (Bean 등록)
- `StreamixRepositoryConfiguration.fileMetadataPort()` (Bean 등록 — 동일)

`StreamixRepositoryConfiguration`만 import되므로 후자만 동작 → JpaConfiguration은 100% 데드 코드.

### 기대 동작
사용자가 두 가지 방식 중 하나를 선택할 수 있어야 함:

**방식 A — `@EnableStreamix` 명시 활성화 (현재 의도)**
```java
@SpringBootApplication
@EnableStreamix
public class MyApp { }
```

**방식 B — 자동 구성 (Spring Boot 표준)**
```java
@SpringBootApplication
public class MyApp { }
// 클래스패스에 starter만 있으면 자동 활성화
```

현재는 둘 다 불완전.

### 원인 분석
- 1.0.x → 2.0.0 작업에서 `@EnableStreamix` 어노테이션 도입
- 동시에 클래스에 `@AutoConfiguration`도 유지 (중간 마이그레이션 흔적)
- imports 파일 작성을 깜빡함
- `StreamixJpaConfiguration`은 1.0.x 시절 만들어진 것을 v2에서 `StreamixRepositoryConfiguration`으로 대체했으나 삭제 누락

## 변경 프로세스

### 옵션 비교
| 옵션 | 변경 | 사용자 경험 | 라이브러리 철학 |
|------|------|------------|----------------|
| A. `@EnableStreamix` 전용 (현재 의도) | imports 파일 안 만듦 + `@AutoConfiguration` → `@Configuration`으로 변경 | 명시적 활성화 필수 | 사용자가 의식적으로 켜는 옵트인 |
| B. 자동 구성 전용 | imports 파일 작성 + `@EnableStreamix` 폐기 또는 alias로만 유지 | classpath에 추가하면 활성화 | Spring Boot 표준 방식 |
| C. 둘 다 지원 | imports 파일 작성 + `@EnableStreamix`도 유지 | 사용자 선택 | 복잡 |

### 채택: 옵션 A (`@EnableStreamix` 전용 + 정리)
이유:
1. README와 Javadoc 전체가 `@EnableStreamix`를 메인 활성화 방법으로 설명
2. 미디어 스트리밍 라이브러리는 무거우므로 기본 비활성이 안전
3. 사용자에게 "내가 이 기능을 켰다"는 명시성이 중요 (대시보드 노출 = 보안 영향)
4. 옵션 B는 v3에서 검토 가능

### Step 1: 6개 `*Configuration.java`의 `@AutoConfiguration` → `@Configuration`
모든 자동 설정 클래스에서 어노테이션 변경:
```java
// Before
import org.springframework.boot.autoconfigure.AutoConfiguration;
@AutoConfiguration

// After
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
```

`proxyBeanMethods = false`는 starter Bean 등록에서 표준. CGLIB proxy 회피로 빠른 시작.

### Step 2: `StreamixJpaConfiguration` 삭제
- `StreamixRepositoryConfiguration`이 같은 Bean(`FileMetadataPort`)을 등록함
- `EnableStreamix`의 `@Import`에 포함되지 않으므로 삭제해도 동작 변화 없음
- 단순 파일 삭제

### Step 3: `StreamixRepositoryConfiguration`에 누락된 JPA 활성화 검증
현재 이미 있음:
```java
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
```
→ 변경 불필요

### Step 4: `EnableStreamix`의 `@Import` 정리
6개 → 5개로 (Jpa 삭제 반영).
```java
// Before
@Import({
    StreamixRepositoryConfiguration.class,
    StreamixAutoConfiguration.class,
    StreamixWebConfiguration.class,
    StreamixThumbnailConfiguration.class,
    StreamixMonitoringConfiguration.class,
    StreamixDashboardConfiguration.class
})

// After (변경 없음 — 이미 Jpa 빠져있음)
@Import({
    StreamixRepositoryConfiguration.class,
    StreamixAutoConfiguration.class,
    StreamixWebConfiguration.class,
    StreamixThumbnailConfiguration.class,
    StreamixMonitoringConfiguration.class,
    StreamixDashboardConfiguration.class
})
```

### Step 5: javadoc 업데이트
모든 Configuration 클래스의 javadoc에서 "META-INF/spring/...AutoConfiguration.imports 파일에 등록되어 자동 로드" 문구 삭제 → "@EnableStreamix를 통해 import됩니다" 명시.

### Step 6: README 업데이트 (작업 범위 외 — 별도 PR)
README에 "반드시 @EnableStreamix를 추가해야 활성화됩니다"를 강조.

## Before / After

### Before — StreamixAutoConfiguration.java (대표)
```java
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(StreamixProperties.class)
public class StreamixAutoConfiguration {
    // ...
}
```

### After — StreamixAutoConfiguration.java
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StreamixProperties.class)
public class StreamixAutoConfiguration {
    // ...
}
```

### Before — 디렉토리
```
streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/
├── StreamixAutoConfiguration.java
├── StreamixDashboardConfiguration.java
├── StreamixJpaConfiguration.java        ⚠️ 데드 코드
├── StreamixMonitoringConfiguration.java
├── StreamixRepositoryConfiguration.java
├── StreamixThumbnailConfiguration.java
└── StreamixWebConfiguration.java
```

### After — 디렉토리
```
streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/
├── StreamixAutoConfiguration.java
├── StreamixDashboardConfiguration.java
├── StreamixMonitoringConfiguration.java
├── StreamixRepositoryConfiguration.java
├── StreamixThumbnailConfiguration.java
└── StreamixWebConfiguration.java
```

### After — javadoc 변경 (StreamixAutoConfiguration 발췌)
```java
/**
 * Streamix 핵심 기능 자동 설정 클래스입니다.
 *
 * <p>이 클래스는 {@link io.github.junhyeong9812.streamix.starter.annotation.EnableStreamix}
 * 어노테이션을 통해 Import됩니다. 사용자 애플리케이션의 메인 클래스에
 * {@code @EnableStreamix}를 추가하면 이 Configuration이 활성화됩니다.</p>
 *
 * ...
 */
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 데드 코드 삭제 확인
```bash
ls streamix-spring-boot-starter/src/main/java/.../autoconfigure/StreamixJpaConfiguration.java
# 결과: No such file
```

### 3. `StreamixAutoConfigurationTest` 동작 검증
기존 테스트가 `AutoConfigurations.of(StreamixAutoConfiguration.class)`로 명시적 등록하므로 영향 없음:
```bash
./gradlew :streamix-spring-boot-starter:test --tests StreamixAutoConfigurationTest
```

### 4. 사용자 시나리오 통합 테스트 (선택)
새 통합 테스트 추가:
```java
@SpringBootTest
@EnableStreamix
class EnableStreamixIntegrationTest {
    @Autowired StreamixApiController controller;
    @Test void contextLoads() { assertThat(controller).isNotNull(); }
}
```

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixAutoConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixDashboardConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixMonitoringConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixRepositoryConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixThumbnailConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixWebConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixJpaConfiguration.java` ❌ 삭제
- `streamix-spring-boot-starter/src/main/java/.../starter/annotation/EnableStreamix.java`

## 참고
- Spring Boot 4.0 Reference Manual §A.1 Auto-configuration
- `@AutoConfiguration` vs `@Configuration` 차이: 전자는 spring-boot-autoconfigure가 ordering을 위한 메타로 사용, 후자는 단순 컴포넌트 등록
