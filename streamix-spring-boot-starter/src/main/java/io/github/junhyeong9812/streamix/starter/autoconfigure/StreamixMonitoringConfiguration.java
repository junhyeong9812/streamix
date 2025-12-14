package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionRepository;
import io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.sql.DataSource;

/**
 * Streamix 모니터링 자동 설정 클래스입니다.
 *
 * <p>스트리밍 세션 모니터링 서비스를 자동으로 구성합니다.
 * JPA가 활성화되어 있고, 대시보드가 활성화된 경우에만 등록됩니다.</p>
 *
 * <h2>활성화 조건</h2>
 * <ul>
 *   <li>JPA가 클래스패스에 존재</li>
 *   <li>DataSource Bean이 존재</li>
 *   <li>{@code streamix.dashboard.enabled=true} (기본값)</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <caption>자동 등록 Bean 목록</caption>
 *   <tr><th>Bean 타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>{@link StreamingMonitoringService}</td>
 *     <td>스트리밍 세션 모니터링 서비스</td>
 *   </tr>
 * </table>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li>스트리밍 세션 기록</li>
 *   <li>실시간 활성 세션 조회</li>
 *   <li>일별/월별/전체 통계</li>
 *   <li>인기 파일 분석</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   dashboard:
 *     enabled: true  # 대시보드 및 모니터링 활성화 (기본: true)
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamingMonitoringService
 * @see StreamingSessionRepository
 */
@AutoConfiguration
@ConditionalOnClass({JpaRepository.class, DataSource.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "streamix.dashboard.enabled", havingValue = "true", matchIfMissing = true)
public class StreamixMonitoringConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixMonitoringConfiguration.class);

  /**
   * StreamixMonitoringConfiguration의 기본 생성자입니다.
   */
  public StreamixMonitoringConfiguration() {
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