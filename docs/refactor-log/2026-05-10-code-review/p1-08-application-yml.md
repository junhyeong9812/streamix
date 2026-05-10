# [P1-08] starter측 application.yml이 사용자 설정 오염

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — 사용자 환경 강제 변경** |
| 카테고리 | 라이브러리 설계 / Spring Boot |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `src/main/resources/application.yml` |

## 문제 분석

### 현재 동작
```yaml
# streamix-spring-boot-starter/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:file:./data/streamix
  jpa:
    hibernate:
      ddl-auto: update

streamix:
  storage:
    base-path: ./uploads
  thumbnail:
    ffmpeg-path: /usr/bin/ffmpeg
```

이 파일은 **starter JAR 안에 포함**되어 사용자 클래스패스에 들어감.

### Spring Boot 설정 병합 동작
Spring Boot의 `ConfigDataLocationResolver`는 클래스패스의 `application.yml`을 발견하면 사용자의 `application.yml`과 **병합**한다 (사용자 값이 우선이지만 사용자가 설정 안 한 키는 starter 값이 적용됨).

**결과**:
- 사용자가 `spring.datasource.url`을 설정 안 하면 **자동으로 H2 file 모드** (`jdbc:h2:file:./data/streamix`)
- 사용자가 `spring.jpa.hibernate.ddl-auto`를 설정 안 하면 **자동으로 update** (프로덕션 위험!)
- `streamix.thumbnail.ffmpeg-path`가 `/usr/bin/ffmpeg`로 강제 — Linux 외 OS에서 실패

### 문제의 심각성

#### 케이스 1: 사용자가 PostgreSQL 사용 의도
```yaml
# 사용자 application.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/myapp
    username: ${DB_USER}
    password: ${DB_PASS}
```
→ 정상 동작. 사용자 설정 우선.

#### 케이스 2: 사용자가 datasource 설정 깜빡함
```yaml
# 사용자 application.yml (datasource 미설정)
streamix:
  api:
    enabled: true
```
→ Streamix가 자동으로 H2 파일 모드로 fallback. 사용자가 의도하지 않은 DB 생성. ddl-auto=update로 스키마 자동 생성.

#### 케이스 3: 운영 배포 시
사용자가 PostgreSQL을 의도했지만 환경변수 누락으로 datasource URL이 비어있는 상황. Streamix가 H2 file mode로 fallback → "왜 데이터가 사라지지?" 디버깅 시간 낭비.

#### 케이스 4: ffmpeg-path
- macOS: `/opt/homebrew/bin/ffmpeg` 또는 PATH의 `ffmpeg`
- Windows: `ffmpeg.exe` PATH
- 컨테이너: `/usr/bin/ffmpeg` 또는 미설치
- starter가 `/usr/bin/ffmpeg`를 기본값으로 강제 → Linux 외 환경에서 video 썸네일 실패

### 기대 동작
**Spring Boot 라이브러리/starter 모범 사례** (Spring Boot Reference §B.1):
- starter는 `application.yml`/`application.properties`를 포함하지 않는다
- 기본값은 `@ConfigurationProperties` 클래스의 default field 또는 `@ConditionalOnProperty(matchIfMissing=true)` 형태로 코드에 표현한다
- 메타데이터는 `META-INF/spring-configuration-metadata.json` (자동 생성) + `additional-spring-configuration-metadata.json` (수동)에 둔다 — IDE 지원용

### 원인 분석
- 개발 중 starter 모듈을 단독으로 실행하기 위해 application.yml 추가
- 모듈을 라이브러리로 publish할 때 이 파일 제외 처리 누락
- StreamixProperties의 compact constructor에 이미 default가 있으므로 application.yml 불필요

## 변경 프로세스

### Step 1: application.yml 삭제
```bash
rm streamix-spring-boot-starter/src/main/resources/application.yml
```

### Step 2: 코드 default 검증
이미 `StreamixProperties.Storage`의 compact constructor에 default 있음:
```java
public Storage {
    basePath = basePath != null && !basePath.isBlank() ? basePath : "./streamix-data";
    maxFileSize = maxFileSize > 0 ? maxFileSize : 104857600L;  // 100MB
    allowedTypes = allowedTypes != null ? Set.copyOf(allowedTypes) : Set.of();
}

public Thumbnail {
    width = width > 0 ? width : 320;
    height = height > 0 ? height : 180;
    ffmpegPath = ffmpegPath != null && !ffmpegPath.isBlank() ? ffmpegPath : "ffmpeg";
}
```
→ 추가 변경 불필요 (`ffmpeg`는 PATH에서 찾는 표준 default)

