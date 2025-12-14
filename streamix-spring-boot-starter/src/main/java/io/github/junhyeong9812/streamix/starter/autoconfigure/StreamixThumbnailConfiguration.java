package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.starter.adapter.out.thumbnail.FFmpegThumbnailAdapter;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Streamix 썸네일 자동 설정 클래스입니다.
 *
 * <p>FFmpeg 기반 비디오 썸네일 생성 어댑터를 자동으로 구성합니다.
 * {@code streamix.thumbnail.enabled=true} (기본값)일 때 활성화됩니다.</p>
 *
 * <h2>활성화 조건</h2>
 * <ul>
 *   <li>{@code streamix.thumbnail.enabled=true} (기본값)</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <tr><th>Bean 이름</th><th>타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>ffmpegThumbnailAdapter</td>
 *     <td>{@link ThumbnailGeneratorPort}</td>
 *     <td>FFmpeg 기반 비디오 썸네일 생성기</td>
 *   </tr>
 * </table>
 *
 * <h2>FFmpeg 요구 사항</h2>
 * <p>비디오 썸네일을 생성하려면 시스템에 FFmpeg가 설치되어 있어야 합니다:</p>
 * <ul>
 *   <li><b>Ubuntu/Debian</b>: {@code sudo apt install ffmpeg}</li>
 *   <li><b>macOS</b>: {@code brew install ffmpeg}</li>
 *   <li><b>Windows</b>: FFmpeg 다운로드 후 PATH 설정</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   thumbnail:
 *     enabled: true           # 썸네일 생성 활성화 (기본: true)
 *     width: 320              # 썸네일 너비 (기본: 320)
 *     height: 180             # 썸네일 높이 (기본: 180)
 *     ffmpeg-path: ffmpeg     # FFmpeg 실행 파일 경로 (기본: ffmpeg)
 * }</pre>
 *
 * <h2>FFmpeg가 없는 경우</h2>
 * <p>FFmpeg가 설치되어 있지 않으면:</p>
 * <ul>
 *   <li>Bean은 생성되지만, 비디오 썸네일 생성 시 실패</li>
 *   <li>이미지 썸네일은 Core의 ImageThumbnailAdapter로 정상 생성</li>
 *   <li>업로드 자체는 성공 (썸네일 실패는 무시됨)</li>
 * </ul>
 *
 * <h2>커스터마이징</h2>
 * <p>직접 Bean을 정의하면 자동 구성이 비활성화됩니다:</p>
 * <pre>{@code
 * @Configuration
 * public class CustomThumbnailConfig {
 *     @Bean
 *     public ThumbnailGeneratorPort ffmpegThumbnailAdapter() {
 *         return new FFmpegThumbnailAdapter("/opt/ffmpeg/bin/ffmpeg");
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FFmpegThumbnailAdapter
 * @see StreamixProperties.Thumbnail
 */
@AutoConfiguration
@ConditionalOnProperty(name = "streamix.thumbnail.enabled", havingValue = "true", matchIfMissing = true)
public class StreamixThumbnailConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixThumbnailConfiguration.class);

  /**
   * FFmpeg 기반 비디오 썸네일 생성 어댑터를 생성합니다.
   *
   * <p>Core 모듈의 {@link ThumbnailGeneratorPort}를 구현하며,
   * FFmpeg를 사용하여 비디오에서 썸네일을 추출합니다.</p>
   *
   * <p>이미지 썸네일은 Core의 ImageThumbnailAdapter가 담당하고,
   * 이 어댑터는 비디오만 처리합니다 (FileType.VIDEO).</p>
   *
   * <p>같은 이름의 Bean이 이미 등록되어 있으면 이 Bean은 생성되지 않습니다.</p>
   *
   * @param properties Streamix 설정
   * @return FFmpeg 썸네일 어댑터
   */
  @Bean(name = "ffmpegThumbnailAdapter")
  @ConditionalOnMissingBean(name = "ffmpegThumbnailAdapter")
  public ThumbnailGeneratorPort ffmpegThumbnailAdapter(StreamixProperties properties) {
    String ffmpegPath = properties.thumbnail().ffmpegPath();
    log.info("Creating FFmpegThumbnailAdapter: ffmpegPath={}", ffmpegPath);

    FFmpegThumbnailAdapter adapter = new FFmpegThumbnailAdapter(ffmpegPath);

    // FFmpeg 설치 여부 확인 및 경고 로깅
    if (adapter.isFFmpegAvailable()) {
      log.info("FFmpeg is available - video thumbnail generation enabled");
    } else {
      log.warn("FFmpeg not found at '{}' - video thumbnails will not be generated. " +
              "Install FFmpeg or set streamix.thumbnail.ffmpeg-path to the correct path.",
          ffmpegPath);
    }

    return adapter;
  }
}