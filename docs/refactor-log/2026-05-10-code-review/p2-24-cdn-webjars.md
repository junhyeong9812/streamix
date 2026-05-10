# [P2-24] Bootstrap CDN 의존 제거 — WebJars로 자체 호스팅

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 인터넷 차단 환경 동작 보장** |
| 카테고리 | 의존성 / 배포 환경 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `templates/streamix/layout.html`, `build.gradle` |

## 문제 분석

### 현재 동작
```html
<!-- layout.html line 11-13, 91 -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
```

### 문제

#### 결함 1: 인터넷 차단 환경에서 깨짐
- 사내 격리 네트워크 (금융, 의료, 정부)
- 컨테이너 환경에서 외부 트래픽 제한
- airgap 환경

→ 대시보드 페이지가 스타일/JS 없이 렌더링 (사실상 사용 불가)

#### 결함 2: CDN downtime 의존
- jsdelivr.net 장애 시 대시보드 깨짐
- DNS 차단/하이재킹 위험

#### 결함 3: 무결성 검증 부재
- `<link>` 태그에 `integrity` 속성 없음
- CDN compromised 시 악성 JS 로드 가능

#### 결함 4: 라이센스 명시
- jsdelivr 또는 cdn.jsdelivr.net의 SLA 불명확
- 라이브러리가 외부 의존을 명시적으로 만들지 않음

### 기대 동작
- starter JAR 안에 Bootstrap/Bootstrap Icons 포함 (WebJars)
- Spring Boot가 `/webjars/` 경로에 자동 매핑

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. WebJars 의존성 추가 | Maven 표준 | starter 자동 포함 | JAR 크기 ↑ (~250KB) |
| B. CDN + integrity 추가 + fallback | SRI 해시 | JAR 크기 유지 | 인터넷 차단 환경 미해결 |
| C. CDN 유지 + 사용자가 webjars 추가 안내 | docs만 | 변경 없음 | 사용자 부담 |

### 채택: 옵션 A (WebJars)
이유:
1. 라이브러리 자체가 모든 의존성 포함 — 사용자 경험 best
2. JAR 크기 250KB 증가는 미디어 스트리밍 라이브러리에서 무시 가능
3. Spring Boot의 webjars-locator가 버전 자동 해석 (URL에서 버전 생략 가능)

### Step 1: build.gradle에 WebJars 의존성 추가
```groovy
// streamix-spring-boot-starter/build.gradle
dependencies {
    // ... 기존 의존성

    // WebJars — 대시보드 UI (Bootstrap, Bootstrap Icons)
    implementation 'org.webjars:bootstrap:5.3.2'
    implementation 'org.webjars.npm:bootstrap-icons:1.11.1'
    implementation 'org.webjars:webjars-locator-lite:1.0.1'
}
```

`webjars-locator-lite`: Spring Boot 3+ 권장. URL에서 버전 생략 가능 (`/webjars/bootstrap/css/bootstrap.min.css` → 자동으로 5.3.2로 해석).

### Step 2: layout.html 변경
```html
<!-- Before -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

<!-- After (webjars-locator-lite 활용 — 버전 생략) -->
<link th:href="@{/webjars/bootstrap/css/bootstrap.min.css}" rel="stylesheet">
<link th:href="@{/webjars/bootstrap-icons/font/bootstrap-icons.css}" rel="stylesheet">
<script th:src="@{/webjars/bootstrap/js/bootstrap.bundle.min.js}"></script>
```

### Step 3: Spring Boot 자동 설정 검증
- spring-boot-starter-web이 이미 포함되어 있고, WebJars 자원이 클래스패스에 있으면 자동으로 `/webjars/**` 패턴이 ResourceHandler로 매핑됨
- 추가 설정 불필요

### Step 4: 라이센스 검토
- Bootstrap: MIT License
- Bootstrap Icons: MIT License
- WebJars Locator: MIT License

→ MIT 호환 (Streamix도 MIT)

### Step 5: JAR 크기 영향 측정
```bash
./gradlew :streamix-spring-boot-starter:bootJar
ls -la streamix-spring-boot-starter/build/libs/
```
- 변경 전 ~ 1MB
- 변경 후 ~ 1.25MB (수치는 추정)
- 라이브러리치고 여전히 작음

