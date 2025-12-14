package io.github.junhyeong9812.streamix.core.application.port.in;

import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;

import java.util.UUID;

/**
 * 파일 스트리밍 유스케이스 인터페이스입니다.
 *
 * <p>헥사고날 아키텍처의 Input Port로, 파일 스트리밍 기능을 정의합니다.
 * HTTP Range 요청을 지원하여 전체 파일 또는 부분 콘텐츠를 스트리밍할 수 있습니다.</p>
 *
 * <h2>HTTP Range 요청 지원</h2>
 * <table border="1">
 *   <caption>HTTP Range 요청 및 응답 매핑</caption>
 *   <tr><th>요청</th><th>응답</th><th>설명</th></tr>
 *   <tr><td>Range 헤더 없음</td><td>200 OK</td><td>전체 파일</td></tr>
 *   <tr><td>Range: bytes=0-1023</td><td>206 Partial Content</td><td>처음 1KB</td></tr>
 *   <tr><td>Range: bytes=1024-</td><td>206 Partial Content</td><td>1KB 이후 전체</td></tr>
 *   <tr><td>Range: bytes=-500</td><td>206 Partial Content</td><td>마지막 500바이트</td></tr>
 * </table>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @GetMapping("/files/{id}/stream")
 * public ResponseEntity<StreamingResponseBody> stream(
 *         @PathVariable UUID id,
 *         @RequestHeader(value = "Range", required = false) String range) {
 *
 *     StreamCommand command = StreamCommand.withRange(id, range);
 *     StreamableFile file = streamFileUseCase.stream(command);
 *
 *     if (file.isPartialContent()) {
 *         return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
 *             .header("Content-Range", file.getContentRange())
 *             .body(outputStream -> file.inputStream().transferTo(outputStream));
 *     }
 *
 *     return ResponseEntity.ok()
 *         .body(outputStream -> file.inputStream().transferTo(outputStream));
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamableFile
 * @see io.github.junhyeong9812.streamix.core.application.service.FileStreamService
 */
public interface StreamFileUseCase {

  /**
   * 파일을 스트리밍합니다.
   *
   * <p>Range 헤더가 있으면 해당 범위만, 없으면 전체 파일을 반환합니다.</p>
   *
   * @param command 스트리밍 명령 (파일 ID, Range 헤더)
   * @return 스트리밍 가능한 파일 (메타데이터 + InputStream)
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         파일 로드 중 오류 발생 시
   */
  StreamableFile stream(StreamCommand command);

  /**
   * 파일 스트리밍 명령을 담는 불변 레코드입니다.
   *
   * @param fileId      스트리밍할 파일의 ID
   * @param rangeHeader HTTP Range 헤더 값 (예: "bytes=0-1023"), 전체 요청 시 null
   */
  record StreamCommand(
      UUID fileId,
      String rangeHeader
  ) {
    /**
     * Compact Constructor - 유효성 검증을 수행합니다.
     *
     * @throws IllegalArgumentException fileId가 null인 경우
     */
    public StreamCommand {
      if (fileId == null) {
        throw new IllegalArgumentException("fileId must not be null");
      }
    }

    /**
     * Range 헤더 없이 전체 파일 스트리밍 명령을 생성합니다.
     *
     * @param fileId 파일 ID
     * @return 전체 스트리밍 명령
     */
    public static StreamCommand of(UUID fileId) {
      return new StreamCommand(fileId, null);
    }

    /**
     * Range 헤더로 부분 스트리밍 명령을 생성합니다.
     *
     * @param fileId      파일 ID
     * @param rangeHeader Range 헤더 값
     * @return 부분 스트리밍 명령
     */
    public static StreamCommand withRange(UUID fileId, String rangeHeader) {
      return new StreamCommand(fileId, rangeHeader);
    }

    /**
     * Range 요청 여부를 확인합니다.
     *
     * @return Range 헤더가 있으면 {@code true}
     */
    public boolean hasRange() {
      return rangeHeader != null && !rangeHeader.isBlank();
    }
  }
}