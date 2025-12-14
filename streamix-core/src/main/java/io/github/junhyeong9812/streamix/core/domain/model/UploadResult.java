package io.github.junhyeong9812.streamix.core.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 파일 업로드 결과를 표현하는 불변 레코드입니다.
 *
 * <p>파일 업로드 완료 후 클라이언트에게 반환되는 응답 데이터를 담습니다.
 * {@link FileMetadata}에서 필요한 정보만 추출하여 가볍게 전달합니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // FileMetadata에서 UploadResult 생성
 * FileMetadata metadata = ...;
 * UploadResult result = UploadResult.from(metadata);
 *
 * // REST API 응답으로 사용
 * return ResponseEntity.ok(result);
 * }</pre>
 *
 * @param id                 업로드된 파일의 고유 식별자
 * @param originalName       원본 파일명
 * @param type               파일 타입 (IMAGE/VIDEO)
 * @param contentType        MIME Content-Type
 * @param size               파일 크기 (바이트)
 * @param thumbnailGenerated 썸네일 생성 성공 여부
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase
 */
public record UploadResult(
    UUID id,
    String originalName,
    FileType type,
    String contentType,
    long size,
    boolean thumbnailGenerated
) {

  /**
   * Compact Constructor - 유효성 검증을 수행합니다.
   *
   * @throws NullPointerException 필수 필드가 null인 경우
   */
  public UploadResult {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(originalName, "originalName must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
  }

  /**
   * FileMetadata로부터 UploadResult를 생성합니다.
   *
   * <p>썸네일 생성 여부는 {@link FileMetadata#hasThumbnail()}로 판단합니다.</p>
   *
   * @param metadata 파일 메타데이터
   * @return 업로드 결과
   * @throws NullPointerException metadata가 null인 경우
   */
  public static UploadResult from(FileMetadata metadata) {
    Objects.requireNonNull(metadata, "metadata must not be null");
    return new UploadResult(
        metadata.id(),
        metadata.originalName(),
        metadata.type(),
        metadata.contentType(),
        metadata.size(),
        metadata.hasThumbnail()
    );
  }
}