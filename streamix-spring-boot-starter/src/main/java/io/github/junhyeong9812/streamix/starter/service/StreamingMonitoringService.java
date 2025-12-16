package io.github.junhyeong9812.streamix.starter.service;

import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionEntity;
import io.github.junhyeong9812.streamix.starter.adapter.out.persistence.StreamingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 스트리밍 모니터링 서비스입니다.
 *
 * <p>스트리밍 세션의 생성, 업데이트, 조회 및 통계 기능을 제공합니다.
 * 대시보드에서 실시간 모니터링 데이터를 제공하는 데 사용됩니다.</p>
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li><b>세션 관리</b>: 시작, 업데이트, 완료, 오류, 취소</li>
 *   <li><b>실시간 현황</b>: 활성 세션 수, 진행 중인 세션 목록</li>
 *   <li><b>통계</b>: 일별/월별 세션 수, 전송 바이트, 평균 시간</li>
 *   <li><b>분석</b>: 인기 파일, 클라이언트 분포</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @Autowired
 * private StreamingMonitoringService monitoringService;
 *
 * // 세션 시작
 * StreamingSessionEntity session = monitoringService.startSession(
 *     fileId, "192.168.1.1", "Mozilla/5.0...", 0L, 1023L
 * );
 *
 * // 세션 완료
 * monitoringService.completeSession(session.getId(), 1024L, 500L);
 *
 * // 대시보드 통계
 * DashboardStats stats = monitoringService.getDashboardStats();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamingSessionEntity
 * @see StreamingSessionRepository
 */
public class StreamingMonitoringService {

  private static final Logger log = LoggerFactory.getLogger(StreamingMonitoringService.class);

  private final StreamingSessionRepository sessionRepository;

