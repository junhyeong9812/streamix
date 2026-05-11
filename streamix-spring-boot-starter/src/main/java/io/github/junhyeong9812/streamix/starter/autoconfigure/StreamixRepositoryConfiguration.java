package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.FileMetadataJpaRepository;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.JpaFileMetadataAdapter;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
 *   <li>StreamingSessionRepository (StreamixMonitoringConfiguration이 사용)</li>
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
 * </table>
 *
 * <p>StreamingMonitoringService는 {@link StreamixMonitoringConfiguration}에서 별도 등록.</p>
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
 * @see StreamixMonitoringConfiguration
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
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
   * Thymeleaf 템플릿에서 {@code @streamixProperties}로 접근하기 위한
   * 명명된 Bean을 등록합니다.
   *
   * <p>{@code @ConfigurationProperties}로 등록된 기본 Bean의 이름은 Spring 내부 명명 규칙
   * ({@code streamix-io.github.junhyeong9812.streamix.starter.properties.StreamixProperties})에 따라
   * 길고 복잡합니다. Thymeleaf SpEL의 {@code @beanName} 접근 문법은 짧은 이름이 필요하므로
   * 이 메서드는 동일 인스턴스를 짧은 이름으로 다시 등록합니다.</p>
   *
   * <p>{@code @Primary}를 사용하여 다른 Bean이 {@code StreamixProperties}를
   * 주입받을 때 모호성 없이 이 Bean이 선택되도록 합니다.</p>
   *
   * <p><b>v3 마이그레이션 노트</b>: 향후 {@code @ControllerAdvice} +
   * {@code @ModelAttribute} 패턴으로 대체될 예정입니다.</p>
   *
   * @param properties {@code @EnableConfigurationProperties}로 등록된 원본 인스턴스
   * @return 동일 인스턴스 (Bean 이름 alias)
   */
  @Bean("streamixProperties")
  @Primary
  public StreamixProperties streamixPropertiesNamedBean(StreamixProperties properties) {
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
}