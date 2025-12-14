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
 *   <tr><td>IMAGE</td><td>jpg, jpeg</td><td>image/jpeg</td></tr>
 *   <tr><td>IMAGE</td><td>png</td><td>image/png</td></tr>
 *   <tr><td>IMAGE</td><td>gif</td><td>image/gif</td></tr>
 *   <tr><td>VIDEO</td><td>mp4</td><td>video/mp4</td></tr>
 *   <tr><td>VIDEO</td><td>avi</td><td>video/x-msvideo</td></tr>
 *   <tr><td>VIDEO</td><td>webm</td><td>video/webm</td></tr>
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
 * // 확장자 추출
 * String ext = detector.extractExtension("photo.JPG");
 * // ext == "jpg"
 *
 * // Content-Type 조회
 * String contentType = detector.getContentType("png");
 * // contentType == "image/png"
 *
 * // 지원 여부 확인
 * boolean supported = detector.isSupported("document.pdf");
 * // supported == false
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
      // Videos
      Map.entry("mp4", "video/mp4"),
      Map.entry("avi", "video/x-msvideo"),
      Map.entry("mov", "video/quicktime"),
      Map.entry("wmv", "video/x-ms-wmv"),
      Map.entry("mkv", "video/x-matroska"),
      Map.entry("webm", "video/webm"),
      Map.entry("flv", "video/x-flv")
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
   * <p>파일명에서 확장자를 추출하여 타입을 결정합니다.</p>
   *
   * @param fileName 파일명
   * @return 감지된 FileType
   * @throws InvalidFileTypeException 지원하지 않는 확장자인 경우
   */
  public FileType detect(String fileName) {
    String extension = extractExtension(fileName);
    FileType type = FileType.fromExtension(extension);

    if (type == null) {
      throw new InvalidFileTypeException(extension);
    }

    return type;
  }

  /**
   * 파일명과 Content-Type으로 FileType을 감지합니다.
   *
   * <p>확장자를 우선으로 판단하고, 확장자로 판단할 수 없으면
   * Content-Type으로 판단합니다.</p>
   *
   * @param fileName    파일명
   * @param contentType HTTP Content-Type 헤더 값
   * @return 감지된 FileType
   * @throws InvalidFileTypeException 둘 다로 타입을 판단할 수 없는 경우
   */
  public FileType detect(String fileName, String contentType) {
    String extension = extractExtension(fileName);

    // 우선 확장자로 판단
    FileType typeByExt = FileType.fromExtension(extension);

    // Content-Type으로 판단
    FileType typeByContent = FileType.fromContentType(contentType);

    // 둘 다 있으면 확장자 우선
    if (typeByExt != null) {
      return typeByExt;
    }

    if (typeByContent != null) {
      return typeByContent;
    }

    throw new InvalidFileTypeException(extension, contentType);
  }

  /**
   * 확장자에 해당하는 Content-Type을 반환합니다.
   *
   * @param extension 파일 확장자 (예: "jpg", "mp4")
   * @return MIME Content-Type, 알 수 없는 확장자면 "application/octet-stream"
   */
  public String getContentType(String extension) {
    return EXTENSION_TO_CONTENT_TYPE.getOrDefault(
        extension.toLowerCase(),
        "application/octet-stream"
    );
  }

  /**
   * 파일명이 지원되는 형식인지 확인합니다.
   *
   * @param fileName 파일명
   * @return 지원 여부
   */
  public boolean isSupported(String fileName) {
    String extension = extractExtension(fileName);
    return FileType.isSupported(extension);
  }

  /**
   * 파일명과 Content-Type 조합이 지원되는지 확인합니다.
   *
   * <p>예외를 발생시키지 않고 boolean으로 결과를 반환합니다.</p>
   *
   * @param fileName    파일명
   * @param contentType Content-Type
   * @return 지원 여부
   */
  public boolean isSupported(String fileName, String contentType) {
    try {
      detect(fileName, contentType);
      return true;
    } catch (InvalidFileTypeException e) {
      return false;
    }
  }
}