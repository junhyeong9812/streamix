# [P2-14] StreamixJpaConfiguration vs StreamixRepositoryConfiguration 통합

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 데드 코드 / 혼란** |
| 카테고리 | 자동 설정 정리 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixJpaConfiguration.java`, `StreamixRepositoryConfiguration.java` |

## 관련성
이 이슈는 [P0-03 AutoConfiguration imports](p0-03-autoconfiguration-imports.md)와 직접 연관됨. P0-03 작업 시 함께 처리됨.

## 문제 분석

### 현재 두 클래스가 같은 일을 함

#### `StreamixJpaConfiguration`
```java
@AutoConfiguration
@ConditionalOnClass({JpaRepository.class, DataSource.class})
@ConditionalOnBean(DataSource.class)
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixJpaConfiguration {
    @Bean
    @ConditionalOnMissingBean(FileMetadataPort.class)
    public FileMetadataPort fileMetadataPort(FileMetadataJpaRepository repository) {
        return new JpaFileMetadataAdapter(repository);
    }
}
```

#### `StreamixRepositoryConfiguration`
```java
@Configuration
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixRepositoryConfiguration {
    @Bean("streamixProperties") @Primary
    public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
        return properties;
    }

    @Bean
    @ConditionalOnMissingBean(FileMetadataPort.class)
    public FileMetadataPort fileMetadataPort(FileMetadataJpaRepository repository) {
        return new JpaFileMetadataAdapter(repository);
    }

    @Bean
    @ConditionalOnMissingBean(StreamingMonitoringService.class)
    public StreamingMonitoringService streamingMonitoringService(StreamingSessionRepository sessionRepository) {
        return new StreamingMonitoringService(sessionRepository);
    }
}
```

### 발견 사항
1. **`StreamixJpaConfiguration`은 `@EnableStreamix.@Import`에 포함되지 않음** → 데드 코드
2. **`StreamixRepositoryConfiguration`이 모든 역할을 수행**: EntityScan + JpaRepositories + FileMetadataPort + StreamingMonitoringService
3. `streamixPropertiesAlias`는 별도 이슈 ([P2-15](p2-15-properties-alias.md))에서 정리

### 영향 범위
- `StreamixJpaConfiguration.java` 파일 자체 — 코드베이스 정돈
- `META-INF/spring/...AutoConfiguration.imports` 파일 부재로 사실상 한 번도 로드되지 않음
- `StreamingMonitoringService` Bean이 `StreamixMonitoringConfiguration`에서도 등록됨 (3중 정의 — 그러나 `@ConditionalOnMissingBean`으로 충돌은 안 남)

## 변경 프로세스

### Step 1: `StreamixJpaConfiguration.java` 삭제
P0-03에서 결정한 옵션 A(`@EnableStreamix` 전용)를 따르므로:
- `StreamixJpaConfiguration`은 import되지 않으므로 삭제해도 동작 변화 없음

### Step 2: `StreamixRepositoryConfiguration` 책임 정리
이름이 "Repository Configuration"인데 실제로는 어댑터/서비스 등록도 함. 이름과 책임이 어긋남.

옵션:
- **A**: 그대로 두되 javadoc에 책임 명시
- **B**: 이름 변경 (`StreamixJpaConfiguration` 또는 `StreamixPersistenceConfiguration`)
- **C**: 분리 — Repository scan과 Bean 등록을 별개 파일로

채택: **A** — 이름 변경은 사용자 import 깨질 수 있고, EnableStreamix.@Import 갱신 필요. 작업 비용 대비 가치 낮음. javadoc만 명확히.

### Step 3: 책임 중복 — `StreamingMonitoringService` Bean
`StreamixRepositoryConfiguration.streamingMonitoringService()`와 `StreamixMonitoringConfiguration.streamingMonitoringService()`가 같은 Bean 등록.
- `@ConditionalOnMissingBean`으로 충돌은 없음
- 하지만 두 곳에 분산되어 있어 혼란

→ `StreamixRepositoryConfiguration`에서 제거. `StreamixMonitoringConfiguration`만 유지 (그쪽이 `@ConditionalOnProperty(streamix.dashboard.enabled)`로 조건부 활성화 의도가 더 명확)

### Step 4: javadoc 갱신
```java
/**
 * Streamix JPA Persistence 자동 설정 클래스입니다.
 *
 * <p>{@code @EnableStreamix}의 {@code @Import} 대상으로,
 * 다음 작업을 수행합니다:</p>
 *
 * <ol>
 *   <li>{@link StreamixProperties} 설정 활성화</li>
 *   <li>JPA Entity 스캔</li>
 *   <li>Spring Data JPA Repository 스캔</li>
 *   <li>{@link FileMetadataPort} Bean 등록 (사용자 정의 미존재 시)</li>
 *   <li>Thymeleaf 접근용 {@code streamixProperties} Bean alias 등록</li>
 * </ol>
 *
 * <p>{@link StreamingMonitoringService}는 {@link StreamixMonitoringConfiguration}에서
 * 별도로 등록됩니다 (대시보드 활성화 조건부).</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixRepositoryConfiguration {
    // streamingMonitoringService Bean 제거
}
```

## Before / After

### Before — 디렉토리
```
autoconfigure/
├── StreamixAutoConfiguration.java
├── StreamixDashboardConfiguration.java
├── StreamixJpaConfiguration.java          ❌ 데드 코드
├── StreamixMonitoringConfiguration.java   (StreamingMonitoringService Bean 등록)
├── StreamixRepositoryConfiguration.java   (StreamingMonitoringService Bean 중복 등록)
├── StreamixThumbnailConfiguration.java
└── StreamixWebConfiguration.java
```

### After — 디렉토리
```
autoconfigure/
├── StreamixAutoConfiguration.java
├── StreamixDashboardConfiguration.java
├── StreamixMonitoringConfiguration.java   (유일한 StreamingMonitoringService 등록처)
├── StreamixRepositoryConfiguration.java   (Persistence 책임만)
├── StreamixThumbnailConfiguration.java
└── StreamixWebConfiguration.java
```

### Before — StreamixRepositoryConfiguration (요약)
```java
@Configuration
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(...)
@EnableJpaRepositories(...)
public class StreamixRepositoryConfiguration {

    @Bean("streamixProperties") @Primary
    public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) { ... }

    @Bean
    @ConditionalOnMissingBean(FileMetadataPort.class)
    public FileMetadataPort fileMetadataPort(FileMetadataJpaRepository repository) {
        return new JpaFileMetadataAdapter(repository);
    }

    @Bean
    @ConditionalOnMissingBean(StreamingMonitoringService.class)
    public StreamingMonitoringService streamingMonitoringService(StreamingSessionRepository sessionRepository) {
        return new StreamingMonitoringService(sessionRepository);
    }
}
```

### After — StreamixRepositoryConfiguration
```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixRepositoryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StreamixRepositoryConfiguration.class);

    public StreamixRepositoryConfiguration() {
        log.info("Streamix Repository Configuration initialized");
    }

    @Bean("streamixProperties") @Primary
    public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
        return properties;
    }

    @Bean
    @ConditionalOnMissingBean(FileMetadataPort.class)
    public FileMetadataPort fileMetadataPort(FileMetadataJpaRepository repository) {
        log.info("Creating JpaFileMetadataAdapter for metadata persistence");
        return new JpaFileMetadataAdapter(repository);
    }
    // streamingMonitoringService Bean 제거 — StreamixMonitoringConfiguration이 담당
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. Bean 중복 검증
```java
// 통합 테스트
@SpringBootTest
@EnableStreamix
class BeanRegistrationTest {
    @Autowired ApplicationContext ctx;
    
    @Test
    void onlyOneFileMetadataPortBean() {
        assertThat(ctx.getBeansOfType(FileMetadataPort.class)).hasSize(1);
    }
    
    @Test
    void onlyOneStreamingMonitoringServiceBean() {
        assertThat(ctx.getBeansOfType(StreamingMonitoringService.class)).hasSize(1);
    }
}
```

### 3. 회귀 테스트
```bash
./gradlew test
```

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixJpaConfiguration.java` ❌ 삭제
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixRepositoryConfiguration.java`

## 참고
- Spring Boot Configuration 책임 분리 패턴 — 한 Configuration은 하나의 관심사
- `@ConditionalOnMissingBean`은 동일 타입 Bean이 여러 Configuration에서 정의될 때 첫 번째 등록만 살리는 안전망 — 의존하지 말 것
