package io.github.junhyeong9812.streamix.core.domain.exception;

/**
 * Streamix 라이브러리의 기본 예외 클래스입니다.
 *
 * <p>sealed class로 선언되어 허용된 하위 클래스만 상속할 수 있습니다.
 * 이를 통해 예외 계층 구조를 명확하게 제한하고, switch 표현식에서
 * exhaustive check가 가능합니다.</p>
 *
 * <h2>예외 계층 구조</h2>
 * <pre>
 * StreamixException (sealed)
 * ├── FileNotFoundException      - 파일을 찾을 수 없음
 * ├── InvalidFileTypeException   - 지원하지 않는 파일 타입
 * ├── StorageException           - 저장소 작업 실패
 * └── ThumbnailGenerationException - 썸네일 생성 실패
 * </pre>
 *
 * <h2>예외 처리 예시</h2>
 * <pre>{@code
 * try {
 *     uploadService.upload(command);
 * } catch (StreamixException e) {
 *     switch (e) {
 *         case FileNotFoundException fnf -> handleNotFound(fnf);
 *         case InvalidFileTypeException ift -> handleInvalidType(ift);
 *         case StorageException se -> handleStorageError(se);
 *         case ThumbnailGenerationException tge -> handleThumbnailError(tge);
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileNotFoundException
 * @see InvalidFileTypeException
 * @see StorageException
 * @see ThumbnailGenerationException
 */
public sealed class StreamixException extends RuntimeException
    permits FileNotFoundException,
    InvalidFileTypeException,
    StorageException,
    ThumbnailGenerationException {

  /**
   * 메시지만으로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   */
  public StreamixException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인 예외로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public StreamixException(String message, Throwable cause) {
    super(message, cause);
  }
}