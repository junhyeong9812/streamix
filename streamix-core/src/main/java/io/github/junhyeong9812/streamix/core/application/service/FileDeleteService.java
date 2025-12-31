package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 파일 삭제 서비스 구현체입니다.
 *
 * <p>{@link DeleteFileUseCase}를 구현하며, 파일 삭제의 전체 프로세스를 처리합니다.</p>
 *
 * <h2>삭제 프로세스</h2>
 * <ol>
 *   <li>메타데이터 조회 (존재 확인)</li>
 *   <li>저장소에서 실제 파일 삭제</li>
 *   <li>썸네일이 있으면 썸네일 삭제</li>
 *   <li>메타데이터 저장소에서 메타데이터 삭제</li>
 * </ol>
 *
 * <h2>오류 처리</h2>
 * <p>실제 파일/썸네일 삭제가 실패해도 메타데이터는 삭제됩니다.
 * 이는 의도된 동작으로, 파일이 이미 수동으로 삭제된 경우를 처리합니다.</p>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileMetadataPort} - 메타데이터 조회/삭제</li>
 *   <li>{@link FileStoragePort} - 파일 삭제</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.7
 * @see DeleteFileUseCase
 */
public class FileDeleteService implements DeleteFileUseCase {

  private static final Logger log = LoggerFactory.getLogger(FileDeleteService.class);

  private final FileMetadataPort metadataRepository;
  private final FileStoragePort storage;

  /**
   * FileDeleteService를 생성합니다.
   *
   * @param metadataRepository 메타데이터 저장소 포트
   * @param storage            파일 저장소 포트
   */
  public FileDeleteService(FileMetadataPort metadataRepository, FileStoragePort storage) {
    this.metadataRepository = metadataRepository;
    this.storage = storage;
  }

  /**
   * {@inheritDoc}
   *
   * <p>실제 파일과 썸네일을 저장소에서 삭제한 후, 메타데이터를 삭제합니다.
   * 파일 삭제 실패는 로그로 기록되고, 메타데이터는 항상 삭제됩니다.</p>
   */
  @Override
  public void delete(UUID fileId) {
    log.info("Deleting file: {}", fileId);

    // 1. 메타데이터 조회 (존재 확인)
    FileMetadata metadata = metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));

    // 2. 실제 파일 삭제
    deleteFileQuietly(metadata.storagePath(), "file");

    // 3. 썸네일 삭제
    if (metadata.hasThumbnail()) {
      deleteFileQuietly(metadata.thumbnailPath(), "thumbnail");
    }

    // 4. 메타데이터 삭제
    metadataRepository.deleteById(fileId);
    log.info("File deleted successfully: {}", fileId);
  }

  /**
   * 파일을 조용히 삭제합니다 (예외 무시).
   *
   * @param path 삭제할 파일 경로
   * @param type 파일 타입 (로깅용)
   */
  private void deleteFileQuietly(String path, String type) {
    try {
      storage.delete(path);
      log.debug("Deleted {}: {}", type, path);
    } catch (Exception e) {
      log.warn("Failed to delete {}: {}", type, path, e);
    }
  }
}