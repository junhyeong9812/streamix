package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetThumbnailUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.StreamFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.GlobalExceptionHandler;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.StreamixApiController;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Streamix Web/REST API 자동 설정 클래스입니다.
 *
 * <p>Spring Web MVC 환경에서 REST API 컨트롤러와 예외 처리기를 자동으로 구성합니다.
 * {@code streamix.api.enabled=true} (기본값)일 때만 활성화됩니다.</p>
 *
 * <h2>활성화 조건</h2>
 * <ul>
 *   <li>Spring Web MVC 환경 ({@link WebMvcConfigurer} 존재)</li>
 *   <li>서블릿 기반 웹 애플리케이션</li>
 *   <li>{@code streamix.api.enabled=true} (기본값)</li>
 * </ul>
 *
 * <h2>자동 등록되는 Bean</h2>
 * <table border="1">
 *   <caption>자동 등록 Bean 목록</caption>
 *   <tr><th>Bean 타입</th><th>설명</th></tr>
 *   <tr>
 *     <td>{@link StreamixApiController}</td>
 *     <td>REST API 컨트롤러</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GlobalExceptionHandler}</td>
 *     <td>전역 예외 처리기</td>
 *   </tr>
 * </table>
 *
 * <h2>제공되는 API 엔드포인트</h2>
 * <p>기본 경로: {@code /api/streamix} (설정 가능)</p>
 * <ul>
 *   <li>{@code POST /files} - 파일 업로드</li>
 *   <li>{@code GET /files} - 파일 목록 조회</li>
 *   <li>{@code GET /files/{id}} - 파일 정보 조회</li>
 *   <li>{@code GET /files/{id}/stream} - 파일 스트리밍</li>
 *   <li>{@code GET /files/{id}/thumbnail} - 썸네일 조회</li>
 *   <li>{@code DELETE /files/{id}} - 파일 삭제</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   api:
 *     enabled: true           # API 활성화 (기본: true)
 *     base-path: /api/media   # API 기본 경로 변경
 * }</pre>
 *
 * <h2>비활성화</h2>
 * <p>API를 비활성화하려면:</p>
 * <pre>{@code
 * streamix:
 *   api:
 *     enabled: false
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamixApiController
 * @see GlobalExceptionHandler
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(name = "streamix.api.enabled", havingValue = "true", matchIfMissing = true)
public class StreamixWebConfiguration {

  private static final Logger log = LoggerFactory.getLogger(StreamixWebConfiguration.class);

  /**
   * StreamixWebConfiguration의 기본 생성자입니다.
   */
  public StreamixWebConfiguration() {
    // 기본 생성자
  }

  /**
   * REST API 컨트롤러를 생성합니다.
   *
   * <p>파일 업로드, 조회, 스트리밍, 삭제 등의 REST API 엔드포인트를 제공합니다.
   * Core 모듈의 UseCase들을 주입받아 비즈니스 로직을 수행합니다.</p>
   *
   * @param uploadFileUseCase      파일 업로드 유스케이스
   * @param streamFileUseCase      파일 스트리밍 유스케이스
   * @param getThumbnailUseCase    썸네일 조회 유스케이스
   * @param getFileMetadataUseCase 메타데이터 조회 유스케이스
   * @param deleteFileUseCase      파일 삭제 유스케이스
   * @param fileMetadataPort       메타데이터 저장소 (count 조회용)
   * @param properties             Streamix 설정
   * @return REST API 컨트롤러
   */
  @Bean
  @ConditionalOnMissingBean(StreamixApiController.class)
  public StreamixApiController streamixApiController(
      UploadFileUseCase uploadFileUseCase,
      StreamFileUseCase streamFileUseCase,
      GetThumbnailUseCase getThumbnailUseCase,
      GetFileMetadataUseCase getFileMetadataUseCase,
      DeleteFileUseCase deleteFileUseCase,
      FileMetadataPort fileMetadataPort,
      StreamixProperties properties
  ) {
    log.info("Creating StreamixApiController: basePath={}", properties.api().basePath());

    return new StreamixApiController(
        uploadFileUseCase,
        streamFileUseCase,
        getThumbnailUseCase,
        getFileMetadataUseCase,
        deleteFileUseCase,
        fileMetadataPort,
        properties
    );
  }

  /**
   * 전역 예외 처리기를 생성합니다.
   *
   * <p>Streamix API에서 발생하는 예외를 일관된 형식의 JSON 응답으로 변환합니다.
   * FileNotFoundException, InvalidFileTypeException 등의 Core 예외를 처리합니다.</p>
   *
   * @return 전역 예외 처리기
   */
  @Bean
  @ConditionalOnMissingBean(GlobalExceptionHandler.class)
  public GlobalExceptionHandler globalExceptionHandler() {
    log.info("Creating GlobalExceptionHandler for Streamix API");
    return new GlobalExceptionHandler();
  }
}