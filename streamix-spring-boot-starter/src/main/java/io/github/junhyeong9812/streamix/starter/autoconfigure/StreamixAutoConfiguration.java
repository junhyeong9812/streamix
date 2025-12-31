package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.adapter.out.storage.LocalFileStorageAdapter;
import io.github.junhyeong9812.streamix.core.adapter.out.thumbnail.ImageThumbnailAdapter;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.application.service.FileDeleteService;
import io.github.junhyeong9812.streamix.core.application.service.FileMetadataService;
import io.github.junhyeong9812.streamix.core.application.service.FileStreamService;
import io.github.junhyeong9812.streamix.core.application.service.FileUploadService;
import io.github.junhyeong9812.streamix.core.application.service.ThumbnailService;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Streamix 핵심 기능 자동 설정 클래스입니다.
 *
 * <p>Spring Boot의 자동 설정 메커니즘을 통해 Streamix Core 모듈의 주요 서비스들을
 * 자동으로 Bean으로 등록합니다. {@link EnableConfigurationProperties}를 통해
 * {@link StreamixProperties} 설정이 바인딩됩니다.</p>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <caption>자동 등록 Bean 목록</caption>
 *   <tr><th>Bean 타입</th><th>설명</th><th>조건</th></tr>
 *   <tr>
 *     <td>{@link FileStoragePort}</td>
 *     <td>로컬 파일 시스템 저장소</td>
 *     <td>FileStoragePort Bean이 없을 때</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ThumbnailGeneratorPort} (imageThumbnailAdapter)</td>
 *     <td>이미지 썸네일 생성기</td>
 *     <td>항상 (기본 제공)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ThumbnailService}</td>
 *     <td>썸네일 생성 통합 서비스</td>
 *     <td>ThumbnailService Bean이 없을 때</td>
 *   </tr>
 *   <tr>
 *     <td>{@link FileUploadService}</td>
 *     <td>파일 업로드 서비스</td>
 *     <td>FileUploadService Bean이 없을 때</td>
 *   </tr>
 *   <tr>
 *     <td>{@link FileStreamService}</td>
 *     <td>파일 스트리밍 서비스</td>
 *     <td>FileStreamService Bean이 없을 때</td>
 *   </tr>
 *   <tr>
 *     <td>{@link FileMetadataService}</td>
 *     <td>메타데이터 조회 서비스</td>
 *     <td>FileMetadataService Bean이 없을 때</td>
 *   </tr>
 *   <tr>
 *     <td>{@link FileDeleteService}</td>
 *     <td>파일 삭제 서비스</td>
 *     <td>FileDeleteService Bean이 없을 때</td>
 *   </tr>
 * </table>
 *
 * <h2>비디오 썸네일 지원</h2>
 * <p>{@code ffmpegThumbnailAdapter}라는 이름의 {@link ThumbnailGeneratorPort} Bean이
 * 존재하면 자동으로 비디오 썸네일 생성이 활성화됩니다. 이 Bean은
 * {@code StreamixThumbnailConfiguration}에서 조건부로 등록됩니다.</p>
 *
 * <h2>커스터마이징</h2>
 * <p>모든 Bean은 {@link ConditionalOnMissingBean}으로 보호되어 있어,
 * 사용자가 직접 Bean을 정의하면 기본 구현을 대체할 수 있습니다.</p>
 *
 * <pre>{@code
 * @Configuration
 * public class CustomStorageConfig {
 *     // S3 저장소로 대체
 *     @Bean
 *     public FileStoragePort fileStoragePort() {
 *         return new S3FileStorageAdapter(s3Client, bucketName);
 *     }
 * }
 * }</pre>
 *
 * <h2>설정 파일 위치</h2>
 * <p>이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 파일에 등록되어 Spring Boot가 자동으로 로드합니다.</p>
 *
 * <h2>로깅</h2>
 * <p>자동 설정 시 주요 설정값과 Bean 생성 정보가 INFO 레벨로 로깅됩니다:</p>
 * <pre>
 * Streamix Auto-Configuration initialized
 *   Storage path: /var/lib/streamix/uploads
 *   Max file size: 100.0 MB
 *   Allowed types: all
 *   Thumbnail enabled: true
 * Creating LocalFileStorageAdapter: basePath=/var/lib/streamix/uploads
 * Creating ThumbnailService with 2 generators
 * Creating FileUploadService: thumbnailEnabled=true, size=320x180, maxFileSize=100MB
 * Creating FileStreamService
 * Creating FileMetadataService
 * Creating FileDeleteService
 * </pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixProperties
 * @see io.github.junhyeong9812.streamix.starter.annotation.EnableStreamix
 */
