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
  <a href="#대시보드">대시보드</a> •
  <a href="#changelog">Changelog</a>
</p>

---

## Features

- 🎬 **HTTP Range 스트리밍**: 동영상 탐색(Seek) 지원
- 🖼️ **자동 썸네일 생성**: 이미지(Thumbnailator) / 비디오(FFmpeg)
- 📊 **대시보드 UI**: 파일 관리, 스트리밍 모니터링, 통계
- 🔧 **Spring Boot 자동 설정**: `@EnableStreamix` 하나로 모든 기능 활성화
- 🏗️ **헥사고날 아키텍처**: 확장 가능한 Port & Adapter 패턴
- 💾 **JPA 기반 메타데이터 저장**: PostgreSQL, MySQL, H2 지원
- 📁 **6가지 파일 타입 지원**: IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER
- 🛡️ **파일 검증**: 크기 제한, 타입 제한 설정 가능

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
    ├── autoconfigure/             # 자동 설정 클래스
    ├── adapter/                   # JPA, REST API, Dashboard, FFmpeg
    ├── service/                   # 모니터링 서비스
    └── templates/                 # Thymeleaf 대시보드 템플릿
```

## 설치

### Gradle

```groovy
dependencies {
    implementation 'io.github.junhyeong9812:streamix-spring-boot-starter:2.0.0'
    
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
    <version>2.0.0</version>
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
    max-file-size: 104857600    # 100MB (v2.0 신규)
    allowed-types: IMAGE,VIDEO  # 허용 타입 (v2.0 신규, 빈 값 = 전체 허용)
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
    base-path: ./data              # 파일 저장 경로
    max-file-size: 104857600       # 최대 파일 크기 (바이트, 기본 100MB)
    allowed-types:                 # 허용 파일 타입 (빈 값 = 전체 허용)

  # 썸네일 설정
  thumbnail:
    enabled: true                  # 썸네일 생성 활성화
    width: 320                     # 썸네일 너비
    height: 180                    # 썸네일 높이
    ffmpeg-path: ffmpeg            # FFmpeg 경로 (비디오 썸네일용)

  # REST API 설정
  api:
    enabled: true                  # API 활성화
    base-path: /api/streamix       # API 기본 경로

  # 대시보드 설정
  dashboard:
    enabled: true                  # 대시보드 활성화
    path: /streamix                # 대시보드 경로
```

### 파일 타입 (FileType)

| 타입 | 설명 | 예시 확장자 |
|------|------|------------|
| `IMAGE` | 이미지 파일 | jpg, png, gif, webp |
| `VIDEO` | 비디오 파일 | mp4, webm, avi, mkv |
| `AUDIO` | 오디오 파일 | mp3, wav, flac, aac |
| `DOCUMENT` | 문서 파일 | pdf, doc, xlsx, txt |
| `ARCHIVE` | 압축 파일 | zip, rar, 7z, tar.gz |
| `OTHER` | 기타 파일 | 그 외 모든 파일 |

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

**응답 (201 Created):**
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

**에러 응답 (413 Payload Too Large):**
```json
{
  "timestamp": "2025-12-31T10:30:00",
  "status": 413,
  "error": "Payload Too Large",
  "code": "FILE_SIZE_EXCEEDED",
  "message": "File size 150MB exceeds maximum allowed size 100MB",
  "path": "/api/streamix/files"
}
```

**에러 응답 (400 Bad Request):**
```json
{
  "timestamp": "2025-12-31T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_FILE_TYPE",
  "message": "File type ARCHIVE is not allowed. Allowed types: [IMAGE, VIDEO]",
  "path": "/api/streamix/files"
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
- 🎬 내장 비디오/오디오 플레이어
- 🖼️ 이미지 미리보기
- 📄 문서/압축파일 다운로드
- 📊 파일별/기간별 스트리밍 통계

### 파일 타입별 미리보기

| 타입 | 미리보기 방식 |
|------|--------------|
| VIDEO | 비디오 플레이어 (썸네일 포스터) |
| IMAGE | 이미지 뷰어 |
| AUDIO | 오디오 플레이어 |
| DOCUMENT | 다운로드/열기 버튼 |
| ARCHIVE | 다운로드 버튼 |
| OTHER | 다운로드 버튼 |

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

#### 커스텀 썸네일 생성기

```java
@Component
public class PdfThumbnailAdapter implements ThumbnailGeneratorPort {
    
    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.DOCUMENT;
    }
    
    @Override
    public int getOrder() {
        return 100;  // 낮을수록 높은 우선순위
    }
    
    @Override
    public String getName() {
        return "PdfThumbnailAdapter";
    }
    
    @Override
    public byte[] generateFromPath(String path, int width, int height) {
        // PDF 첫 페이지 이미지 추출 로직
    }
    
    @Override
    public byte[] generate(InputStream source, int width, int height) {
        // InputStream에서 썸네일 생성
    }
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
- [리팩토링 이력](docs/REFACTORING_LOG.md)
- [기본 개념](docs/concepts/)
    - [아키텍처](docs/concepts/architecture/README.md)
    - [스트리밍](docs/concepts/streaming/README.md)
    - [Java 25](docs/concepts/java/README.md)
    - [Spring Boot Starter](docs/concepts/spring-boot-starter/README.md)

## Changelog

### v2.0.0 (2025-12-31)

#### New Features
- **FileType 확장**: 6가지 파일 타입 지원 (IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER)
- **파일 검증**: 업로드 시 크기/타입 검증 (`max-file-size`, `allowed-types`)
- **FileDeleteService**: 파일 삭제 전용 서비스 (SRP 분리)
- **ThumbnailGeneratorPort 확장**: 우선순위(`getOrder()`) 및 이름(`getName()`) 지원

#### Dashboard
- 6가지 FileType별 아이콘 및 Badge 색상
- 파일 타입별 미리보기 (비디오/이미지/오디오 플레이어)
- 업로드 모달에서 허용 타입 안내

#### Breaking Changes
- `FileMetadataService`에서 삭제 기능 제거 → `FileDeleteService` 사용
- `ThumbnailGeneratorPort`에 `getOrder()`, `getName()` default 메서드 추가

### v1.0.6 (2025-12-14)
- Bean 충돌 해결 (`@Primary` alias Bean)
- `avgDurationFormatted` 메서드 추가

### v1.0.0 (2025-12-14)
- 최초 릴리스
- HTTP Range 스트리밍
- 이미지/비디오 썸네일 자동 생성
- 대시보드 UI
- JPA 기반 메타데이터 저장

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
  Made by <a href="https://github.com/junhyeong9812">junhyeong9812</a>
</p>