package io.github.junhyeong9812.streamix.core.domain.util;

/**
 * 바이트 크기를 사람이 읽기 쉬운 문자열로 변환하는 유틸리티 클래스입니다.
 *
 * <p>1024 단위(IEC binary prefix)를 사용합니다. 단위는 B/KB/MB/GB/TB까지 지원합니다.
 * 모든 메서드는 static입니다. 인스턴스 생성 불가합니다.</p>
 *
 * <h2>출력 예시</h2>
 * <pre>
 *   500           → "500 B"
 *   2048          → "2.0 KB"
 *   1572864       → "1.5 MB"
 *   2147483648    → "2.0 GB"
 *   1099511627776 → "1.0 TB"
 *   -1            → "0 B"
 * </pre>
 *
 * @author junhyeong9812
 * @since 2.0.1
 */
public final class ByteSizeFormatter {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;
  private static final long TB = GB * 1024;

  private ByteSizeFormatter() {
    throw new AssertionError("Utility class — do not instantiate");
  }

  /**
   * 바이트 크기를 단위 문자열로 변환합니다.
   *
   * <p>음수는 "0 B"로 처리합니다. KB 이상은 소수점 한 자리 정밀도입니다.</p>
   *
   * @param bytes 바이트 크기
   * @return 포맷된 문자열 (예: "1.5 GB")
   */
  public static String format(long bytes) {
    if (bytes < 0) return "0 B";
    if (bytes < KB) return bytes + " B";
    if (bytes < MB) return String.format("%.1f KB", bytes / (double) KB);
    if (bytes < GB) return String.format("%.1f MB", bytes / (double) MB);
    if (bytes < TB) return String.format("%.1f GB", bytes / (double) GB);
    return String.format("%.1f TB", bytes / (double) TB);
  }
}
