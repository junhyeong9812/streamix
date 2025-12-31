package io.github.junhyeong9812.streamix.core.domain.service;

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
    @DisplayName("오디오 파일명은 AUDIO를 반환한다")
    void detectsAudio() {
      assertThat(detector.detect("music.mp3")).isEqualTo(FileType.AUDIO);
      assertThat(detector.detect("sound.wav")).isEqualTo(FileType.AUDIO);
    }

    @Test
    @DisplayName("문서 파일명은 DOCUMENT를 반환한다")
    void detectsDocument() {
      assertThat(detector.detect("report.pdf")).isEqualTo(FileType.DOCUMENT);
      assertThat(detector.detect("data.xlsx")).isEqualTo(FileType.DOCUMENT);
    }

    @Test
    @DisplayName("압축 파일명은 ARCHIVE를 반환한다")
    void detectsArchive() {
      assertThat(detector.detect("backup.zip")).isEqualTo(FileType.ARCHIVE);
      assertThat(detector.detect("files.tar")).isEqualTo(FileType.ARCHIVE);
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 OTHER를 반환한다")
    void returnsOtherForUnsupportedExtension() {
      assertThat(detector.detect("unknown.xyz")).isEqualTo(FileType.OTHER);
      assertThat(detector.detect("program.exe")).isEqualTo(FileType.OTHER);
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
    @DisplayName("확장자가 OTHER면 contentType으로 판단한다")
    void fallsBackToContentType() {
      FileType result = detector.detect("noextension", "image/jpeg");
      assertThat(result).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("둘 다 OTHER면 OTHER를 반환한다")
    void returnsOtherWhenBothUnsupported() {
      FileType result = detector.detect("file.xyz", "application/octet-stream");
      assertThat(result).isEqualTo(FileType.OTHER);
    }

    @Test
    @DisplayName("확장자 없는 파일도 contentType으로 감지한다")
    void detectsWithoutExtension() {
      assertThat(detector.detect("file", "audio/mpeg")).isEqualTo(FileType.AUDIO);
      assertThat(detector.detect("file", "application/pdf")).isEqualTo(FileType.DOCUMENT);
      assertThat(detector.detect("file", "application/zip")).isEqualTo(FileType.ARCHIVE);
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
        "webm, video/webm",
        "mp3, audio/mpeg",
        "wav, audio/wav",
        "pdf, application/pdf",
        "zip, application/zip"
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

    @Test
    @DisplayName("null은 application/octet-stream을 반환한다")
    void returnsDefaultForNull() {
      assertThat(detector.getContentType(null)).isEqualTo("application/octet-stream");
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
      assertThat(detector.isSupported("music.mp3")).isTrue();
      assertThat(detector.isSupported("report.pdf")).isTrue();
      assertThat(detector.isSupported("backup.zip")).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 파일명(OTHER)은 false를 반환한다")
    void returnsFalseForUnsupported() {
      assertThat(detector.isSupported("unknown.xyz")).isFalse();
      assertThat(detector.isSupported("program.exe")).isFalse();
    }

    @Test
    @DisplayName("fileName과 contentType 조합으로 지원 여부를 확인한다")
    void checksSupportWithContentType() {
      assertThat(detector.isSupported("file", "image/jpeg")).isTrue();
      assertThat(detector.isSupported("file", "audio/mpeg")).isTrue();
      assertThat(detector.isSupported("file", "application/octet-stream")).isFalse();
    }
  }

  @Nested
  @DisplayName("detectAllowingAll 테스트")
  class DetectAllowingAllTest {

    @Test
    @DisplayName("모든 파일을 허용하며 OTHER도 반환한다")
    void allowsAllTypes() {
      assertThat(detector.detectAllowingAll("photo.jpg")).isEqualTo(FileType.IMAGE);
      assertThat(detector.detectAllowingAll("unknown.xyz")).isEqualTo(FileType.OTHER);
    }
  }
}