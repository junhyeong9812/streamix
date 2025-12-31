package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * 파일 메타데이터 조회 서비스 구현체입니다.
 *
 * <p>{@link GetFileMetadataUseCase}를 구현하며, 파일 메타데이터 조회를 처리합니다.</p>
 *
 * <h2>역할</h2>
 * <ul>
 *   <li>단건 메타데이터 조회</li>
 *   <li>목록 조회 (페이징)</li>
 * </ul>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileMetadataPort} - 메타데이터 저장소</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see GetFileMetadataUseCase
 */
public class FileMetadataService implements GetFileMetadataUseCase {

  private static final Logger log = LoggerFactory.getLogger(FileMetadataService.class);

  private final FileMetadataPort metadataRepository;

  /**
   * FileMetadataService를 생성합니다.
   *
   * @param metadataRepository 메타데이터 저장소 포트
   */
  public FileMetadataService(FileMetadataPort metadataRepository) {
    this.metadataRepository = metadataRepository;
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
}