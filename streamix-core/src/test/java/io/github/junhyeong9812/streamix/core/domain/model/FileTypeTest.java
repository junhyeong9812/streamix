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
    @ValueSource(strings = {"jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"})
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
    @ValueSource(strings = {"mp4", "avi", "mov", "wmv", "mkv", "webm", "flv"})
    @DisplayName("비디오 확장자는 VIDEO 타입을 반환한다")
    void videoExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isEqualTo(FileType.VIDEO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "pdf", "doc", "exe", "zip"})
    @DisplayName("지원하지 않는 확장자는 null을 반환한다")
    void unsupportedExtensions(String ext) {
      FileType result = FileType.fromExtension(ext);
      assertThat(result).isNull();
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
    @DisplayName("지원하지 않는 Content-Type은 null을 반환한다")
    void unsupportedContentType() {
      assertThat(FileType.fromContentType("application/pdf")).isNull();
      assertThat(FileType.fromContentType("text/plain")).isNull();
    }

    @Test
    @DisplayName("null Content-Type은 null을 반환한다")
    void nullContentType() {
      assertThat(FileType.fromContentType(null)).isNull();
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
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 false를 반환한다")
    void unsupportedExtension() {
      assertThat(FileType.isSupported("txt")).isFalse();
      assertThat(FileType.isSupported("pdf")).isFalse();
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
  }
}