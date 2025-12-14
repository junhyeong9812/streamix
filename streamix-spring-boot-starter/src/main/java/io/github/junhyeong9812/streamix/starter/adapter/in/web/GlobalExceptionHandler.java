package io.github.junhyeong9812.streamix.starter.adapter.in.web;

import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException;
import io.github.junhyeong9812.streamix.core.domain.exception.StorageException;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.starter.adapter.in.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Streamix API 전역 예외 처리기입니다.
 *
 * <p>Streamix REST API에서 발생하는 모든 예외를 일관된 형식의
 * {@link ErrorResponse}로 변환하여 클라이언트에게 반환합니다.</p>
 *
 * <h2>처리하는 예외</h2>
 * <table border="1">
 *   <caption>예외 처리 매핑</caption>
 *   <tr><th>예외</th><th>HTTP 상태</th><th>에러 코드</th></tr>
 *   <tr><td>{@link FileNotFoundException}</td><td>404</td><td>FILE_NOT_FOUND</td></tr>
 *   <tr><td>{@link InvalidFileTypeException}</td><td>400</td><td>INVALID_FILE_TYPE</td></tr>
 *   <tr><td>{@link StorageException}</td><td>500</td><td>STORAGE_ERROR</td></tr>
 *   <tr><td>{@link ThumbnailGenerationException}</td><td>500</td><td>THUMBNAIL_ERROR</td></tr>
 *   <tr><td>{@link MaxUploadSizeExceededException}</td><td>413</td><td>FILE_TOO_LARGE</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>400</td><td>INVALID_REQUEST</td></tr>
 *   <tr><td>{@link Exception}</td><td>500</td><td>INTERNAL_ERROR</td></tr>
 * </table>
 *
 * <h2>로깅</h2>
 * <ul>
 *   <li>4xx 에러: WARN 레벨</li>
 *   <li>5xx 에러: ERROR 레벨 (스택 트레이스 포함)</li>
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
 * @see ErrorResponse
 */
@RestControllerAdvice(basePackages = "io.github.junhyeong9812.streamix.starter.adapter.in.web")
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * GlobalExceptionHandler의 기본 생성자입니다.
   */
  public GlobalExceptionHandler() {
    // 기본 생성자
  }

  /**
   * FileNotFoundException 처리 (404 Not Found).
   *
   * <p>요청한 파일이 존재하지 않는 경우 발생합니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 404 에러 응답
   */
  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleFileNotFound(
      FileNotFoundException ex,
      HttpServletRequest request
  ) {
    log.warn("File not found: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.notFound(
        ex.getMessage(),
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  /**
   * InvalidFileTypeException 처리 (400 Bad Request).
   *
   * <p>지원하지 않는 파일 타입을 업로드하려 할 때 발생합니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 400 에러 응답
   */
  @ExceptionHandler(InvalidFileTypeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFileType(
      InvalidFileTypeException ex,
      HttpServletRequest request
  ) {
    log.warn("Invalid file type: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.badRequest(
        "INVALID_FILE_TYPE",
        ex.getMessage(),
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * StorageException 처리 (500 Internal Server Error).
   *
   * <p>파일 저장, 로드, 삭제 중 오류가 발생한 경우입니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 500 에러 응답
   */
  @ExceptionHandler(StorageException.class)
  public ResponseEntity<ErrorResponse> handleStorageError(
      StorageException ex,
      HttpServletRequest request
  ) {
    log.error("Storage error: {}", ex.getMessage(), ex);

    ErrorResponse response = ErrorResponse.internalError(
        "STORAGE_ERROR",
        "An error occurred while accessing file storage",
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * ThumbnailGenerationException 처리 (500 Internal Server Error).
   *
   * <p>썸네일 생성 중 오류가 발생한 경우입니다.
   * 업로드 시에는 이 예외가 내부적으로 처리되어 발생하지 않습니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 500 에러 응답
   */
  @ExceptionHandler(ThumbnailGenerationException.class)
  public ResponseEntity<ErrorResponse> handleThumbnailError(
      ThumbnailGenerationException ex,
      HttpServletRequest request
  ) {
    log.error("Thumbnail generation error: {}", ex.getMessage(), ex);

    ErrorResponse response = ErrorResponse.internalError(
        "THUMBNAIL_ERROR",
        "An error occurred while generating thumbnail",
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * MaxUploadSizeExceededException 처리 (413 Payload Too Large).
   *
   * <p>업로드 파일 크기가 설정된 최대 크기를 초과한 경우입니다.
   * {@code spring.servlet.multipart.max-file-size} 설정과 관련됩니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 413 에러 응답
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex,
      HttpServletRequest request
  ) {
    log.warn("File too large: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.of(
        413,
        "Payload Too Large",
        "FILE_TOO_LARGE",
        "Uploaded file exceeds the maximum allowed size",
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }

  /**
   * IllegalArgumentException 처리 (400 Bad Request).
   *
   * <p>잘못된 파라미터나 요청 값이 전달된 경우입니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 400 에러 응답
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    log.warn("Invalid request: {}", ex.getMessage());

    ErrorResponse response = ErrorResponse.badRequest(
        "INVALID_REQUEST",
        ex.getMessage(),
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * 기타 예외 처리 (500 Internal Server Error).
   *
   * <p>예상치 못한 예외가 발생한 경우 catch-all 핸들러입니다.
   * 상세 정보는 로그에만 기록하고, 클라이언트에게는 일반적인 메시지를 반환합니다.</p>
   *
   * @param ex      발생한 예외
   * @param request HTTP 요청
   * @return 500 에러 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex,
      HttpServletRequest request
  ) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    ErrorResponse response = ErrorResponse.internalError(
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        request.getRequestURI()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}