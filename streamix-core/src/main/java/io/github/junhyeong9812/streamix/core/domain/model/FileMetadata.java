package io.github.junhyeong9812.streamix.core.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 파일 메타데이터를 표현하는 불변 레코드입니다.
 *
 * <p>업로드된 파일의 모든 메타정보를 담고 있으며, 파일 저장소와 메타데이터 저장소 간의
 * 도메인 객체로 사용됩니다. Java Record로 구현되어 불변성을 보장합니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 새 메타데이터 생성
 * FileMetadata metadata = FileMetadata.create(
 *     "photo.jpg",
 *     FileType.IMAGE,
 *     "image/jpeg",
 *     1024L,
 *     "/storage/uuid.jpg"
 * );
 *
 * // 썸네일 경로 추가 (불변 - 새 객체 반환)
 * FileMetadata withThumb = metadata.withThumbnailPath("/storage/uuid_thumb.jpg");
 * }</pre>
 *
 * @param id            파일 고유 식별자 (UUID)
 * @param originalName  업로드된 원본 파일명
 * @param type          파일 타입 (IMAGE/VIDEO)
 * @param contentType   MIME Content-Type (예: image/jpeg, video/mp4)
 * @param size          파일 크기 (바이트)
 * @param storagePath   파일 저장소 내 실제 저장 경로
 * @param thumbnailPath 썸네일 저장 경로 (nullable)
 * @param createdAt     파일 생성 시각
 * @param updatedAt     파일 수정 시각
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileType
 * @see UploadResult
 */
public record FileMetadata(
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

  /**
   * Compact Constructor - 유효성 검증을 수행합니다.
   *
   * @throws NullPointerException     필수 필드가 null인 경우
   * @throws IllegalArgumentException size가 음수이거나 originalName이 blank인 경우
   */
  public FileMetadata {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(originalName, "originalName must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
    Objects.requireNonNull(storagePath, "storagePath must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    if (size < 0) {
      throw new IllegalArgumentException("size must be positive");
    }

    if (originalName.isBlank()) {
      throw new IllegalArgumentException("originalName must not be blank");
    }
  }

  /**
   * 썸네일 존재 여부를 확인합니다.
   *
   * @return 썸네일이 존재하면 {@code true}
   */
  public boolean hasThumbnail() {
    return thumbnailPath != null && !thumbnailPath.isBlank();
  }

  /**
   * 비디오 파일인지 확인합니다.
   *
   * @return VIDEO 타입이면 {@code true}
   */
  public boolean isVideo() {
    return type == FileType.VIDEO;
  }

  /**
   * 이미지 파일인지 확인합니다.
   *
   * @return IMAGE 타입이면 {@code true}
   */
  public boolean isImage() {
    return type == FileType.IMAGE;
  }

  /**
   * 파일 확장자를 추출합니다.
   *
   * @return 소문자로 변환된 확장자, 없으면 빈 문자열
   */
  public String getExtension() {
    int lastDot = originalName.lastIndexOf('.');
    if (lastDot == -1 || lastDot == originalName.length() - 1) {
      return "";
    }
    return originalName.substring(lastDot + 1).toLowerCase();
  }

  /**
   * 새 FileMetadata를 생성합니다.
   *
   * <p>ID는 자동으로 UUID가 생성되고, 생성/수정 시각은 현재 시각으로 설정됩니다.
   * 썸네일 경로는 null로 초기화됩니다.</p>
   *
   * @param originalName 원본 파일명
   * @param type         파일 타입
   * @param contentType  MIME 타입
   * @param size         파일 크기 (바이트)
   * @param storagePath  저장 경로
   * @return 새로 생성된 FileMetadata
   */
  public static FileMetadata create(
      String originalName,
      FileType type,
      String contentType,
      long size,
      String storagePath
  ) {
    LocalDateTime now = LocalDateTime.now();
    return new FileMetadata(
        UUID.randomUUID(),
        originalName,
        type,
        contentType,
        size,
        storagePath,
        null,
        now,
        now
    );
  }

  /**
   * 썸네일 경로가 설정된 새 객체를 반환합니다.
   *
   * <p>원본 객체는 변경되지 않습니다 (불변성 보장).</p>
   *
   * @param thumbnailPath 썸네일 저장 경로
   * @return 썸네일 경로가 설정된 새 FileMetadata
   */
  public FileMetadata withThumbnailPath(String thumbnailPath) {
    return new FileMetadata(
        this.id,
        this.originalName,
        this.type,
        this.contentType,
        this.size,
        this.storagePath,
        thumbnailPath,
        this.createdAt,
        LocalDateTime.now()
    );
  }

  /**
   * 수정 시각이 갱신된 새 객체를 반환합니다.
   *
   * @return updatedAt이 현재 시각으로 갱신된 새 FileMetadata
   */
  public FileMetadata touch() {
    return new FileMetadata(
        this.id,
        this.originalName,
        this.type,
        this.contentType,
        this.size,
        this.storagePath,
        this.thumbnailPath,
        this.createdAt,
        LocalDateTime.now()
    );
  }
}