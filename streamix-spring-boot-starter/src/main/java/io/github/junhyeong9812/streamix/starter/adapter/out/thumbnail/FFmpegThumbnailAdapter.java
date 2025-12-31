package io.github.junhyeong9812.streamix.starter.adapter.out.thumbnail;

import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 기반 비디오 썸네일 생성 어댑터입니다.
 *
 * <p>Core 모듈의 {@link ThumbnailGeneratorPort}를 구현하여 비디오 파일에서
 * 썸네일을 추출합니다. FFmpeg를 외부 프로세스로 실행하여 특정 프레임을
 * JPEG 이미지로 추출합니다.</p>
 *
 * <h2>FFmpeg 요구 사항</h2>
 * <p>이 어댑터를 사용하려면 시스템에 FFmpeg가 설치되어 있어야 합니다:</p>
 * <ul>
 *   <li><b>Linux (Ubuntu/Debian)</b>: {@code sudo apt install ffmpeg}</li>
 *   <li><b>Linux (CentOS/RHEL)</b>: {@code sudo yum install ffmpeg}</li>
 *   <li><b>macOS</b>: {@code brew install ffmpeg}</li>
 *   <li><b>Windows</b>: <a href="https://ffmpeg.org/download.html">FFmpeg 다운로드</a> 후 PATH 설정</li>
 * </ul>
 *
 * <h2>썸네일 추출 설정</h2>
 * <ul>
 *   <li><b>추출 시점</b>: 비디오 시작 후 1초 (미리보기로 적합)</li>
 *   <li><b>출력 형식</b>: JPEG (품질 2, 고품질)</li>
 *   <li><b>크기 조정</b>: 지정된 너비/높이로 스케일링</li>
 *   <li><b>타임아웃</b>: 30초 (무한 대기 방지)</li>
 * </ul>
 *
 * <h2>FFmpeg 명령어</h2>
 * <pre>{@code
 * ffmpeg -i input.mp4 -ss 00:00:01 -vframes 1 \
 *        -vf scale=320:180:force_original_aspect_ratio=decrease \
 *        -q:v 2 -f image2pipe -vcodec mjpeg -
 * }</pre>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   thumbnail:
 *     enabled: true
 *     width: 320
 *     height: 180
 *     ffmpeg-path: /usr/bin/ffmpeg  # 커스텀 FFmpeg 경로
 * }</pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 자동 구성으로 Bean 등록됨
 * @Autowired
 * @Qualifier("ffmpegThumbnailAdapter")
 * private ThumbnailGeneratorPort videoThumbnailGenerator;
 *
 * // 비디오 썸네일 생성
 * byte[] thumbnail = videoThumbnailGenerator.generateFromPath(
 *     "/storage/video.mp4", 320, 180
 * );
 * }</pre>
 *
 * <h2>에러 처리</h2>
 * <ul>
 *   <li>FFmpeg 미설치: ThumbnailGenerationException + 로그 경고</li>
 *   <li>잘못된 비디오: ThumbnailGenerationException + FFmpeg stderr 로깅</li>
 *   <li>타임아웃: ThumbnailGenerationException (30초 초과)</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see ThumbnailGeneratorPort
 * @see io.github.junhyeong9812.streamix.core.adapter.out.thumbnail.ImageThumbnailAdapter
 */
public class FFmpegThumbnailAdapter implements ThumbnailGeneratorPort {

  private static final Logger log = LoggerFactory.getLogger(FFmpegThumbnailAdapter.class);

  /**
   * 썸네일 추출 시점 (비디오 시작 후 초).
   */
  private static final String SEEK_POSITION = "00:00:01";

  /**
   * FFmpeg 프로세스 타임아웃 (초).
   */
  private static final int PROCESS_TIMEOUT_SECONDS = 30;

  /**
   * 생성기 우선순위 (VIDEO 전용, ImageThumbnailAdapter와 동일).
   */
  private static final int DEFAULT_ORDER = 500;

  /**
   * FFmpeg 실행 파일 경로.
   */
  private final String ffmpegPath;

  /**
   * 기본 FFmpeg 경로로 어댑터를 생성합니다.
   *
   * <p>시스템 PATH에서 ffmpeg를 찾습니다.</p>
   */
  public FFmpegThumbnailAdapter() {
    this("ffmpeg");
  }

  /**
   * 지정된 FFmpeg 경로로 어댑터를 생성합니다.
   *
   * @param ffmpegPath FFmpeg 실행 파일 경로 (예: "/usr/bin/ffmpeg")
   */
  public FFmpegThumbnailAdapter(String ffmpegPath) {
    this.ffmpegPath = ffmpegPath;
    log.info("FFmpegThumbnailAdapter initialized with path: {}", ffmpegPath);
  }

  /**
   * {@inheritDoc}
   *
   * <p>VIDEO 타입만 지원합니다.</p>
   *
   * @param fileType 파일 타입
   * @return VIDEO이면 {@code true}
   */
  @Override
  public boolean supports(FileType fileType) {
    return fileType == FileType.VIDEO;
  }

  /**
   * {@inheritDoc}
   *
   * @return 500 (ImageThumbnailAdapter와 동일한 우선순위)
   * @since 1.0.7
   */
  @Override
  public int getOrder() {
    return DEFAULT_ORDER;
  }

  /**
   * {@inheritDoc}
   *
   * @return "FFmpegThumbnailAdapter"
   * @since 1.0.7
   */
  @Override
  public String getName() {
    return "FFmpegThumbnailAdapter";
  }

