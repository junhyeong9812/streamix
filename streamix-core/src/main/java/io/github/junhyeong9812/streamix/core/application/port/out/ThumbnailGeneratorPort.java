package io.github.junhyeong9812.streamix.core.application.port.out;

import io.github.junhyeong9812.streamix.core.domain.model.FileType;

import java.io.InputStream;

/**
 * 썸네일 생성기 포트 인터페이스입니다 (Output Port / SPI).
 *
 * <p>헥사고날 아키텍처의 Output Port로, 이미지/비디오 썸네일 생성을 담당합니다.
 * 파일 타입별로 다른 구현체를 사용할 수 있습니다.</p>
 *
 * <h2>구현체 예시</h2>
 * <ul>
 *   <li>{@link io.github.junhyeong9812.streamix.core.adapter.out.thumbnail.ImageThumbnailAdapter}
 *       - Thumbnailator를 이용한 이미지 썸네일</li>
 *   <li>VideoThumbnailAdapter - FFmpeg를 이용한 비디오 썸네일 (별도 구현 필요)</li>
 * </ul>
 *
 * <h2>커스텀 구현 예시</h2>
 * <pre>{@code
 * @Component
 * public class VideoThumbnailAdapter implements ThumbnailGeneratorPort {
 *
 *     @Override
 *     public boolean supports(FileType fileType) {
 *         return fileType == FileType.VIDEO;
 *     }
 *
 *     @Override
 *     public byte[] generateFromPath(String sourcePath, int width, int height) {
 *         // FFmpeg를 이용한 프레임 추출
 *         ProcessBuilder pb = new ProcessBuilder(
 *             "ffmpeg", "-i", sourcePath,
 *             "-ss", "00:00:01", "-vframes", "1",
 *             "-vf", "scale=" + width + ":" + height,
 *             "-f", "image2pipe", "-"
 *         );
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * <h2>다중 구현체 등록</h2>
 * <p>여러 ThumbnailGeneratorPort 구현체를 Bean으로 등록하면,
 * FileUploadService가 {@link #supports(FileType)}를 호출하여 적절한 구현체를 선택합니다.</p>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileType
 * @see io.github.junhyeong9812.streamix.core.adapter.out.thumbnail.ImageThumbnailAdapter
 */
public interface ThumbnailGeneratorPort {

  /**
   * 이 생성기가 지정된 파일 타입을 지원하는지 확인합니다.
   *
   * <p>FileUploadService는 이 메서드를 호출하여 적절한 생성기를 선택합니다.</p>
   *
   * @param fileType 파일 타입
   * @return 지원 여부
   */
  boolean supports(FileType fileType);

  /**
   * InputStream으로부터 썸네일을 생성합니다.
   *
   * <p>주로 이미지 파일에 사용됩니다.</p>
   *
   * @param sourceStream 원본 파일 스트림
   * @param width        썸네일 너비 (픽셀)
   * @param height       썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException
   *         썸네일 생성 실패 시
   */
  byte[] generate(InputStream sourceStream, int width, int height);

  /**
   * 파일 경로로부터 썸네일을 생성합니다.
   *
   * <p>주로 비디오 파일에 사용됩니다. 비디오의 경우 InputStream으로는
   * 특정 프레임에 접근하기 어려우므로 파일 경로가 필요합니다.</p>
   *
   * @param sourcePath 원본 파일 경로
   * @param width      썸네일 너비 (픽셀)
   * @param height     썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException
   *         썸네일 생성 실패 시
   */
  byte[] generateFromPath(String sourcePath, int width, int height);
}