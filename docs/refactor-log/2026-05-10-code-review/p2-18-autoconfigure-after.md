# [P2-18] EnableStreamix 의존 순서 어노테이션 부재

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 의존 순서 / 시작 안정성** |
| 카테고리 | Spring Boot 자동 설정 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | 6개 `*Configuration.java` |

## 문제 분석

### 현재 동작
모든 `*Configuration` 클래스에 의존 순서 어노테이션 없음.
```java
@AutoConfiguration   // 또는 @Configuration
public class StreamixJpaConfiguration { ... }
```

### Spring Boot 자동 설정 의존
- `DataSourceAutoConfiguration` → `JpaRepositoriesAutoConfiguration` → 사용자 정의 JPA 설정
- 만약 `StreamixRepositoryConfiguration`이 `DataSourceAutoConfiguration`보다 먼저 초기화되면 `DataSource` Bean이 아직 없어 `@ConditionalOnBean(DataSource.class)`이 false → JPA 활성화 안 됨

### Spring Boot 표준 패턴
```java
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(JpaRepository.class)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)  // 또는 @AutoConfiguration의 after 속성
public class MyJpaAutoConfiguration { }
```

### 현재 동작이 OK인 이유 (대부분의 경우)
- `@EnableStreamix` `@Import`는 명시적이라 사용자가 `@SpringBootApplication`에 추가하면 boot 자동 설정 후에 처리됨
- DataSource는 사용자가 `spring.datasource.url`을 설정하면 boot가 자동 등록 → boot의 단계가 사용자 `@Configuration`보다 먼저
- 따라서 일반적으로 동작

### 잠재적 문제 케이스
- 사용자가 직접 DataSource Bean을 `@Bean` 메서드로 정의 + `@DependsOn` 없이 사용 → 순서 모호
- Multiple DataSource → 어느 것에 묶일지 불명확
- `@Lazy` DataSource 사용 시

### 영향 범위
- 표준 Spring Boot 사용에서는 영향 없음
- 비표준 설정(다중 DS, 수동 정의)에서 시작 실패 가능
- 명시적인 의도 표현으로 코드 가독성 향상

## 변경 프로세스

### Step 1: 의존 순서 명시
```java
// StreamixRepositoryConfiguration
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "...persistence")
@EnableJpaRepositories(basePackages = "...persistence")
public class StreamixRepositoryConfiguration { ... }
```

```java
// StreamixMonitoringConfiguration
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(StreamixRepositoryConfiguration.class)
@ConditionalOnClass({JpaRepository.class, DataSource.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "streamix.dashboard.enabled", havingValue = "true", matchIfMissing = true)
public class StreamixMonitoringConfiguration { ... }
```

```java
// StreamixDashboardConfiguration
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(StreamixMonitoringConfiguration.class)
@ConditionalOnWebApplication(...)
public class StreamixDashboardConfiguration { ... }
```

```java
// StreamixWebConfiguration
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(StreamixAutoConfiguration.class)
@ConditionalOnWebApplication(...)
public class StreamixWebConfiguration { ... }
```

```java
// StreamixThumbnailConfiguration — 의존성 없음 (FileStoragePort/MetadataPort 안 받음)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(...)
public class StreamixThumbnailConfiguration { ... }
```

```java
// StreamixAutoConfiguration — Repository에서 FileMetadataPort 받음
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({
    StreamixRepositoryConfiguration.class,
    StreamixThumbnailConfiguration.class
})
@EnableConfigurationProperties(StreamixProperties.class)
public class StreamixAutoConfiguration { ... }
```

### Step 2: import 추가
```java
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
```

### Step 3: 의존성 그래프 시각화 (문서)
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

### Step 4: 충돌 가능성 검토
- `@AutoConfigureAfter`는 hint일 뿐 강제 아님 — boot가 가능하면 따름
- 사용자가 `@Order` 또는 `@DependsOn` 사용 시 혼합 동작
- `@ConditionalOnBean`이 1차 안전장치

## Before / After

### Before — StreamixRepositoryConfiguration
```java
@Configuration
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "...")
@EnableJpaRepositories(basePackages = "...")
public class StreamixRepositoryConfiguration { ... }
```

### After — StreamixRepositoryConfiguration
```java
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixRepositoryConfiguration { ... }
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 시작 순서 검증 (debug 로그)
```yaml
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
    io.github.junhyeong9812.streamix: DEBUG
```
실행 후 로그에서:
```
Streamix Repository Configuration initialized   ← DataSource 초기화 후에 와야 함
Creating JpaFileMetadataAdapter for metadata persistence
Streamix Auto-Configuration initialized
Creating LocalFileStorageAdapter: ...
Creating ThumbnailService with N generators
```
순서 확인.

### 3. 회귀 테스트
```bash
./gradlew :streamix-spring-boot-starter:test
```
`StreamixAutoConfigurationTest`는 ApplicationContextRunner로 명시적 등록이라 영향 없음.

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixRepositoryConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixAutoConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixMonitoringConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixDashboardConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixWebConfiguration.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixThumbnailConfiguration.java`

## 참고
- Spring Boot Reference §A.1 Auto-configuration Ordering
- `@AutoConfigureAfter` / `@AutoConfigureBefore` / `@AutoConfigureOrder`
- 차이: `@DependsOn`은 Bean instantiation 순서, `@AutoConfigureAfter`는 Configuration 처리 순서
