package io.github.junhyeong9812.streamix.core.application.port.in;

import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;

import java.util.UUID;

/**
 * 썸네일 조회 유스케이스 인터페이스입니다.
 *
 * <p>헥사고날 아키텍처의 Input Port로, 파일의 썸네일을 조회하는 기능을 정의합니다.
 * 업로드 시 자동 생성된 썸네일 이미지를 스트리밍 형태로 반환합니다.</p>
 *
 * <h2>썸네일 특성</h2>
 * <ul>
 *   <li>형식: JPEG</li>
 *   <li>기본 크기: 320x180 (설정 가능)</li>
 *   <li>Content-Type: image/jpeg</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @GetMapping("/files/{id}/thumbnail")
 * public ResponseEntity<StreamingResponseBody> thumbnail(@PathVariable UUID id) {
 *     StreamableFile thumbnail = getThumbnailUseCase.getThumbnail(id);
 *
 *     return ResponseEntity.ok()
 *         .contentType(MediaType.IMAGE_JPEG)
 *         .body(outputStream -> thumbnail.inputStream().transferTo(outputStream));
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamableFile
 * @see io.github.junhyeong9812.streamix.core.application.service.FileStreamService
 */
public interface GetThumbnailUseCase {

  /**
   * 파일의 썸네일을 조회합니다.
   *
   * @param fileId 파일 ID
   * @return 썸네일 이미지 스트림
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않거나 썸네일이 없는 경우
   */
  StreamableFile getThumbnail(UUID fileId);
}