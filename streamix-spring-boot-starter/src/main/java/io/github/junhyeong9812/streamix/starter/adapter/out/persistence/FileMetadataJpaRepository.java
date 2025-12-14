package io.github.junhyeong9812.streamix.starter.adapter.out.persistence;

import io.github.junhyeong9812.streamix.core.domain.model.FileType;
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
 * 파일 메타데이터 JPA 리포지토리 인터페이스입니다.
 *
 * <p>Spring Data JPA를 사용하여 {@link FileMetadataEntity}의 CRUD 및
 * 커스텀 쿼리 기능을 제공합니다. 기본적인 JPA 메서드 외에
 * 대시보드와 통계를 위한 추가 쿼리 메서드를 정의합니다.</p>
 *
 * <h2>제공 기능</h2>
 * <ul>
 *   <li><b>기본 CRUD</b>: JpaRepository 상속</li>
 *   <li><b>타입별 조회</b>: 이미지/비디오 필터링</li>
 *   <li><b>통계</b>: 타입별 개수, 전체 용량</li>
 *   <li><b>페이징 조회</b>: 정렬된 목록 조회</li>
 * </ul>
 *
 * <h2>쿼리 메서드 명명 규칙</h2>
 * <p>Spring Data JPA의 메서드 이름 기반 쿼리 생성을 활용합니다:</p>
 * <ul>
 *   <li>{@code findBy...}: SELECT WHERE 절 생성</li>
 *   <li>{@code countBy...}: COUNT 쿼리 생성</li>
 *   <li>{@code OrderBy...Desc/Asc}: 정렬 조건 추가</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @Service
 * public class FileService {
 *     private final FileMetadataJpaRepository repository;
 *
 *     // 최신 파일 10개 조회
 *     public List<FileMetadataEntity> getRecentFiles() {
 *         return repository.findTop10ByOrderByCreatedAtDesc();
 *     }
 *
 *     // 이미지 파일만 페이징 조회
 *     public Page<FileMetadataEntity> getImages(Pageable pageable) {
 *         return repository.findByType(FileType.IMAGE, pageable);
 *     }
 *
 *     // 전체 저장 용량 조회
 *     public long getTotalStorageUsed() {
 *         return repository.sumTotalSize();
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadataEntity
 * @see JpaFileMetadataAdapter
 */
@Repository
public interface FileMetadataJpaRepository extends JpaRepository<FileMetadataEntity, UUID> {

  /**
   * 파일 타입별로 조회합니다.
   *
   * <p>IMAGE 또는 VIDEO 타입으로 필터링된 결과를 페이징하여 반환합니다.</p>
   *
   * @param type     파일 타입 (IMAGE 또는 VIDEO)
   * @param pageable 페이징 정보
   * @return 필터링된 파일 메타데이터 페이지
   */
  Page<FileMetadataEntity> findByType(FileType type, Pageable pageable);

  /**
   * 파일 타입별 개수를 조회합니다.
   *
   * @param type 파일 타입
   * @return 해당 타입의 파일 개수
   */
  long countByType(FileType type);

  /**
   * 최신 파일 N개를 조회합니다.
   *
   * <p>생성일 역순(최신순)으로 정렬하여 상위 N개를 반환합니다.
   * 대시보드의 "최근 업로드" 목록에 사용됩니다.</p>
   *
   * @return 최신 파일 10개
   */
  List<FileMetadataEntity> findTop10ByOrderByCreatedAtDesc();

  /**
   * 생성일 역순으로 정렬된 전체 목록을 페이징 조회합니다.
   *
   * @param pageable 페이징 정보
   * @return 정렬된 파일 메타데이터 페이지
   */
  Page<FileMetadataEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /**
   * 특정 기간 내에 생성된 파일 개수를 조회합니다.
   *
   * @param start 시작 시각 (inclusive)
   * @param end   종료 시각 (exclusive)
   * @return 해당 기간의 파일 개수
   */
  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  /**
   * 전체 파일의 총 용량을 계산합니다.
   *
   * <p>대시보드의 "전체 저장 용량" 표시에 사용됩니다.
   * 파일이 없는 경우 0을 반환합니다.</p>
   *
   * @return 전체 파일 크기 합계 (바이트)
   */
  @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadataEntity f")
  long sumTotalSize();

  /**
   * 특정 타입 파일의 총 용량을 계산합니다.
   *
   * @param type 파일 타입
   * @return 해당 타입 파일 크기 합계 (바이트)
   */
  @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadataEntity f WHERE f.type = :type")
  long sumSizeByType(@Param("type") FileType type);

  /**
   * 특정 기간 내에 생성된 파일의 총 용량을 계산합니다.
   *
   * @param start 시작 시각 (inclusive)
   * @param end   종료 시각 (exclusive)
   * @return 해당 기간 파일 크기 합계 (바이트)
   */
  @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadataEntity f WHERE f.createdAt BETWEEN :start AND :end")
  long sumSizeByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /**
   * 원본 파일명으로 검색합니다.
   *
   * <p>대소문자를 구분하지 않는 부분 일치 검색을 수행합니다.
   * 대시보드의 파일 검색 기능에 사용됩니다.</p>
   *
   * @param keyword  검색 키워드
   * @param pageable 페이징 정보
   * @return 검색된 파일 메타데이터 페이지
   */
  @Query("SELECT f FROM FileMetadataEntity f WHERE LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY f.createdAt DESC")
  Page<FileMetadataEntity> searchByOriginalName(@Param("keyword") String keyword, Pageable pageable);
}