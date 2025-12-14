package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.FileMetadataJpaRepository;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.JpaFileMetadataAdapter;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionRepository;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Streamix JPA Repository 및 Entity 스캔 설정 클래스입니다.
 *
 * <p>{@link io.github.junhyeong9812.streamix.starter.annotation.EnableStreamix} 어노테이션을 통해
 * Import되어, Streamix의 JPA Entity와 Repository를 자동으로 스캔합니다.</p>
 *
 * <h2>스캔 대상 패키지</h2>
 * <ul>
 *   <li><b>Entity</b>: {@code io.github.junhyeong9812.streamix.starter.adapter.out.persistence}</li>
 *   <li><b>Repository</b>: 동일 패키지</li>
 * </ul>
 *
 * <h2>등록되는 Entity</h2>
 * <ul>
 *   <li>{@link io.github.junhyeong9812.streamix.starter.adapter.out.persistence.FileMetadataEntity}</li>
 *   <li>{@link io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionEntity}</li>
 * </ul>
 *
 * <h2>등록되는 Repository</h2>
 * <ul>
 *   <li>{@link FileMetadataJpaRepository}</li>
 *   <li>{@link StreamingSessionRepository}</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <caption>자동 등록 Bean 목록</caption>
 *   <tr><th>Bean 타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>{@link FileMetadataPort}</td>
 *     <td>JPA 기반 메타데이터 저장소 어댑터</td>
 *   </tr>
 *   <tr>
 *     <td>{@link StreamingMonitoringService}</td>
 *     <td>스트리밍 세션 모니터링 서비스</td>
 *   </tr>
 * </table>
 *
 * <h2>사용 방법</h2>
 * <p>이 Configuration은 {@code @EnableStreamix} 어노테이션을 통해 자동으로 Import됩니다.
 * 직접 Import할 필요가 없습니다.</p>
 * <pre>{@code
 * @SpringBootApplication
 * @EnableStreamix  // StreamixRepositoryConfiguration 자동 Import
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see io.github.junhyeong9812.streamix.starter.annotation.EnableStreamix
 * @see FileMetadataJpaRepository
 * @see StreamingSessionRepository
 */
@Configuration
@EnableConfigurationProperties(StreamixProperties.class)
@EntityScan(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence")
public class StreamixRepositoryConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixRepositoryConfiguration.class);

  /**
   * StreamixRepositoryConfiguration의 기본 생성자입니다.
   */
  public StreamixRepositoryConfiguration() {
    log.info("Streamix Repository Configuration initialized");
    log.info("  Entity scan: io.github.junhyeong9812.streamix.starter.adapter.out.persistence");
    log.info("  Repository scan: io.github.junhyeong9812.streamix.starter.adapter.out.persistence");
  }

  /**
   * Streamix 설정 Properties Bean을 명시적 이름으로 등록합니다.
   *
   * <p>Thymeleaf 템플릿에서 {@code @streamixProperties}로 접근할 수 있도록
   * 'streamixProperties'라는 명시적인 Bean 이름으로 alias를 등록합니다.</p>
   *
   * <p>{@code @EnableConfigurationProperties}로 등록된 Bean은 내부 명명 규칙을 따르므로,
   * Thymeleaf에서 접근 가능하도록 동일한 Bean을 'streamixProperties' 이름으로 다시 등록합니다.</p>
   *
   * <p>{@code @Primary}를 사용하여 여러 Bean이 있을 때 이 Bean이 우선 주입되도록 합니다.</p>
   *
   * @param properties @EnableConfigurationProperties로 등록된 Properties
   * @return 동일한 Properties 인스턴스
   */
  @Bean("streamixProperties")
  @Primary
  public StreamixProperties streamixPropertiesAlias(StreamixProperties properties) {
    log.info("Registering StreamixProperties bean alias for Thymeleaf access");
    return properties;
  }

  /**
   * JPA 기반 메타데이터 저장소 어댑터를 생성합니다.
   *
   * <p>Core 모듈의 {@link FileMetadataPort} 인터페이스를 구현하며,
   * Spring Data JPA를 통해 데이터베이스에 메타데이터를 저장합니다.</p>
   *
   * <p>다른 {@link FileMetadataPort} Bean이 이미 등록되어 있으면
   * 이 Bean은 생성되지 않습니다 (커스터마이징 지원).</p>
   *
   * @param repository JPA 리포지토리 (Spring Data가 자동 생성)
   * @return JPA 기반 메타데이터 어댑터
   */
  @Bean
  @ConditionalOnMissingBean(FileMetadataPort.class)
  public FileMetadataPort fileMetadataPort(FileMetadataJpaRepository repository) {
    log.info("Creating JpaFileMetadataAdapter for metadata persistence");
    return new JpaFileMetadataAdapter(repository);
  }

  /**
   * 스트리밍 모니터링 서비스를 생성합니다.
   *
   * <p>스트리밍 세션의 기록, 통계, 분석 기능을 제공합니다.
   * 대시보드에서 실시간 현황과 통계 데이터를 표시하는 데 사용됩니다.</p>
   *
   * @param sessionRepository 세션 리포지토리
   * @return 모니터링 서비스
   */
  @Bean
  @ConditionalOnMissingBean(StreamingMonitoringService.class)
  public StreamingMonitoringService streamingMonitoringService(StreamingSessionRepository sessionRepository) {
    log.info("Creating StreamingMonitoringService for dashboard statistics");
    return new StreamingMonitoringService(sessionRepository);
  }
}