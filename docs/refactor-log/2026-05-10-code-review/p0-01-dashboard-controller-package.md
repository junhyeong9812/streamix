# [P0-01] StreamixDashboardController 디렉토리/패키지 불일치

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P0 — 빌드/런타임 깨짐** |
| 카테고리 | 빌드 / 모듈 구조 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixDashboardController.java`, `StreamixDashboardConfiguration.java` |

## 문제 분석

### 현재 동작
- 파일이 존재하는 디렉토리: `src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/web/`
- 파일 안의 `package` 선언:
  ```java
  package io.github.junhyeong9812.streamix.starter.adapter.in.dashboard;
  ```
- `StreamixDashboardConfiguration.java:6`에서도 동일하게 `.dashboard` 패키지로 import

### 기대 동작
JLS(Java Language Specification) §7.1: "package 선언이 source file의 디렉토리 구조와 일치해야 한다"는 강한 권장 사항.

빌드 도구별 동작:
- **javac 단독**: 패키지/디렉토리 불일치를 컴파일 에러로 처리하지 않음 (경고도 옵션). 클래스 파일은 `.dashboard` 패키지 이름으로 생성됨
- **Gradle/Maven**: javac에 위임하므로 컴파일 자체는 통과
- **Spring 클래스로딩 시점**: 클래스 파일이 `dashboard/` 디렉토리에 있다고 기대하지만 실제 jar 안에서는 `web/` 디렉토리에 위치 → ClassLoader 동작 방식에 따라 NoClassDefFoundError/ClassNotFoundException 가능
- **IDE (IntelliJ/Eclipse)**: 즉시 빨간 줄로 경고

### 원인 분석
v2.0.0 작업 중 패키지 분리를 의도(`.web` 컨트롤러와 `.dashboard` 컨트롤러 분리)했으나 실제 파일 이동을 완료하지 않은 것으로 추정. 또는 파일 이동 후 디렉토리 구조 갱신이 누락.

### 영향 범위
- **컴파일**: `./gradlew build` 환경에 따라 통과/실패 가변 (대부분의 환경에서 통과)
- **런타임**: Spring Boot가 `@Configuration`을 스캔할 때 ClassNotFoundException 가능
- **IDE 작업**: 자동완성/리팩토링 도구가 깨짐
- **JAR 배포**: 사용자가 jar를 받으면 클래스 위치가 일관성 없음

## 변경 프로세스

### 옵션 비교
| 옵션 | 변경 | 장점 | 단점 |
|------|------|------|------|
| A. 패키지 선언을 `.web`으로 변경 | 1 파일 1 줄 + import 1줄 | 최소 변경 | 기존 의도(분리) 무시 |
| B. 파일을 `dashboard/` 디렉토리로 이동 | 파일 이동 + 디렉토리 신설 | 의도 보존, 명확한 책임 분리 | git history 추적 |

### 채택: 옵션 B (디렉토리 분리)
이유:
1. v2.0.0 의도(`adapter.in.web` = REST API, `adapter.in.dashboard` = HTML 컨트롤러)를 보존
2. 향후 dashboard에 추가 클래스(서비스/DTO) 들어올 때 자연스러운 위치
3. `GlobalExceptionHandler`의 `@RestControllerAdvice(basePackages = "...adapter.in.web")` 범위와 dashboard 분리하면 — dashboard는 RESTful JSON이 아니라 HTML 응답이므로 advice 적용에서 제외되는 것이 맞음 (현재는 `.web` 안에 있어 적용됨)

### Step 1: 신규 디렉토리 생성
```bash
mkdir -p streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/dashboard
```

### Step 2: 파일 이동 (git mv 권장)
```bash
git mv streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/web/StreamixDashboardController.java \
       streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/dashboard/StreamixDashboardController.java
```

### Step 3: 파일 내용은 그대로 유지
이미 `package` 선언이 `.dashboard`로 되어 있으므로 파일 내부 수정 불필요.

### Step 4: `StreamixDashboardConfiguration.java`의 import 확인
이미 `io.github.junhyeong9812.streamix.starter.adapter.in.dashboard.StreamixDashboardController`로 import되어 있음 → 변경 불필요.

### Step 5: `GlobalExceptionHandler`의 영향 검토
```java
@RestControllerAdvice(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.in.web")
```
- dashboard controller가 web 패키지 밖으로 나가므로 advice 적용 제외됨
- dashboard는 `try/catch`로 RedirectAttributes에 errorMessage 추가하는 자체 처리가 있어 OK
- HTML 페이지 렌더링 중 발생하는 예외는 Spring Boot 기본 ErrorController로 위임됨 → 문제 없음

## Before / After

### Before — 디렉토리 위치
```
streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/
├── web/
│   ├── GlobalExceptionHandler.java       (package: ...adapter.in.web)
│   ├── StreamixApiController.java        (package: ...adapter.in.web)
│   ├── StreamixDashboardController.java  (package: ...adapter.in.dashboard) ⚠️ 불일치
│   └── dto/
```

### After — 디렉토리 위치
```
streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/
├── web/
│   ├── GlobalExceptionHandler.java       (package: ...adapter.in.web)
│   ├── StreamixApiController.java        (package: ...adapter.in.web)
│   └── dto/
└── dashboard/
    └── StreamixDashboardController.java  (package: ...adapter.in.dashboard) ✓
```

### 파일 내부 (변경 없음)
```java
// StreamixDashboardController.java line 1
package io.github.junhyeong9812.streamix.starter.adapter.in.dashboard;
```

```java
// StreamixDashboardConfiguration.java line 6 (변경 없음)
import io.github.junhyeong9812.streamix.starter.adapter.in.dashboard.StreamixDashboardController;
```

## 검증 방법

### 1. 컴파일 검증
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```
기대: BUILD SUCCESSFUL

### 2. 클래스 파일 위치 검증
```bash
find streamix-spring-boot-starter/build/classes -name "StreamixDashboardController.class"
```
기대 결과: `.../io/github/junhyeong9812/streamix/starter/adapter/in/dashboard/StreamixDashboardController.class`

### 3. 의존 클래스 컴파일 확인
```bash
./gradlew :streamix-spring-boot-starter:compileJava --rerun-tasks 2>&1 | grep -i "error\|warning"
```
기대: `StreamixDashboardController` 관련 경고/에러 없음

### 4. 테스트 실행
```bash
./gradlew :streamix-spring-boot-starter:test
```
기대: BUILD SUCCESSFUL (StreamixAutoConfigurationTest 통과)

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/web/StreamixDashboardController.java` (이동 대상)
- `streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/autoconfigure/StreamixDashboardConfiguration.java` (검증)

## 회고/메모
- IDE에서 작업했다면 즉시 발견됐을 이슈. CI에 IDE 수준 검사(`-Werror`, `-Xlint:all`) 추가 검토 필요 → P2 후속 개선 후보
