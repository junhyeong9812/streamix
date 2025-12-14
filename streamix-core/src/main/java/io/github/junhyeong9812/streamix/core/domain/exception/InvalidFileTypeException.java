package io.github.junhyeong9812.streamix.core.domain.exception;

/**
 * 지원하지 않는 파일 타입일 때 발생하는 예외입니다.
 *
 * <p>Streamix는 이미지(jpg, png, gif 등)와 비디오(mp4, avi 등) 파일만 지원합니다.
 * 지원하지 않는 확장자나 Content-Type의 파일을 업로드하려 할 때 발생합니다.</p>
 *
 * <h2>지원 파일 형식</h2>
 * <ul>
 *   <li><b>이미지</b>: jpg, jpeg, png, gif, webp, bmp, svg</li>
 *   <li><b>비디오</b>: mp4, avi, mov, wmv, mkv, webm, flv</li>
 * </ul>
 *
 * <h2>HTTP 매핑</h2>
 * <p>일반적으로 HTTP 400 Bad Request 또는 415 Unsupported Media Type으로 매핑됩니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 확장자만으로 예외 생성
 * throw new InvalidFileTypeException("pdf");
 *
 * // 확장자와 Content-Type으로 예외 생성
 * throw new InvalidFileTypeException("pdf", "application/pdf");
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixException
 * @see io.github.junhyeong9812.streamix.core.domain.model.FileType
 */
public final class InvalidFileTypeException extends StreamixException {

  /**
   * 지원되지 않는 파일 확장자.
   */
  private final String extension;

  /**
   * 지원되지 않는 Content-Type. 확장자만으로 예외 생성 시 {@code null}.
   */
  private final String contentType;

  /**
   * 확장자로 예외를 생성합니다.
   *
   * @param extension 지원되지 않는 파일 확장자
   */
  public InvalidFileTypeException(String extension) {
    super("Unsupported file extension: " + extension);
    this.extension = extension;
    this.contentType = null;
  }

  /**
   * 확장자와 Content-Type으로 예외를 생성합니다.
   *
   * @param extension   지원되지 않는 파일 확장자
   * @param contentType 지원되지 않는 Content-Type
   */
  public InvalidFileTypeException(String extension, String contentType) {
    super(String.format("Unsupported file type - extension: %s, contentType: %s",
        extension, contentType));
    this.extension = extension;
    this.contentType = contentType;
  }

  /**
   * 지원되지 않는 파일 확장자를 반환합니다.
   *
   * @return 파일 확장자
   */
  public String getExtension() {
    return extension;
  }

  /**
   * 지원되지 않는 Content-Type을 반환합니다.
   *
   * @return Content-Type, 확장자만으로 예외 생성 시 {@code null}
   */
  public String getContentType() {
    return contentType;
  }
}