package io.github.junhyeong9812.streamix.starter.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 스트리밍 세션을 기록하는 JPA 엔티티입니다.
 *
 * <p>파일 스트리밍 요청이 발생할 때마다 세션 정보를 기록합니다.
 * 대시보드에서 실시간 스트리밍 현황, 통계, 분석에 사용됩니다.</p>
 *
 * <h2>테이블 구조</h2>
 * <pre>
 * CREATE TABLE streamix_streaming_session (
 *     id              BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     file_id         UUID NOT NULL,
 *     client_ip       VARCHAR(45),        -- IPv6 지원
 *     user_agent      VARCHAR(500),
 *     range_start     BIGINT,
 *     range_end       BIGINT,
 *     bytes_sent      BIGINT NOT NULL,
 *     duration_ms     BIGINT,
 *     status          VARCHAR(20) NOT NULL,
 *     started_at      TIMESTAMP NOT NULL,
 *     ended_at        TIMESTAMP
 * );
 * </pre>
 *
 * <h2>인덱스</h2>
 * <ul>
 *   <li>{@code idx_session_file_id}: 파일별 세션 조회</li>
 *   <li>{@code idx_session_started_at}: 시간대별 조회</li>
 *   <li>{@code idx_session_status}: 상태별 조회 (진행 중/완료)</li>
 * </ul>
 *
 * <h2>세션 상태</h2>
 * <ul>
 *   <li>{@code STARTED}: 스트리밍 시작됨</li>
 *   <li>{@code STREAMING}: 스트리밍 진행 중</li>
 *   <li>{@code COMPLETED}: 정상 완료</li>
 *   <li>{@code ERROR}: 오류 발생</li>
 *   <li>{@code CANCELLED}: 클라이언트가 취소</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 세션 시작
 * StreamingSessionEntity session = StreamingSessionEntity.start(
 *     fileId, clientIp, userAgent, rangeStart, rangeEnd
 * );
 * repository.save(session);
 *
 * // 세션 완료
 * session.complete(bytesSent, durationMs);
 * repository.save(session);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamingSessionRepository
 */
@Entity
@Table(
    name = "streamix_streaming_session",
    indexes = {
        @Index(name = "idx_session_file_id", columnList = "file_id"),
        @Index(name = "idx_session_started_at", columnList = "started_at DESC"),
        @Index(name = "idx_session_status", columnList = "status")
    }
)
public class StreamingSessionEntity {

  /**
   * 세션 고유 식별자 (자동 생성).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 스트리밍 대상 파일 ID.
   */
  @Column(name = "file_id", nullable = false)
  private UUID fileId;

  /**
   * 클라이언트 IP 주소.
   *
   * <p>IPv6 주소를 지원하기 위해 45자까지 허용합니다.</p>
   */
  @Column(name = "client_ip", length = 45)
  private String clientIp;

  /**
   * 클라이언트 User-Agent 헤더.
   */
  @Column(name = "user_agent", length = 500)
  private String userAgent;

  /**
   * HTTP Range 시작 바이트 (Range 요청인 경우).
   */
  @Column(name = "range_start")
  private Long rangeStart;

  /**
   * HTTP Range 종료 바이트 (Range 요청인 경우).
   */
  @Column(name = "range_end")
  private Long rangeEnd;

  /**
   * 전송된 바이트 수.
   */
  @Column(name = "bytes_sent", nullable = false)
  private long bytesSent;

  /**
   * 스트리밍 소요 시간 (밀리초).
   */
  @Column(name = "duration_ms")
  private Long durationMs;

  /**
   * 세션 상태.
   */
  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SessionStatus status;

  /**
   * 세션 시작 시각.
   */
  @Column(name = "started_at", nullable = false, updatable = false)
  private LocalDateTime startedAt;

  /**
   * 세션 종료 시각.
   */
  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  /**
   * 세션 상태 열거형.
   */
  public enum SessionStatus {
    /** 스트리밍 시작됨 */
    STARTED,
    /** 스트리밍 진행 중 */
    STREAMING,
    /** 정상 완료 */
    COMPLETED,
    /** 오류 발생 */
    ERROR,
    /** 클라이언트 취소 */
    CANCELLED
  }

  /**
   * JPA 기본 생성자.
   */
  protected StreamingSessionEntity() {
  }

