# 구현 계획

## 개요

Streamix는 `@EnableStreamix` 어노테이션으로 활성화되는 미디어 파일 스트리밍 라이브러리입니다.

헥사고날 아키텍처를 적용하여 Core 모듈은 Spring 의존성 없이 순수 Java로 구현하고, Spring Boot Starter 모듈에서 자동 설정을 제공합니다.

---

## 모듈 구조

```
streamix/
├── streamix-core/                    # 순수 Java (Spring 의존성 없음)
│   ├── domain/                       # 도메인 모델
│   │   ├── model/
│   │   ├── exception/
│   │   └── service/
│   ├── application/                  # 유스케이스
│   │   ├── port/in/                  # Input Ports (인터페이스)
│   │   ├── port/out/                 # Output Ports (인터페이스)
│   │   └── service/                  # 유스케이스 구현체
│   └── adapter/out/                  # 기본 어댑터 구현
│       ├── storage/
│       ├── metadata/
│       └── thumbnail/
│
└── streamix-spring-boot-starter/     # Spring Boot 자동 설정
    ├── annotation/                   # @EnableStreamix
    ├── autoconfigure/                # 자동 설정 클래스
    ├── properties/                   # 설정 프로퍼티
    ├── adapter/in/web/               # REST Controller
    ├── adapter/out/persistence/      # JPA 어댑터
    └── client/                       # StreamixClient
```

---

## 핵심 컴포넌트

### Domain Model

| 클래스 | 설명 |
|--------|------|
| `FileMetadata` | 파일 메타데이터 (id, name, type, size, path 등) |
| `FileType` | 파일 타입 enum (IMAGE, VIDEO) |
| `StreamableFile` | 스트리밍 가능한 파일 (InputStream + 메타데이터) |

### Input Ports (Use Cases)

| 인터페이스 | 설명 |
|------------|------|
| `UploadFileUseCase` | 파일 업로드 |
| `StreamFileUseCase` | 파일 스트리밍 (Range 지원) |
| `GetThumbnailUseCase` | 썸네일 조회 |
| `GetFileMetadataUseCase` | 메타데이터 조회 |
| `DeleteFileUseCase` | 파일 삭제 |

### Output Ports (SPI)

| 인터페이스 | 설명 |
|------------|------|
| `FileStoragePort` | 파일 저장소 (로컬, S3 등) |
| `FileMetadataPort` | 메타데이터 저장소 (JPA, MongoDB 등) |
| `ThumbnailGeneratorPort` | 썸네일 생성기 |

---

## 구현 순서

### Phase 1: 기반 구조 (streamix-core)

1. 도메인 모델 정의
    - [ ] `FileMetadata` (record)
    - [ ] `FileType` (enum)
    - [ ] `StreamableFile`
    - [ ] `UploadResult`

2. 예외 클래스 정의
    - [ ] `StreamixException`
    - [ ] `FileNotFoundException`
    - [ ] `InvalidFileTypeException`
    - [ ] `StorageException`

3. Port 인터페이스 정의
    - [ ] Input Ports (Use Cases)
    - [ ] Output Ports (SPI)

### Phase 2: Core 서비스 구현

1. 서비스 구현
    - [ ] `FileUploadService`
    - [ ] `FileStreamService`
    - [ ] `ThumbnailService`

2. 기본 어댑터 구현
    - [ ] `LocalFileStorageAdapter`
    - [ ] `InMemoryMetadataAdapter`
    - [ ] `ImageThumbnailAdapter`

### Phase 3: Spring Boot Starter

1. 자동 설정
    - [ ] `@EnableStreamix` 어노테이션
    - [ ] `StreamixAutoConfiguration`
    - [ ] `StreamixProperties`

2. Web 어댑터
    - [ ] `StreamixController`
    - [ ] DTO 클래스들
    - [ ] `StreamixExceptionHandler`

3. JPA 어댑터
    - [ ] `FileMetadataEntity`
    - [ ] `FileMetadataRepository`
    - [ ] `JpaFileMetadataAdapter`

4. Client
    - [ ] `StreamixClient` 인터페이스
    - [ ] `StreamixClientImpl`

### Phase 4: 테스트

- [ ] 단위 테스트
- [ ] 통합 테스트

---

## API 설계

### 파일 업로드

```
POST /api/streamix/files
Content-Type: multipart/form-data

Response:
{
  "id": "uuid",
  "originalName": "video.mp4",
  "type": "VIDEO",
  "contentType": "video/mp4",
  "size": 15728640,
  "url": "/api/streamix/files/{id}/stream",
  "thumbnailUrl": "/api/streamix/files/{id}/thumbnail",
  "createdAt": "2025-12-13T10:30:00"
}
```

### 파일 스트리밍

```
GET /api/streamix/files/{id}/stream
Range: bytes=0-1023 (선택)

Response:
- 200 OK (전체)
- 206 Partial Content (Range 요청)
```

### 썸네일 조회

```
GET /api/streamix/files/{id}/thumbnail

Response: image/jpeg
```

---

## 데이터베이스 스키마

```sql
CREATE TABLE streamix_file_metadata (
    id              UUID PRIMARY KEY,
    original_name   VARCHAR(255) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size            BIGINT NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    thumbnail_path  VARCHAR(500),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_streamix_type ON streamix_file_metadata(type);
CREATE INDEX idx_streamix_created ON streamix_file_metadata(created_at DESC);
```

---

## 설정 옵션

```yaml
streamix:
  storage:
    type: local              # local | s3 | custom
    base-path: ./data        # 파일 저장 경로
  
  thumbnail:
    enabled: true            # 썸네일 생성 여부
    width: 320
    height: 180
  
  streaming:
    chunk-size: 1MB          # 스트리밍 청크 크기
    buffer-size: 8KB
  
  api:
    enabled: true            # REST API 활성화
    base-path: /api/streamix
  
  metadata:
    type: jpa                # jpa | memory
```