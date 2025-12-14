# Streamix Spring Boot Starter 구현 설계

## 1. 개요

### 1.1 목표
`@EnableStreamix` 어노테이션 하나로 미디어 파일 서버를 즉시 구동할 수 있는 Spring Boot Starter 구현

### 1.2 핵심 기능
| 기능 | 설명 |
|------|------|
| **파일 업로드** | 이미지/비디오 업로드 및 메타데이터 DB 저장 |
| **스트리밍** | Range 요청 지원하는 HTTP 스트리밍 |
| **썸네일 자동 생성** | 이미지(Thumbnailator) + 비디오(FFmpeg) |
| **REST API** | 자동 생성되는 파일 관리 엔드포인트 |
| **관리 대시보드** | 파일 목록, 미리보기, 스트리밍 모니터링 웹 UI |

### 1.3 사용 예시
```java
@SpringBootApplication
@EnableStreamix
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

```yaml
streamix:
  storage:
    base-path: ./uploads
  thumbnail:
    enabled: true
    ffmpeg-path: /usr/bin/ffmpeg  # 비디오 썸네일용
  dashboard:
    enabled: true
    path: /streamix
```

---

## 2. 모듈 구조

```
streamix-spring-boot-starter/
├── src/main/java/.../streamix/starter/
│   │
│   ├── annotation/
│   │   └── EnableStreamix.java              # 활성화 어노테이션
│   │
│   ├── autoconfigure/
│   │   ├── StreamixAutoConfiguration.java   # 메인 자동 설정
│   │   ├── StreamixWebConfiguration.java    # REST API 설정
│   │   ├── StreamixDashboardConfiguration.java  # 대시보드 설정
│   │   └── ConditionalOnStreamixEnabled.java
│   │
│   ├── properties/
│   │   └── StreamixProperties.java          # 설정 프로퍼티 (Record)
│   │
│   ├── adapter/
│   │   ├── in/web/
│   │   │   ├── StreamixController.java      # REST API
│   │   │   ├── StreamixDashboardController.java  # 대시보드 뷰
│   │   │   ├── dto/
│   │   │   │   ├── UploadResponse.java
│   │   │   │   ├── FileInfoResponse.java
│   │   │   │   ├── PagedResponse.java
│   │   │   │   └── StreamingStatsResponse.java
│   │   │   └── exception/
│   │   │       └── StreamixExceptionHandler.java
│   │   │
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── FileMetadataEntity.java
│   │       │   ├── StreamingLogEntity.java  # 스트리밍 로그
│   │       │   ├── FileMetadataJpaRepository.java
│   │       │   ├── StreamingLogJpaRepository.java
│   │       │   └── JpaFileMetadataAdapter.java
│   │       │
│   │       └── thumbnail/
│   │           └── FFmpegThumbnailAdapter.java  # 비디오 썸네일
│   │
│   ├── service/
│   │   └── StreamingMonitorService.java     # 스트리밍 통계
│   │
│   └── client/
│       ├── StreamixClient.java              # 프로그래밍 방식 접근
│       └── StreamixClientImpl.java
│
├── src/main/resources/
│   ├── META-INF/spring/
│   │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   ├── templates/streamix/                  # Thymeleaf 템플릿
│   │   ├── dashboard.html                   # 메인 대시보드
│   │   ├── files.html                       # 파일 목록
│   │   └── preview.html                     # 미리보기
│   └── static/streamix/                     # 정적 리소스
│       ├── css/dashboard.css
│       └── js/dashboard.js
│
└── build.gradle
```

---

## 3. 핵심 컴포넌트 설계

### 3.1 @EnableStreamix 어노테이션

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(StreamixAutoConfiguration.class)
public @interface EnableStreamix {
    
    /**
     * REST API 활성화 여부 (기본: true)
     */
    boolean enableApi() default true;
    
    /**
     * 대시보드 활성화 여부 (기본: true)
     */
    boolean enableDashboard() default true;
}
```

### 3.2 StreamixProperties (Record)

