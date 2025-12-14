package io.github.junhyeong9812.streamix.starter.adapter.in.web;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetThumbnailUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.StreamFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;
import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.dto.FileInfoResponse;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.dto.PagedResponse;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.dto.UploadResponse;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Streamix REST API 컨트롤러입니다.
 *
 * <p>파일 업로드, 조회, 스트리밍, 삭제 등의 REST API 엔드포인트를 제공합니다.
 * 헥사고날 아키텍처의 Driving Adapter로, Core 모듈의 UseCase들을 호출합니다.</p>
 *
 * <h2>제공 API</h2>
 * <table border="1">
 *   <caption>REST API 엔드포인트 목록</caption>
 *   <tr><th>메서드</th><th>경로</th><th>설명</th></tr>
 *   <tr><td>POST</td><td>/files</td><td>파일 업로드</td></tr>
 *   <tr><td>GET</td><td>/files</td><td>파일 목록 조회 (페이징)</td></tr>
 *   <tr><td>GET</td><td>/files/{id}</td><td>파일 정보 조회</td></tr>
 *   <tr><td>GET</td><td>/files/{id}/stream</td><td>파일 스트리밍 (Range 지원)</td></tr>
 *   <tr><td>GET</td><td>/files/{id}/thumbnail</td><td>썸네일 조회</td></tr>
 *   <tr><td>DELETE</td><td>/files/{id}</td><td>파일 삭제</td></tr>
 * </table>
 *
 * <h2>HTTP Range 스트리밍</h2>
 * <p>비디오 스트리밍 시 HTTP Range 요청을 지원하여 탐색(seek)이 가능합니다:</p>
 * <ul>
 *   <li>Range 헤더 없음: 200 OK + 전체 파일</li>
 *   <li>Range: bytes=0-1023: 206 Partial Content + 부분 콘텐츠</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <p>API 기본 경로는 {@code streamix.api.base-path}로 설정합니다 (기본: /api/streamix).</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * # 파일 업로드
 * curl -X POST -F "file=@video.mp4" http://localhost:8080/api/streamix/files
 *
 * # 파일 목록 조회
 * curl http://localhost:8080/api/streamix/files?page=0&size=20
 *
 * # 파일 스트리밍 (Range 요청)
 * curl -H "Range: bytes=0-1023" http://localhost:8080/api/streamix/files/{id}/stream
 *
 * # 파일 삭제
 * curl -X DELETE http://localhost:8080/api/streamix/files/{id}
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see UploadFileUseCase
 * @see StreamFileUseCase
 * @see GetFileMetadataUseCase
 */
@RestController
public class StreamixApiController {

  private static final Logger log = LoggerFactory.getLogger(StreamixApiController.class);

  private final UploadFileUseCase uploadFileUseCase;
  private final StreamFileUseCase streamFileUseCase;
  private final GetThumbnailUseCase getThumbnailUseCase;
  private final GetFileMetadataUseCase getFileMetadataUseCase;
  private final DeleteFileUseCase deleteFileUseCase;
  private final FileMetadataPort fileMetadataPort;
  private final String apiBasePath;

  /**
   * StreamixApiController를 생성합니다.
   *
   * @param uploadFileUseCase      파일 업로드 유스케이스
   * @param streamFileUseCase      파일 스트리밍 유스케이스
   * @param getThumbnailUseCase    썸네일 조회 유스케이스
   * @param getFileMetadataUseCase 메타데이터 조회 유스케이스
   * @param deleteFileUseCase      파일 삭제 유스케이스
   * @param fileMetadataPort       메타데이터 저장소 (count 조회용)
   * @param properties             Streamix 설정
   */
  public StreamixApiController(
      UploadFileUseCase uploadFileUseCase,
      StreamFileUseCase streamFileUseCase,
      GetThumbnailUseCase getThumbnailUseCase,
      GetFileMetadataUseCase getFileMetadataUseCase,
      DeleteFileUseCase deleteFileUseCase,
      FileMetadataPort fileMetadataPort,
      StreamixProperties properties
  ) {
    this.uploadFileUseCase = uploadFileUseCase;
    this.streamFileUseCase = streamFileUseCase;
    this.getThumbnailUseCase = getThumbnailUseCase;
    this.getFileMetadataUseCase = getFileMetadataUseCase;
    this.deleteFileUseCase = deleteFileUseCase;
    this.fileMetadataPort = fileMetadataPort;
    this.apiBasePath = properties.api().basePath();
  }

  /**
   * 파일을 업로드합니다.
   *
   * <p>이미지(JPEG, PNG, GIF, WebP) 또는 비디오(MP4, WebM, AVI, MOV) 파일만 허용됩니다.
   * 업로드 시 썸네일이 자동으로 생성됩니다 (설정에 따라).</p>
   *
   * @param file 업로드할 파일
   * @return 업로드 결과 (파일 ID, URL 등)
   * @throws IOException 파일 읽기 오류 시
   */
  @PostMapping("${streamix.api.base-path:/api/streamix}/files")
  public ResponseEntity<UploadResponse> upload(
      @RequestParam(name = "file") MultipartFile file
  ) throws IOException {
    log.info("Uploading file: name={}, size={}, contentType={}",
        file.getOriginalFilename(), file.getSize(), file.getContentType());

    UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
        file.getOriginalFilename(),
        file.getContentType(),
        file.getSize(),
        file.getInputStream()
    );

