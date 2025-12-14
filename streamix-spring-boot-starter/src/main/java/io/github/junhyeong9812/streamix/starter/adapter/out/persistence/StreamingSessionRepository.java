package io.github.junhyeong9812.streamix.starter.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 스트리밍 세션 JPA 리포지토리 인터페이스입니다.
 *
 * <p>Spring Data JPA를 사용하여 {@link StreamingSessionEntity}의 CRUD 및
 * 통계/분석용 쿼리를 제공합니다.</p>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li><b>기본 CRUD</b>: JpaRepository 상속</li>
 *   <li><b>파일별 조회</b>: 특정 파일의 스트리밍 기록</li>
 *   <li><b>실시간 모니터링</b>: 진행 중인 세션 조회</li>
 *   <li><b>통계</b>: 기간별 세션 수, 전송 바이트 합계</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 현재 활성 세션 수
 * long activeSessions = repository.countByStatus(SessionStatus.STREAMING);
 *
 * // 오늘 전송된 총 바이트
 * long bytesToday = repository.sumBytesSentAfter(LocalDate.now().atStartOfDay());
 *
 * // 최근 세션 10개
 * List<StreamingSessionEntity> recent = repository.findTop10ByOrderByStartedAtDesc();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamingSessionEntity
 */
@Repository
public interface StreamingSessionRepository extends JpaRepository<StreamingSessionEntity, Long> {

  /**
   * 특정 파일의 스트리밍 세션을 최신순으로 조회합니다.
   *
   * @param fileId   파일 ID
   * @param pageable 페이징 정보
   * @return 스트리밍 세션 페이지
   */
  Page<StreamingSessionEntity> findByFileIdOrderByStartedAtDesc(UUID fileId, Pageable pageable);

  /**
   * 특정 상태의 세션 수를 조회합니다.
   *
   * @param status 세션 상태
   * @return 해당 상태의 세션 수
   */
  long countByStatus(StreamingSessionEntity.SessionStatus status);

  /**
   * 현재 진행 중인 세션 목록을 조회합니다.
   *
   * @return 진행 중인 세션 목록 (STARTED 또는 STREAMING)
   */
  @Query("SELECT s FROM StreamingSessionEntity s WHERE s.status IN ('STARTED', 'STREAMING') ORDER BY s.startedAt DESC")
  List<StreamingSessionEntity> findActiveSessions();

  /**
   * 최신 세션 N개를 조회합니다.
   *
   * @return 최신 세션 10개
   */
  List<StreamingSessionEntity> findTop10ByOrderByStartedAtDesc();

  /**
   * 최신 세션을 지정된 수만큼 조회합니다.
   *
   * @param pageable 페이징 정보 (size로 개수 지정)
   * @return 최신 세션 목록
   */
  Page<StreamingSessionEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

  /**
   * 특정 기간 내의 세션 수를 조회합니다.
   *
   * @param start 시작 시각 (inclusive)
   * @param end   종료 시각 (exclusive)
   * @return 해당 기간의 세션 수
   */
  long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);

  /**
   * 특정 시각 이후의 세션 수를 조회합니다.
   *
   * @param after 기준 시각
   * @return 해당 시각 이후의 세션 수
   */
  long countByStartedAtAfter(LocalDateTime after);

  /**
   * 특정 시각 이후에 전송된 총 바이트 수를 조회합니다.
   *
   * @param after 기준 시각
   * @return 전송된 바이트 합계
   */
  @Query("SELECT COALESCE(SUM(s.bytesSent), 0) FROM StreamingSessionEntity s WHERE s.startedAt > :after")
  long sumBytesSentAfter(@Param("after") LocalDateTime after);

  /**
   * 특정 기간 내에 전송된 총 바이트 수를 조회합니다.
   *
   * @param start 시작 시각
   * @param end   종료 시각
   * @return 전송된 바이트 합계
   */
  @Query("SELECT COALESCE(SUM(s.bytesSent), 0) FROM StreamingSessionEntity s WHERE s.startedAt BETWEEN :start AND :end")
  long sumBytesSentBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /**
   * 전체 전송 바이트 합계를 조회합니다.
   *
   * @return 전체 전송 바이트
   */
  @Query("SELECT COALESCE(SUM(s.bytesSent), 0) FROM StreamingSessionEntity s")
  long sumTotalBytesSent();

  /**
   * 완료된 세션의 평균 소요 시간을 조회합니다 (밀리초).
   *
   * @return 평균 소요 시간
   */
  @Query("SELECT COALESCE(AVG(s.durationMs), 0) FROM StreamingSessionEntity s WHERE s.status = 'COMPLETED' AND s.durationMs IS NOT NULL")
  double avgDurationMs();

  /**
   * 특정 파일의 총 스트리밍 횟수를 조회합니다.
   *
   * @param fileId 파일 ID
   * @return 스트리밍 횟수
   */
  long countByFileId(UUID fileId);

  /**
   * 특정 파일의 총 전송 바이트를 조회합니다.
   *
   * @param fileId 파일 ID
   * @return 전송 바이트 합계
   */
  @Query("SELECT COALESCE(SUM(s.bytesSent), 0) FROM StreamingSessionEntity s WHERE s.fileId = :fileId")
  long sumBytesSentByFileId(@Param("fileId") UUID fileId);

  /**
   * 가장 많이 스트리밍된 파일 목록을 조회합니다.
   *
   * @param pageable 페이징 정보 (상위 N개)
   * @return 파일 ID와 스트리밍 횟수 목록
   */
  @Query("SELECT s.fileId, COUNT(s) as cnt FROM StreamingSessionEntity s GROUP BY s.fileId ORDER BY cnt DESC")
  List<Object[]> findMostStreamedFiles(Pageable pageable);

  /**
   * 특정 IP에서의 세션 목록을 조회합니다.
   *
   * @param clientIp 클라이언트 IP
   * @param pageable 페이징 정보
   * @return 세션 페이지
   */
  Page<StreamingSessionEntity> findByClientIpOrderByStartedAtDesc(String clientIp, Pageable pageable);

  /**
   * 오래된 세션을 삭제합니다 (정리용).
   *
   * @param before 기준 시각 (이 시각 이전의 세션 삭제)
   * @return 삭제된 세션 수
   */
  @Query("DELETE FROM StreamingSessionEntity s WHERE s.startedAt < :before")
  int deleteByStartedAtBefore(@Param("before") LocalDateTime before);
}