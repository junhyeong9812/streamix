package io.github.junhyeong9812.streamix.core.domain.service;

import io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileTypeDetector 테스트")
class FileTypeDetectorTest {

  private FileTypeDetector detector;

  @BeforeEach
  void setUp() {
    detector = new FileTypeDetector();
  }

  @Nested
  @DisplayName("extractExtension 테스트")
  class ExtractExtensionTest {

    @Test
    @DisplayName("파일명에서 확장자를 추출한다")
    void extractsExtension() {
      assertThat(detector.extractExtension("photo.jpg")).isEqualTo("jpg");
      assertThat(detector.extractExtension("video.MP4")).isEqualTo("mp4");
      assertThat(detector.extractExtension("file.name.png")).isEqualTo("png");
    }

    @Test
    @DisplayName("확장자가 없으면 빈 문자열을 반환한다")
    void returnsEmptyWhenNoExtension() {
      assertThat(detector.extractExtension("noextension")).isEmpty();
      assertThat(detector.extractExtension("file.")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("null이나 빈 문자열은 빈 문자열을 반환한다")
    void returnsEmptyForNullOrEmpty(String fileName) {
      assertThat(detector.extractExtension(fileName)).isEmpty();
    }
  }

  @Nested
  @DisplayName("detect(fileName) 테스트")
  class DetectByFileNameTest {

    @Test
    @DisplayName("이미지 파일명은 IMAGE를 반환한다")
    void detectsImage() {
      assertThat(detector.detect("photo.jpg")).isEqualTo(FileType.IMAGE);
      assertThat(detector.detect("image.png")).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("비디오 파일명은 VIDEO를 반환한다")
    void detectsVideo() {
      assertThat(detector.detect("movie.mp4")).isEqualTo(FileType.VIDEO);
      assertThat(detector.detect("clip.avi")).isEqualTo(FileType.VIDEO);
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    void throwsForUnsupportedExtension() {
      assertThatThrownBy(() -> detector.detect("document.pdf"))
          .isInstanceOf(InvalidFileTypeException.class);
    }
  }

  @Nested
  @DisplayName("detect(fileName, contentType) 테스트")
  class DetectByFileNameAndContentTypeTest {

    @Test
    @DisplayName("확장자가 우선 적용된다")
    void extensionTakesPriority() {
      // 확장자는 jpg인데 contentType은 video/mp4 -> 확장자 우선
      FileType result = detector.detect("photo.jpg", "video/mp4");
      assertThat(result).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("확장자가 없으면 contentType으로 판단한다")
    void fallsBackToContentType() {
      FileType result = detector.detect("noextension", "image/jpeg");
      assertThat(result).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("둘 다 없으면 예외가 발생한다")
    void throwsWhenBothUnsupported() {
      assertThatThrownBy(() -> detector.detect("file.txt", "text/plain"))
          .isInstanceOf(InvalidFileTypeException.class);
    }
  }

  @Nested
  @DisplayName("getContentType 테스트")
  class GetContentTypeTest {

    @ParameterizedTest
    @CsvSource({
        "jpg, image/jpeg",
        "jpeg, image/jpeg",
        "png, image/png",
        "gif, image/gif",
        "mp4, video/mp4",
        "avi, video/x-msvideo",
        "webm, video/webm"
    })
    @DisplayName("확장자에 해당하는 Content-Type을 반환한다")
    void returnsContentType(String extension, String expectedContentType) {
      assertThat(detector.getContentType(extension)).isEqualTo(expectedContentType);
    }

    @Test
    @DisplayName("알 수 없는 확장자는 application/octet-stream을 반환한다")
    void returnsDefaultForUnknown() {
      assertThat(detector.getContentType("xyz")).isEqualTo("application/octet-stream");
    }
  }

  @Nested
  @DisplayName("isSupported 테스트")
  class IsSupportedTest {

    @Test
    @DisplayName("지원하는 파일명은 true를 반환한다")
    void returnsTrueForSupported() {
      assertThat(detector.isSupported("photo.jpg")).isTrue();
      assertThat(detector.isSupported("video.mp4")).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 파일명은 false를 반환한다")
    void returnsFalseForUnsupported() {
      assertThat(detector.isSupported("document.pdf")).isFalse();
      assertThat(detector.isSupported("script.js")).isFalse();
    }

    @Test
    @DisplayName("fileName과 contentType 조합으로 지원 여부를 확인한다")
    void checksSupportWithContentType() {
      assertThat(detector.isSupported("file", "image/jpeg")).isTrue();
      assertThat(detector.isSupported("file", "text/plain")).isFalse();
    }
  }
}