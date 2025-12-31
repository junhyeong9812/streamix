package io.github.junhyeong9812.streamix.core.domain.service;

import io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;

import java.util.Map;

/**
 * 파일 타입을 감지하는 도메인 서비스입니다.
 *
 * <p>파일명(확장자)이나 Content-Type을 기반으로 파일 타입을 감지하고,
 * 확장자에 해당하는 MIME Content-Type을 조회할 수 있습니다.</p>
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>파일명에서 확장자 추출</li>
 *   <li>확장자 또는 Content-Type으로 FileType 감지</li>
 *   <li>확장자에 해당하는 Content-Type 조회</li>
 *   <li>지원 파일 여부 확인</li>
 * </ul>
 *
 * <h2>지원 파일 형식</h2>
 * <table border="1">
 *   <caption>지원하는 파일 타입 및 MIME 타입</caption>
 *   <tr><th>타입</th><th>확장자</th><th>Content-Type</th></tr>
 *   <tr><td>IMAGE</td><td>jpg, jpeg, png, gif, webp, bmp, svg</td><td>image/*</td></tr>
 *   <tr><td>VIDEO</td><td>mp4, avi, mov, mkv, webm</td><td>video/*</td></tr>
 *   <tr><td>AUDIO</td><td>mp3, wav, flac, aac, ogg</td><td>audio/*</td></tr>
 *   <tr><td>DOCUMENT</td><td>pdf, doc, docx, xls, xlsx, txt</td><td>application/pdf, text/*</td></tr>
 *   <tr><td>ARCHIVE</td><td>zip, rar, 7z, tar, gz</td><td>application/zip</td></tr>
 *   <tr><td>OTHER</td><td>기타 모든 파일</td><td>application/octet-stream</td></tr>
 * </table>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * FileTypeDetector detector = new FileTypeDetector();
 *
 * // 파일명에서 타입 감지
 * FileType type = detector.detect("video.mp4");
 * // type == FileType.VIDEO
 *
 * // 알 수 없는 확장자도 OTHER로 반환
 * FileType type = detector.detect("data.xyz");
 * // type == FileType.OTHER
 *
 * // 확장자 추출
 * String ext = detector.extractExtension("photo.JPG");
 * // ext == "jpg"
 *
 * // Content-Type 조회
 * String contentType = detector.getContentType("png");
 * // contentType == "image/png"
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileType
 * @see InvalidFileTypeException
 */
public class FileTypeDetector {

  /**
   * FileTypeDetector의 기본 생성자입니다.
   */
  public FileTypeDetector() {
  }

  /**
   * 확장자별 Content-Type 매핑 테이블.
   */
  private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(
      // Images
      Map.entry("jpg", "image/jpeg"),
      Map.entry("jpeg", "image/jpeg"),
      Map.entry("png", "image/png"),
      Map.entry("gif", "image/gif"),
      Map.entry("webp", "image/webp"),
      Map.entry("bmp", "image/bmp"),
      Map.entry("svg", "image/svg+xml"),
      Map.entry("ico", "image/x-icon"),
      Map.entry("tiff", "image/tiff"),
      Map.entry("tif", "image/tiff"),
      // Videos
      Map.entry("mp4", "video/mp4"),
      Map.entry("avi", "video/x-msvideo"),
      Map.entry("mov", "video/quicktime"),
      Map.entry("wmv", "video/x-ms-wmv"),
      Map.entry("mkv", "video/x-matroska"),
      Map.entry("webm", "video/webm"),
      Map.entry("flv", "video/x-flv"),
      Map.entry("m4v", "video/x-m4v"),
      Map.entry("mpeg", "video/mpeg"),
      Map.entry("mpg", "video/mpeg"),
      Map.entry("3gp", "video/3gpp"),
      // Audios (1.0.7+)
      Map.entry("mp3", "audio/mpeg"),
      Map.entry("wav", "audio/wav"),
      Map.entry("flac", "audio/flac"),
      Map.entry("aac", "audio/aac"),
      Map.entry("ogg", "audio/ogg"),
      Map.entry("m4a", "audio/mp4"),
      Map.entry("wma", "audio/x-ms-wma"),
      Map.entry("opus", "audio/opus"),
      Map.entry("aiff", "audio/aiff"),
      // Documents (1.0.7+)
      Map.entry("pdf", "application/pdf"),
      Map.entry("doc", "application/msword"),
      Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
      Map.entry("xls", "application/vnd.ms-excel"),
      Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
      Map.entry("ppt", "application/vnd.ms-powerpoint"),
      Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
      Map.entry("txt", "text/plain"),
      Map.entry("rtf", "application/rtf"),
      Map.entry("csv", "text/csv"),
      Map.entry("md", "text/markdown"),
      Map.entry("json", "application/json"),
      Map.entry("xml", "application/xml"),
      Map.entry("html", "text/html"),
      Map.entry("htm", "text/html"),
      // Archives (1.0.7+)
      Map.entry("zip", "application/zip"),
      Map.entry("rar", "application/x-rar-compressed"),
      Map.entry("7z", "application/x-7z-compressed"),
      Map.entry("tar", "application/x-tar"),
      Map.entry("gz", "application/gzip"),
      Map.entry("bz2", "application/x-bzip2"),
      Map.entry("xz", "application/x-xz"),
      Map.entry("tgz", "application/gzip")
  );