  /**
   * {@inheritDoc}
   *
   * <p>비디오는 InputStream으로 썸네일을 생성할 수 없습니다.
   * 임시 파일로 저장 후 {@link #generateFromPath(String, int, int)}를 호출합니다.</p>
   *
   * @param sourceStream 원본 비디오 스트림
   * @param width        썸네일 너비
   * @param height       썸네일 높이
   * @return 썸네일 이미지 데이터 (JPEG)
   * @throws ThumbnailGenerationException 썸네일 생성 실패 시
   */
  @Override
  public byte[] generate(InputStream sourceStream, int width, int height) {
    Path tempFile = null;
    try {
      // 임시 파일로 저장
      tempFile = Files.createTempFile("streamix-video-", ".tmp");
      Files.copy(sourceStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      log.debug("Video saved to temp file for thumbnail generation: {}", tempFile);

      // 경로 기반 메서드 호출
      return generateFromPath(tempFile.toString(), width, height);

    } catch (IOException e) {
      throw new ThumbnailGenerationException("Failed to create temp file for video thumbnail", e);
    } finally {
      // 임시 파일 정리
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
        } catch (IOException e) {
          log.warn("Failed to delete temp file: {}", tempFile, e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>FFmpeg를 사용하여 비디오의 특정 프레임을 추출합니다.
   * 비디오 시작 1초 후의 프레임을 JPEG로 변환하여 반환합니다.</p>
   *
   * @param sourcePath 원본 비디오 파일 경로
   * @param width      썸네일 너비
   * @param height     썸네일 높이
   * @return 썸네일 이미지 데이터 (JPEG)
   * @throws ThumbnailGenerationException FFmpeg 실행 실패 또는 타임아웃 시
   */
  @Override
  public byte[] generateFromPath(String sourcePath, int width, int height) {
    log.debug("Generating video thumbnail: path={}, size={}x{}", sourcePath, width, height);

    // FFmpeg 명령어 구성
    ProcessBuilder processBuilder = new ProcessBuilder(
        ffmpegPath,
        "-i", sourcePath,                    // 입력 파일
        "-ss", SEEK_POSITION,                // 탐색 위치 (1초)
        "-vframes", "1",                     // 1프레임만 추출
        "-vf", buildScaleFilter(width, height), // 크기 조정
        "-q:v", "2",                         // JPEG 품질 (1-31, 낮을수록 고품질)
        "-f", "image2pipe",                  // 파이프로 출력
        "-vcodec", "mjpeg",                  // MJPEG 코덱
        "-"                                  // stdout으로 출력
    );

    processBuilder.redirectErrorStream(false);

    try {
      Process process = processBuilder.start();

      // stdout에서 이미지 데이터 읽기
      byte[] thumbnailData;
      try (InputStream stdout = process.getInputStream();
          ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = stdout.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        thumbnailData = baos.toByteArray();
      }

      // 프로세스 완료 대기
      boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (!finished) {
        process.destroyForcibly();
        throw new ThumbnailGenerationException(
            "FFmpeg process timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds"
        );
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        // stderr 읽기 (디버깅용)
        String errorMessage = readErrorStream(process);
        log.error("FFmpeg failed with exit code {}: {}", exitCode, errorMessage);
        throw new ThumbnailGenerationException(
            "FFmpeg failed with exit code " + exitCode + ": " + errorMessage
        );
      }

      if (thumbnailData.length == 0) {
        throw new ThumbnailGenerationException("FFmpeg produced empty output");
      }

      log.debug("Video thumbnail generated: {} bytes", thumbnailData.length);
      return thumbnailData;

    } catch (IOException e) {
      throw new ThumbnailGenerationException("Failed to execute FFmpeg: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ThumbnailGenerationException("FFmpeg process interrupted", e);
    }
  }

  /**
   * FFmpeg scale 필터 문자열을 생성합니다.
   *
   * <p>비율을 유지하면서 지정된 크기 내에 맞추는 스케일 필터입니다.</p>
   *
   * @param width  목표 너비
   * @param height 목표 높이
   * @return FFmpeg scale 필터 문자열
   */
  private String buildScaleFilter(int width, int height) {
    // force_original_aspect_ratio=decrease: 비율 유지하면서 지정 크기 내에 맞춤
    // pad: 남는 공간은 검정색으로 채움 (선택적)
    return String.format("scale=%d:%d:force_original_aspect_ratio=decrease", width, height);
  }

  /**
   * 프로세스의 stderr를 읽어 반환합니다.
   *
   * @param process FFmpeg 프로세스
   * @return stderr 내용 (최대 500자)
   */
  private String readErrorStream(Process process) {
    try (InputStream stderr = process.getErrorStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = stderr.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }

      String error = baos.toString();
      // 너무 길면 잘라냄
      if (error.length() > 500) {
        error = error.substring(0, 500) + "...";
      }
      return error;

    } catch (IOException e) {
      return "Failed to read error stream: " + e.getMessage();
    }
  }

  /**
   * FFmpeg가 시스템에 설치되어 있는지 확인합니다.
   *
   * <p>Bean 초기화 시 호출하여 사전 검증할 수 있습니다.</p>
   *
   * @return FFmpeg 사용 가능 여부
   */
  public boolean isFFmpegAvailable() {
    try {
      ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
      Process process = pb.start();
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);

      if (!finished) {
        process.destroyForcibly();
        return false;
      }

      return process.exitValue() == 0;

    } catch (IOException | InterruptedException e) {
      log.warn("FFmpeg not available at path '{}': {}", ffmpegPath, e.getMessage());
      return false;
    }
  }
}