```java
@ConfigurationProperties(prefix = "streamix")
public record StreamixProperties(
    Storage storage,
    Thumbnail thumbnail,
    Api api,
    Dashboard dashboard,
    Streaming streaming
) {
    public record Storage(
        String basePath,           // 파일 저장 경로 (기본: ./streamix-data)
        long maxFileSize           // 최대 파일 크기 (기본: 100MB)
    ) {
        public Storage {
            basePath = basePath != null ? basePath : "./streamix-data";
            maxFileSize = maxFileSize > 0 ? maxFileSize : 104857600L;
        }
    }
    
    public record Thumbnail(
        boolean enabled,           // 썸네일 생성 여부 (기본: true)
        int width,                 // 썸네일 너비 (기본: 320)
        int height,                // 썸네일 높이 (기본: 180)
        String ffmpegPath,         // FFmpeg 경로 (기본: ffmpeg)
        int videoFrameSeconds      // 비디오 프레임 추출 시점 (기본: 1초)
    ) {
        public Thumbnail {
            width = width > 0 ? width : 320;
            height = height > 0 ? height : 180;
            ffmpegPath = ffmpegPath != null ? ffmpegPath : "ffmpeg";
            videoFrameSeconds = videoFrameSeconds > 0 ? videoFrameSeconds : 1;
        }
    }
    
    public record Api(
        boolean enabled,           // API 활성화 (기본: true)
        String basePath            // API 기본 경로 (기본: /api/streamix)
    ) {
        public Api {
            basePath = basePath != null ? basePath : "/api/streamix";
        }
    }
    
    public record Dashboard(
        boolean enabled,           // 대시보드 활성화 (기본: true)
        String path,               // 대시보드 경로 (기본: /streamix)
        String username,           // 인증 사용자명 (선택)
        String password            // 인증 비밀번호 (선택)
    ) {
        public Dashboard {
            path = path != null ? path : "/streamix";
        }
    }
    
    public record Streaming(
        int chunkSize,             // 스트리밍 청크 크기 (기본: 1MB)
        int bufferSize,            // 버퍼 크기 (기본: 8KB)
        boolean logEnabled         // 스트리밍 로그 활성화 (기본: true)
    ) {
        public Streaming {
            chunkSize = chunkSize > 0 ? chunkSize : 1048576;
            bufferSize = bufferSize > 0 ? bufferSize : 8192;
        }
    }
}
```

### 3.3 JPA Entity 설계

#### FileMetadataEntity
```java
@Entity
@Table(name = "streamix_file_metadata", indexes = {
    @Index(name = "idx_streamix_type", columnList = "type"),
    @Index(name = "idx_streamix_created", columnList = "createdAt DESC")
})
public class FileMetadataEntity {
    
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String originalName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType type;
    
    @Column(nullable = false, length = 100)
    private String contentType;
    
    @Column(nullable = false)
    private Long size;
    
    @Column(nullable = false, length = 500)
    private String storagePath;
    
    @Column(length = 500)
    private String thumbnailPath;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Domain 변환 메서드
    public static FileMetadataEntity from(FileMetadata domain) { ... }
    public FileMetadata toDomain() { ... }
}
```

#### StreamingLogEntity (모니터링용)
```java
@Entity
@Table(name = "streamix_streaming_log", indexes = {
    @Index(name = "idx_streaming_file", columnList = "fileId"),
    @Index(name = "idx_streaming_time", columnList = "startedAt DESC")
})
public class StreamingLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID fileId;
    
    @Column(length = 50)
    private String clientIp;
    
    @Column(length = 200)
    private String userAgent;
    
    private Long bytesStart;       // Range 시작
    private Long bytesEnd;         // Range 끝
    private Long bytesTransferred; // 실제 전송량
    
    @Column(nullable = false)
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StreamingStatus status;  // STARTED, COMPLETED, ABORTED
}
```

### 3.4 FFmpegThumbnailAdapter