## Before / After

### Before — layout.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Streamix Dashboard</title>

  <!-- Bootstrap 5 CSS -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
  <!-- Bootstrap Icons -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
  <!-- Streamix Dashboard CSS -->
  <link th:href="@{/streamix/css/dashboard.css}" rel="stylesheet">
</head>
<body ...>
  ...
  <!-- Bootstrap JS -->
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
  <!-- Streamix Dashboard JS -->
  <script th:src="@{/streamix/js/dashboard.js}"></script>
</body>
</html>
```

### After — layout.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Streamix Dashboard</title>

  <!-- Bootstrap 5 CSS (WebJars — starter 내장) -->
  <link th:href="@{/webjars/bootstrap/css/bootstrap.min.css}" rel="stylesheet">
  <!-- Bootstrap Icons (WebJars) -->
  <link th:href="@{/webjars/bootstrap-icons/font/bootstrap-icons.css}" rel="stylesheet">
  <!-- Streamix Dashboard CSS -->
  <link th:href="@{/streamix/css/dashboard.css}" rel="stylesheet">
</head>
<body ...>
  ...
  <!-- Bootstrap JS (WebJars) -->
  <script th:src="@{/webjars/bootstrap/js/bootstrap.bundle.min.js}"></script>
  <!-- Streamix Dashboard JS -->
  <script th:src="@{/streamix/js/dashboard.js}"></script>
</body>
</html>
```

### Before — build.gradle
```groovy
dependencies {
    api project(':streamix-core')
    implementation "org.springframework.boot:spring-boot-starter:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-web:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-thymeleaf:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-validation:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-autoconfigure:${springBootVersion}"
    annotationProcessor "org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}"
    implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0'
    compileOnly 'org.postgresql:postgresql:42.7.4'
    compileOnly 'com.mysql:mysql-connector-j:9.1.0'
    testImplementation "org.springframework.boot:spring-boot-starter-test:${springBootVersion}"
    testRuntimeOnly 'com.h2database:h2:2.3.232'
}
```

### After — build.gradle
```groovy
dependencies {
    api project(':streamix-core')
    implementation "org.springframework.boot:spring-boot-starter:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-web:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-thymeleaf:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-starter-validation:${springBootVersion}"
    implementation "org.springframework.boot:spring-boot-autoconfigure:${springBootVersion}"
    annotationProcessor "org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}"
    implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0'

    // WebJars — Bootstrap UI (CDN 의존 제거)
    implementation 'org.webjars:bootstrap:5.3.2'
    implementation 'org.webjars.npm:bootstrap-icons:1.11.1'
    implementation 'org.webjars:webjars-locator-lite:1.0.1'

    compileOnly 'org.postgresql:postgresql:42.7.4'
    compileOnly 'com.mysql:mysql-connector-j:9.1.0'
    testImplementation "org.springframework.boot:spring-boot-starter-test:${springBootVersion}"
    testRuntimeOnly 'com.h2database:h2:2.3.232'
}
```

## 검증 방법

### 1. 의존성 확인
```bash
./gradlew :streamix-spring-boot-starter:dependencies --configuration runtimeClasspath | grep -i webjar
```

### 2. JAR 안에 webjars 포함 확인
```bash
./gradlew :streamix-spring-boot-starter:jar
unzip -l streamix-spring-boot-starter/build/libs/streamix-spring-boot-starter-*.jar | grep -i webjar
# META-INF/resources/webjars/bootstrap/...
```

### 3. 인터넷 차단 환경 시뮬레이션
```bash
# 사용자 앱 실행 후 네트워크 차단
# 브라우저 dev tools → Network → Block jsdelivr.net
# 대시보드 페이지 새로고침 → Bootstrap 정상 로드 확인
```

### 4. 회귀
```bash
./gradlew test
```

## 관련 파일
- `streamix-spring-boot-starter/build.gradle`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/layout.html`

## 참고
- [WebJars](https://www.webjars.org/)
- [webjars-locator-lite](https://github.com/webjars/webjars-locator-lite) — Spring Boot 3+ 권장
- Spring Boot WebMvcAutoConfiguration의 webjars 자동 매핑
- BootJar 크기 ~250KB 증가는 라이브러리 사용성 대비 합리적
