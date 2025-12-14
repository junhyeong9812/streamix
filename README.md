# Streamix

<p align="center">
  <strong>@EnableStreamix 어노테이션으로 활성화하는 미디어 파일 스트리밍 서버 라이브러리</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#설치">설치</a> •
  <a href="#빠른-시작">빠른 시작</a> •
  <a href="#설정">설정</a> •
  <a href="#api-reference">API</a> •
  <a href="#대시보드">대시보드</a>
</p>

---

## Features

- 🎬 **HTTP Range 스트리밍**: 동영상 탐색(Seek) 지원
- 🖼️ **자동 썸네일 생성**: 이미지(Thumbnailator) / 비디오(FFmpeg)
- 📊 **대시보드 UI**: 파일 관리, 스트리밍 모니터링, 통계
- 🔧 **Spring Boot 자동 설정**: `@EnableStreamix` 하나로 모든 기능 활성화
- 🏗️ **헥사고날 아키텍처**: 확장 가능한 Port & Adapter 패턴
- 💾 **JPA 기반 메타데이터 저장**: PostgreSQL, MySQL, H2 지원

## 기술 스택

| 기술 | 버전 |
|------|------|
| Java | 25 |
| Spring Boot | 4.0 |
| Spring Framework | 7.0 |
| Spring Data JPA | 4.0 |
| Thymeleaf | 3.1 |
| Thumbnailator | 0.4.20 |
| FFmpeg | 6.x (선택) |

## 모듈 구조

```
streamix/
├── streamix-core/                 # 핵심 도메인 (순수 Java)
│   ├── domain/                    # 도메인 모델 (FileMetadata, FileType 등)
│   ├── application/               # 유스케이스 & 포트
│   └── adapter/                   # 기본 어댑터 (Local Storage, Image Thumbnail)
│
└── streamix-spring-boot-starter/  # Spring Boot 자동 설정
    ├── autoconfigure/             # 6개의 자동 설정 클래스
    ├── adapter/                   # JPA, REST API, Dashboard, FFmpeg
    ├── service/                   # 모니터링 서비스
    └── templates/                 # Thymeleaf 대시보드 템플릿
```

## 설치

### Gradle

```groovy
dependencies {
    implementation 'io.github.junhyeong9812:streamix-spring-boot-starter:1.0.0'
    
    // 데이터베이스 드라이버 (선택)
    runtimeOnly 'org.postgresql:postgresql:42.7.4'
    // 또는
    runtimeOnly 'com.h2database:h2:2.3.232'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.junhyeong9812</groupId>
    <artifactId>streamix-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 빠른 시작

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
spring:
  datasource:
    url: jdbc:h2:mem:streamix
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

streamix:
  storage:
    base-path: ./data
  thumbnail:
    enabled: true
    width: 320
    height: 180
```

### 3. 실행

```bash
./gradlew bootRun
```

- **REST API**: http://localhost:8080/api/streamix/files
- **대시보드**: http://localhost:8080/streamix

## 설정

### 전체 설정 옵션

```yaml
streamix:
  # 저장소 설정
  storage:
    base-path: ./data          # 파일 저장 경로

  # 썸네일 설정
  thumbnail:
    enabled: true              # 썸네일 생성 활성화
    width: 320                 # 썸네일 너비
    height: 180                # 썸네일 높이
    ffmpeg-path: ffmpeg        # FFmpeg 경로 (비디오 썸네일용)

  # REST API 설정
  api:
    enabled: true              # API 활성화
    base-path: /api/streamix   # API 기본 경로

  # 대시보드 설정
  dashboard:
    enabled: true              # 대시보드 활성화
    path: /streamix            # 대시보드 경로
```

### FFmpeg 설치 (비디오 썸네일용)

```bash
# Ubuntu/Debian
sudo apt install ffmpeg

# macOS
brew install ffmpeg

# Windows
# https://ffmpeg.org/download.html 에서 다운로드
```

## API Reference

### 파일 업로드

```bash
POST /api/streamix/files
Content-Type: multipart/form-data

# cURL 예시
curl -X POST http://localhost:8080/api/streamix/files \
  -F "file=@video.mp4"
```

