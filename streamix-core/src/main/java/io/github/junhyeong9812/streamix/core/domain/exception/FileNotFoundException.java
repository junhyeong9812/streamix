package io.github.junhyeong9812.streamix.core.domain.exception;

import java.util.UUID;

/**
 * 요청한 파일을 찾을 수 없을 때 발생하는 예외입니다.
 *
 * <p>다음 상황에서 발생합니다:</p>
 * <ul>
 *   <li>존재하지 않는 파일 ID로 조회/스트리밍/삭제 시도</li>
 *   <li>저장소에서 파일이 물리적으로 삭제된 경우</li>
 *   <li>썸네일이 없는 파일의 썸네일 요청</li>
 * </ul>
 *
 * <h2>HTTP 매핑</h2>
 * <p>일반적으로 HTTP 404 Not Found로 매핑됩니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // ID로 찾을 수 없는 경우
 * throw new FileNotFoundException(fileId);
 *
 * // 경로로 찾을 수 없는 경우
 * throw new FileNotFoundException("/storage/file.jpg");
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixException
 */
public final class FileNotFoundException extends StreamixException {

  /**
   * 찾을 수 없는 파일의 ID. 경로로 조회한 경우 {@code null}.
   */
  private final UUID fileId;

  /**
   * 파일 ID로 예외를 생성합니다.
   *
   * @param fileId 찾을 수 없는 파일의 ID
   */
  public FileNotFoundException(UUID fileId) {
    super("File not found: " + fileId);
    this.fileId = fileId;
  }

  /**
   * 파일 경로로 예외를 생성합니다.
   *
   * @param path 찾을 수 없는 파일의 경로
   */
  public FileNotFoundException(String path) {
    super("File not found at path: " + path);
    this.fileId = null;
  }

  /**
   * 찾을 수 없는 파일의 ID를 반환합니다.
   *
   * @return 파일 ID, 경로로 조회한 경우 {@code null}
   */
  public UUID getFileId() {
    return fileId;
  }
}