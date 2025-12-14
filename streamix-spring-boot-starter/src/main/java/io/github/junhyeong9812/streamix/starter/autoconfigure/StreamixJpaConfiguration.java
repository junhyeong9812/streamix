package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.FileMetadataJpaRepository;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.JpaFileMetadataAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

/**
 * Streamix JPA 자동 설정 클래스입니다.
 *
 * <p>JPA/Hibernate가 클래스패스에 존재하고 DataSource가 설정되어 있을 때
 * JPA 기반 메타데이터 저장소를 자동으로 구성합니다.</p>
 *
 * <h2>활성화 조건</h2>
 * <ul>
 *   <li>{@code JpaRepository} 클래스가 클래스패스에 존재</li>
 *   <li>{@code DataSource} Bean이 존재</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <tr><th>Bean 타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>{@link FileMetadataPort}</td>
 *     <td>JPA 기반 메타데이터 저장소 어댑터</td>
 *   </tr>
 * </table>
 *
 * <h2>JPA 구성</h2>
 * <ul>
 *   <li><b>Entity Scan</b>: {@code io.github.junhyeong9812.streamix.starter.adapter.out.persistence}</li>
 *   <li><b>Repository Scan</b>: 동일 패키지</li>
 * </ul>
 *
 * <h2>테이블 자동 생성</h2>
 * <p>Hibernate의 DDL 자동 생성 기능을 사용하여 테이블을 생성할 수 있습니다:</p>
 * <pre>{@code
 * spring:
 *   jpa:
 *     hibernate:
 *       ddl-auto: create-drop  # 개발용
 *       # ddl-auto: validate  # 프로덕션용
 * }</pre>
 *
 * <h2>커스터마이징</h2>
 * <p>{@link FileMetadataPort} Bean을 직접 정의하면 JPA 구현 대신 사용됩니다:</p>
 * <pre>{@code
 * @Configuration
 * public class CustomMetadataConfig {
 *     @Bean
 *     public FileMetadataPort fileMetadataPort() {
 *         // MongoDB, Redis 등 다른 저장소 사용
 *         return new MongoFileMetadataAdapter(...);
 *     }
 * }
 * }</pre>
 *
 * <h2>필요한 의존성</h2>
 * <pre>{@code
 * dependencies {
 *     implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
 *     runtimeOnly 'com.h2database:h2'           // 개발용
 *     runtimeOnly 'org.postgresql:postgresql'   // 프로덕션용
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see JpaFileMetadataAdapter
 * @see FileMetadataJpaRepository
 */
@AutoConfiguration
@ConditionalOnClass({JpaRepository.class, DataSource.class})
@ConditionalOnBean(DataSource.class)
@EnableJpaRepositories(
    basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence"
)
@EntityScan(
    basePackages = "io.github.junhyeong9812.streamix.starter.adapter.out.persistence"
)
public class StreamixJpaConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixJpaConfiguration.class);

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