@AutoConfiguration
@EnableConfigurationProperties(StreamixProperties.class)
public class StreamixAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixAutoConfiguration.class);

  private final StreamixProperties properties;

  /**
   * StreamixAutoConfiguration을 생성합니다.
   *
   * <p>생성 시 설정값을 로깅합니다.</p>
   *
   * @param properties Streamix 설정 프로퍼티
   */
  public StreamixAutoConfiguration(StreamixProperties properties) {
    this.properties = properties;
    log.info("Streamix Auto-Configuration initialized");
    log.info("  Storage path: {}", properties.storage().getResolvedBasePath());
    log.info("  Max file size: {}", formatSize(properties.storage().maxFileSize()));
    log.info("  Allowed types: {}",
        properties.storage().isAllTypesAllowed() ? "all" : properties.storage().allowedTypes());
    log.info("  Thumbnail enabled: {}", properties.thumbnail().enabled());
  }

  /**
   * 로컬 파일 시스템 기반 저장소를 생성합니다.
   *
   * <p>파일은 {@code streamix.storage.base-path}에 지정된 경로에 저장됩니다.
   * 경로가 존재하지 않으면 자동으로 생성됩니다.</p>
   *
   * @return 로컬 파일 저장소 어댑터
   * @see LocalFileStorageAdapter
   */
  @Bean
  @ConditionalOnMissingBean(FileStoragePort.class)
  public FileStoragePort fileStoragePort() {
    String basePath = properties.storage().getResolvedBasePath();
    log.info("Creating LocalFileStorageAdapter: basePath={}", basePath);
    return new LocalFileStorageAdapter(basePath);
  }

  /**
   * 이미지 썸네일 생성기를 생성합니다.
   *
   * <p>Thumbnailator 라이브러리를 사용하여 JPEG, PNG, GIF 등의
   * 이미지 파일에서 썸네일을 생성합니다.</p>
   *
   * @return 이미지 썸네일 생성 어댑터
   * @see ImageThumbnailAdapter
   */
  @Bean
  @ConditionalOnMissingBean(name = "imageThumbnailAdapter")
  public ThumbnailGeneratorPort imageThumbnailAdapter() {
    return new ImageThumbnailAdapter();
  }

  /**
   * 썸네일 서비스를 생성합니다.
   *
   * <p>등록된 모든 {@link ThumbnailGeneratorPort} Bean을 List로 주입받아
   * 우선순위(order)에 따라 정렬하여 관리합니다.</p>
   *
   * <p>Composite 패턴을 적용하여 파일 타입에 따라 적절한 생성기를 선택합니다.</p>
   *
   * @param generators       등록된 모든 썸네일 생성기
   * @param fileStoragePort  파일 저장소
   * @param fileMetadataPort 메타데이터 저장소
   * @return 통합 썸네일 서비스
   * @see ThumbnailService
   * @since 1.0.7 List 주입 방식으로 변경
   */
  @Bean
  @ConditionalOnMissingBean(ThumbnailService.class)
  public ThumbnailService thumbnailService(
      List<ThumbnailGeneratorPort> generators,
      FileStoragePort fileStoragePort,
      FileMetadataPort fileMetadataPort
  ) {
    log.info("Creating ThumbnailService with {} generators", generators.size());
    generators.forEach(g ->
        log.debug("  - {} (order={})", g.getName(), g.getOrder()));
    return new ThumbnailService(generators, fileStoragePort, fileMetadataPort);
  }

  /**
   * 파일 업로드 서비스를 생성합니다.
   *
   * <p>파일 업로드 시 다음 작업이 수행됩니다:</p>
   * <ol>
   *   <li>파일 크기 검증 (maxFileSize)</li>
   *   <li>파일 타입 검증 (allowedTypes)</li>
   *   <li>UUID 기반 고유 파일명 생성</li>
   *   <li>저장소에 파일 저장</li>
   *   <li>썸네일 생성 (활성화된 경우)</li>
   *   <li>메타데이터 저장</li>
   * </ol>
   *
   * @param fileStoragePort  파일 저장소
   * @param fileMetadataPort 메타데이터 저장소
   * @param thumbnailService 썸네일 서비스
   * @return 파일 업로드 서비스
   * @see FileUploadService
   * @since 1.0.7 maxFileSize, allowedTypes 파라미터 추가
   */
  @Bean
  @ConditionalOnMissingBean(FileUploadService.class)
  public FileUploadService fileUploadService(
      FileStoragePort fileStoragePort,
      FileMetadataPort fileMetadataPort,
      ThumbnailService thumbnailService
  ) {
    boolean enabled = properties.thumbnail().enabled();
    int width = properties.thumbnail().width();
    int height = properties.thumbnail().height();
    long maxFileSize = properties.storage().maxFileSize();
    Set<FileType> allowedTypes = parseAllowedTypes(properties.storage().allowedTypes());

    log.info("Creating FileUploadService: thumbnailEnabled={}, size={}x{}, maxFileSize={}, allowedTypes={}",
        enabled, width, height, formatSize(maxFileSize),
        allowedTypes.isEmpty() ? "all" : allowedTypes);

    return new FileUploadService(
        fileStoragePort,
        fileMetadataPort,
        thumbnailService,
        enabled,
        width,
        height,
        maxFileSize,
        allowedTypes
    );
  }

  /**
   * 파일 스트리밍 서비스를 생성합니다.
   *
   * <p>HTTP Range 요청을 지원하여 비디오 탐색(seek)이 가능합니다.</p>
   *
   * @param fileStoragePort  파일 저장소
   * @param fileMetadataPort 메타데이터 저장소
   * @return 파일 스트리밍 서비스
   * @see FileStreamService
   */
  @Bean
  @ConditionalOnMissingBean(FileStreamService.class)
  public FileStreamService fileStreamService(
      FileStoragePort fileStoragePort,
      FileMetadataPort fileMetadataPort
  ) {
    log.info("Creating FileStreamService");
    return new FileStreamService(fileStoragePort, fileMetadataPort);
  }

  /**
   * 파일 메타데이터 서비스를 생성합니다.
   *
   * <p>파일 정보 조회, 목록 조회 기능을 제공합니다.</p>
   *
   * @param fileMetadataPort 메타데이터 저장소
   * @return 파일 메타데이터 서비스
   * @see FileMetadataService
   * @since 1.0.7 FileStoragePort 의존성 제거 (조회 전용)
   */
  @Bean
  @ConditionalOnMissingBean(FileMetadataService.class)
  public FileMetadataService fileMetadataService(
      FileMetadataPort fileMetadataPort
  ) {
    log.info("Creating FileMetadataService");
    return new FileMetadataService(fileMetadataPort);
  }

  /**
   * 파일 삭제 서비스를 생성합니다.
   *
   * <p>파일 삭제 시 실제 파일, 썸네일, 메타데이터가 모두 삭제됩니다.</p>
   *
   * @param fileMetadataPort 메타데이터 저장소
   * @param fileStoragePort  파일 저장소
   * @return 파일 삭제 서비스
   * @see FileDeleteService
   * @since 1.0.7 FileMetadataService에서 분리
   */
  @Bean
  @ConditionalOnMissingBean(FileDeleteService.class)
  public FileDeleteService fileDeleteService(
      FileMetadataPort fileMetadataPort,
      FileStoragePort fileStoragePort
  ) {
    log.info("Creating FileDeleteService");
    return new FileDeleteService(fileMetadataPort, fileStoragePort);
  }

  // ==================== 유틸리티 ====================

  /**
   * 문자열 Set을 FileType Set으로 변환합니다.
   *
   * @param typeStrings 문자열 Set (예: ["IMAGE", "VIDEO"])
   * @return FileType Set (잘못된 값은 무시)
   */
  private Set<FileType> parseAllowedTypes(Set<String> typeStrings) {
    if (typeStrings == null || typeStrings.isEmpty()) {
      return Set.of();
    }

    return typeStrings.stream()
        .map(String::toUpperCase)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> {
          try {
            return FileType.valueOf(s);
          } catch (IllegalArgumentException e) {
            log.warn("Invalid FileType in allowed-types: '{}' (ignoring)", s);
            return null;
          }
        })
        .filter(t -> t != null)
        .collect(Collectors.toSet());
  }

  /**
   * 바이트 크기를 읽기 좋은 형식으로 포맷합니다.
   */
  private static String formatSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024L * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    } else {
      return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
  }
}