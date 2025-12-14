package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;
import io.github.junhyeong9812.streamix.core.domain.service.FileTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 파일 업로드 서비스 구현체입니다.
 *
 * <p>{@link UploadFileUseCase}를 구현하며, 파일 업로드의 전체 프로세스를 처리합니다.</p>
 *
 * <h2>업로드 프로세스</h2>
 * <ol>
 *   <li>파일 타입 감지 (확장자/Content-Type 기반)</li>
 *   <li>고유 파일명 생성 (UUID + 확장자)</li>
 *   <li>파일 저장소에 저장</li>
 *   <li>메타데이터 생성</li>
 *   <li>썸네일 생성 (설정에 따라)</li>
 *   <li>메타데이터 저장</li>
 * </ol>
 *
 * <h2>썸네일 생성</h2>
 * <p>썸네일 생성은 선택적이며, 실패해도 업로드 자체는 성공합니다.
 * {@link ThumbnailService}를 통해 이미지와 비디오 모두에 대해 썸네일을 생성합니다.</p>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileStoragePort} - 파일 저장</li>
 *   <li>{@link FileMetadataPort} - 메타데이터 저장</li>
 *   <li>{@link ThumbnailService} - 썸네일 생성 (Composite)</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 기본 구성
 * FileUploadService service = new FileUploadService(
 *     new LocalFileStorageAdapter("/data"),
 *     new InMemoryMetadataAdapter(),
 *     new ThumbnailService(),
 *     true,   // 썸네일 활성화
 *     320,    // 썸네일 너비
 *     180     // 썸네일 높이
 * );
 *
 * // 업로드
 * UploadResult result = service.upload(command);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see UploadFileUseCase
 * @see FileStoragePort
 * @see ThumbnailService
 */
public class FileUploadService implements UploadFileUseCase {

  private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

  private final FileStoragePort storage;
  private final FileMetadataPort metadataRepository;
  private final ThumbnailService thumbnailService;
  private final FileTypeDetector fileTypeDetector;

  private final boolean thumbnailEnabled;
  private final int thumbnailWidth;
  private final int thumbnailHeight;

  /**
   * FileUploadService를 생성합니다.
   *
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   * @param thumbnailService   썸네일 생성 서비스
   * @param thumbnailEnabled   썸네일 생성 활성화 여부
   * @param thumbnailWidth     썸네일 너비 (픽셀)
   * @param thumbnailHeight    썸네일 높이 (픽셀)
   */
  public FileUploadService(
      FileStoragePort storage,
      FileMetadataPort metadataRepository,
      ThumbnailService thumbnailService,
      boolean thumbnailEnabled,
      int thumbnailWidth,
      int thumbnailHeight
  ) {
    this.storage = storage;
    this.metadataRepository = metadataRepository;
    this.thumbnailService = thumbnailService;
    this.fileTypeDetector = new FileTypeDetector();
    this.thumbnailEnabled = thumbnailEnabled;
    this.thumbnailWidth = thumbnailWidth;
    this.thumbnailHeight = thumbnailHeight;
  }

  /**
   * 썸네일 비활성화 상태로 FileUploadService를 생성합니다.
   *
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   */
  public FileUploadService(
      FileStoragePort storage,
      FileMetadataPort metadataRepository
  ) {
    this(storage, metadataRepository, null, false, 0, 0);
  }

  /**
   * {@inheritDoc}
   *
   * <p>파일 타입을 검증하고, 저장소에 저장한 후 메타데이터를 생성합니다.
   * 썸네일 생성이 활성화되어 있으면 썸네일도 생성합니다.</p>
   */
  @Override
  public UploadResult upload(UploadCommand command) {
    log.info("Uploading file: {}", command.originalName());

    // 1. 파일 타입 감지
    FileType fileType = fileTypeDetector.detect(command.originalName(), command.contentType());

    // 2. 고유 파일명 생성
    UUID fileId = UUID.randomUUID();
    String extension = fileTypeDetector.extractExtension(command.originalName());
    String storedFileName = fileId + "." + extension;

    // 3. 파일 저장
    String storagePath = storage.save(storedFileName, command.inputStream(), command.size());
    log.debug("File saved to: {}", storagePath);

    // 4. 메타데이터 생성
    FileMetadata metadata = FileMetadata.create(
        command.originalName(),
        fileType,
        command.contentType(),
        command.size(),
        storagePath
    );

    // ID를 fileId로 설정하기 위해 새로 생성
    metadata = new FileMetadata(
        fileId,
        metadata.originalName(),
        metadata.type(),
        metadata.contentType(),
        metadata.size(),
        metadata.storagePath(),
        null,
        metadata.createdAt(),
        metadata.updatedAt()
    );

    // 5. 썸네일 생성 (옵션)
    if (thumbnailEnabled && thumbnailService != null) {
      metadata = generateThumbnail(metadata, fileType, storagePath, fileId);
    }

    // 6. 메타데이터 저장
    FileMetadata saved = metadataRepository.save(metadata);
    log.info("Upload completed: id={}, name={}", saved.id(), saved.originalName());

    return UploadResult.from(saved);
  }

  /**
   * 썸네일을 생성합니다.
   *
   * <p>ThumbnailService를 사용하여 파일 타입에 맞는 썸네일을 생성합니다.
   * 실패해도 예외를 던지지 않고 원본 메타데이터를 반환합니다.</p>
   *
   * @param metadata    파일 메타데이터
   * @param fileType    파일 타입
   * @param storagePath 저장 경로
   * @param fileId      파일 ID
   * @return 썸네일 경로가 추가된 메타데이터 (실패시 원본)
   */
  private FileMetadata generateThumbnail(
      FileMetadata metadata,
      FileType fileType,
      String storagePath,
      UUID fileId
  ) {
    try {
      // ThumbnailService를 통해 썸네일 생성
      byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);

      // 썸네일 저장
      String thumbnailFileName = fileId + "_thumb.jpg";
      String thumbnailPath = storage.save(
          thumbnailFileName,
          new ByteArrayInputStream(thumbnailBytes),
          thumbnailBytes.length
      );

      log.debug("Thumbnail generated: {}", thumbnailPath);
      return metadata.withThumbnailPath(thumbnailPath);

    } catch (Exception e) {
      log.warn("Failed to generate thumbnail for file: {}", fileId, e);
      // 썸네일 생성 실패해도 업로드는 성공 처리
      return metadata;
    }
  }
}