package io.github.junhyeong9812.streamix.core.application.port.in;

import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;

import java.io.InputStream;

/**
 * 파일 업로드 유스케이스 인터페이스입니다.
 *
 * <p>헥사고날 아키텍처의 Input Port로, 파일 업로드 기능을 정의합니다.
 * Driving Adapter(예: REST Controller)가 이 인터페이스를 통해
 * 애플리케이션 로직을 호출합니다.</p>
 *
 * <h2>기능</h2>
 * <ul>
 *   <li>파일 저장소에 파일 저장</li>
 *   <li>파일 타입 검증 (이미지/비디오만 허용)</li>
 *   <li>썸네일 자동 생성 (설정에 따라)</li>
 *   <li>메타데이터 저장</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @RestController
 * public class FileController {
 *     private final UploadFileUseCase uploadFileUseCase;
 *
 *     @PostMapping("/files")
 *     public UploadResult upload(@RequestParam MultipartFile file) {
 *         UploadCommand command = new UploadCommand(
 *             file.getOriginalFilename(),
 *             file.getContentType(),
 *             file.getSize(),
 *             file.getInputStream()
 *         );
 *         return uploadFileUseCase.upload(command);
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see UploadResult
 * @see io.github.junhyeong9812.streamix.core.application.service.FileUploadService
 */
public interface UploadFileUseCase {

  /**
   * 파일을 업로드합니다.
   *
   * <p>파일 타입을 검증하고, 저장소에 파일을 저장한 후,
   * 썸네일을 생성하고 메타데이터를 저장합니다.</p>
   *
   * @param command 업로드 명령 (파일명, Content-Type, 크기, 데이터 스트림)
   * @return 업로드 결과 (파일 ID, 메타정보, 썸네일 생성 여부)
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException
   *         지원하지 않는 파일 타입인 경우
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         파일 저장 중 오류 발생 시
   */
  UploadResult upload(UploadCommand command);

  /**
   * 파일 업로드 명령을 담는 불변 레코드입니다.
   *
   * <p>CQRS 패턴의 Command 객체로, 업로드에 필요한 모든 정보를 캡슐화합니다.</p>
   *
   * @param originalName 원본 파일명 (확장자 포함)
   * @param contentType  MIME Content-Type (예: "image/jpeg")
   * @param size         파일 크기 (바이트)
   * @param inputStream  파일 데이터 스트림
   */
  record UploadCommand(
      String originalName,
      String contentType,
      long size,
      InputStream inputStream
  ) {
    /**
     * Compact Constructor - 유효성 검증을 수행합니다.
     *
     * @throws IllegalArgumentException 필수 값이 없거나 잘못된 경우
     */
    public UploadCommand {
      if (originalName == null || originalName.isBlank()) {
        throw new IllegalArgumentException("originalName must not be blank");
      }
      if (contentType == null || contentType.isBlank()) {
        throw new IllegalArgumentException("contentType must not be blank");
      }
      if (size < 0) {
        throw new IllegalArgumentException("size must be positive");
      }
      if (inputStream == null) {
        throw new IllegalArgumentException("inputStream must not be null");
      }
    }
  }
}