  /**
   * 모든 필드를 초기화하는 생성자.
   */
  private StreamingSessionEntity(
      UUID fileId,
      String clientIp,
      String userAgent,
      Long rangeStart,
      Long rangeEnd,
      SessionStatus status,
      LocalDateTime startedAt
  ) {
    this.fileId = fileId;
    this.clientIp = clientIp;
    this.userAgent = userAgent;
    this.rangeStart = rangeStart;
    this.rangeEnd = rangeEnd;
    this.bytesSent = 0;
    this.status = status;
    this.startedAt = startedAt;
  }

  /**
   * 새 스트리밍 세션을 시작합니다.
   *
   * @param fileId     스트리밍 대상 파일 ID
   * @param clientIp   클라이언트 IP
   * @param userAgent  클라이언트 User-Agent
   * @param rangeStart Range 시작 (nullable)
   * @param rangeEnd   Range 종료 (nullable)
   * @return 시작된 세션 엔티티
   */
  public static StreamingSessionEntity start(
      UUID fileId,
      String clientIp,
      String userAgent,
      Long rangeStart,
      Long rangeEnd
  ) {
    return new StreamingSessionEntity(
        fileId,
        clientIp,
        userAgent,
        rangeStart,
        rangeEnd,
        SessionStatus.STARTED,
        LocalDateTime.now()
    );
  }

  /**
   * 세션을 완료 상태로 변경합니다.
   *
   * @param bytesSent  전송된 바이트 수
   * @param durationMs 소요 시간 (밀리초)
   */
  public void complete(long bytesSent, long durationMs) {
    this.bytesSent = bytesSent;
    this.durationMs = durationMs;
    this.status = SessionStatus.COMPLETED;
    this.endedAt = LocalDateTime.now();
  }

  /**
   * 세션을 오류 상태로 변경합니다.
   *
   * @param bytesSent 오류 발생 전까지 전송된 바이트 수
   */
  public void error(long bytesSent) {
    this.bytesSent = bytesSent;
    this.status = SessionStatus.ERROR;
    this.endedAt = LocalDateTime.now();
  }

  /**
   * 세션을 취소 상태로 변경합니다.
   *
   * @param bytesSent 취소 전까지 전송된 바이트 수
   */
  public void cancel(long bytesSent) {
    this.bytesSent = bytesSent;
    this.status = SessionStatus.CANCELLED;
    this.endedAt = LocalDateTime.now();
  }

  /**
   * 진행 중 상태로 업데이트합니다.
   *
   * @param bytesSent 현재까지 전송된 바이트 수
   */
  public void updateProgress(long bytesSent) {
    this.bytesSent = bytesSent;
    this.status = SessionStatus.STREAMING;
  }

  // ========== Getters ==========

  /** @return 세션 ID */
  public Long getId() {
    return id;
  }

  /** @return 파일 ID */
  public UUID getFileId() {
    return fileId;
  }

  /** @return 클라이언트 IP */
  public String getClientIp() {
    return clientIp;
  }

  /** @return User-Agent */
  public String getUserAgent() {
    return userAgent;
  }

  /** @return Range 시작 바이트 */
  public Long getRangeStart() {
    return rangeStart;
  }

  /** @return Range 종료 바이트 */
  public Long getRangeEnd() {
    return rangeEnd;
  }

  /** @return 전송 바이트 수 */
  public long getBytesSent() {
    return bytesSent;
  }

  /** @return 지속 시간(ms) */
  public Long getDurationMs() {
    return durationMs;
  }

  /** @return 세션 상태 */
  public SessionStatus getStatus() {
    return status;
  }

  /** @return 시작 시각 */
  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  /** @return 종료 시각 */
  public LocalDateTime getEndedAt() {
    return endedAt;
  }

  /**
   * Range 요청인지 확인합니다.
   *
   * @return Range 요청이면 {@code true}
   */
  public boolean isRangeRequest() {
    return rangeStart != null;
  }

  /**
   * 완료된 세션인지 확인합니다.
   *
   * @return 완료/오류/취소 상태면 {@code true}
   */
  public boolean isEnded() {
    return status == SessionStatus.COMPLETED
        || status == SessionStatus.ERROR
        || status == SessionStatus.CANCELLED;
  }
}