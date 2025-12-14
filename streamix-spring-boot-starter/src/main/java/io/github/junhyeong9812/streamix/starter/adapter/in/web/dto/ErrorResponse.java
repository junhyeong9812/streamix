package io.github.junhyeong9812.streamix.starter.adapter.in.web.dto;

import java.time.LocalDateTime;

/**
 * API 에러 응답 DTO입니다.
 *
 * <p>API 요청 처리 중 발생한 에러를 클라이언트에게 일관된 형식으로 전달합니다.
 * HTTP 상태 코드, 에러 코드, 메시지 등의 정보를 포함합니다.</p>
 *
 * <h2>응답 필드</h2>
 * <table border="1">
 *   <tr><th>필드</th><th>타입</th><th>설명</th></tr>
 *   <tr><td>timestamp</td><td>LocalDateTime</td><td>에러 발생 시각</td></tr>
 *   <tr><td>status</td><td>int</td><td>HTTP 상태 코드</td></tr>
 *   <tr><td>error</td><td>String</td><td>HTTP 상태 텍스트</td></tr>
 *   <tr><td>code</td><td>String</td><td>Streamix 에러 코드</td></tr>
 *   <tr><td>message</td><td>String</td><td>에러 메시지</td></tr>
 *   <tr><td>path</td><td>String</td><td>요청 경로</td></tr>
 * </table>
 *
 * <h2>에러 코드 목록</h2>
 * <ul>
 *   <li>{@code FILE_NOT_FOUND}: 파일을 찾을 수 없음 (404)</li>
 *   <li>{@code INVALID_FILE_TYPE}: 지원하지 않는 파일 타입 (400)</li>
 *   <li>{@code STORAGE_ERROR}: 파일 저장소 오류 (500)</li>
 *   <li>{@code THUMBNAIL_ERROR}: 썸네일 생성 오류 (500)</li>
 *   <li>{@code INVALID_REQUEST}: 잘못된 요청 (400)</li>
 *   <li>{@code INTERNAL_ERROR}: 내부 서버 오류 (500)</li>
 * </ul>
 *
 * <h2>JSON 응답 예시</h2>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "code": "FILE_NOT_FOUND",
 *   "message": "File not found: 550e8400-e29b-41d4-a716-446655440000",
 *   "path": "/api/streamix/files/550e8400-e29b-41d4-a716-446655440000"
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see GlobalExceptionHandler
 */
public record ErrorResponse(
    /**
     * 에러 발생 시각.
     */
    LocalDateTime timestamp,

    /**
     * HTTP 상태 코드.
     *
     * <p>예: 400, 404, 500</p>
     */
    int status,

    /**
     * HTTP 상태 텍스트.
     *
     * <p>예: "Bad Request", "Not Found", "Internal Server Error"</p>
     */
    String error,

    /**
     * Streamix 에러 코드.
     *
     * <p>클라이언트에서 에러 유형을 식별하는 데 사용됩니다.</p>
     */
    String code,

    /**
     * 에러 메시지.
     *
     * <p>사용자에게 표시할 수 있는 설명 메시지입니다.</p>
     */
    String message,

    /**
     * 요청 경로.
     *
     * <p>에러가 발생한 API 엔드포인트 경로입니다.</p>
     */
    String path
) {
  /**
   * 에러 응답을 생성합니다.
   *
   * <p>timestamp는 현재 시각으로 자동 설정됩니다.</p>
   *
   * @param status  HTTP 상태 코드
   * @param error   HTTP 상태 텍스트
   * @param code    Streamix 에러 코드
   * @param message 에러 메시지
   * @param path    요청 경로
   * @return 에러 응답 DTO
   */
  public static ErrorResponse of(int status, String error, String code, String message, String path) {
    return new ErrorResponse(
        LocalDateTime.now(),
        status,
        error,
        code,
        message,
        path
    );
  }

  /**
   * 404 Not Found 에러 응답을 생성합니다.
   *
   * @param message 에러 메시지
   * @param path    요청 경로
   * @return 404 에러 응답
   */
  public static ErrorResponse notFound(String message, String path) {
    return of(404, "Not Found", "FILE_NOT_FOUND", message, path);
  }

  /**
   * 400 Bad Request 에러 응답을 생성합니다.
   *
   * @param code    에러 코드
   * @param message 에러 메시지
   * @param path    요청 경로
   * @return 400 에러 응답
   */
  public static ErrorResponse badRequest(String code, String message, String path) {
    return of(400, "Bad Request", code, message, path);
  }

  /**
   * 500 Internal Server Error 응답을 생성합니다.
   *
   * @param code    에러 코드
   * @param message 에러 메시지
   * @param path    요청 경로
   * @return 500 에러 응답
   */
  public static ErrorResponse internalError(String code, String message, String path) {
    return of(500, "Internal Server Error", code, message, path);
  }
}