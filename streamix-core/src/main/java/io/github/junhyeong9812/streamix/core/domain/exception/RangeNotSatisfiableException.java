package io.github.junhyeong9812.streamix.core.domain.exception;

/**
 * HTTP Range 요청 범위가 파일 크기를 벗어났을 때 발생하는 예외입니다.
 *
 * <p>RFC 7233 §4.4 — 416 Range Not Satisfiable 응답에 대응합니다.</p>
 *
 * <h2>발생 조건</h2>
 * <ul>
 *   <li>Range start가 fileSize 이상</li>
 *   <li>Range start가 0 미만</li>
 *   <li>start &gt; end (수정 후)</li>
 * </ul>
 *
 * <h2>HTTP 매핑</h2>
 * <p>HTTP 416 Range Not Satisfiable + {@code Content-Range: bytes &#42;/{fileSize}} 헤더로 매핑됩니다.</p>
 *
 * @author junhyeong9812
 * @since 2.0.1
 * @see StreamixException
 */
public final class RangeNotSatisfiableException extends StreamixException {

  /**
   * 요청 시점의 파일 전체 크기. 응답 헤더 {@code Content-Range: bytes &#42;/{fileSize}}에 사용됩니다.
   */
  private final long fileSize;

  /**
   * 파일 크기로 예외를 생성합니다.
   *
   * @param fileSize 파일 전체 크기
   */
  public RangeNotSatisfiableException(long fileSize) {
    super("Requested range not satisfiable for file size " + fileSize);
    this.fileSize = fileSize;
  }

  /**
   * 파일 크기를 반환합니다.
   *
   * @return 파일 전체 크기
   */
  public long getFileSize() {
    return fileSize;
  }
}
