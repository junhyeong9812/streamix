package io.github.junhyeong9812.streamix.starter.adapter.out.persistence;

import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 파일 메타데이터를 저장하는 JPA 엔티티입니다.
 *
 * <p>Core 모듈의 {@link FileMetadata} 도메인 모델을 데이터베이스에 영속화하기 위한
 * 엔티티 클래스입니다. 헥사고날 아키텍처에서 Driven Adapter의 일부로,
 * 도메인 모델과 데이터베이스 테이블 간의 매핑을 담당합니다.</p>
 *
 * <h2>테이블 구조</h2>
 * <pre>
 * CREATE TABLE streamix_file_metadata (
 *     id              UUID PRIMARY KEY,
 *     original_name   VARCHAR(500) NOT NULL,
 *     type            VARCHAR(20) NOT NULL,      -- IMAGE, VIDEO
 *     content_type    VARCHAR(100) NOT NULL,     -- MIME type
 *     size            BIGINT NOT NULL,
 *     storage_path    VARCHAR(1000) NOT NULL,
 *     thumbnail_path  VARCHAR(1000),
 *     created_at      TIMESTAMP NOT NULL,
 *     updated_at      TIMESTAMP NOT NULL
 * );
 * </pre>
 *
 * <h2>인덱스</h2>
 * <ul>
 *   <li>{@code idx_file_metadata_type}: 파일 타입별 조회 최적화</li>
 *   <li>{@code idx_file_metadata_created_at}: 생성일 정렬 최적화</li>
 * </ul>
 *
 * <h2>도메인 변환</h2>
 * <p>엔티티와 도메인 모델 간의 변환은 정적 팩토리 메서드를 통해 수행됩니다:</p>
 * <ul>
 *   <li>{@link #from(FileMetadata)}: 도메인 → 엔티티</li>
 *   <li>{@link #toDomain()}: 엔티티 → 도메인</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 도메인 모델에서 엔티티 생성
 * FileMetadata domain = FileMetadata.create(...);
 * FileMetadataEntity entity = FileMetadataEntity.from(domain);
 *
 * // 엔티티 저장
 * repository.save(entity);
 *
 * // 엔티티에서 도메인 모델로 변환
 * FileMetadata restored = entity.toDomain();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see FileMetadataJpaRepository
 * @see JpaFileMetadataAdapter
 */
@Entity
@Table(
    name = "streamix_file_metadata",
    indexes = {
        @Index(name = "idx_file_metadata_type", columnList = "type"),
        @Index(name = "idx_file_metadata_created_at", columnList = "created_at DESC")
    }
)
public class FileMetadataEntity {

  /**
   * 파일 고유 식별자 (UUID).
   *
   * <p>업로드 시 Core에서 생성된 UUID를 그대로 사용합니다.
   * 자동 생성되지 않으며, 도메인 모델의 ID와 동일합니다.</p>
   */
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  /**
   * 업로드된 원본 파일명.
   *
   * <p>사용자가 업로드한 파일의 원래 이름입니다.
   * 확장자를 포함합니다 (예: "vacation_photo.jpg").</p>
   */
  @Column(name = "original_name", nullable = false, length = 500)
  private String originalName;

  /**
   * 파일 타입 (IMAGE 또는 VIDEO).
   *
   * <p>{@link FileType} enum을 문자열로 저장합니다.</p>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private FileType type;

  /**
   * MIME Content-Type.
   *
   * <p>파일의 MIME 타입입니다 (예: "image/jpeg", "video/mp4").</p>
   */
  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  /**
   * 파일 크기 (바이트 단위).
   */
  @Column(name = "size", nullable = false)
  private long size;

  /**
   * 파일 저장소 내 실제 저장 경로.
   *
   * <p>UUID 기반의 고유한 파일명으로 저장됩니다
   * (예: "/storage/550e8400-e29b-41d4-a716-446655440000.jpg").</p>
   */
  @Column(name = "storage_path", nullable = false, length = 1000)
  private String storagePath;

  /**
   * 썸네일 저장 경로.
   *
   * <p>썸네일이 생성되지 않은 경우 {@code null}입니다.</p>
   */
  @Column(name = "thumbnail_path", length = 1000)
  private String thumbnailPath;

  /**
   * 파일 생성(업로드) 시각.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 파일 수정 시각.
   *
   * <p>메타데이터가 변경될 때마다 갱신됩니다.</p>
   */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * JPA 기본 생성자.
   *
   * <p>JPA 스펙에 따라 기본 생성자가 필요합니다.
   * 직접 호출하지 않고 {@link #from(FileMetadata)}를 사용하세요.</p>
   */
  protected FileMetadataEntity() {
    // JPA 기본 생성자
  }

  /**
   * 모든 필드를 초기화하는 생성자.
   *
   * @param id            파일 ID
   * @param originalName  원본 파일명
   * @param type          파일 타입
   * @param contentType   MIME 타입
   * @param size          파일 크기
   * @param storagePath   저장 경로
   * @param thumbnailPath 썸네일 경로 (nullable)
   * @param createdAt     생성 시각
   * @param updatedAt     수정 시각
   */
  public FileMetadataEntity(
      UUID id,
      String originalName,
      FileType type,
      String contentType,
      long size,
      String storagePath,
      String thumbnailPath,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    this.id = id;
    this.originalName = originalName;
    this.type = type;
    this.contentType = contentType;
    this.size = size;
    this.storagePath = storagePath;
    this.thumbnailPath = thumbnailPath;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * 도메인 모델에서 엔티티를 생성합니다.
   *
   * <p>모든 필드를 도메인 모델에서 복사합니다.
   * ID를 포함한 모든 값이 보존됩니다.</p>
   *
   * @param domain Core 도메인 모델
   * @return 생성된 JPA 엔티티
   * @throws NullPointerException domain이 null인 경우
   */
  public static FileMetadataEntity from(FileMetadata domain) {
    return new FileMetadataEntity(
        domain.id(),
        domain.originalName(),
        domain.type(),
        domain.contentType(),
        domain.size(),
        domain.storagePath(),
        domain.thumbnailPath(),
        domain.createdAt(),
        domain.updatedAt()
    );
  }

  /**
   * 엔티티를 도메인 모델로 변환합니다.
   *
   * <p>데이터베이스에서 조회한 엔티티를 Core 도메인 모델로 변환합니다.
   * 모든 필드가 그대로 복사됩니다.</p>
   *
   * @return Core 도메인 모델
   */
  public FileMetadata toDomain() {
    return new FileMetadata(
        id,
        originalName,
        type,
        contentType,
        size,
        storagePath,
        thumbnailPath,
        createdAt,
        updatedAt
    );
  }

  // ========== Getters ==========

  /**
   * 파일 ID를 반환합니다.
   *
   * @return 파일 UUID
   */
  public UUID getId() {
    return id;
  }

  /**
   * 원본 파일명을 반환합니다.
   *
   * @return 원본 파일명
   */
  public String getOriginalName() {
    return originalName;
  }

  /**
   * 파일 타입을 반환합니다.
   *
   * @return 파일 타입 (IMAGE 또는 VIDEO)
   */
  public FileType getType() {
    return type;
  }

  /**
   * Content-Type을 반환합니다.
   *
   * @return MIME 타입
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * 파일 크기를 반환합니다.
   *
   * @return 파일 크기 (바이트)
   */
  public long getSize() {
    return size;
  }

  /**
   * 저장 경로를 반환합니다.
   *
   * @return 파일 저장 경로
   */
  public String getStoragePath() {
    return storagePath;
  }

  /**
   * 썸네일 경로를 반환합니다.
   *
   * @return 썸네일 경로 (없으면 null)
   */
  public String getThumbnailPath() {
    return thumbnailPath;
  }

  /**
   * 생성 시각을 반환합니다.
   *
   * @return 파일 생성 시각
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * 수정 시각을 반환합니다.
   *
   * @return 파일 수정 시각
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}