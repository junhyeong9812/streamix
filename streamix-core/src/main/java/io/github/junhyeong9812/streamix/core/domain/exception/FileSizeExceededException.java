package io.github.junhyeong9812.streamix.core.domain.exception;

/**
 * 파일 크기가 허용된 최대 크기를 초과했을 때 발생하는 예외입니다.
 *
 * <p>다음 상황에서 발생합니다:</p>
 * <ul>
 *   <li>업로드된 파일이 설정된 최대 크기를 초과할 때</li>
 *   <li>스토리지 할당량을 초과할 때</li>
 * </ul>
 *
 * <h2>HTTP 매핑</h2>
 * <p>일반적으로 HTTP 413 Payload Too Large로 매핑됩니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 파일 크기 검증
 * if (file.getSize() > maxFileSize) {
 *     throw new FileSizeExceededException(file.getSize(), maxFileSize);
 * }
 *
 * // 파일명과 함께 예외 생성
 * throw new FileSizeExceededException("video.mp4", file.getSize(), maxFileSize);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.7
 * @see StreamixException
 */
public final class FileSizeExceededException extends StreamixException {

  /**
   * 실제 파일 크기 (바이트).
   */
  private final long actualSize;

  /**
   * 허용된 최대 크기 (바이트).
   */
  private final long maxSize;

  /**
   * 파일명. 크기만으로 예외 생성 시 {@code null}.
   */
  private final String fileName;

  /**
   * 파일 크기 초과 예외를 생성합니다.
   *
   * @param actualSize 실제 파일 크기 (바이트)
   * @param maxSize    허용된 최대 크기 (바이트)
   */
  public FileSizeExceededException(long actualSize, long maxSize) {
    super(String.format(
        "File size %s exceeds maximum allowed size %s",
        formatSize(actualSize),
        formatSize(maxSize)
    ));
    this.actualSize = actualSize;
    this.maxSize = maxSize;
    this.fileName = null;
  }

  /**
   * 파일명과 함께 예외를 생성합니다.
   *
   * @param fileName   파일명
   * @param actualSize 실제 파일 크기 (바이트)
   * @param maxSize    허용된 최대 크기 (바이트)
   */
  public FileSizeExceededException(String fileName, long actualSize, long maxSize) {
    super(String.format(
        "File '%s' size %s exceeds maximum allowed size %s",
        fileName,
        formatSize(actualSize),
        formatSize(maxSize)
    ));
    this.actualSize = actualSize;
    this.maxSize = maxSize;
    this.fileName = fileName;
  }

  /**
   * 실제 파일 크기를 반환합니다.
   *
   * @return 파일 크기 (바이트)
   */
  public long getActualSize() {
    return actualSize;
  }

  /**
   * 허용된 최대 크기를 반환합니다.
   *
   * @return 최대 크기 (바이트)
   */
  public long getMaxSize() {
    return maxSize;
  }

  /**
   * 초과된 크기를 반환합니다.
   *
   * @return 초과된 크기 (바이트)
   */
  public long getExceededBy() {
    return actualSize - maxSize;
  }

  /**
   * 파일명을 반환합니다.
   *
   * @return 파일명, 크기만으로 예외 생성 시 {@code null}
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * 바이트 크기를 사람이 읽기 쉬운 형식으로 변환합니다.
   *
   * @param bytes 바이트 크기
   * @return 포맷된 크기 문자열 (예: "15.5 MB")
   */
  private static String formatSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024L * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    } else {
      return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
  }
}