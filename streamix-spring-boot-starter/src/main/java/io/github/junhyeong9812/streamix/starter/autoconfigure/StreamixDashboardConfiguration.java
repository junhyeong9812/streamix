package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.starter.adapter.in.dashboard.StreamixDashboardController;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Streamix 대시보드 자동 설정 클래스입니다.
 *
 * <p>웹 환경에서 대시보드 컨트롤러를 자동으로 구성합니다.
 * Thymeleaf 템플릿을 사용하여 파일 관리 UI를 제공합니다.</p>
 *
 * <h2>활성화 조건</h2>
 * <ul>
 *   <li>서블릿 기반 웹 애플리케이션</li>
 *   <li>{@code streamix.dashboard.enabled=true} (기본값)</li>
 *   <li>{@link StreamingMonitoringService} Bean 존재</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <caption>자동 등록 Bean 목록</caption>
 *   <tr><th>Bean 타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>{@link StreamixDashboardController}</td>
 *     <td>대시보드 웹 컨트롤러</td>
 *   </tr>
 * </table>
 *
 * <h2>제공 페이지</h2>
 * <ul>
 *   <li>{@code /streamix} - 메인 대시보드</li>
 *   <li>{@code /streamix/files} - 파일 목록</li>
 *   <li>{@code /streamix/files/{id}} - 파일 상세</li>
 *   <li>{@code /streamix/sessions} - 스트리밍 세션</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   dashboard:
 *     enabled: true       # 대시보드 활성화 (기본: true)
 *     path: /streamix     # 대시보드 경로 (기본: /streamix)
 * }</pre>
 *
 * <h2>비활성화</h2>
 * <pre>{@code
 * streamix:
 *   dashboard:
 *     enabled: false
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixDashboardController
 * @see StreamingMonitoringService
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(name = "streamix.dashboard.enabled", havingValue = "true", matchIfMissing = true)
public class StreamixDashboardConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixDashboardConfiguration.class);

  /**
   * StreamixDashboardConfiguration의 기본 생성자입니다.
   */
  public StreamixDashboardConfiguration() {
  }

  /**
   * 대시보드 컨트롤러를 생성합니다.
   *
   * <p>파일 관리, 통계, 스트리밍 세션 모니터링을 위한 웹 UI를 제공합니다.
   * Thymeleaf 템플릿을 사용하여 뷰를 렌더링합니다.</p>
   *
   * @param monitoringService    스트리밍 모니터링 서비스
   * @param getFileMetadataUseCase 파일 메타데이터 조회 유스케이스
   * @param deleteFileUseCase    파일 삭제 유스케이스
   * @param fileMetadataPort     파일 메타데이터 포트
   * @param properties           Streamix 설정
   * @return 대시보드 컨트롤러
   */
  @Bean
  @ConditionalOnMissingBean(StreamixDashboardController.class)
  @ConditionalOnBean(StreamingMonitoringService.class)
  public StreamixDashboardController streamixDashboardController(
      StreamingMonitoringService monitoringService,
      GetFileMetadataUseCase getFileMetadataUseCase,
      DeleteFileUseCase deleteFileUseCase,
      FileMetadataPort fileMetadataPort,
      StreamixProperties properties
  ) {
    log.info("Creating StreamixDashboardController: path={}", properties.dashboard().path());
    return new StreamixDashboardController(
        monitoringService,
        getFileMetadataUseCase,
        deleteFileUseCase,
        fileMetadataPort,
        properties
    );
  }
}