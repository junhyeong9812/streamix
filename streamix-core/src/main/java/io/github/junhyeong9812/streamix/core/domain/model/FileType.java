package io.github.junhyeong9812.streamix.core.domain.model;

import java.util.Arrays;
import java.util.Set;

/**
 * 지원하는 미디어 파일 타입을 정의하는 열거형입니다.
 *
 * <p>Streamix 라이브러리에서 처리할 수 있는 파일 형식을 분류합니다.
 * 각 타입은 지원하는 확장자 목록을 가지며, 확장자나 Content-Type을 기반으로
 * 파일 타입을 감지할 수 있습니다.</p>
 *
 * <h2>지원 파일 형식</h2>
 * <ul>
 *   <li><b>IMAGE</b>: jpg, jpeg, png, gif, webp, bmp, svg</li>
 *   <li><b>VIDEO</b>: mp4, avi, mov, wmv, mkv, webm, flv</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 확장자로 타입 감지
 * FileType type = FileType.fromExtension("mp4");
 * // type == FileType.VIDEO
 *
 * // Content-Type으로 타입 감지
 * FileType type = FileType.fromContentType("image/jpeg");
 * // type == FileType.IMAGE
 *
 * // 지원 여부 확인
 * boolean supported = FileType.isSupported("jpg");
 * // supported == true
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see io.github.junhyeong9812.streamix.core.domain.service.FileTypeDetector
 */
public enum FileType {

  /**
   * 이미지 파일 타입.
   *
   * <p>지원 확장자: jpg, jpeg, png, gif, webp, bmp, svg</p>
   */
  IMAGE("image", Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")),

  /**
   * 비디오 파일 타입.
   *
   * <p>지원 확장자: mp4, avi, mov, wmv, mkv, webm, flv</p>
   */
  VIDEO("video", Set.of("mp4", "avi", "mov", "wmv", "mkv", "webm", "flv"));

  private final String category;
  private final Set<String> extensions;

  /**
   * FileType 생성자.
   *
   * @param category   파일 카테고리 (예: "image", "video")
   * @param extensions 지원하는 확장자 집합
   */
  FileType(String category, Set<String> extensions) {
    this.category = category;
    this.extensions = extensions;
  }

  /**
   * 파일 카테고리를 반환합니다.
   *
   * @return 카테고리 문자열 (예: "image", "video")
   */
  public String getCategory() {
    return category;
  }

  /**
   * 지원하는 확장자 집합을 반환합니다.
   *
   * @return 확장자 집합 (불변)
   */
  public Set<String> getExtensions() {
    return extensions;
  }

  /**
   * 주어진 확장자를 지원하는지 확인합니다.
   *
   * <p>대소문자를 구분하지 않습니다.</p>
   *
   * @param extension 확인할 확장자
   * @return 지원 여부
   */
  public boolean supports(String extension) {
    return extensions.contains(extension.toLowerCase());
  }

  /**
   * 확장자로 FileType을 찾습니다.
   *
   * <p>대소문자를 구분하지 않습니다.</p>
   *
   * @param extension 파일 확장자 (예: "jpg", "mp4")
   * @return 해당하는 FileType, 지원하지 않는 확장자면 {@code null}
   */
  public static FileType fromExtension(String extension) {
    String ext = extension.toLowerCase().trim();

    return Arrays.stream(values())
        .filter(type -> type.supports(ext))
        .findFirst()
        .orElse(null);
  }

  /**
   * Content-Type으로 FileType을 찾습니다.
   *
   * <p>Content-Type의 prefix를 기준으로 판단합니다.
   * (예: "image/*" → IMAGE, "video/*" → VIDEO)</p>
   *
   * @param contentType HTTP Content-Type 헤더 값 (예: "image/jpeg", "video/mp4")
   * @return 해당하는 FileType, 지원하지 않는 타입이면 {@code null}
   */
  public static FileType fromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }

    String type = contentType.toLowerCase();

    if (type.startsWith("image/")) {
      return IMAGE;
    }
    if (type.startsWith("video/")) {
      return VIDEO;
    }

    return null;
  }

  /**
   * 주어진 확장자가 지원되는지 확인합니다.
   *
   * @param extension 확인할 확장자
   * @return 지원 여부
   */
  public static boolean isSupported(String extension) {
    return fromExtension(extension) != null;
  }
}