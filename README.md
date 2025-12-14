# Streamix

`@EnableStreamix` 어노테이션으로 활성화하는 미디어 파일 스트리밍 서버 라이브러리

## 기술 스택

- Java 25
- Spring Boot 4.0
- Spring Framework 7.0
- PostgreSQL

## 모듈 구조

```
streamix/
├── streamix-core/                 # 핵심 도메인 (순수 Java, Spring 의존성 없음)
└── streamix-spring-boot-starter/  # Spring Boot 자동 설정
```

## 설치

### Gradle

```groovy
implementation 'io.github.junhyeong9812:streamix-spring-boot-starter:1.0.0'
```

### Maven

```xml
<dependency>
    <groupId>io.github.junhyeong9812</groupId>
    <artifactId>streamix-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 사용법

### 1. 어노테이션 활성화

```java
@SpringBootApplication
@EnableStreamix
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 2. 설정 (application.yml)

```yaml
streamix:
  storage:
    type: local
    base-path: ./data
  thumbnail:
    enabled: true
    width: 320
    height: 180
  api:
    base-path: /api/streamix
```

### 3. API 사용

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/streamix/files` | 파일 업로드 |
| GET | `/api/streamix/files/{id}` | 메타데이터 조회 |
| GET | `/api/streamix/files/{id}/stream` | 파일 스트리밍 |
| GET | `/api/streamix/files/{id}/thumbnail` | 썸네일 조회 |
| DELETE | `/api/streamix/files/{id}` | 파일 삭제 |

## 빌드

```bash
./gradlew build
```

## 테스트

```bash
./gradlew test
```

## 문서

- [구현 계획](docs/implement/README.md)
- [기본 개념](docs/concepts/)

## 라이센스

MIT License