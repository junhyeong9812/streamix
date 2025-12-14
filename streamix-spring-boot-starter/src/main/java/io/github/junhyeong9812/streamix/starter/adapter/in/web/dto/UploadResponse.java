package io.github.junhyeong9812.streamix.starter.adapter.in.web.dto;

import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;

import java.util.UUID;

/**
 * 파일 업로드 응답 DTO입니다.
 *
 * <p>파일 업로드 완료 후 클라이언트에게 반환되는 JSON 응답을 정의합니다.
 * Core 모듈의 {@link UploadResult}를 기반으로 스트리밍 URL 등의
 * API 관련 정보를 추가합니다.</p>
 *
 * <h2>응답 필드</h2>
 * <table border="1">
 *   <tr><th>필드</th><th>타입</th><th>설명</th></tr>
 *   <tr><td>id</td><td>UUID</td><td>업로드된 파일의 고유 식별자</td></tr>
 *   <tr><td>originalName</td><td>String</td><td>원본 파일명</td></tr>
 *   <tr><td>type</td><td>FileType</td><td>파일 타입 (IMAGE/VIDEO)</td></tr>
 *   <tr><td>contentType</td><td>String</td><td>MIME 타입</td></tr>
 *   <tr><td>size</td><td>long</td><td>파일 크기 (바이트)</td></tr>
 *   <tr><td>thumbnailGenerated</td><td>boolean</td><td>썸네일 생성 여부</td></tr>
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
 *   "thumbnailGenerated": true,
 *   "streamUrl": "/api/streamix/files/550e8400.../stream",
 *   "thumbnailUrl": "/api/streamix/files/550e8400.../thumbnail"
 * }
 * }</pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @PostMapping("/files")
 * public UploadResponse upload(MultipartFile file) {
 *     UploadResult result = uploadFileUseCase.upload(command);
 *     return UploadResponse.from(result, "/api/streamix");
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see UploadResult
 */
public record UploadResponse(
    /**
     * 업로드된 파일의 고유 식별자.
     */
    UUID id,

    /**
     * 원본 파일명.
     */
    String originalName,

    /**
     * 파일 타입 (IMAGE 또는 VIDEO).
     */
    FileType type,

    /**
     * MIME Content-Type.
     */
    String contentType,

    /**
     * 파일 크기 (바이트 단위).
     */
    long size,

    /**
     * 썸네일 생성 성공 여부.
     */
    boolean thumbnailGenerated,

    /**
     * 파일 스트리밍 API URL.
     *
     * <p>예: /api/streamix/files/{id}/stream</p>
     */
    String streamUrl,

    /**
     * 썸네일 API URL.
     *
     * <p>썸네일이 생성되지 않은 경우 {@code null}입니다.</p>
     */
    String thumbnailUrl
) {
  /**
   * Core의 UploadResult로부터 응답 DTO를 생성합니다.
   *
   * <p>API 기본 경로를 사용하여 스트리밍 및 썸네일 URL을 생성합니다.
   * 썸네일이 생성되지 않은 경우 thumbnailUrl은 null이 됩니다.</p>
   *
   * @param result      업로드 결과 (Core 도메인)
   * @param apiBasePath API 기본 경로 (예: "/api/streamix")
   * @return 응답 DTO
   * @throws NullPointerException result나 apiBasePath가 null인 경우
   */
  public static UploadResponse from(UploadResult result, String apiBasePath) {
    String streamUrl = apiBasePath + "/files/" + result.id() + "/stream";
    String thumbnailUrl = result.thumbnailGenerated()
        ? apiBasePath + "/files/" + result.id() + "/thumbnail"
        : null;

    return new UploadResponse(
        result.id(),
        result.originalName(),
        result.type(),
        result.contentType(),
        result.size(),
        result.thumbnailGenerated(),
        streamUrl,
        thumbnailUrl
    );
  }
}