**응답:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "originalName": "video.mp4",
  "type": "VIDEO",
  "contentType": "video/mp4",
  "size": 15728640,
  "thumbnailGenerated": true,
  "streamUrl": "/api/streamix/files/550e.../stream",
  "thumbnailUrl": "/api/streamix/files/550e.../thumbnail"
}
```

### 파일 목록

```bash
GET /api/streamix/files?page=0&size=10
```

**응답:**
```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

### 파일 스트리밍

```bash
# 전체 파일
GET /api/streamix/files/{id}/stream

# Range 요청 (동영상 탐색)
GET /api/streamix/files/{id}/stream
Range: bytes=0-1023
```

**응답:**
- 전체: `200 OK`
- Range: `206 Partial Content` + `Content-Range` 헤더

### 썸네일 조회

```bash
GET /api/streamix/files/{id}/thumbnail
```

**응답:** `image/jpeg`

### 파일 정보

```bash
GET /api/streamix/files/{id}
```

### 파일 삭제

```bash
DELETE /api/streamix/files/{id}
```

## 대시보드

| 페이지 | 경로 | 설명 |
|--------|------|------|
| 메인 | `/streamix` | 통계 요약, 최근 파일, 활성 세션 |
| 파일 목록 | `/streamix/files` | 파일 관리 (목록, 삭제) |
| 파일 상세 | `/streamix/files/{id}` | 파일 정보, 플레이어, 통계 |
| 세션 목록 | `/streamix/sessions` | 스트리밍 세션 모니터링 |

### 대시보드 기능

- 📈 실시간 통계 (활성 세션, 오늘 스트리밍, 전송량)
- 📁 파일 관리 (업로드, 삭제, 상세 보기)
- 🎬 내장 비디오 플레이어
- 📊 파일별/기간별 스트리밍 통계

## 아키텍처

### 자동 설정 클래스

| 클래스 | 조건 | 등록 Bean |
|--------|------|----------|
| `StreamixAutoConfiguration` | 항상 | Core 서비스, 기본 어댑터 |
| `StreamixJpaConfiguration` | JPA + DataSource | JPA 어댑터 |
| `StreamixWebConfiguration` | Servlet Web | REST 컨트롤러 |
| `StreamixThumbnailConfiguration` | 항상 | FFmpeg 어댑터 |
| `StreamixMonitoringConfiguration` | JPA + DataSource | 모니터링 서비스 |
| `StreamixDashboardConfiguration` | Thymeleaf + JPA | 대시보드 컨트롤러 |

### 확장 포인트

#### 커스텀 저장소

```java
@Configuration
public class S3StorageConfig {
    
    @Bean
    public FileStoragePort fileStoragePort(AmazonS3 s3Client) {
        return new S3FileStorageAdapter(s3Client, "my-bucket");
    }
}
```

#### 커스텀 메타데이터 저장소

```java
@Bean
public FileMetadataPort fileMetadataPort(MongoTemplate mongoTemplate) {
    return new MongoFileMetadataAdapter(mongoTemplate);
}
```

## 빌드

```bash
# 전체 빌드
./gradlew build

# 테스트
./gradlew test

# JAR 생성
./gradlew :streamix-spring-boot-starter:bootJar
```

## 테스트

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :streamix-core:test
./gradlew :streamix-spring-boot-starter:test

# 테스트 리포트
open build/reports/tests/test/index.html
```

## 문서

- [구현 계획](docs/implement/README.md)
- [기본 개념](docs/concepts/)
    - [아키텍처](docs/concepts/architecture/README.md)
    - [스트리밍](docs/concepts/streaming/README.md)
    - [Java 25](docs/concepts/java/README.md)
    - [Spring Boot Starter](docs/concepts/spring-boot-starter/README.md)

## 요구 사항

- JDK 25+
- Spring Boot 4.0+
- PostgreSQL 16+ / MySQL 8+ / H2 (개발용)
- FFmpeg 6+ (비디오 썸네일, 선택)

## 라이센스

MIT License - [LICENSE](LICENSE) 파일 참조

## 기여

이슈와 PR을 환영합니다!

1. Fork
2. Feature branch (`git checkout -b feature/amazing`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing`)
5. Pull Request

---

<p align="center">
  Made with by <a href="https://github.com/junhyeong9812">junhyeong9812</a>
</p>