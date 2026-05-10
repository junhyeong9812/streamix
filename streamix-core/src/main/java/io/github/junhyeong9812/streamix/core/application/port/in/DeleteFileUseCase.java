package io.github.junhyeong9812.streamix.core.application.port.in;

import java.util.UUID;

/**
 * 파일 삭제 유스케이스 인터페이스입니다.
 *
 * <p>헥사고날 아키텍처의 Input Port로, 파일 삭제 기능을 정의합니다.
 * 실제 파일, 썸네일, 메타데이터를 모두 삭제합니다.</p>
 *
 * <h2>삭제 순서</h2>
 * <ol>
 *   <li>저장소에서 실제 파일 삭제</li>
 *   <li>썸네일이 있으면 썸네일 삭제</li>
 *   <li>메타데이터 저장소에서 메타데이터 삭제</li>
 * </ol>
 *
 * <h2>주의사항</h2>
 * <p>실제 파일 삭제가 실패해도 메타데이터는 삭제됩니다 (soft delete 효과).</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @DeleteMapping("/files/{id}")
 * public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
 *     deleteFileUseCase.delete(id);
 *     return ResponseEntity.noContent().build();
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see io.github.junhyeong9812.streamix.core.application.service.FileMetadataService
 */
public interface DeleteFileUseCase {

  /**
   * 파일을 삭제합니다.
   *
   * <p>실제 파일, 썸네일, 메타데이터를 모두 삭제합니다.</p>
   *
   * @param fileId 삭제할 파일의 ID
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   */
  void delete(UUID fileId);

  /**
   * 파일을 멱등하게 삭제합니다.
   *
   * <p>존재하지 않아도 예외 없이 false를 반환합니다.
   * 같은 ID를 여러 번 호출해도 안전합니다 (REST DELETE 멱등성, RFC 7231 §4.2.2).</p>
   *
   * @param fileId 삭제할 파일 ID
   * @return 실제 삭제 동작이 발생했으면 {@code true}
   * @since 2.0.1
   */
  boolean deleteIdempotent(UUID fileId);
}