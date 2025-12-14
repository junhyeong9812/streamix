package io.github.junhyeong9812.streamix.core.adapter.out.thumbnail;

import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImageThumbnailAdapter 테스트")
class ImageThumbnailAdapterTest {

  private ImageThumbnailAdapter adapter;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    adapter = new ImageThumbnailAdapter();
  }

  @Nested
  @DisplayName("supports 테스트")
  class SupportsTest {

    @Test
    @DisplayName("IMAGE 타입을 지원한다")
    void supportsImage() {
      assertThat(adapter.supports(FileType.IMAGE)).isTrue();
    }

    @Test
    @DisplayName("VIDEO 타입은 지원하지 않는다")
    void doesNotSupportVideo() {
      assertThat(adapter.supports(FileType.VIDEO)).isFalse();
    }
  }

  @Nested
  @DisplayName("generate 테스트")
  class GenerateTest {

    @Test
    @DisplayName("InputStream에서 썸네일을 생성한다")
    void generateFromInputStream() throws IOException {
      // given
      byte[] originalImage = createTestImage(800, 600, "png");
      InputStream inputStream = new ByteArrayInputStream(originalImage);

      // when
      byte[] thumbnail = adapter.generate(inputStream, 320, 180);

      // then
      assertThat(thumbnail).isNotNull();
      assertThat(thumbnail.length).isGreaterThan(0);

      // 생성된 이미지 검증
      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      assertThat(result).isNotNull();
      // 비율 유지하므로 정확한 크기는 아닐 수 있음, 최대 크기 이내인지 확인
      assertThat(result.getWidth()).isLessThanOrEqualTo(320);
      assertThat(result.getHeight()).isLessThanOrEqualTo(180);
    }

    @Test
    @DisplayName("JPEG 이미지에서 썸네일을 생성한다")
    void generateFromJpeg() throws IOException {
      // given
      byte[] originalImage = createTestImage(1920, 1080, "jpg");
      InputStream inputStream = new ByteArrayInputStream(originalImage);

      // when
      byte[] thumbnail = adapter.generate(inputStream, 320, 180);

      // then
      assertThat(thumbnail).isNotNull();
      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("작은 이미지도 처리한다")
    void generateFromSmallImage() throws IOException {
      // given - 썸네일보다 작은 원본
      byte[] originalImage = createTestImage(100, 100, "png");
      InputStream inputStream = new ByteArrayInputStream(originalImage);

      // when
      byte[] thumbnail = adapter.generate(inputStream, 320, 180);

      // then
      assertThat(thumbnail).isNotNull();
      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("잘못된 데이터는 예외가 발생한다")
    void throwsForInvalidData() {
      // given
      byte[] invalidData = "not an image".getBytes();
      InputStream inputStream = new ByteArrayInputStream(invalidData);

      // when & then
      assertThatThrownBy(() -> adapter.generate(inputStream, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class);
    }

    @Test
    @DisplayName("빈 스트림은 예외가 발생한다")
    void throwsForEmptyStream() {
      // given
      InputStream inputStream = new ByteArrayInputStream(new byte[0]);

      // when & then
      assertThatThrownBy(() -> adapter.generate(inputStream, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class);
    }
  }

  @Nested
  @DisplayName("generateFromPath 테스트")
  class GenerateFromPathTest {

    @Test
    @DisplayName("파일 경로에서 썸네일을 생성한다")
    void generateFromPath() throws IOException {
      // given
      Path imagePath = createTestImageFile(tempDir, "test.png", 800, 600);

      // when
      byte[] thumbnail = adapter.generateFromPath(imagePath.toString(), 320, 180);

      // then
      assertThat(thumbnail).isNotNull();
      assertThat(thumbnail.length).isGreaterThan(0);

      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      assertThat(result).isNotNull();
      assertThat(result.getWidth()).isLessThanOrEqualTo(320);
      assertThat(result.getHeight()).isLessThanOrEqualTo(180);
    }

    @Test
    @DisplayName("JPEG 파일에서 썸네일을 생성한다")
    void generateFromJpegPath() throws IOException {
      // given
      Path imagePath = createTestImageFile(tempDir, "test.jpg", 1920, 1080);

      // when
      byte[] thumbnail = adapter.generateFromPath(imagePath.toString(), 160, 90);

      // then
      assertThat(thumbnail).isNotNull();
      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 파일은 예외가 발생한다")
    void throwsForNonExistentFile() {
      // given
      String nonExistentPath = tempDir.resolve("nonexistent.jpg").toString();

      // when & then
      assertThatThrownBy(() -> adapter.generateFromPath(nonExistentPath, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class);
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 예외가 발생한다")
    void throwsForNonImageFile() throws IOException {
      // given
      Path textFile = tempDir.resolve("test.txt");
      Files.writeString(textFile, "This is not an image");

      // when & then
      assertThatThrownBy(() -> adapter.generateFromPath(textFile.toString(), 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class);
    }
  }

  @Nested
  @DisplayName("출력 형식 테스트")
  class OutputFormatTest {

    @Test
    @DisplayName("출력은 항상 JPEG 형식이다")
    void outputIsJpeg() throws IOException {
      // given
      byte[] pngImage = createTestImage(400, 300, "png");
      InputStream inputStream = new ByteArrayInputStream(pngImage);

      // when
      byte[] thumbnail = adapter.generate(inputStream, 100, 100);

      // then - JPEG 시그니처 확인 (FFD8)
      assertThat(thumbnail[0] & 0xFF).isEqualTo(0xFF);
      assertThat(thumbnail[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("원본 비율이 유지된다")
    void maintainsAspectRatio() throws IOException {
      // given - 16:9 비율 이미지
      byte[] originalImage = createTestImage(1600, 900, "png");
      InputStream inputStream = new ByteArrayInputStream(originalImage);

      // when
      byte[] thumbnail = adapter.generate(inputStream, 320, 320);

      // then
      BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));
      double ratio = (double) result.getWidth() / result.getHeight();
      // 16:9 ≈ 1.77
      assertThat(ratio).isBetween(1.7, 1.8);
    }
  }

  /**
   * 테스트용 이미지를 생성합니다.
   */
  private byte[] createTestImage(int width, int height, String format) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // 배경
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 0, width, height);

    // 패턴
    g2d.setColor(Color.WHITE);
    g2d.fillOval(width / 4, height / 4, width / 2, height / 2);

    g2d.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, format, baos);
    return baos.toByteArray();
  }

  /**
   * 테스트용 이미지 파일을 생성합니다.
   */
  private Path createTestImageFile(Path dir, String fileName, int width, int height) throws IOException {
    String format = fileName.substring(fileName.lastIndexOf('.') + 1);
    byte[] imageData = createTestImage(width, height, format);

    Path filePath = dir.resolve(fileName);
    Files.write(filePath, imageData);
    return filePath;
  }
}