```java
@Component
@ConditionalOnProperty(prefix = "streamix.thumbnail", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FFmpegThumbnailAdapter implements ThumbnailGeneratorPort {
    
    private final String ffmpegPath;
    private final int frameSeconds;
    
    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.VIDEO;
    }
    
    @Override
    public byte[] generateFromPath(String sourcePath, int width, int height) {
        // FFmpeg 명령어 실행
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-i", sourcePath,
            "-ss", String.format("00:00:%02d", frameSeconds),
            "-vframes", "1",
            "-vf", String.format("scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2", 
                                 width, height, width, height),
            "-f", "image2pipe",
            "-vcodec", "mjpeg",
            "-"
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // stdout에서 이미지 데이터 읽기
        return process.getInputStream().readAllBytes();
    }
    
    @Override
    public byte[] generate(InputStream sourceStream, int width, int height) {
        throw new ThumbnailGenerationException(
            "Video thumbnail from InputStream is not supported. Use generateFromPath()."
        );
    }
}
```

---

## 4. REST API 설계

### 4.1 엔드포인트 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/streamix/files` | 파일 업로드 |
| `GET` | `/api/streamix/files` | 파일 목록 (페이징) |
| `GET` | `/api/streamix/files/{id}` | 파일 메타데이터 조회 |
| `GET` | `/api/streamix/files/{id}/stream` | 파일 스트리밍 |
| `GET` | `/api/streamix/files/{id}/thumbnail` | 썸네일 조회 |
| `DELETE` | `/api/streamix/files/{id}` | 파일 삭제 |
| `GET` | `/api/streamix/stats` | 통계 정보 |

### 4.2 StreamixController

```java
@RestController
@RequestMapping("${streamix.api.base-path:/api/streamix}")
@ConditionalOnProperty(prefix = "streamix.api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StreamixController {
    
    private final UploadFileUseCase uploadFileUseCase;
    private final StreamFileUseCase streamFileUseCase;
    private final GetFileMetadataUseCase getFileMetadataUseCase;
    private final GetThumbnailUseCase getThumbnailUseCase;
    private final DeleteFileUseCase deleteFileUseCase;
    private final StreamingMonitorService monitorService;
    
    /**
     * 파일 업로드
     */
    @PostMapping("/files")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            file.getInputStream()
        );
        
        UploadResult result = uploadFileUseCase.upload(command);
        return ResponseEntity.ok(UploadResponse.from(result));
    }
    
    /**
     * 파일 스트리밍 (Range 지원)
     */
    @GetMapping("/files/{id}/stream")
    public ResponseEntity<Resource> stream(
            @PathVariable UUID id,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) {
        
        StreamFileUseCase.StreamCommand command = new StreamFileUseCase.StreamCommand(id, rangeHeader);
        StreamableFile streamable = streamFileUseCase.stream(command);
        
        // 스트리밍 로그 기록
        monitorService.logStreamingStart(id, request);
        
        if (streamable.isPartial()) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header("Content-Range", streamable.contentRange())
                .header("Accept-Ranges", "bytes")
                .contentType(MediaType.parseMediaType(streamable.contentType()))
                .contentLength(streamable.contentLength())
                .body(new InputStreamResource(streamable.inputStream()));
        }
        
        return ResponseEntity.ok()
            .header("Accept-Ranges", "bytes")
            .contentType(MediaType.parseMediaType(streamable.contentType()))
            .contentLength(streamable.contentLength())
            .body(new InputStreamResource(streamable.inputStream()));
    }
    
    /**
     * 썸네일 조회
     */
    @GetMapping("/files/{id}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable UUID id) {
        byte[] thumbnail = getThumbnailUseCase.getThumbnail(id);
        
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
            .body(thumbnail);
    }
    
    /**
     * 파일 목록 조회
     */
    @GetMapping("/files")
    public ResponseEntity<PagedResponse<FileInfoResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FileType type) {
        
        List<FileMetadata> files = getFileMetadataUseCase.getAll(page, size);
        long total = getFileMetadataUseCase.count();
        
        PagedResponse<FileInfoResponse> response = PagedResponse.of(
            files.stream().map(FileInfoResponse::from).toList(),
            page, size, total
        );
        
        return ResponseEntity.ok(response);
    }
    
    // ... 기타 엔드포인트
}
```

### 4.3 Response DTO

