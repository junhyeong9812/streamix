package io.github.junhyeong9812.streamix.core.adapter.out.thumbnail;

import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Thumbnailator를 이용한 이미지 썸네일 생성 어댑터입니다.
 *
 * <p>{@link ThumbnailGeneratorPort}를 구현하며, 이미지 파일에 대한
 * 썸네일을 생성합니다. {@link FileType#IMAGE} 타입만 지원합니다.</p>
 *
 * <h2>지원 이미지 형식</h2>
 * <ul>
 *   <li>JPEG / JPG</li>
 *   <li>PNG</li>
 *   <li>GIF</li>
 *   <li>BMP</li>
 *   <li>WebP (Java 버전에 따라 지원)</li>
 * </ul>
 *
 * <h2>출력 형식</h2>
 * <ul>
 *   <li>형식: JPEG</li>
 *   <li>품질: 80%</li>
 *   <li>비율: 원본 비율 유지 (지정된 크기 내에서 fit)</li>
 * </ul>
 *
 * <h2>의존성</h2>
 * <p>Thumbnailator 라이브러리가 필요합니다:</p>
 * <pre>
 * implementation 'net.coobird:thumbnailator:0.4.20'
 * </pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * ThumbnailGeneratorPort generator = new ImageThumbnailAdapter();
 *
 * // 스트림에서 썸네일 생성
 * try (InputStream is = Files.newInputStream(imagePath)) {
 *     byte[] thumbnail = generator.generate(is, 320, 180);
 *     // thumbnail은 JPEG 이미지 바이트 배열
 * }
 *
 * // 파일 경로에서 썸네일 생성
 * byte[] thumbnail = generator.generateFromPath("/path/to/image.jpg", 320, 180);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see ThumbnailGeneratorPort
 * @see FileType#IMAGE
 */
public class ImageThumbnailAdapter implements ThumbnailGeneratorPort {

  private static final Logger log = LoggerFactory.getLogger(ImageThumbnailAdapter.class);

  /**
   * 출력 JPEG 품질 (0.0 ~ 1.0).
   */
  private static final double OUTPUT_QUALITY = 0.8;

  /**
   * 출력 이미지 형식.
   */
  private static final String OUTPUT_FORMAT = "jpg";

  /**
   * {@inheritDoc}
   *
   * @return {@link FileType#IMAGE}일 때만 {@code true}
   */
  @Override
  public boolean supports(FileType fileType) {
    return fileType == FileType.IMAGE;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Thumbnailator를 사용하여 이미지를 리사이즈합니다.
   * 원본 비율을 유지하며 지정된 크기에 맞춥니다.</p>
   *
   * @throws ThumbnailGenerationException 썸네일 생성 실패 시
   */
  @Override
  public byte[] generate(InputStream sourceStream, int width, int height) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      Thumbnails.of(sourceStream)
          .size(width, height)
          .outputFormat(OUTPUT_FORMAT)
          .outputQuality(OUTPUT_QUALITY)
          .toOutputStream(outputStream);

      log.debug("Thumbnail generated: {}x{}", width, height);
      return outputStream.toByteArray();

    } catch (IOException e) {
      throw new ThumbnailGenerationException("Failed to generate image thumbnail", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>파일 경로에서 직접 이미지를 읽어 썸네일을 생성합니다.
   * 이미지 파일의 경우 이 메서드와 {@link #generate}의 결과는 동일합니다.</p>
   *
   * @throws ThumbnailGenerationException 썸네일 생성 실패 시
   */
  @Override
  public byte[] generateFromPath(String sourcePath, int width, int height) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      Thumbnails.of(sourcePath)
          .size(width, height)
          .outputFormat(OUTPUT_FORMAT)
          .outputQuality(OUTPUT_QUALITY)
          .toOutputStream(outputStream);

      log.debug("Thumbnail generated from path: {}", sourcePath);
      return outputStream.toByteArray();

    } catch (IOException e) {
      throw new ThumbnailGenerationException("Failed to generate thumbnail from path: " + sourcePath, e);
    }
  }
}