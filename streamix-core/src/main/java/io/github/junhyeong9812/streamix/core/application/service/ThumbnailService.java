package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.adapter.out.thumbnail.ImageThumbnailAdapter;
import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 썸네일 생성을 통합 관리하는 서비스입니다.
 *
 * <p>Composite 패턴을 적용하여 이미지와 비디오 썸네일 생성을 단일 진입점으로 제공합니다.
 * 파일 타입에 따라 적절한 {@link ThumbnailGeneratorPort} 구현체를 선택하여 처리합니다.</p>
 *
 * <h2>기본 구성</h2>
 * <ul>
 *   <li>이미지: {@link ImageThumbnailAdapter} (Thumbnailator 기반, 기본 제공)</li>
 *   <li>비디오: 기본 미제공 (Starter에서 FFmpeg 기반 구현체 주입 필요)</li>
 * </ul>
 *
 * <h2>확장성</h2>
 * <p>생성자를 통해 비디오 썸네일 생성기를 주입할 수 있습니다.
 * streamix-spring-boot-starter에서는 FFmpeg 기반 비디오 썸네일 생성기를 제공합니다.</p>
 *
 * <pre>{@code
 * // 기본 사용 (이미지만 지원)
 * ThumbnailService service = new ThumbnailService();
 *
 * // 비디오 지원 추가 (Starter에서 제공)
 * ThumbnailService service = new ThumbnailService(
 *     new ImageThumbnailAdapter(),
 *     ffmpegThumbnailAdapter  // Starter에서 제공
 * );
 * }</pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * ThumbnailService service = new ThumbnailService();
 *
 * // 이미지 썸네일 생성
 * byte[] thumbnail = service.generate(FileType.IMAGE, "/path/to/image.jpg", 320, 180);
 *
 * // 비디오 썸네일 (비디오 생성기가 주입된 경우에만 동작)
 * if (service.supports(FileType.VIDEO)) {
 *     byte[] videoThumb = service.generate(FileType.VIDEO, "/path/to/video.mp4", 320, 180);
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see ThumbnailGeneratorPort
 * @see ImageThumbnailAdapter
 */
public class ThumbnailService {

  private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

  private final ThumbnailGeneratorPort imageGenerator;
  private final ThumbnailGeneratorPort videoGenerator;

  /**
   * 기본 생성자 - 이미지 썸네일만 지원합니다.
   *
   * <p>비디오 썸네일이 필요하면 {@link #ThumbnailService(ThumbnailGeneratorPort, ThumbnailGeneratorPort)}를
   * 사용하여 비디오 생성기를 주입하세요.</p>
   */
  public ThumbnailService() {
    this.imageGenerator = new ImageThumbnailAdapter();
    this.videoGenerator = null;
  }

  /**
   * 커스텀 생성기로 서비스를 생성합니다.
   *
   * @param imageGenerator 이미지 썸네일 생성기 (null이면 기본 어댑터 사용)
   * @param videoGenerator 비디오 썸네일 생성기 (null이면 비디오 미지원)
   */
  public ThumbnailService(ThumbnailGeneratorPort imageGenerator, ThumbnailGeneratorPort videoGenerator) {
    this.imageGenerator = imageGenerator != null ? imageGenerator : new ImageThumbnailAdapter();
    this.videoGenerator = videoGenerator;
  }

  /**
   * 파일 경로에서 썸네일을 생성합니다.
   *
   * @param fileType   파일 타입 (IMAGE 또는 VIDEO)
   * @param sourcePath 원본 파일 경로
   * @param width      썸네일 너비 (픽셀)
   * @param height     썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws ThumbnailGenerationException 썸네일 생성 실패 또는 미지원 타입
   */
  public byte[] generate(FileType fileType, String sourcePath, int width, int height) {
    log.debug("Generating thumbnail: type={}, path={}, size={}x{}", fileType, sourcePath, width, height);

    ThumbnailGeneratorPort generator = getGenerator(fileType);
    if (generator == null) {
      throw new ThumbnailGenerationException(
          "No thumbnail generator available for type: " + fileType +
              ". Video thumbnails require FFmpeg (provided by streamix-spring-boot-starter)."
      );
    }

    return generator.generateFromPath(sourcePath, width, height);
  }

  /**
   * InputStream에서 썸네일을 생성합니다.
   *
   * <p><b>주의:</b> 비디오 파일은 InputStream에서 썸네일 생성을 지원하지 않습니다.
   * 비디오는 반드시 {@link #generate(FileType, String, int, int)}를 사용하세요.</p>
   *
   * @param fileType    파일 타입 (IMAGE만 지원)
   * @param inputStream 원본 파일 스트림
   * @param width       썸네일 너비 (픽셀)
   * @param height      썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws ThumbnailGenerationException 비디오 타입이거나 생성 실패 시
   */
  public byte[] generateFromStream(FileType fileType, InputStream inputStream, int width, int height) {
    if (fileType == FileType.VIDEO) {
      throw new ThumbnailGenerationException(
          "Video thumbnail generation from InputStream is not supported. " +
              "Use generate(FileType, String, int, int) with file path instead."
      );
    }

    log.debug("Generating image thumbnail from stream: size={}x{}", width, height);
    return imageGenerator.generate(inputStream, width, height);
  }

  /**
   * 주어진 파일 타입의 썸네일 생성을 지원하는지 확인합니다.
   *
   * @param fileType 파일 타입
   * @return 지원 여부
   */
  public boolean supports(FileType fileType) {
    return getGenerator(fileType) != null;
  }

  /**
   * 파일 타입에 맞는 생성기를 반환합니다.
   */
  private ThumbnailGeneratorPort getGenerator(FileType fileType) {
    return switch (fileType) {
      case IMAGE -> imageGenerator;
      case VIDEO -> videoGenerator;
    };
  }
}