    UploadResult result = uploadFileUseCase.upload(command);
    UploadResponse response = UploadResponse.from(result, apiBasePath);

    log.info("File uploaded: id={}", result.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 파일 목록을 페이징하여 조회합니다.
   *
   * <p>생성일 역순(최신순)으로 정렬됩니다.</p>
   *
   * @param page 페이지 번호 (0부터 시작, 기본값: 0)
   * @param size 페이지 크기 (1~100, 기본값: 20)
   * @return 페이징된 파일 목록
   */
  @GetMapping("${streamix.api.base-path:/api/streamix}/files")
  public ResponseEntity<PagedResponse<FileInfoResponse>> listFiles(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    log.debug("Listing files: page={}, size={}", page, size);

    List<FileMetadata> files = getFileMetadataUseCase.getAll(page, size);
    long totalElements = fileMetadataPort.count();

    List<FileInfoResponse> content = files.stream()
        .map(m -> FileInfoResponse.from(m, apiBasePath))
        .toList();

    PagedResponse<FileInfoResponse> response = PagedResponse.of(content, page, size, totalElements);

    return ResponseEntity.ok(response);
  }

  /**
   * 파일 정보를 조회합니다.
   *
   * @param id 파일 ID
   * @return 파일 상세 정보
   */
  @GetMapping("${streamix.api.base-path:/api/streamix}/files/{id}")
  public ResponseEntity<FileInfoResponse> getFile(
      @PathVariable(name = "id") UUID id
  ) {
    log.debug("Getting file info: id={}", id);

    FileMetadata metadata = getFileMetadataUseCase.getById(id);
    FileInfoResponse response = FileInfoResponse.from(metadata, apiBasePath);

    return ResponseEntity.ok(response);
  }

  /**
   * 파일을 스트리밍합니다.
   *
   * <p>HTTP Range 요청을 지원합니다:</p>
   * <ul>
   *   <li>Range 헤더 없음: 200 OK + 전체 파일</li>
   *   <li>Range: bytes=0-1023: 206 Partial Content + 처음 1KB</li>
   *   <li>Range: bytes=1024-: 206 Partial Content + 1KB 이후 전체</li>
   * </ul>
   *
   * @param id    파일 ID
   * @param range HTTP Range 헤더 (선택적)
   * @return 스트리밍 응답
   */
  @GetMapping("${streamix.api.base-path:/api/streamix}/files/{id}/stream")
  public ResponseEntity<StreamingResponseBody> streamFile(
      @PathVariable(name = "id") UUID id,
      @RequestHeader(name = HttpHeaders.RANGE, required = false) String range
  ) {
    log.debug("Streaming file: id={}, range={}", id, range);

    StreamFileUseCase.StreamCommand command = StreamFileUseCase.StreamCommand.withRange(id, range);
    StreamableFile streamableFile = streamFileUseCase.stream(command);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(streamableFile.getContentType()));
    headers.setContentLength(streamableFile.contentLength());
    headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

    StreamingResponseBody body = outputStream -> {
      try (var inputStream = streamableFile.inputStream()) {
        inputStream.transferTo(outputStream);
      }
    };

    if (streamableFile.isPartialContent()) {
      headers.set(HttpHeaders.CONTENT_RANGE, streamableFile.getContentRange());
      return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
          .headers(headers)
          .body(body);
    }

    return ResponseEntity.ok()
        .headers(headers)
        .body(body);
  }

  /**
   * 파일의 썸네일을 조회합니다.
   *
   * <p>썸네일은 JPEG 형식으로 반환됩니다.
   * 썸네일이 존재하지 않으면 404 에러가 반환됩니다.</p>
   *
   * @param id 파일 ID
   * @return 썸네일 이미지
   */
  @GetMapping("${streamix.api.base-path:/api/streamix}/files/{id}/thumbnail")
  public ResponseEntity<StreamingResponseBody> getThumbnail(
      @PathVariable(name = "id") UUID id
  ) {
    log.debug("Getting thumbnail: id={}", id);

    StreamableFile thumbnail = getThumbnailUseCase.getThumbnail(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_JPEG);
    headers.setContentLength(thumbnail.contentLength());

    StreamingResponseBody body = outputStream -> {
      try (var inputStream = thumbnail.inputStream()) {
        inputStream.transferTo(outputStream);
      }
    };

    return ResponseEntity.ok()
        .headers(headers)
        .body(body);
  }

  /**
   * 파일을 삭제합니다.
   *
   * <p>실제 파일, 썸네일, 메타데이터가 모두 삭제됩니다.</p>
   *
   * @param id 파일 ID
   * @return 204 No Content
   */
  @DeleteMapping("${streamix.api.base-path:/api/streamix}/files/{id}")
  public ResponseEntity<Void> deleteFile(
      @PathVariable(name = "id") UUID id
  ) {
    log.info("Deleting file: id={}", id);

    deleteFileUseCase.delete(id);

    log.info("File deleted: id={}", id);
    return ResponseEntity.noContent().build();
  }
}