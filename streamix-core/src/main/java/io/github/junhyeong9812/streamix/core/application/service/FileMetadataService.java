package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * 파일 메타데이터 관리 서비스 구현체입니다.
 *
 * <p>{@link GetFileMetadataUseCase}와 {@link DeleteFileUseCase}를 구현하며,
 * 파일 메타데이터 조회와 파일 삭제를 처리합니다.</p>
 *
 * <h2>삭제 프로세스</h2>
 * <p>파일 삭제 시 다음 순서로 처리됩니다:</p>
 * <ol>
 *   <li>저장소에서 실제 파일 삭제</li>
 *   <li>썸네일이 있으면 썸네일 삭제</li>
 *   <li>메타데이터 저장소에서 메타데이터 삭제</li>
 * </ol>
 *
 * <h2>오류 처리</h2>
 * <p>실제 파일 삭제가 실패해도 메타데이터는 삭제됩니다.
 * 이는 의도된 동작으로, 파일이 이미 수동으로 삭제된 경우를 처리합니다.</p>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileMetadataPort} - 메타데이터 조회/삭제</li>
 *   <li>{@link FileStoragePort} - 파일 삭제</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see GetFileMetadataUseCase
 * @see DeleteFileUseCase
 */
public class FileMetadataService implements GetFileMetadataUseCase, DeleteFileUseCase {

  private static final Logger log = LoggerFactory.getLogger(FileMetadataService.class);

  private final FileMetadataPort metadataRepository;
  private final FileStoragePort storage;

  /**
   * FileMetadataService를 생성합니다.
   *
   * @param metadataRepository 메타데이터 저장소 포트
   * @param storage            파일 저장소 포트
   */
  public FileMetadataService(FileMetadataPort metadataRepository, FileStoragePort storage) {
    this.metadataRepository = metadataRepository;
    this.storage = storage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileMetadata getById(UUID fileId) {
    log.debug("Getting metadata for file: {}", fileId);

    return metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException page가 음수이거나 size가 1~100 범위를 벗어난 경우
   */
  @Override
  public List<FileMetadata> getAll(int page, int size) {
    log.debug("Getting all metadata: page={}, size={}", page, size);

    if (page < 0) {
      throw new IllegalArgumentException("Page must be >= 0");
    }
    if (size <= 0 || size > 100) {
      throw new IllegalArgumentException("Size must be between 1 and 100");
    }

    return metadataRepository.findAll(page, size);
  }

  /**
   * {@inheritDoc}
   *
   * <p>실제 파일과 썸네일을 저장소에서 삭제한 후, 메타데이터를 삭제합니다.
   * 파일 삭제 실패는 무시되고, 메타데이터는 항상 삭제됩니다.</p>
   */
  @Override
  public void delete(UUID fileId) {
    log.info("Deleting file: {}", fileId);

    FileMetadata metadata = metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));

    // 1. 실제 파일 삭제
    try {
      storage.delete(metadata.storagePath());
      log.debug("Deleted file: {}", metadata.storagePath());
    } catch (Exception e) {
      log.warn("Failed to delete file: {}", metadata.storagePath(), e);
    }

    // 2. 썸네일 삭제
    if (metadata.hasThumbnail()) {
      try {
        storage.delete(metadata.thumbnailPath());
        log.debug("Deleted thumbnail: {}", metadata.thumbnailPath());
      } catch (Exception e) {
        log.warn("Failed to delete thumbnail: {}", metadata.thumbnailPath(), e);
      }
    }

    // 3. 메타데이터 삭제
    metadataRepository.deleteById(fileId);
    log.info("File deleted successfully: {}", fileId);
  }
}