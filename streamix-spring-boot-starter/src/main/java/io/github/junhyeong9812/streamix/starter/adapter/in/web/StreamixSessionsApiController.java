package io.github.junhyeong9812.streamix.starter.adapter.in.web;

import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionEntity;
import io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Streamix 활성 스트리밍 세션 JSON API 컨트롤러입니다.
 *
 * <p>대시보드의 sessions 페이지에서 자동 새로고침(5초 폴링)을 위해
 * 활성 세션 목록을 JSON으로 제공합니다. JPA 엔티티 대신 가볍고
 * 직렬화에 안전한 record DTO({@link ActiveSessionResponse})로 변환하여 반환합니다.</p>
 *
 * <h2>제공 엔드포인트</h2>
 * <table border="1">
 *   <caption>Sessions JSON API</caption>
 *   <tr><th>메서드</th><th>경로</th><th>설명</th></tr>
 *   <tr><td>GET</td><td>/sessions/active</td><td>활성 스트리밍 세션 목록</td></tr>
 * </table>
 *
 * <h2>설정</h2>
 * <p>API 기본 경로는 {@code streamix.api.base-path}로 설정합니다 (기본: /api/streamix).</p>
 *
 * <h2>응답 예시</h2>
 * <pre>{@code
 * GET /api/streamix/sessions/active
 * [
 *   {
 *     "id": 12345,
 *     "fileId": "550e8400-e29b-41d4-a716-446655440000",
 *     "clientIp": "192.168.1.4",
 *     "startedAt": "2026-05-11T10:30:00",
 *     "bytesSent": 1048576,
 *     "status": "STREAMING"
 *   }
 * ]
 * }</pre>
 *
 * @author junhyeong9812
 * @since 3.0.0
 * @see StreamingMonitoringService#getActiveSessions()
 */
@RestController
public class StreamixSessionsApiController {

  private static final Logger log = LoggerFactory.getLogger(StreamixSessionsApiController.class);

  private final StreamingMonitoringService monitoringService;

  /**
   * StreamixSessionsApiController를 생성합니다.
   *
   * @param monitoringService 스트리밍 모니터링 서비스
   */
  public StreamixSessionsApiController(StreamingMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  /**
   * 현재 활성 스트리밍 세션 목록을 JSON으로 반환합니다.
   *
   * <p>STARTED, STREAMING 상태의 세션만 포함됩니다.</p>
   *
   * @return 활성 세션 목록
   */
  @GetMapping(
      value = "${streamix.api.base-path:/api/streamix}/sessions/active",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<List<ActiveSessionResponse>> activeSessions() {
    List<StreamingSessionEntity> sessions = monitoringService.getActiveSessions();
    log.debug("Active sessions polled: count={}", sessions.size());

    List<ActiveSessionResponse> body = sessions.stream()
        .map(ActiveSessionResponse::from)
        .toList();

    return ResponseEntity.ok(body);
  }

  /**
   * 활성 세션 응답 DTO입니다.
   *
   * <p>JPA 엔티티를 직접 노출하지 않고 직렬화에 안전한 필드만 노출합니다.</p>
   *
   * @param id        세션 ID
   * @param fileId    파일 ID
   * @param clientIp  클라이언트 IP
   * @param startedAt 세션 시작 시각
   * @param bytesSent 현재까지 전송된 바이트 수
   * @param status    세션 상태명 (STARTED / STREAMING)
   */
  public record ActiveSessionResponse(
      Long id,
      UUID fileId,
      String clientIp,
      LocalDateTime startedAt,
      long bytesSent,
      String status
  ) {

    /**
     * 엔티티에서 응답 DTO를 생성합니다.
     *
     * @param entity 세션 엔티티
     * @return 응답 DTO
     */
    public static ActiveSessionResponse from(StreamingSessionEntity entity) {
      return new ActiveSessionResponse(
          entity.getId(),
          entity.getFileId(),
          entity.getClientIp(),
          entity.getStartedAt(),
          entity.getBytesSent(),
          entity.getStatus().name()
      );
    }
  }
}
