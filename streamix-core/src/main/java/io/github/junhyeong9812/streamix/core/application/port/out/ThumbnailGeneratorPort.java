package io.github.junhyeong9812.streamix.core.application.port.out;

import io.github.junhyeong9812.streamix.core.domain.model.FileType;

import java.io.InputStream;

/**
 * 썸네일 생성기 포트 인터페이스입니다 (Output Port / SPI).
 *
 * <p>헥사고날 아키텍처의 Output Port로, 파일 타입별 썸네일 생성을 담당합니다.
 * 여러 구현체를 등록하면 {@link #supports(FileType)}와 {@link #getOrder()}를 기준으로
 * 적절한 생성기가 선택됩니다.</p>
 *
 * <h2>구현체 예시</h2>
 * <ul>
 *   <li>{@code ImageThumbnailAdapter} - Thumbnailator를 이용한 이미지 썸네일</li>
 *   <li>{@code VideoThumbnailAdapter} - FFmpeg를 이용한 비디오 썸네일</li>
 *   <li>{@code PdfThumbnailAdapter} - PDF 첫 페이지 썸네일</li>
 *   <li>{@code DefaultThumbnailGenerator} - 기본 아이콘 (폴백)</li>
 * </ul>
 *
 * <h2>생성기 선택 로직</h2>
 * <ol>
 *   <li>{@link #supports(FileType)}가 true인 생성기 필터링</li>
 *   <li>{@link #getOrder()}가 가장 낮은 생성기 선택</li>
 *   <li>동일 우선순위면 먼저 등록된 생성기 선택</li>
 * </ol>
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
 *     public int getOrder() {
 *         return 10;  // 기본값보다 높은 우선순위
 *     }
 *
 *     @Override
 *     public byte[] generateFromPath(String sourcePath, int width, int height) {
 *         // FFmpeg를 이용한 프레임 추출
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileType
 */
public interface ThumbnailGeneratorPort {

  /**
   * 이 생성기가 지정된 파일 타입을 지원하는지 확인합니다.
   *
   * <p>ThumbnailService는 이 메서드를 호출하여 적절한 생성기를 선택합니다.</p>
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

  /**
   * 생성기의 우선순위를 반환합니다.
   *
   * <p>여러 생성기가 동일한 파일 타입을 지원할 때, 낮은 값이 높은 우선순위를 가집니다.
   * 예를 들어, 특정 타입에 대한 커스텀 생성기(order=10)가 기본 생성기(order=1000)보다
   * 먼저 선택됩니다.</p>
   *
   * <h3>권장 우선순위</h3>
   * <ul>
   *   <li>0-99: 커스텀 고우선순위 생성기</li>
   *   <li>100-499: 일반 커스텀 생성기</li>
   *   <li>500-999: 라이브러리 기본 생성기 (IMAGE, VIDEO 등)</li>
   *   <li>1000+: 폴백 생성기 (DefaultThumbnailGenerator)</li>
   * </ul>
   *
   * @return 우선순위 (낮을수록 높은 우선순위)
   * @since 1.0.7
   */
  default int getOrder() {
    return 500;
  }

  /**
   * 생성기의 이름을 반환합니다.
   *
   * <p>로깅과 디버깅 목적으로 사용됩니다.</p>
   *
   * @return 생성기 이름 (기본값: 클래스 단순 이름)
   * @since 1.0.7
   */
  default String getName() {
    return getClass().getSimpleName();
  }
}