  /**
   * StreamingMonitoringService를 생성합니다.
   *
   * @param sessionRepository 세션 리포지토리
   */
  public StreamingMonitoringService(StreamingSessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  // ==================== 세션 관리 ====================

  /**
   * 새 스트리밍 세션을 시작합니다.
   *
   * @param fileId     파일 ID
   * @param clientIp   클라이언트 IP
   * @param userAgent  User-Agent 헤더
   * @param rangeStart Range 시작 바이트 (nullable)
   * @param rangeEnd   Range 종료 바이트 (nullable)
   * @return 생성된 세션 엔티티
   */
  @Transactional
  public StreamingSessionEntity startSession(
      UUID fileId,
      String clientIp,
      String userAgent,
      Long rangeStart,
      Long rangeEnd
  ) {
    log.debug("Starting streaming session: fileId={}, clientIp={}", fileId, clientIp);

    StreamingSessionEntity session = StreamingSessionEntity.start(
        fileId, clientIp, userAgent, rangeStart, rangeEnd
    );

    StreamingSessionEntity saved = sessionRepository.save(session);
    log.debug("Streaming session started: id={}", saved.getId());

    return saved;
  }

  /**
   * 세션 진행 상황을 업데이트합니다.
   *
   * @param sessionId 세션 ID
   * @param bytesSent 현재까지 전송된 바이트
   */
  @Transactional
  public void updateProgress(Long sessionId, long bytesSent) {
    sessionRepository.findById(sessionId).ifPresent(session -> {
      session.updateProgress(bytesSent);
      sessionRepository.save(session);
    });
  }

  /**
   * 세션을 완료 처리합니다.
   *
   * @param sessionId  세션 ID
   * @param bytesSent  전송된 총 바이트
   * @param durationMs 소요 시간 (밀리초)
   */
  @Transactional
  public void completeSession(Long sessionId, long bytesSent, long durationMs) {
    log.debug("Completing session: id={}, bytes={}, duration={}ms", sessionId, bytesSent, durationMs);

    sessionRepository.findById(sessionId).ifPresent(session -> {
      session.complete(bytesSent, durationMs);
      sessionRepository.save(session);
    });
  }

  /**
   * 세션을 오류 처리합니다.
   *
   * @param sessionId 세션 ID
   * @param bytesSent 오류 발생 전까지 전송된 바이트
   */
  @Transactional
  public void errorSession(Long sessionId, long bytesSent) {
    log.debug("Session error: id={}, bytesSent={}", sessionId, bytesSent);

    sessionRepository.findById(sessionId).ifPresent(session -> {
      session.error(bytesSent);
      sessionRepository.save(session);
    });
  }

  /**
   * 세션을 취소 처리합니다.
   *
   * @param sessionId 세션 ID
   * @param bytesSent 취소 전까지 전송된 바이트
   */
  @Transactional
  public void cancelSession(Long sessionId, long bytesSent) {
    log.debug("Session cancelled: id={}, bytesSent={}", sessionId, bytesSent);

    sessionRepository.findById(sessionId).ifPresent(session -> {
      session.cancel(bytesSent);
      sessionRepository.save(session);
    });
  }

  // ==================== 실시간 현황 ====================

  /**
   * 현재 진행 중인 세션 목록을 조회합니다.
   *
   * @return 활성 세션 목록
   */
  @Transactional(readOnly = true)
  public List<StreamingSessionEntity> getActiveSessions() {
    return sessionRepository.findActiveSessions();
  }

  /**
   * 현재 진행 중인 세션 수를 조회합니다.
   *
   * @return 활성 세션 수
   */
  @Transactional(readOnly = true)
  public long getActiveSessionCount() {
    return sessionRepository.countByStatus(StreamingSessionEntity.SessionStatus.STARTED)
        + sessionRepository.countByStatus(StreamingSessionEntity.SessionStatus.STREAMING);
  }

  /**
   * 최근 세션 목록을 조회합니다.
   *
   * @param limit 조회할 개수
   * @return 최근 세션 목록
   */
  @Transactional(readOnly = true)
  public List<StreamingSessionEntity> getRecentSessions(int limit) {
    return sessionRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit))
        .getContent();
  }

  // ==================== 통계 ====================

  /**
   * 대시보드용 통계를 조회합니다.
   *
   * @return 대시보드 통계
   */
  @Transactional(readOnly = true)
  public DashboardStats getDashboardStats() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
    LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

    return new DashboardStats(
        getActiveSessionCount(),
        sessionRepository.countByStartedAtAfter(todayStart),
        sessionRepository.countByStartedAtAfter(monthStart),
        sessionRepository.count(),
        sessionRepository.sumBytesSentAfter(todayStart),
        sessionRepository.sumBytesSentAfter(monthStart),
        sessionRepository.sumTotalBytesSent(),
        sessionRepository.avgDurationMs()
    );
  }

  /**
   * 특정 파일의 스트리밍 통계를 조회합니다.
   *
   * @param fileId 파일 ID
   * @return 파일 스트리밍 통계
   */
  @Transactional(readOnly = true)
  public FileStreamingStats getFileStats(UUID fileId) {
    return new FileStreamingStats(
        fileId,
        sessionRepository.countByFileId(fileId),
        sessionRepository.sumBytesSentByFileId(fileId)
    );
  }

  /**
   * 가장 많이 스트리밍된 파일 목록을 조회합니다.
   *
   * @param limit 조회할 개수
   * @return 인기 파일 목록 (파일 ID, 스트리밍 횟수)
   */
  @Transactional(readOnly = true)
  public List<PopularFile> getMostStreamedFiles(int limit) {
    return sessionRepository.findMostStreamedFiles(PageRequest.of(0, limit))
        .stream()
        .map(row -> new PopularFile((UUID) row[0], (Long) row[1]))
        .toList();
  }

  // ==================== 정리 ====================

  /**
   * 오래된 세션을 삭제합니다.
   *
   * @param retentionDays 보관 기간 (일)
   * @return 삭제된 세션 수
   */
  @Transactional
  public int cleanupOldSessions(int retentionDays) {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    log.info("Cleaning up sessions older than {}", cutoff);

    int deleted = sessionRepository.deleteByStartedAtBefore(cutoff);
    log.info("Deleted {} old streaming sessions", deleted);

    return deleted;
  }

  // ==================== DTO ====================

  /**
   * 대시보드 통계 DTO.
   *
   * <p>Thymeleaf/SpEL에서 property 접근을 위해 JavaBean 규약의 getter 메서드를 제공합니다.
   * {@code stats.todayBytesFormatted}와 같이 safe navigation 연산자({@code ?.})와 함께 사용할 수 있습니다.</p>
   *
   * @param activeSessions 현재 활성 세션 수
   * @param todaySessions  오늘 세션 수
   * @param monthSessions  이번 달 세션 수
   * @param totalSessions  전체 세션 수
   * @param todayBytes     오늘 전송 바이트
   * @param monthBytes     이번 달 전송 바이트
   * @param totalBytes     전체 전송 바이트
   * @param avgDurationMs  평균 스트리밍 시간 (밀리초)
   */
  public record DashboardStats(
      long activeSessions,
      long todaySessions,
      long monthSessions,
      long totalSessions,
      long todayBytes,
      long monthBytes,
      long totalBytes,
      double avgDurationMs
  ) {

    // ==================== JavaBean Getter (Thymeleaf/SpEL용) ====================

    /**
     * 오늘 전송량을 읽기 쉬운 형식으로 반환합니다.
     * <p>Thymeleaf에서 {@code ${stats?.todayBytesFormatted}}로 접근 가능합니다.</p>
     *
     * @return 예: "1.5 GB", "256 MB"
     */
    public String getTodayBytesFormatted() {
      return formatBytes(todayBytes);
    }

    /**
     * 이번 달 전송량을 읽기 쉬운 형식으로 반환합니다.
     * <p>Thymeleaf에서 {@code ${stats?.monthBytesFormatted}}로 접근 가능합니다.</p>
     *
     * @return 예: "10.2 TB", "1.5 GB"
     */
    public String getMonthBytesFormatted() {
      return formatBytes(monthBytes);
    }

    /**
     * 전체 전송량을 읽기 쉬운 형식으로 반환합니다.
     * <p>Thymeleaf에서 {@code ${stats?.totalBytesFormatted}}로 접근 가능합니다.</p>
     *
     * @return 예: "10.2 TB", "1.5 GB"
     */
    public String getTotalBytesFormatted() {
      return formatBytes(totalBytes);
    }

    /**
     * 평균 스트리밍 시간을 읽기 쉬운 형식으로 반환합니다.
     * <p>Thymeleaf에서 {@code ${stats?.avgDurationFormatted}}로 접근 가능합니다.</p>
     *
     * @return 예: "1분 30초", "45초", "2분 15초"
     */
    public String getAvgDurationFormatted() {
      return formatDuration(avgDurationMs);
    }

    // ==================== 기존 메서드 (하위 호환성 유지) ====================

    /**
     * 오늘 전송량을 읽기 쉬운 형식으로 반환합니다.
     *
     * @return 예: "1.5 GB", "256 MB"
     * @deprecated {@link #getTodayBytesFormatted()} 사용 권장
     */
    @Deprecated
    public String todayBytesFormatted() {
      return getTodayBytesFormatted();
    }

    /**
     * 이번 달 전송량을 읽기 쉬운 형식으로 반환합니다.
     *
     * @return 예: "10.2 TB", "1.5 GB"
     * @deprecated {@link #getMonthBytesFormatted()} 사용 권장
     */
    @Deprecated
    public String monthBytesFormatted() {
      return getMonthBytesFormatted();
    }

    /**
     * 전체 전송량을 읽기 쉬운 형식으로 반환합니다.
     *
     * @return 예: "10.2 TB", "1.5 GB"
     * @deprecated {@link #getTotalBytesFormatted()} 사용 권장
     */
    @Deprecated
    public String totalBytesFormatted() {
      return getTotalBytesFormatted();
    }

    /**
     * 평균 스트리밍 시간을 읽기 쉬운 형식으로 반환합니다.
     *
     * @return 예: "1분 30초", "45초", "2분 15초"
     * @deprecated {@link #getAvgDurationFormatted()} 사용 권장
     */
    @Deprecated
    public String avgDurationFormatted() {
      return getAvgDurationFormatted();
    }

    // ==================== 포맷팅 유틸리티 ====================

    /**
     * 바이트 수를 읽기 쉬운 형식으로 변환합니다.
     *
     * @param bytes 바이트 수
     * @return 포맷된 문자열 (예: "1.5 GB")
     */
    private String formatBytes(long bytes) {
      if (bytes < 1024) return bytes + " B";
      if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
      if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
      if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
      return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    /**
     * 밀리초를 읽기 쉬운 시간 형식으로 변환합니다.
     *
     * @param durationMs 밀리초
     * @return 포맷된 문자열 (예: "1분 30초")
     */
    private String formatDuration(double durationMs) {
      if (durationMs <= 0) return "0초";

      long totalSeconds = (long) (durationMs / 1000);
      long hours = totalSeconds / 3600;
      long minutes = (totalSeconds % 3600) / 60;
      long seconds = totalSeconds % 60;

      if (hours > 0) {
        return String.format("%d시간 %d분 %d초", hours, minutes, seconds);
      } else if (minutes > 0) {
        return String.format("%d분 %d초", minutes, seconds);
      } else {
        return String.format("%d초", seconds);
      }
    }
  }

  /**
   * 파일별 스트리밍 통계 DTO.
   *
   * <p>Thymeleaf/SpEL에서 property 접근을 위해 JavaBean 규약의 getter 메서드를 제공합니다.</p>
   *
   * @param fileId         파일 ID
   * @param streamCount    총 스트리밍 횟수
   * @param totalBytesSent 총 전송 바이트
   */
  public record FileStreamingStats(
      UUID fileId,
      long streamCount,
      long totalBytesSent
  ) {

    /**
     * 총 전송 바이트를 읽기 쉬운 형식으로 반환합니다.
     * <p>Thymeleaf에서 {@code ${fileStats?.totalBytesSentFormatted}}로 접근 가능합니다.</p>
     *
     * @return 예: "1.5 GB", "256 MB"
     */
    public String getTotalBytesSentFormatted() {
      return formatBytes(totalBytesSent);
    }

    /**
     * 총 전송 바이트를 읽기 쉬운 형식으로 반환합니다.
     *
     * @return 예: "1.5 GB", "256 MB"
     * @deprecated {@link #getTotalBytesSentFormatted()} 사용 권장
     */
    @Deprecated
    public String totalBytesSentFormatted() {
      return getTotalBytesSentFormatted();
    }

    /**
     * 바이트 수를 읽기 쉬운 형식으로 변환합니다.
     *
     * @param bytes 바이트 수
     * @return 포맷된 문자열 (예: "1.5 GB")
     */
    private String formatBytes(long bytes) {
      if (bytes < 1024) return bytes + " B";
      if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
      if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
      if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
      return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }
  }

  /**
   * 인기 파일 DTO.
   *
   * @param fileId      파일 ID
   * @param streamCount 스트리밍 횟수
   */
  public record PopularFile(
      UUID fileId,
      long streamCount
  ) {
  }
}