# [P2-15] streamixPropertiesAlias `@Primary` Hack 정리

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 임시방편 정리** |
| 카테고리 | 리팩토링 / Spring 설정 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixRepositoryConfiguration.java`, `StreamixProperties.java` |

## 문제 분석

### 현재 동작
```java
// StreamixRepositoryConfiguration.java:107-112
@Bean("streamixProperties") @Primary
public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
    log.info("Registering StreamixProperties bean alias for Thymeleaf access");
    return properties;
}
```

### 동기 — Thymeleaf SpEL 접근
Thymeleaf 템플릿에서:
```html
<a th:href="@{${@streamixProperties.dashboard.path}}">대시보드</a>
```
- `@beanName` 문법으로 Bean 직접 참조
- `streamixProperties`라는 명시적 이름이 필요

### `@ConfigurationProperties` Bean의 기본 이름
- `@EnableConfigurationProperties(StreamixProperties.class)`로 등록된 Bean의 이름은:
  ```
  streamix-io.github.junhyeong9812.streamix.starter.properties.StreamixProperties
  ```
- 이 이름으로 Thymeleaf에서 `@...`로 접근 불가능 (SpEL parsing 깨짐)

### v1.0.5 → v1.0.6 history
- `refactor-log/README.md`에 기록: "1.0.5 → 1.0.6: Bean 충돌 + avgDurationFormatted 누락"
- alias 추가는 그 시기 임시 해결책

### 문제점
1. 같은 인스턴스를 두 이름으로 등록 → Spring 컨테이너에 중복
2. `@Primary` 필요 — 다른 Bean이 `StreamixProperties`를 주입받을 때 어떤 것 받을지 모호
3. 의도가 코드만 봐서는 안 보임 (Thymeleaf 접근 위해서라는 사실)

### 더 나은 해결책
**`@ConfigurationProperties`에 명시적 이름 지정** — Spring Boot 2.3+ 지원:
```java
@ConfigurationProperties(prefix = "streamix")
public record StreamixProperties(...) { }
```

→ `@Component` 또는 `@Bean`으로 등록 시 이름 지정 가능. 하지만 record + `@EnableConfigurationProperties` 조합에서는 직접 이름 부여가 까다로움.

**대안 1**: `@ConfigurationProperties` + `@Component("streamixProperties")` 조합
```java
@ConfigurationProperties(prefix = "streamix")
@Component("streamixProperties")
public record StreamixProperties(...) { }
```
- `@EnableConfigurationProperties` 제거 (`@ConfigurationPropertiesScan` 사용 또는 component scan 의존)
- starter는 사용자 패키지 외부이므로 `@ConfigurationPropertiesScan(basePackages = "io.github.junhyeong9812.streamix.starter.properties")` 추가 필요

**대안 2**: Thymeleaf의 다른 접근 방식
- `Model`에 매번 추가 — 컨트롤러마다 `model.addAttribute("streamixProperties", properties)` 호출 — 반복적
- `@ModelAttribute` 메서드 — 컨트롤러 advice에 `@ControllerAdvice` + `@ModelAttribute("streamixProperties")` — 깔끔

### 채택: 대안 2 — `@ControllerAdvice`
이유:
1. Bean 중복 등록 회피
2. 의도가 명확 (대시보드 컨트롤러용 모델 속성)
3. `@Primary` 제거 가능

### 절충 (현실적 권장): 현재 구조 유지 + 코드 명확화
- alias hack을 제거하면 마이그레이션 비용
- 사용자가 `streamixProperties` Bean 이름에 의존하는 코드를 작성했을 가능성
- → **alias 유지하되 명시적 javadoc + deprecate 표시**

## 변경 프로세스

### 옵션 비교
| 옵션 | 변경 | 마이그레이션 | 미래 변경 비용 |
|------|------|------------|------------|
| A. `@ControllerAdvice` 도입 + alias 제거 | 큼 | 사용자 영향 | 깔끔 |
| B. `@ConfigurationPropertiesScan` 도입 | 중 | 사용자 영향 적음 | 깔끔 |
| C. alias 유지 + javadoc 명시 | 작음 | 영향 없음 | 부채 유지 |

### 채택: 옵션 B — `@ConfigurationPropertiesScan` (절충)
이유:
1. Spring Boot 2.2+ 표준 방식
2. `@Primary` alias 제거 가능
3. Thymeleaf `@streamixProperties` 접근 그대로 동작 (Spring이 짧은 이름으로 등록)

### Step 1: `StreamixProperties`에 `@Component` 추가
주의: record + `@Component` + `@ConfigurationProperties`는 잘 동작하지만, `@EnableConfigurationProperties`와 충돌하지 않도록 주의.

```java
@ConfigurationProperties(prefix = "streamix")
public record StreamixProperties(
    Storage storage,
    Thumbnail thumbnail,
    Api api,
    Dashboard dashboard
) {
    // ...
}
```

→ record 자체에는 `@Component` 추가 안 함 (불변 객체에 어울리지 않음).

### Step 2: `@EnableConfigurationProperties` 유지하되 Bean 이름 지정
Spring Boot에서 `@EnableConfigurationProperties`로 등록 시 Bean 이름은 자동 생성. 명시 이름 부여는 별도의 `@Bean` 메서드를 통해 수동:

가장 안전한 방식 — **alias만 깔끔하게**:
```java
// StreamixRepositoryConfiguration
@Bean("streamixProperties")
public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
    return properties;
}
// @Primary 제거: 같은 인스턴스 반환이므로 Primary 불필요
```

`@Primary`를 제거하면 다른 곳에서 `@Autowired StreamixProperties` 시 두 Bean이 보여 모호 → `@Primary` 필요. 그래서 그대로 유지.

→ 결국 **현재 구조가 합리적인 절충**. 단지 javadoc과 명명을 개선:

### Step 3: 메서드 이름 개선
`streamixPropertiesAlias` → `streamixPropertiesNamedBean` (의도 명확화):
```java
/**
 * Thymeleaf 템플릿에서 {@code @streamixProperties}로 접근하기 위한
 * 명명된 Bean을 등록합니다.
 *
 * <p>{@code @ConfigurationProperties}로 등록된 기본 Bean의 이름은
 * 길고 복잡(예: {@code streamix-io.github...StreamixProperties})하여
 * Thymeleaf SpEL의 {@code @beanName} 문법으로 접근하기 어렵습니다.
 * 이 메서드는 동일 인스턴스를 짧은 이름으로 다시 노출합니다.</p>
 *
 * <p>{@code @Primary}를 사용하여 다른 Bean이 {@code StreamixProperties}를
 * 주입받을 때 이 Bean이 우선 선택되도록 합니다 (이름 모호성 회피).</p>
 *
 * @param properties {@code @EnableConfigurationProperties}로 등록된 인스턴스
 * @return 동일 인스턴스 (Bean 이름만 추가)
 */