  /**
   * 파일명에서 확장자를 추출합니다.
   *
   * <p>대소문자를 구분하지 않고 소문자로 반환합니다.</p>
   *
   * @param fileName 파일명 (예: "photo.JPG")
   * @return 소문자 확장자 (예: "jpg"), 확장자가 없으면 빈 문자열
   */
  public String extractExtension(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "";
    }

    int lastDot = fileName.lastIndexOf('.');
    if (lastDot == -1 || lastDot == fileName.length() - 1) {
      return "";
    }

    return fileName.substring(lastDot + 1).toLowerCase();
  }

  /**
   * 파일명으로 FileType을 감지합니다.
   *
   * <p>파일명에서 확장자를 추출하여 타입을 결정합니다.
   * 알 수 없는 확장자는 {@link FileType#OTHER}를 반환합니다.</p>
   *
   * @param fileName 파일명
   * @return 감지된 FileType (알 수 없으면 OTHER)
   */
  public FileType detect(String fileName) {
    String extension = extractExtension(fileName);
    return FileType.fromExtension(extension);
  }

  /**
   * 파일명과 Content-Type으로 FileType을 감지합니다.
   *
   * <p>확장자를 우선으로 판단하고, 확장자로 OTHER가 반환되면
   * Content-Type으로 재시도합니다.</p>
   *
   * @param fileName    파일명
   * @param contentType HTTP Content-Type 헤더 값
   * @return 감지된 FileType (알 수 없으면 OTHER)
   */
  public FileType detect(String fileName, String contentType) {
    // 우선 확장자로 판단
    FileType typeByExt = FileType.fromExtension(extractExtension(fileName));

    // OTHER가 아니면 확장자 결과 사용
    if (typeByExt != FileType.OTHER) {
      return typeByExt;
    }

    // Content-Type으로 재시도
    return FileType.fromContentType(contentType);
  }

  /**
   * 확장자에 해당하는 Content-Type을 반환합니다.
   *
   * @param extension 파일 확장자 (예: "jpg", "mp4")
   * @return MIME Content-Type, 알 수 없는 확장자면 "application/octet-stream"
   */
  public String getContentType(String extension) {
    if (extension == null) {
      return "application/octet-stream";
    }
    return EXTENSION_TO_CONTENT_TYPE.getOrDefault(
        extension.toLowerCase(),
        "application/octet-stream"
    );
  }

  /**
   * 파일명이 지원되는 형식인지 확인합니다.
   *
   * <p>OTHER 타입은 지원되지 않는 것으로 간주합니다.</p>
   *
   * @param fileName 파일명
   * @return 지원 여부 (OTHER면 false)
   */
  public boolean isSupported(String fileName) {
    String extension = extractExtension(fileName);
    return FileType.isSupported(extension);
  }

  /**
   * 파일명과 Content-Type 조합이 지원되는지 확인합니다.
   *
   * <p>OTHER 타입은 지원되지 않는 것으로 간주합니다.</p>
   *
   * @param fileName    파일명
   * @param contentType Content-Type
   * @return 지원 여부 (OTHER면 false)
   */
  public boolean isSupported(String fileName, String contentType) {
    FileType type = detect(fileName, contentType);
    return type != FileType.OTHER;
  }

  /**
   * 모든 파일을 허용할 때 FileType을 감지합니다.
   *
   * <p>{@link #detect(String)}과 동일하지만, 의도를 명확히 합니다.
   * OTHER 타입도 정상적으로 반환됩니다.</p>
   *
   * @param fileName 파일명
   * @return 감지된 FileType
   * @since 1.0.7
   */
  public FileType detectAllowingAll(String fileName) {
    return detect(fileName);
  }
}