```java
public record UploadResponse(
    UUID id,
    String originalName,
    FileType type,
    String contentType,
    long size,
    String streamUrl,
    String thumbnailUrl,
    boolean thumbnailGenerated,
    LocalDateTime createdAt
) {
    public static UploadResponse from(UploadResult result) {
        return new UploadResponse(
            result.id(),
            result.originalName(),
            result.type(),
            result.contentType(),
            result.size(),
            "/api/streamix/files/" + result.id() + "/stream",
            result.thumbnailGenerated() 
                ? "/api/streamix/files/" + result.id() + "/thumbnail" 
                : null,
            result.thumbnailGenerated(),
            result.createdAt()
        );
    }
}

public record FileInfoResponse(
    UUID id,
    String originalName,
    FileType type,
    String contentType,
    long size,
    String streamUrl,
    String thumbnailUrl,
    LocalDateTime createdAt
) {
    public static FileInfoResponse from(FileMetadata metadata) { ... }
}

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long total) { ... }
}
```

---

## 5. 대시보드 설계

### 5.1 대시보드 기능

| 화면 | 기능 |
|------|------|
| **메인 대시보드** | 전체 통계, 최근 업로드, 활성 스트리밍 |
| **파일 목록** | 그리드/리스트 뷰, 검색, 필터, 정렬 |
| **파일 미리보기** | 이미지 뷰어, 비디오 플레이어 |
| **스트리밍 모니터** | 실시간 스트리밍 현황, 대역폭 사용량 |

### 5.2 StreamixDashboardController

```java
@Controller
@RequestMapping("${streamix.dashboard.path:/streamix}")
@ConditionalOnProperty(prefix = "streamix.dashboard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StreamixDashboardController {
    
    private final GetFileMetadataUseCase getFileMetadataUseCase;
    private final StreamingMonitorService monitorService;
    
    /**
     * 메인 대시보드
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", monitorService.getOverviewStats());
        model.addAttribute("recentFiles", getFileMetadataUseCase.getAll(0, 10));
        model.addAttribute("activeStreams", monitorService.getActiveStreams());
        return "streamix/dashboard";
    }
    
    /**
     * 파일 목록 페이지
     */
    @GetMapping("/files")
    public String files(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FileType type,
            @RequestParam(defaultValue = "grid") String view,
            Model model) {
        
        model.addAttribute("files", getFileMetadataUseCase.getAll(page, size));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("viewMode", view);
        return "streamix/files";
    }
    
    /**
     * 파일 미리보기 페이지
     */
    @GetMapping("/files/{id}")
    public String preview(@PathVariable UUID id, Model model) {
        FileMetadata file = getFileMetadataUseCase.getById(id);
        model.addAttribute("file", file);
        return "streamix/preview";
    }
    
    /**
     * 스트리밍 모니터 페이지
     */
    @GetMapping("/monitor")
    public String monitor(Model model) {
        model.addAttribute("activeStreams", monitorService.getActiveStreams());
        model.addAttribute("recentLogs", monitorService.getRecentLogs(50));
        model.addAttribute("stats", monitorService.getStreamingStats());
        return "streamix/monitor";
    }
}
```

### 5.3 대시보드 UI 구성