@Bean("streamixProperties") @Primary
public StreamixProperties streamixPropertiesNamedBean(StreamixProperties properties) {
    return properties;
}
```

### Step 4 (별도 PR로 권장): 진정한 정리
v3.0.0에서 `@ControllerAdvice` 도입 + alias 제거 검토:
```java
@ControllerAdvice(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.in.dashboard")
public class StreamixDashboardModelAdvice {
    private final StreamixProperties properties;
    public StreamixDashboardModelAdvice(StreamixProperties properties) {
        this.properties = properties;
    }
    @ModelAttribute("streamixProperties")
    public StreamixProperties streamixProperties() {
        return properties;
    }
}
```
→ Thymeleaf에서 `${streamixProperties.api.basePath}` 접근 가능 (`@` 없이)
→ 템플릿 `${@streamixProperties.api.basePath}` → `${streamixProperties.api.basePath}` 갱신 필요
→ 사용자 영향 큼 — v3에서 검토

## Before / After

### Before
```java
/**
 * Streamix 설정 Properties Bean을 명시적 이름으로 등록합니다.
 *
 * <p>Thymeleaf 템플릿에서 {@code @streamixProperties}로 접근할 수 있도록
 * 'streamixProperties'라는 명시적인 Bean 이름으로 alias를 등록합니다.</p>
 * ...
 */
@Bean("streamixProperties")
@Primary
public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
    log.info("Registering StreamixProperties bean alias for Thymeleaf access");
    return properties;
}
```

### After (이번 작업 — 명명/javadoc 개선)
```java
/**
 * Thymeleaf 템플릿에서 {@code @streamixProperties}로 접근하기 위한
 * 명명된 Bean을 등록합니다.
 *
 * <p>{@code @ConfigurationProperties}로 등록된 기본 Bean의 이름은
 * Spring 내부 명명 규칙({@code streamix-io.github.junhyeong9812.streamix.starter.properties.StreamixProperties})에 따라
 * 길고 복잡합니다. Thymeleaf SpEL의 {@code @beanName} 접근 문법은 짧은 이름이 필요하므로
 * 이 메서드는 동일 인스턴스를 짧은 이름으로 다시 등록합니다.</p>
 *
 * <p>{@code @Primary}를 사용하여 다른 Bean이 {@code StreamixProperties}를
 * 주입받을 때 모호성 없이 이 Bean이 선택되도록 합니다.</p>
 *
 * <p><b>v3 마이그레이션 노트</b>: 향후 {@code @ControllerAdvice} +
 * {@code @ModelAttribute} 패턴으로 대체될 예정입니다.</p>
 *
 * @param properties {@code @EnableConfigurationProperties}로 등록된 원본 인스턴스
 * @return 동일 인스턴스 (Bean 이름 alias)
 */
@Bean("streamixProperties")
@Primary
public StreamixProperties streamixPropertiesNamedBean(StreamixProperties properties) {
    return properties;
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. Bean 등록 검증
```bash
./gradlew :streamix-spring-boot-starter:test
```

기존 `StreamixAutoConfigurationTest`는 영향 없음 (alias는 Repository Configuration에 있음).

### 3. Thymeleaf 접근 동작 확인 (수동)
대시보드 페이지 로드 후 사이드바의 링크 정상 표시 확인.

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixRepositoryConfiguration.java`

## 참고
- Spring Boot Reference §B.2 ConfigurationProperties Naming
- v3 검토 항목: `@ControllerAdvice` + `@ModelAttribute` 패턴