### Step 3: 사용자용 예시 application.yml 위치 변경
사용자가 참고할 예시는 README에 두거나 `docs/`에 별도 파일로:
- `docs/examples/application-h2-dev.yml`
- `docs/examples/application-postgres-prod.yml`

이 파일들은 jar에 포함되지 않아 사용자 설정 오염 없음.

### Step 4: README의 빠른 시작 섹션 업데이트
README "빠른 시작" 섹션은 이미 사용자가 application.yml에 명시하도록 안내하고 있어 변경 불필요. 다만 v2.0.1 changelog에 "starter는 더 이상 application.yml을 포함하지 않음"을 명시.

### Step 5: spring-boot-configuration-processor가 자동 생성하는 메타데이터 활용
`build.gradle`에 이미 있음:
```groovy
annotationProcessor "org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}"
```
→ `StreamixProperties`의 javadoc과 default 값으로부터 IDE 자동완성 메타데이터 생성됨 ✓

(선택) `additional-spring-configuration-metadata.json`을 추가하여 더 풍부한 IDE 힌트 제공:
```
streamix-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json
```

## Before / After

### Before — 디렉토리
```
streamix-spring-boot-starter/src/main/resources/
├── application.yml          ⚠ 삭제 대상
├── static/streamix/...
└── templates/streamix/...
```

### After — 디렉토리
```
streamix-spring-boot-starter/src/main/resources/
├── static/streamix/...
└── templates/streamix/...
```

### Before — 사용자가 datasource 미설정 시
- Streamix가 자동으로 H2 file mode로 동작
- ddl-auto=update 강제 적용
- ffmpeg-path가 /usr/bin/ffmpeg 강제

### After — 사용자가 datasource 미설정 시
- Spring Boot가 H2 in-memory 자동 구성 (Spring Boot 표준 default)
- ddl-auto는 사용자 설정 또는 Spring 기본 (none)
- ffmpeg는 PATH에서 찾기

### 사용자 마이그레이션 가이드 (v2.0.0 → v2.0.1)
이전에 starter의 application.yml에 의존하던 사용자라면, 자신의 application.yml에 다음을 추가:
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/streamix      # 기존 동작 유지하려면
  jpa:
    hibernate:
      ddl-auto: update                       # 기존 동작 유지하려면

streamix:
  storage:
    base-path: ./uploads                     # 기존 동작 유지하려면
  thumbnail:
    ffmpeg-path: /usr/bin/ffmpeg             # 기존 동작 유지하려면
```

## 검증 방법

### 1. 파일 삭제 확인
```bash
ls streamix-spring-boot-starter/src/main/resources/application.yml 2>&1
# 결과: No such file
```

### 2. JAR 검증
```bash
./gradlew :streamix-spring-boot-starter:jar
unzip -l streamix-spring-boot-starter/build/libs/streamix-spring-boot-starter-*.jar | grep application
# 결과: application.yml 미포함 확인
```

### 3. 통합 테스트 — 사용자 설정 우선 검증
```java
@Test
void userDataSourceUrl_takesEffect() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StreamixAutoConfiguration.class))
        .withPropertyValues("spring.datasource.url=jdbc:h2:mem:test")
        .run(context -> {
            DataSource ds = context.getBean(DataSource.class);
            // ds URL이 jdbc:h2:mem:test (사용자 설정) 사용 확인
        });
}
```

### 4. 기존 테스트 회귀
```bash
./gradlew :streamix-spring-boot-starter:test
```
- `StreamixAutoConfigurationTest`는 사용자 설정 우선이므로 영향 없음

## 관련 파일
- `streamix-spring-boot-starter/src/main/resources/application.yml` ❌ 삭제

## 참고
- Spring Boot Reference §2.1 Starters: "Starter POMs should not include `application.yml`"
- Spring Boot ConfigDataEnvironment 병합 우선순위: 사용자 application.yml > library application.yml > @ConfigurationProperties default
- Sonatype guideline: 라이브러리 jar는 사용자가 명시적으로 옵트인하지 않은 설정을 강제하지 않아야 함