#### 메인 대시보드 (dashboard.html)
```
┌─────────────────────────────────────────────────────────────────┐
│  🎬 Streamix Dashboard                              [Monitor]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 📁 125   │  │ 🖼️ 80    │  │ 🎬 45    │  │ 📊 2.5GB │        │
│  │ Total    │  │ Images   │  │ Videos   │  │ Storage  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                 │
│  ┌─────────────────────────────┐  ┌─────────────────────────┐  │
│  │ 📤 Recent Uploads           │  │ 🔴 Active Streams       │  │
│  │ ─────────────────────────── │  │ ─────────────────────── │  │
│  │ video.mp4      2 min ago    │  │ movie.mp4 → 192.168.1.5 │  │
│  │ photo.jpg      5 min ago    │  │ clip.mp4  → 10.0.0.23   │  │
│  │ image.png      10 min ago   │  │                         │  │
│  └─────────────────────────────┘  └─────────────────────────┘  │
│                                                                 │
│  [View All Files]  [Upload New]                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 파일 목록 (files.html)
```
┌─────────────────────────────────────────────────────────────────┐
│  📁 Files                    [🔍 Search]  [Grid/List] [Upload]  │
├─────────────────────────────────────────────────────────────────┤
│  Filter: [All ▼] [Images ▼] [Videos ▼]   Sort: [Newest ▼]       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │ 🎬      │  │ 🖼️      │  │ 🎬      │  │ 🖼️      │            │
│  │[thumb]  │  │[thumb]  │  │[thumb]  │  │[thumb]  │            │
│  │ ▶ Play  │  │         │  │ ▶ Play  │  │         │            │
│  ├─────────┤  ├─────────┤  ├─────────┤  ├─────────┤            │
│  │video.mp4│  │photo.jpg│  │clip.mp4 │  │img.png  │            │
│  │ 15.2 MB │  │ 2.1 MB  │  │ 8.7 MB  │  │ 512 KB  │            │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │
│                                                                 │
│  [◀ Prev]  Page 1 of 5  [Next ▶]                                │
└─────────────────────────────────────────────────────────────────┘
```

#### 미리보기 (preview.html)
```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back to Files              video.mp4                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                                                         │   │
│  │                    🎬 VIDEO PLAYER                      │   │
│  │                         ▶                               │   │
│  │                     advancement bar                      │   │
│  │                                                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 File Information                                     │   │
│  │ ─────────────────────────────────────────────────────── │   │
│  │ Name:       video.mp4                                   │   │
│  │ Type:       VIDEO (video/mp4)                           │   │
│  │ Size:       15.2 MB                                     │   │
│  │ Uploaded:   2025-12-14 10:30:00                         │   │
│  │ Stream URL: /api/streamix/files/{id}/stream             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  [🔗 Copy Stream URL]  [📥 Download]  [🗑️ Delete]               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. 스트리밍 모니터링

### 6.1 StreamingMonitorService

```java
@Service
public class StreamingMonitorService {
    
    private final StreamingLogJpaRepository logRepository;
    private final FileMetadataJpaRepository fileRepository;
    
    // 현재 활성 스트리밍 (메모리 캐시)
    private final ConcurrentMap<UUID, ActiveStream> activeStreams = new ConcurrentHashMap<>();
    
    /**
     * 스트리밍 시작 로그
     */
    public void logStreamingStart(UUID fileId, HttpServletRequest request) {
        StreamingLogEntity log = new StreamingLogEntity();
        log.setFileId(fileId);
        log.setClientIp(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setStartedAt(LocalDateTime.now());
        log.setStatus(StreamingStatus.STARTED);
        
        logRepository.save(log);
        activeStreams.put(fileId, new ActiveStream(fileId, log.getId(), LocalDateTime.now()));
    }
    
    /**
     * 스트리밍 완료 로그
     */
    public void logStreamingComplete(UUID fileId, long bytesTransferred) {
        activeStreams.remove(fileId);
        // ... 로그 업데이트
    }
    
    /**
     * 전체 통계 조회
     */
    public OverviewStats getOverviewStats() {
        return new OverviewStats(
            fileRepository.count(),
            fileRepository.countByType(FileType.IMAGE),
            fileRepository.countByType(FileType.VIDEO),
            fileRepository.sumSize(),
            logRepository.countToday(),
            logRepository.sumBytesToday()
        );
    }
    
    /**
     * 활성 스트리밍 목록
     */
    public List<ActiveStream> getActiveStreams() {
        return List.copyOf(activeStreams.values());
    }
    
    /**
     * 최근 스트리밍 로그
     */
    public List<StreamingLog> getRecentLogs(int limit) {
        return logRepository.findTopNByOrderByStartedAtDesc(limit);
    }
}

public record OverviewStats(
    long totalFiles,
    long imageCount,
    long videoCount,
    long totalStorageBytes,
    long todayStreamCount,
    long todayBytesTransferred
) {}

public record ActiveStream(
    UUID fileId,
    Long logId,
    LocalDateTime startedAt,
    String clientIp,
    String fileName
) {}
```

---

## 7. 데이터베이스 스키마

