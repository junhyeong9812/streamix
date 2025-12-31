package io.github.junhyeong9812.streamix.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileType 테스트")
class FileTypeTest {

  @Nested
  @DisplayName("fromExtension 테스트")
  class FromExtensionTest {

    @ParameterizedTest
    @ValueSource(strings = {"jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "tiff"})
    @DisplayName("이미지 확장자는 IMAGE 타입을 반환한다")
    void imageExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.IMAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"JPG", "JPEG", "PNG", "GIF"})
    @DisplayName("대문자 확장자도 IMAGE 타입을 반환한다")
    void upperCaseExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.IMAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mp4", "avi", "mov", "wmv", "mkv", "webm", "flv", "m4v", "mpeg", "3gp"})
    @DisplayName("비디오 확장자는 VIDEO 타입을 반환한다")
    void videoExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.VIDEO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus"})
    @DisplayName("오디오 확장자는 AUDIO 타입을 반환한다")
    void audioExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.AUDIO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml", "html"})
    @DisplayName("문서 확장자는 DOCUMENT 타입을 반환한다")
    void documentExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.DOCUMENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"zip", "rar", "7z", "tar", "gz", "bz2", "tgz"})
    @DisplayName("압축 파일 확장자는 ARCHIVE 타입을 반환한다")
    void archiveExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.ARCHIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"xyz", "abc", "unknown", "exe"})
    @DisplayName("지원하지 않는 확장자는 OTHER를 반환한다")
    void unsupportedExtensionsReturnOther(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.OTHER);
    }

    @Test
    @DisplayName("null이나 빈 문자열은 OTHER를 반환한다")
    void nullOrEmptyReturnsOther() {
      assertThat(FileType.fromExtension(null)).isEqualTo(FileType.OTHER);
      assertThat(FileType.fromExtension("")).isEqualTo(FileType.OTHER);
      assertThat(FileType.fromExtension("  ")).isEqualTo(FileType.OTHER);
    }

    @Test
    @DisplayName("앞에 점이 있어도 정상 처리된다")
    void handlesLeadingDot() {
      assertThat(FileType.fromExtension(".jpg")).isEqualTo(FileType.IMAGE);
      assertThat(FileType.fromExtension(".mp4")).isEqualTo(FileType.VIDEO);
    }
  }

  @Nested
  @DisplayName("fromContentType 테스트")
  class FromContentTypeTest {

    @Test
    @DisplayName("image/* Content-Type은 IMAGE를 반환한다")
    void imageContentType() {
      assertThat(FileType.fromContentType("image/jpeg")).isEqualTo(FileType.IMAGE);
      assertThat(FileType.fromContentType("image/png")).isEqualTo(FileType.IMAGE);
      assertThat(FileType.fromContentType("image/gif")).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("video/* Content-Type은 VIDEO를 반환한다")
    void videoContentType() {
      assertThat(FileType.fromContentType("video/mp4")).isEqualTo(FileType.VIDEO);
      assertThat(FileType.fromContentType("video/webm")).isEqualTo(FileType.VIDEO);
    }

    @Test
    @DisplayName("audio/* Content-Type은 AUDIO를 반환한다")
    void audioContentType() {
      assertThat(FileType.fromContentType("audio/mpeg")).isEqualTo(FileType.AUDIO);
      assertThat(FileType.fromContentType("audio/wav")).isEqualTo(FileType.AUDIO);
    }

    @Test
    @DisplayName("application/pdf는 DOCUMENT를 반환한다")
    void pdfContentType() {
      assertThat(FileType.fromContentType("application/pdf")).isEqualTo(FileType.DOCUMENT);
    }

    @Test
    @DisplayName("MS Office Content-Type은 DOCUMENT를 반환한다")
    void msOfficeContentType() {
      assertThat(FileType.fromContentType("application/msword")).isEqualTo(FileType.DOCUMENT);
      assertThat(FileType.fromContentType("application/vnd.ms-excel")).isEqualTo(FileType.DOCUMENT);
      assertThat(FileType.fromContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
          .isEqualTo(FileType.DOCUMENT);
    }

    @Test
    @DisplayName("text/* Content-Type은 DOCUMENT를 반환한다")
    void textContentType() {
      assertThat(FileType.fromContentType("text/plain")).isEqualTo(FileType.DOCUMENT);
      assertThat(FileType.fromContentType("text/html")).isEqualTo(FileType.DOCUMENT);
    }

    @Test
    @DisplayName("압축 파일 Content-Type은 ARCHIVE를 반환한다")
    void archiveContentType() {
      assertThat(FileType.fromContentType("application/zip")).isEqualTo(FileType.ARCHIVE);
      assertThat(FileType.fromContentType("application/gzip")).isEqualTo(FileType.ARCHIVE);
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 OTHER를 반환한다")
    void unsupportedContentTypeReturnsOther() {
      assertThat(FileType.fromContentType("application/octet-stream")).isEqualTo(FileType.OTHER);
    }

    @Test
    @DisplayName("null Content-Type은 OTHER를 반환한다")
    void nullContentTypeReturnsOther() {
      assertThat(FileType.fromContentType(null)).isEqualTo(FileType.OTHER);
    }
  }

  @Nested
  @DisplayName("fromFileName 테스트")
  class FromFileNameTest {

    @Test
    @DisplayName("파일명에서 확장자를 추출하여 타입을 반환한다")
    void extractsTypeFromFileName() {
      assertThat(FileType.fromFileName("photo.jpg")).isEqualTo(FileType.IMAGE);
      assertThat(FileType.fromFileName("video.mp4")).isEqualTo(FileType.VIDEO);
      assertThat(FileType.fromFileName("music.mp3")).isEqualTo(FileType.AUDIO);
      assertThat(FileType.fromFileName("report.pdf")).isEqualTo(FileType.DOCUMENT);
      assertThat(FileType.fromFileName("backup.zip")).isEqualTo(FileType.ARCHIVE);
    }

    @Test
    @DisplayName("확장자가 없으면 OTHER를 반환한다")
    void noExtensionReturnsOther() {
      assertThat(FileType.fromFileName("noextension")).isEqualTo(FileType.OTHER);
      assertThat(FileType.fromFileName("file.")).isEqualTo(FileType.OTHER);
    }
  }

  @Nested
  @DisplayName("isSupported 테스트")
  class IsSupportedTest {

    @Test
    @DisplayName("지원하는 확장자는 true를 반환한다")
    void supportedExtension() {
      assertThat(FileType.isSupported("jpg")).isTrue();
      assertThat(FileType.isSupported("mp4")).isTrue();
      assertThat(FileType.isSupported("mp3")).isTrue();
      assertThat(FileType.isSupported("pdf")).isTrue();
      assertThat(FileType.isSupported("zip")).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 확장자(OTHER)는 false를 반환한다")
    void unsupportedExtension() {
      assertThat(FileType.isSupported("xyz")).isFalse();
      assertThat(FileType.isSupported("exe")).isFalse();
    }
  }

  @Nested
  @DisplayName("supports 테스트")
  class SupportsTest {

    @Test
    @DisplayName("IMAGE 타입은 이미지 확장자를 지원한다")
    void imageSupportsImageExtensions() {
      assertThat(FileType.IMAGE.supports("jpg")).isTrue();
      assertThat(FileType.IMAGE.supports("png")).isTrue();
      assertThat(FileType.IMAGE.supports("mp4")).isFalse();
    }

    @Test
    @DisplayName("VIDEO 타입은 비디오 확장자를 지원한다")
    void videoSupportsVideoExtensions() {
      assertThat(FileType.VIDEO.supports("mp4")).isTrue();
      assertThat(FileType.VIDEO.supports("avi")).isTrue();
      assertThat(FileType.VIDEO.supports("jpg")).isFalse();
    }

    @Test
    @DisplayName("null 확장자는 false를 반환한다")
    void nullExtensionReturnsFalse() {
      assertThat(FileType.IMAGE.supports(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("supportsThumbnail 테스트")
  class SupportsThumbnailTest {

    @Test
    @DisplayName("IMAGE, VIDEO, DOCUMENT는 썸네일을 지원한다")
    void thumbnailSupportedTypes() {
      assertThat(FileType.IMAGE.supportsThumbnail()).isTrue();
      assertThat(FileType.VIDEO.supportsThumbnail()).isTrue();
      assertThat(FileType.DOCUMENT.supportsThumbnail()).isTrue();
    }

    @Test
    @DisplayName("AUDIO, ARCHIVE, OTHER는 썸네일을 지원하지 않는다")
    void thumbnailUnsupportedTypes() {
      assertThat(FileType.AUDIO.supportsThumbnail()).isFalse();
      assertThat(FileType.ARCHIVE.supportsThumbnail()).isFalse();
      assertThat(FileType.OTHER.supportsThumbnail()).isFalse();
    }
  }

  @Nested
  @DisplayName("isMedia 테스트")
  class IsMediaTest {

    @Test
    @DisplayName("IMAGE, VIDEO, AUDIO는 미디어 타입이다")
    void mediaTypes() {
      assertThat(FileType.IMAGE.isMedia()).isTrue();
      assertThat(FileType.VIDEO.isMedia()).isTrue();
      assertThat(FileType.AUDIO.isMedia()).isTrue();
    }

    @Test
    @DisplayName("DOCUMENT, ARCHIVE, OTHER는 미디어 타입이 아니다")
    void nonMediaTypes() {
      assertThat(FileType.DOCUMENT.isMedia()).isFalse();
      assertThat(FileType.ARCHIVE.isMedia()).isFalse();
      assertThat(FileType.OTHER.isMedia()).isFalse();
    }
  }

  @Nested
  @DisplayName("isStreamable 테스트")
  class IsStreamableTest {

    @Test
    @DisplayName("VIDEO, AUDIO는 스트리밍 가능하다")
    void streamableTypes() {
      assertThat(FileType.VIDEO.isStreamable()).isTrue();
      assertThat(FileType.AUDIO.isStreamable()).isTrue();
    }

    @Test
    @DisplayName("IMAGE, DOCUMENT, ARCHIVE, OTHER는 스트리밍 대상이 아니다")
    void nonStreamableTypes() {
      assertThat(FileType.IMAGE.isStreamable()).isFalse();
      assertThat(FileType.DOCUMENT.isStreamable()).isFalse();
      assertThat(FileType.ARCHIVE.isStreamable()).isFalse();
      assertThat(FileType.OTHER.isStreamable()).isFalse();
    }
  }
}