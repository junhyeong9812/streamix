package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileSizeExceededException;
import io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;
import io.github.junhyeong9812.streamix.core.domain.service.FileTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 업로드 서비스 구현체입니다.
 *
 * <p>{@link UploadFileUseCase}를 구현하며, 파일 업로드의 전체 프로세스를 처리합니다.</p>
 *
 * <h2>업로드 프로세스</h2>
 * <ol>
 *   <li>파일 타입 감지 (확장자/Content-Type 기반)</li>
 *   <li>파일 크기 검증 (maxFileSize)</li>
 *   <li>파일 타입 검증 (allowedTypes)</li>
 *   <li>고유 파일명 생성 (UUID + 확장자)</li>
 *   <li>파일 저장소에 저장</li>
 *   <li>메타데이터 생성</li>
 *   <li>썸네일 생성 (설정에 따라)</li>
 *   <li>메타데이터 저장</li>
 * </ol>
 *
 * <h2>파일 제한</h2>
 * <ul>
 *   <li><b>maxFileSize</b>: 0 이하면 제한 없음</li>
 *   <li><b>allowedTypes</b>: 빈 Set이면 모든 타입 허용</li>
 * </ul>
 *
 * <h2>썸네일 생성</h2>
 * <p>썸네일 생성은 선택적이며, 실패해도 업로드 자체는 성공합니다.
 * {@link ThumbnailService}를 통해 파일 타입에 맞는 썸네일을 생성합니다.</p>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileStoragePort} - 파일 저장</li>
 *   <li>{@link FileMetadataPort} - 메타데이터 저장</li>
 *   <li>{@link ThumbnailService} - 썸네일 생성</li>
 * </ul>
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
   * 파일 크기 제한 (바이트). 0 이하면 제한 없음.
   *
   * @since 1.0.7
   */
  private final long maxFileSize;

  /**
   * 허용된 파일 타입. 빈 Set이면 모든 타입 허용.
   *
   * @since 1.0.7
   */
  private final Set<FileType> allowedTypes;

  /**
   * FileUploadService를 생성합니다 (모든 옵션).
   *
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   * @param thumbnailService   썸네일 생성 서비스
   * @param thumbnailEnabled   썸네일 생성 활성화 여부
   * @param thumbnailWidth     썸네일 너비 (픽셀)
   * @param thumbnailHeight    썸네일 높이 (픽셀)
   * @param maxFileSize        최대 파일 크기 (바이트), 0 이하면 제한 없음
   * @param allowedTypes       허용된 파일 타입, null/빈 Set이면 모든 타입 허용
   */
  public FileUploadService(
      FileStoragePort storage,
      FileMetadataPort metadataRepository,
      ThumbnailService thumbnailService,
      boolean thumbnailEnabled,
      int thumbnailWidth,
      int thumbnailHeight,
      long maxFileSize,
      Set<FileType> allowedTypes
  ) {
    this.storage = storage;
    this.metadataRepository = metadataRepository;
    this.thumbnailService = thumbnailService;
    this.fileTypeDetector = new FileTypeDetector();
    this.thumbnailEnabled = thumbnailEnabled;
    this.thumbnailWidth = thumbnailWidth;
    this.thumbnailHeight = thumbnailHeight;
    this.maxFileSize = maxFileSize;
    this.allowedTypes = allowedTypes != null ? Set.copyOf(allowedTypes) : Set.of();

    log.info("FileUploadService initialized: maxFileSize={}, allowedTypes={}, thumbnail={}",
        maxFileSize > 0 ? formatSize(maxFileSize) : "unlimited",
        this.allowedTypes.isEmpty() ? "all" : this.allowedTypes,
        thumbnailEnabled);
  }

  /**
   * FileUploadService를 생성합니다 (기본 옵션, 파일 제한 없음).
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
    this(storage, metadataRepository, thumbnailService,
        thumbnailEnabled, thumbnailWidth, thumbnailHeight,
        0, null);
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
    this(storage, metadataRepository, null, false, 0, 0, 0, null);
  }

  /**
   * {@inheritDoc}
   *
   * <p>파일 크기와 타입을 검증하고, 저장소에 저장한 후 메타데이터를 생성합니다.
   * 썸네일 생성이 활성화되어 있으면 썸네일도 생성합니다.</p>
   *
   * @throws FileSizeExceededException 파일 크기가 제한을 초과한 경우
   * @throws InvalidFileTypeException  허용되지 않은 파일 타입인 경우
   */
  @Override
  public UploadResult upload(UploadCommand command) {
    log.info("Uploading file: {}, size: {}", command.originalName(), formatSize(command.size()));

    // 1. 파일 타입 감지
    FileType fileType = fileTypeDetector.detect(command.originalName(), command.contentType());

    // 2. 파일 크기 검증
    validateFileSize(command.originalName(), command.size());

    // 3. 파일 타입 검증
    validateFileType(command.originalName(), fileType);

    // 4. 고유 파일명 생성
    UUID fileId = UUID.randomUUID();
    String extension = fileTypeDetector.extractExtension(command.originalName());
    String storedFileName = extension.isEmpty() ? fileId.toString() : fileId + "." + extension;

    // 5. 파일 저장
    String storagePath = storage.save(storedFileName, command.inputStream(), command.size());
    log.debug("File saved to: {}", storagePath);

    // 6. 메타데이터 생성
    FileMetadata metadata = new FileMetadata(
        fileId,
        command.originalName(),
        fileType,
        command.contentType(),
        command.size(),
        storagePath,
        null,
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now()
    );

    // 7. 썸네일 생성 (옵션)
    if (thumbnailEnabled && thumbnailService != null && thumbnailService.supports(fileType)) {
      metadata = generateThumbnail(metadata, fileType, storagePath, fileId);
    }

    // 8. 메타데이터 저장
    FileMetadata saved = metadataRepository.save(metadata);
    log.info("Upload completed: id={}, name={}, type={}", saved.id(), saved.originalName(), saved.type());

    return UploadResult.from(saved);
  }

  // ==================== 검증 메서드 ====================

  /**
   * 파일 크기를 검증합니다.
   *
   * @param fileName 파일명 (에러 메시지용)
   * @param size     파일 크기 (바이트)
   * @throws FileSizeExceededException 크기 초과 시
   */
  private void validateFileSize(String fileName, long size) {
    if (maxFileSize > 0 && size > maxFileSize) {
      log.warn("File size exceeded: {} ({} > {})", fileName, formatSize(size), formatSize(maxFileSize));
      throw new FileSizeExceededException(fileName, size, maxFileSize);
    }
  }

  /**
   * 파일 타입을 검증합니다.
   *
   * @param fileName 파일명 (에러 메시지용)
   * @param fileType 파일 타입
   * @throws InvalidFileTypeException 허용되지 않은 타입일 경우
   */
  private void validateFileType(String fileName, FileType fileType) {
    if (!isTypeAllowed(fileType)) {
      log.warn("File type not allowed: {} ({})", fileName, fileType);
      throw new InvalidFileTypeException(
          fileTypeDetector.extractExtension(fileName),
          "File type " + fileType + " is not allowed. Allowed types: " + allowedTypes
      );
    }
  }

  /**
   * 파일 타입이 허용되는지 확인합니다.
   *
   * @param fileType 파일 타입
   * @return 허용 여부 (빈 Set이면 모든 타입 허용)
   */
  private boolean isTypeAllowed(FileType fileType) {
    return allowedTypes.isEmpty() || allowedTypes.contains(fileType);
  }

  // ==================== 썸네일 생성 ====================

  /**
   * 썸네일을 생성합니다.
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
      byte[] thumbnailBytes = thumbnailService.generate(fileType, storagePath, thumbnailWidth, thumbnailHeight);

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
      return metadata;
    }
  }

  // ==================== 유틸리티 ====================

  /**
   * 바이트 크기를 포맷합니다.
   */
  private static String formatSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024L * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    } else {
      return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
  }

  // ==================== Getter ====================

  /**
   * 최대 파일 크기를 반환합니다.
   *
   * @return 최대 크기 (바이트), 0 이하면 제한 없음
   * @since 1.0.7
   */
  public long getMaxFileSize() {
    return maxFileSize;
  }

  /**
   * 허용된 파일 타입을 반환합니다.
   *
   * @return 허용된 타입 Set (빈 Set이면 모든 타입 허용)
   * @since 1.0.7
   */
  public Set<FileType> getAllowedTypes() {
    return allowedTypes;
  }

  /**
   * 모든 파일 타입이 허용되는지 확인합니다.
   *
   * @return 모든 타입 허용 시 true
   * @since 1.0.7
   */
  public boolean isAllTypesAllowed() {
    return allowedTypes.isEmpty();
  }
}