```sql
-- 파일 메타데이터
CREATE TABLE streamix_file_metadata (
    id              UUID PRIMARY KEY,
    original_name   VARCHAR(255) NOT NULL,
    type            VARCHAR(20) NOT NULL,  -- IMAGE, VIDEO
    content_type    VARCHAR(100) NOT NULL,
    size            BIGINT NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    thumbnail_path  VARCHAR(500),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_streamix_type ON streamix_file_metadata(type);
CREATE INDEX idx_streamix_created ON streamix_file_metadata(created_at DESC);

-- 스트리밍 로그
CREATE TABLE streamix_streaming_log (
    id                BIGSERIAL PRIMARY KEY,
    file_id           UUID NOT NULL,
    client_ip         VARCHAR(50),
    user_agent        VARCHAR(200),
    bytes_start       BIGINT,
    bytes_end         BIGINT,
    bytes_transferred BIGINT,
    started_at        TIMESTAMP NOT NULL,
    completed_at      TIMESTAMP,
    status            VARCHAR(20) NOT NULL,  -- STARTED, COMPLETED, ABORTED
    
    CONSTRAINT fk_streaming_file 
        FOREIGN KEY (file_id) REFERENCES streamix_file_metadata(id) ON DELETE CASCADE
);

CREATE INDEX idx_streaming_file ON streamix_streaming_log(file_id);
CREATE INDEX idx_streaming_time ON streamix_streaming_log(started_at DESC);
CREATE INDEX idx_streaming_status ON streamix_streaming_log(status);
```

---

## 8. 설정 예시

### 8.1 최소 설정
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: password
  jpa:
    hibernate:
      ddl-auto: update

streamix:
  storage:
    base-path: ./uploads
```

### 8.2 전체 설정
```yaml
streamix:
  storage:
    base-path: ./uploads
    max-file-size: 104857600  # 100MB
  
  thumbnail:
    enabled: true
    width: 320
    height: 180
    ffmpeg-path: /usr/bin/ffmpeg
    video-frame-seconds: 1
  
  api:
    enabled: true
    base-path: /api/streamix
  
  dashboard:
    enabled: true
    path: /streamix
    # username: admin  # 선택적 인증
    # password: secret
  
  streaming:
    chunk-size: 1048576   # 1MB
    buffer-size: 8192     # 8KB
    log-enabled: true
```

---

## 9. 구현 순서

### Phase 1: 기본 인프라 (Day 1)
- [ ] `@EnableStreamix` 어노테이션
- [ ] `StreamixProperties` (Record)
- [ ] `StreamixAutoConfiguration`
- [ ] `build.gradle` 의존성 설정

### Phase 2: JPA 어댑터 (Day 1-2)
- [ ] `FileMetadataEntity`
- [ ] `FileMetadataJpaRepository`
- [ ] `JpaFileMetadataAdapter`

### Phase 3: FFmpeg 썸네일 (Day 2)
- [ ] `FFmpegThumbnailAdapter`
- [ ] 프로세스 실행 및 에러 핸들링
- [ ] 타임아웃 처리

### Phase 4: REST API (Day 2-3)
- [ ] `StreamixController`
- [ ] DTO 클래스들
- [ ] `StreamixExceptionHandler`

### Phase 5: 스트리밍 모니터링 (Day 3)
- [ ] `StreamingLogEntity`
- [ ] `StreamingMonitorService`
- [ ] 통계 API

### Phase 6: 대시보드 (Day 3-4)
- [ ] `StreamixDashboardController`
- [ ] Thymeleaf 템플릿 (dashboard, files, preview, monitor)
- [ ] CSS/JS 정적 리소스

### Phase 7: 테스트 및 문서화 (Day 4)
- [ ] 통합 테스트
- [ ] README 업데이트
- [ ] 사용 예제

---

## 10. 의존성

```groovy
// build.gradle
dependencies {
    // Core 모듈
    api project(':streamix-core')
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Configuration Processor
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // Thymeleaf Layout
    implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.h2database:h2'
}
```

---

## 11. 예상 파일 수

| 카테고리 | 파일 수 |
|---------|--------|
| Annotation | 2 |
| AutoConfiguration | 4 |
| Properties | 1 |
| Controller | 2 |
| DTO | 5 |
| Entity | 2 |
| Repository | 2 |
| Adapter | 2 |
| Service | 1 |
| Exception Handler | 1 |
| Templates (HTML) | 4 |
| Static (CSS/JS) | 2 |
| **Total** | **~28개** |