package io.github.junhyeong9812.streamix.starter.adapter.in.web.dto;

import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 파일 정보 응답 DTO입니다.
 *
 * <p>파일 상세 조회 또는 목록 조회 시 클라이언트에게 반환되는 JSON 응답을 정의합니다.
 * Core 모듈의 {@link FileMetadata}를 기반으로 스트리밍 URL 등의
 * API 관련 정보를 추가합니다.</p>
 *
 * <h2>응답 필드</h2>
 * <table border="1">
 *   <caption>파일 정보 응답 필드</caption>
 *   <tr><th>필드</th><th>타입</th><th>설명</th></tr>
 *   <tr><td>id</td><td>UUID</td><td>파일 고유 식별자</td></tr>
 *   <tr><td>originalName</td><td>String</td><td>원본 파일명</td></tr>
 *   <tr><td>type</td><td>FileType</td><td>파일 타입 (IMAGE/VIDEO)</td></tr>
 *   <tr><td>contentType</td><td>String</td><td>MIME 타입</td></tr>
 *   <tr><td>size</td><td>long</td><td>파일 크기 (바이트)</td></tr>
 *   <tr><td>hasThumbnail</td><td>boolean</td><td>썸네일 존재 여부</td></tr>
 *   <tr><td>createdAt</td><td>LocalDateTime</td><td>업로드 시각</td></tr>
 *   <tr><td>updatedAt</td><td>LocalDateTime</td><td>수정 시각</td></tr>
 *   <tr><td>streamUrl</td><td>String</td><td>스트리밍 API URL</td></tr>
 *   <tr><td>thumbnailUrl</td><td>String</td><td>썸네일 API URL (null 가능)</td></tr>
 * </table>
 *
 * <h2>JSON 응답 예시</h2>
 * <pre>{@code
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "originalName": "vacation.mp4",
 *   "type": "VIDEO",
 *   "contentType": "video/mp4",
 *   "size": 15728640,
 *   "hasThumbnail": true,
 *   "createdAt": "2024-01-15T10:30:00",
 *   "updatedAt": "2024-01-15T10:30:00",
 *   "streamUrl": "/api/streamix/files/550e8400.../stream",
 *   "thumbnailUrl": "/api/streamix/files/550e8400.../thumbnail"
 * }
 * }</pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @GetMapping("/files/{id}")
 * public FileInfoResponse getFile(@PathVariable UUID id) {
 *     FileMetadata metadata = getFileMetadataUseCase.getById(id);
 *     return FileInfoResponse.from(metadata, "/api/streamix");
 * }
 * }</pre>
 *
 * @param id           파일 고유 식별자
 * @param originalName 원본 파일명
 * @param type         파일 타입 (IMAGE/VIDEO)
 * @param contentType  MIME Content-Type
 * @param size         파일 크기 (바이트)
 * @param hasThumbnail 썸네일 존재 여부
 * @param createdAt    파일 생성(업로드) 시각
 * @param updatedAt    파일 수정 시각
 * @param streamUrl    파일 스트리밍 API URL
 * @param thumbnailUrl 썸네일 API URL (null 가능)
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 */
public record FileInfoResponse(
    UUID id,
    String originalName,
    FileType type,
    String contentType,
    long size,
    boolean hasThumbnail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String streamUrl,
    String thumbnailUrl
) {
  /**
   * Core의 FileMetadata로부터 응답 DTO를 생성합니다.
   *
   * <p>API 기본 경로를 사용하여 스트리밍 및 썸네일 URL을 생성합니다.
   * 썸네일이 없는 경우 thumbnailUrl은 null이 됩니다.</p>
   *
   * @param metadata    파일 메타데이터 (Core 도메인)
   * @param apiBasePath API 기본 경로 (예: "/api/streamix")
   * @return 응답 DTO
   * @throws NullPointerException metadata나 apiBasePath가 null인 경우
   */
  public static FileInfoResponse from(FileMetadata metadata, String apiBasePath) {
    String streamUrl = apiBasePath + "/files/" + metadata.id() + "/stream";
    String thumbnailUrl = metadata.hasThumbnail()
        ? apiBasePath + "/files/" + metadata.id() + "/thumbnail"
        : null;

    return new FileInfoResponse(
        metadata.id(),
        metadata.originalName(),
        metadata.type(),
        metadata.contentType(),
        metadata.size(),
        metadata.hasThumbnail(),
        metadata.createdAt(),
        metadata.updatedAt(),
        streamUrl,
        thumbnailUrl
    );
  }
}