package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("ThumbnailService 테스트")
class ThumbnailServiceTest {

  @Nested
  @DisplayName("기본 생성자 테스트")
  class DefaultConstructorTest {

    private ThumbnailService thumbnailService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
      thumbnailService = new ThumbnailService();
    }

    @Test
    @DisplayName("기본 생성자로 서비스를 생성한다")
    void createWithDefaultConstructor() {
      assertThat(thumbnailService).isNotNull();
    }

    @Test
    @DisplayName("IMAGE 타입을 지원한다")
    void supportsImage() {
      assertThat(thumbnailService.supports(FileType.IMAGE)).isTrue();
    }

    @Test
    @DisplayName("VIDEO 타입은 기본적으로 지원하지 않는다")
    void doesNotSupportVideoByDefault() {
      assertThat(thumbnailService.supports(FileType.VIDEO)).isFalse();
    }

    @Test
    @DisplayName("이미지 파일에서 썸네일을 생성한다")
    void generateImageThumbnail() throws IOException {
      // given
      Path imagePath = createTestImageFile(tempDir, "test.png", 800, 600);

      // when
      byte[] thumbnail = thumbnailService.generate(FileType.IMAGE, imagePath.toString(), 320, 180);

      // then
      assertThat(thumbnail).isNotNull();
      assertThat(thumbnail.length).isGreaterThan(0);

      // JPEG 시그니처 확인
      assertThat(thumbnail[0] & 0xFF).isEqualTo(0xFF);
      assertThat(thumbnail[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("InputStream에서 이미지 썸네일을 생성한다")
    void generateImageThumbnailFromStream() throws IOException {
      // given
      byte[] imageData = createTestImage(400, 300, "png");
      InputStream inputStream = new ByteArrayInputStream(imageData);

      // when
      byte[] thumbnail = thumbnailService.generateFromStream(FileType.IMAGE, inputStream, 100, 100);

      // then
      assertThat(thumbnail).isNotNull();
      assertThat(thumbnail.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("비디오 썸네일 생성 시 예외가 발생한다 (생성기 미등록)")
    void throwsForVideoWithoutGenerator() {
      // when & then
      assertThatThrownBy(() ->
          thumbnailService.generate(FileType.VIDEO, "/path/to/video.mp4", 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class)
          .hasMessageContaining("No thumbnail generator available")
          .hasMessageContaining("FFmpeg");
    }

    @Test
    @DisplayName("비디오는 InputStream에서 썸네일 생성을 지원하지 않는다")
    void throwsForVideoInputStream() {
      // given
      InputStream inputStream = new ByteArrayInputStream(new byte[100]);

      // when & then
      assertThatThrownBy(() ->
          thumbnailService.generateFromStream(FileType.VIDEO, inputStream, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class)
          .hasMessageContaining("not supported");
    }
  }

  @Nested
  @DisplayName("커스텀 생성기 주입 테스트")
  @ExtendWith(MockitoExtension.class)
  class CustomGeneratorTest {

    @Mock
    private ThumbnailGeneratorPort mockImageGenerator;

    @Mock
    private ThumbnailGeneratorPort mockVideoGenerator;

    @Test
    @DisplayName("비디오 생성기가 주입되면 VIDEO를 지원한다")
    void supportsVideoWhenGeneratorInjected() {
      // given
      ThumbnailService service = new ThumbnailService(mockImageGenerator, mockVideoGenerator);

      // then
      assertThat(service.supports(FileType.VIDEO)).isTrue();
    }

    @Test
    @DisplayName("이미지 요청 시 이미지 생성기를 사용한다")
    void usesImageGeneratorForImage() {
      // given
      ThumbnailService service = new ThumbnailService(mockImageGenerator, mockVideoGenerator);
      byte[] expectedThumbnail = new byte[100];
      given(mockImageGenerator.generateFromPath(anyString(), anyInt(), anyInt()))
          .willReturn(expectedThumbnail);

      // when
      byte[] result = service.generate(FileType.IMAGE, "/path/to/image.jpg", 320, 180);

      // then
      assertThat(result).isEqualTo(expectedThumbnail);
      verify(mockImageGenerator).generateFromPath("/path/to/image.jpg", 320, 180);
    }

    @Test
    @DisplayName("비디오 요청 시 비디오 생성기를 사용한다")
    void usesVideoGeneratorForVideo() {
      // given
      ThumbnailService service = new ThumbnailService(mockImageGenerator, mockVideoGenerator);
      byte[] expectedThumbnail = new byte[100];
      given(mockVideoGenerator.generateFromPath(anyString(), anyInt(), anyInt()))
          .willReturn(expectedThumbnail);

      // when
      byte[] result = service.generate(FileType.VIDEO, "/path/to/video.mp4", 320, 180);

      // then
      assertThat(result).isEqualTo(expectedThumbnail);
      verify(mockVideoGenerator).generateFromPath("/path/to/video.mp4", 320, 180);
    }

    @Test
    @DisplayName("InputStream 이미지 요청 시 이미지 생성기를 사용한다")
    void usesImageGeneratorForStreamImage() {
      // given
      ThumbnailService service = new ThumbnailService(mockImageGenerator, mockVideoGenerator);
      byte[] expectedThumbnail = new byte[100];
      InputStream inputStream = new ByteArrayInputStream(new byte[50]);
      given(mockImageGenerator.generate(any(InputStream.class), anyInt(), anyInt()))
          .willReturn(expectedThumbnail);

      // when
      byte[] result = service.generateFromStream(FileType.IMAGE, inputStream, 320, 180);

      // then
      assertThat(result).isEqualTo(expectedThumbnail);
      verify(mockImageGenerator).generate(any(InputStream.class), eq(320), eq(180));
    }
  }

  @Nested
  @DisplayName("null 생성기 처리 테스트")
  class NullGeneratorTest {

    @Test
    @DisplayName("null 이미지 생성기는 기본 어댑터로 대체된다")
    void nullImageGeneratorReplacedWithDefault() {
      // given
      ThumbnailService service = new ThumbnailService(null, null);

      // when & then - 예외 없이 생성되고 IMAGE 지원
      assertThat(service).isNotNull();
      assertThat(service.supports(FileType.IMAGE)).isTrue();
    }

    @Test
    @DisplayName("null 비디오 생성기는 VIDEO 미지원")
    void nullVideoGeneratorMeansNoVideoSupport() {
      // given
      ThumbnailService service = new ThumbnailService(null, null);

      // when & then
      assertThat(service.supports(FileType.VIDEO)).isFalse();
    }
  }

  /**
   * 테스트용 이미지를 생성합니다.
   */
  private byte[] createTestImage(int width, int height, String format) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 0, width, height);
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