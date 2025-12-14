package io.github.junhyeong9812.streamix.starter.annotation;

import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixAutoConfiguration;
import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixDashboardConfiguration;
import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixMonitoringConfiguration;
import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixRepositoryConfiguration;
import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixThumbnailConfiguration;
import io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixWebConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Streamix 미디어 스트리밍 기능을 활성화하는 어노테이션입니다.
 *
 * <p>Spring Boot 애플리케이션의 메인 클래스에 이 어노테이션을 추가하면
 * Streamix의 모든 기능(파일 업로드, 스트리밍, 썸네일 생성 등)이 자동으로 구성됩니다.</p>
 *
 * <h2>활성화되는 기능</h2>
 * <ul>
 *   <li><b>파일 업로드</b>: 이미지/비디오 파일 업로드 및 저장</li>
 *   <li><b>HTTP Range 스트리밍</b>: 비디오 탐색(seek) 지원</li>
 *   <li><b>썸네일 자동 생성</b>: 이미지/비디오 썸네일 생성</li>
 *   <li><b>REST API</b>: 파일 관리 API 엔드포인트 (설정 시)</li>
 *   <li><b>대시보드</b>: 웹 기반 관리 UI (설정 시)</li>
 *   <li><b>JPA 저장소</b>: 메타데이터 영속화</li>
 *   <li><b>모니터링</b>: 스트리밍 세션 통계</li>
 * </ul>
 *
 * <h2>기본 사용법</h2>
 * <pre>{@code
 * @SpringBootApplication
 * @EnableStreamix
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * <h2>설정 예시 (application.yml)</h2>
 * <pre>{@code
 * streamix:
 *   storage:
 *     base-path: ./uploads          # 파일 저장 경로
 *     max-file-size: 104857600      # 최대 파일 크기 (100MB)
 *   thumbnail:
 *     enabled: true                 # 썸네일 생성 활성화
 *     width: 320                    # 썸네일 너비
 *     height: 180                   # 썸네일 높이
 *   api:
 *     enabled: true                 # REST API 활성화
 *     base-path: /api/streamix      # API 기본 경로
 *   dashboard:
 *     enabled: true                 # 대시보드 활성화
 *     path: /streamix               # 대시보드 경로
 * }</pre>
 *
 * <h2>자동 구성되는 Bean</h2>
 * <ul>
 *   <li>{@link io.github.junhyeong9812.streamix.core.application.service.FileUploadService}</li>
 *   <li>{@link io.github.junhyeong9812.streamix.core.application.service.FileStreamService}</li>
 *   <li>{@link io.github.junhyeong9812.streamix.core.application.service.FileMetadataService}</li>
 *   <li>{@link io.github.junhyeong9812.streamix.core.application.service.ThumbnailService}</li>
 *   <li>{@link io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService}</li>
 * </ul>
 *
 * <h2>Import되는 Configuration</h2>
 * <ul>
 *   <li>{@link StreamixRepositoryConfiguration} - JPA Entity/Repository 스캔 및 FileMetadataPort Bean</li>
 *   <li>{@link StreamixAutoConfiguration} - Core 서비스 Bean 등록</li>
 *   <li>{@link StreamixWebConfiguration} - REST API 컨트롤러</li>
 *   <li>{@link StreamixThumbnailConfiguration} - FFmpeg 썸네일 어댑터</li>
 *   <li>{@link StreamixMonitoringConfiguration} - 모니터링 서비스</li>
 *   <li>{@link StreamixDashboardConfiguration} - 대시보드 컨트롤러</li>
 * </ul>
 *
 * <h2>커스터마이징</h2>
 * <p>각 Bean은 {@code @ConditionalOnMissingBean}으로 등록되므로,
 * 직접 Bean을 정의하면 기본 구현을 대체할 수 있습니다.</p>
 * <pre>{@code
 * @Configuration
 * public class CustomStreamixConfig {
 *     @Bean
 *     public FileStoragePort customStorage() {
 *         return new S3FileStorageAdapter(...);
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixAutoConfiguration
 * @see StreamixRepositoryConfiguration
 * @see io.github.junhyeong9812.streamix.starter.properties.StreamixProperties
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    StreamixRepositoryConfiguration.class,
    StreamixAutoConfiguration.class,
    StreamixWebConfiguration.class,
    StreamixThumbnailConfiguration.class,
    StreamixMonitoringConfiguration.class,
    StreamixDashboardConfiguration.class
})
public @interface EnableStreamix {
}