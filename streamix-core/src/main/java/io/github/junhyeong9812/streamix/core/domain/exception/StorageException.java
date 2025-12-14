package io.github.junhyeong9812.streamix.core.domain.exception;

/**
 * 파일 저장소 작업 중 오류가 발생했을 때 던지는 예외입니다.
 *
 * <p>다음 상황에서 발생합니다:</p>
 * <ul>
 *   <li>파일 저장 실패 (디스크 공간 부족, 권한 오류 등)</li>
 *   <li>파일 로드 실패 (I/O 오류)</li>
 *   <li>파일 삭제 실패</li>
 *   <li>디렉토리 생성 실패</li>
 * </ul>
 *
 * <h2>HTTP 매핑</h2>
 * <p>일반적으로 HTTP 500 Internal Server Error로 매핑됩니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 팩토리 메서드 사용
 * throw StorageException.saveFailed("video.mp4", ioException);
 * throw StorageException.loadFailed("/storage/file.jpg", ioException);
 * throw StorageException.deleteFailed("/storage/file.jpg", ioException);
 *
 * // 직접 생성
 * throw new StorageException("Custom error message", cause);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixException
 * @see io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort
 */
public final class StorageException extends StreamixException {

  /**
   * 메시지만으로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   */
  public StorageException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인 예외로 예외를 생성합니다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 파일 저장 실패 예외를 생성합니다.
   *
   * @param fileName 저장 실패한 파일명
   * @param cause    원인 예외
   * @return StorageException
   */
  public static StorageException saveFailed(String fileName, Throwable cause) {
    return new StorageException("Failed to save file: " + fileName, cause);
  }

  /**
   * 파일 로드 실패 예외를 생성합니다.
   *
   * @param path  로드 실패한 파일 경로
   * @param cause 원인 예외
   * @return StorageException
   */
  public static StorageException loadFailed(String path, Throwable cause) {
    return new StorageException("Failed to load file: " + path, cause);
  }

  /**
   * 파일 삭제 실패 예외를 생성합니다.
   *
   * @param path  삭제 실패한 파일 경로
   * @param cause 원인 예외
   * @return StorageException
   */
  public static StorageException deleteFailed(String path, Throwable cause) {
    return new StorageException("Failed to delete file: " + path, cause);
  }

  /**
   * 디렉토리 생성 실패 예외를 생성합니다.
   *
   * @param path  생성 실패한 디렉토리 경로
   * @param cause 원인 예외
   * @return StorageException
   */
  public static StorageException directoryCreationFailed(String path, Throwable cause) {
    return new StorageException("Failed to create directory: " + path, cause);
  }
}