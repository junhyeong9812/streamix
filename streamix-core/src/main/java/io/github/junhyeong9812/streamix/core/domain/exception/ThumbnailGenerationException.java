package io.github.junhyeong9812.streamix.core.domain.exception;

import java.util.UUID;

/**
 * 썸네일 생성에 실패했을 때 발생하는 예외입니다.
 *
 * <p>다음 상황에서 발생합니다:</p>
 * <ul>
 *   <li>이미지 리사이징 실패</li>
 *   <li>비디오 프레임 추출 실패</li>
 *   <li>지원하지 않는 이미지/비디오 코덱</li>
 *   <li>손상된 파일</li>
 * </ul>
 *
 * <h2>주의사항</h2>
 * <p>썸네일 생성 실패는 파일 업로드 자체를 실패시키지 않습니다.
 * 업로드는 성공하고, 썸네일만 없는 상태로 저장됩니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 메시지로 예외 생성
 * throw new ThumbnailGenerationException("Unsupported image format");
 *
 * // 파일 ID로 예외 생성
 * throw new ThumbnailGenerationException(fileId, ioException);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixException
 * @see io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort
 */
public final class ThumbnailGenerationException extends StreamixException {

  /**
   * 썸네일 생성에 실패한 파일의 ID. 메시지로 생성 시 {@code null}.
   */
  private final UUID fileId;

  /**
   * 메시지로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   */
  public ThumbnailGenerationException(String message) {
    super(message);
    this.fileId = null;
  }

  /**
   * 메시지와 원인 예외로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public ThumbnailGenerationException(String message, Throwable cause) {
    super(message, cause);
    this.fileId = null;
  }

  /**
   * 파일 ID와 원인 예외로 예외를 생성합니다.
   *
   * @param fileId 썸네일 생성에 실패한 파일의 ID
   * @param cause  원인 예외
   */
  public ThumbnailGenerationException(UUID fileId, Throwable cause) {
    super("Failed to generate thumbnail for file: " + fileId, cause);
    this.fileId = fileId;
  }

  /**
   * 썸네일 생성에 실패한 파일의 ID를 반환합니다.
   *
   * @return 파일 ID, 메시지로 생성된 경우 {@code null}
   */
  public UUID getFileId() {
    return fileId;
  }
}