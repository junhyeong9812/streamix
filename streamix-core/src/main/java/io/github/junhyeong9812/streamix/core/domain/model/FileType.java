package io.github.junhyeong9812.streamix.core.domain.model;

import java.util.Arrays;
import java.util.Set;

/**
 * 지원하는 파일 타입을 정의하는 열거형입니다.
 *
 * <p>Streamix 라이브러리에서 처리할 수 있는 파일 형식을 분류합니다.
 * 각 타입은 지원하는 확장자 목록을 가지며, 확장자나 Content-Type을 기반으로
 * 파일 타입을 감지할 수 있습니다.</p>
 *
 * <h2>지원 파일 형식</h2>
 * <ul>
 *   <li><b>IMAGE</b>: jpg, jpeg, png, gif, webp, bmp, svg, ico, tiff</li>
 *   <li><b>VIDEO</b>: mp4, avi, mov, wmv, mkv, webm, flv, m4v, mpeg, 3gp</li>
 *   <li><b>AUDIO</b>: mp3, wav, flac, aac, ogg, m4a, wma, opus, aiff</li>
 *   <li><b>DOCUMENT</b>: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf, csv, md, json, xml, html</li>
 *   <li><b>ARCHIVE</b>: zip, rar, 7z, tar, gz, bz2, xz, tgz</li>
 *   <li><b>OTHER</b>: 위에 해당하지 않는 모든 파일</li>
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
 * // 알 수 없는 확장자는 OTHER 반환
 * FileType type = FileType.fromExtension("xyz");
 * // type == FileType.OTHER
 *
 * // 썸네일 지원 여부 확인
 * boolean canThumb = FileType.VIDEO.supportsThumbnail();
 * // canThumb == true
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
   * <p>지원 확장자: jpg, jpeg, png, gif, webp, bmp, svg, ico, tiff, tif</p>
   * <p>썸네일: 이미지 리사이즈</p>
   */
  IMAGE("image", Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "tiff", "tif"), true),

  /**
   * 비디오 파일 타입.
   *
   * <p>지원 확장자: mp4, avi, mov, wmv, mkv, webm, flv, m4v, mpeg, mpg, 3gp</p>
   * <p>썸네일: 프레임 추출 (FFmpeg)</p>
   */
  VIDEO("video", Set.of("mp4", "avi", "mov", "wmv", "mkv", "webm", "flv", "m4v", "mpeg", "mpg", "3gp"), true),

  /**
   * 오디오 파일 타입.
   *
   * <p>지원 확장자: mp3, wav, flac, aac, ogg, m4a, wma, opus, aiff</p>
   * <p>썸네일: 기본 아이콘</p>
   *
   * @since 1.0.7
   */
  AUDIO("audio", Set.of("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "aiff"), false),

  /**
   * 문서 파일 타입.
   *
   * <p>지원 확장자: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf, odt, ods, odp, csv, md, json, xml, html, htm</p>
   * <p>썸네일: PDF는 첫 페이지, 그 외 기본 아이콘</p>
   *
   * @since 1.0.7
   */
  DOCUMENT("document", Set.of(
      "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
      "txt", "rtf", "odt", "ods", "odp", "csv", "md", "json", "xml", "html", "htm"
  ), true),

  /**
   * 압축 파일 타입.
   *
   * <p>지원 확장자: zip, rar, 7z, tar, gz, bz2, xz, tgz</p>
   * <p>썸네일: 기본 아이콘</p>
   *
   * @since 1.0.7
   */
  ARCHIVE("archive", Set.of("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz"), false),

  /**
   * 기타 파일 타입.
   *
   * <p>위 카테고리에 해당하지 않는 모든 파일입니다.</p>
   * <p>썸네일: 기본 아이콘</p>
   *
   * @since 1.0.7
   */
  OTHER("other", Set.of(), false);

  private final String category;
  private final Set<String> extensions;
  private final boolean thumbnailSupported;

  /**
   * FileType 생성자.
   *
   * @param category           파일 카테고리 (예: "image", "video")
   * @param extensions         지원하는 확장자 집합
   * @param thumbnailSupported 썸네일 생성 지원 여부
   */
  FileType(String category, Set<String> extensions, boolean thumbnailSupported) {
    this.category = category;
    this.extensions = extensions;
    this.thumbnailSupported = thumbnailSupported;
  }

  /**
   * 파일 카테고리를 반환합니다.
   *
   * @return 카테고리 문자열 (예: "image", "video", "audio", "document", "archive", "other")
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
   * 썸네일 생성을 지원하는지 확인합니다.
   *
   * <p>IMAGE, VIDEO, DOCUMENT(PDF)는 썸네일을 지원합니다.
   * AUDIO, ARCHIVE, OTHER는 기본 아이콘을 반환합니다.</p>
   *
   * @return 썸네일 생성 지원 시 {@code true}
   * @since 1.0.7
   */
  public boolean supportsThumbnail() {
    return thumbnailSupported;
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
    if (extension == null) {
      return false;
    }
    return extensions.contains(extension.toLowerCase());
  }

  /**
   * 미디어 파일인지 확인합니다.
   *
   * <p>IMAGE, VIDEO, AUDIO 타입이 미디어 파일입니다.</p>
   *
   * @return 미디어 파일이면 {@code true}
   * @since 1.0.7
   */
  public boolean isMedia() {
    return this == IMAGE || this == VIDEO || this == AUDIO;
  }

  /**
   * 스트리밍에 적합한 파일인지 확인합니다.
   *
   * <p>VIDEO, AUDIO 타입이 스트리밍에 적합합니다.</p>
   *
   * @return 스트리밍 적합 시 {@code true}
   * @since 1.0.7
   */
  public boolean isStreamable() {
    return this == VIDEO || this == AUDIO;
  }

  /**
   * 브라우저에서 미리보기 가능한지 확인합니다.
   *
   * <p>IMAGE, VIDEO, AUDIO, PDF 파일이 미리보기 가능합니다.</p>
   *
   * @return 미리보기 가능 시 {@code true}
   * @since 1.0.7
   */
  public boolean isPreviewable() {
    return this == IMAGE || this == VIDEO || this == AUDIO ||
        (this == DOCUMENT && extensions.contains("pdf"));
  }

  /**
   * 확장자로 FileType을 찾습니다.
   *
   * <p>대소문자를 구분하지 않습니다.
   * 지원하지 않는 확장자는 {@link #OTHER}를 반환합니다.</p>
   *
   * @param extension 파일 확장자 (예: "jpg", "mp4")
   * @return 해당하는 FileType, 지원하지 않는 확장자면 {@link #OTHER}
   */
  public static FileType fromExtension(String extension) {
    if (extension == null || extension.isBlank()) {
      return OTHER;
    }

    String ext = extension.toLowerCase().trim();
    // 앞에 점이 있으면 제거
    if (ext.startsWith(".")) {
      ext = ext.substring(1);
    }

    for (FileType type : values()) {
      if (type.supports(ext)) {
        return type;
      }
    }

    return OTHER;
  }

  /**
   * Content-Type으로 FileType을 찾습니다.
   *
   * <p>Content-Type의 prefix를 기준으로 판단합니다.
   * 지원하지 않는 타입은 {@link #OTHER}를 반환합니다.</p>
   *
   * @param contentType HTTP Content-Type 헤더 값 (예: "image/jpeg", "video/mp4")
   * @return 해당하는 FileType, 지원하지 않는 타입이면 {@link #OTHER}
   */
  public static FileType fromContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return OTHER;
    }

    String type = contentType.toLowerCase().trim();

    if (type.startsWith("image/")) {
      return IMAGE;
    }
    if (type.startsWith("video/")) {
      return VIDEO;
    }
    if (type.startsWith("audio/")) {
      return AUDIO;
    }
    if (type.equals("application/pdf")) {
      return DOCUMENT;
    }
    if (type.startsWith("application/vnd.ms-") ||
        type.startsWith("application/vnd.openxmlformats-") ||
        type.equals("application/msword") ||
        type.startsWith("application/vnd.oasis.opendocument")) {
      return DOCUMENT;
    }
    if (type.startsWith("text/")) {
      return DOCUMENT;
    }
    if (type.equals("application/json") || type.equals("application/xml")) {
      return DOCUMENT;
    }
    if (type.equals("application/zip") ||
        type.equals("application/x-rar-compressed") ||
        type.equals("application/x-7z-compressed") ||
        type.equals("application/gzip") ||
        type.equals("application/x-tar") ||
        type.equals("application/x-bzip2")) {
      return ARCHIVE;
    }

    return OTHER;
  }

  /**
   * 파일명에서 확장자를 추출하여 FileType을 찾습니다.
   *
   * @param fileName 파일명 (예: "document.pdf")
   * @return 해당하는 FileType, 확장자가 없거나 지원하지 않으면 {@link #OTHER}
   * @since 1.0.7
   */
  public static FileType fromFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return OTHER;
    }

    int lastDot = fileName.lastIndexOf('.');
    if (lastDot == -1 || lastDot == fileName.length() - 1) {
      return OTHER;
    }

    return fromExtension(fileName.substring(lastDot + 1));
  }

  /**
   * 주어진 확장자가 지원되는지 확인합니다.
   *
   * <p>OTHER 타입에 해당하는 확장자는 {@code false}를 반환합니다.</p>
   *
   * @param extension 확인할 확장자
   * @return 지원 여부 (OTHER면 false)
   */
  public static boolean isSupported(String extension) {
    FileType type = fromExtension(extension);
    return type != OTHER;
  }
}