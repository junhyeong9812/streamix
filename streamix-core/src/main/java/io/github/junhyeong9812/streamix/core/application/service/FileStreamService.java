package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.StreamFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.UUID;

/**
 * 파일 스트리밍 서비스 구현체입니다.
 *
 * <p>{@link StreamFileUseCase}를 구현하며, HTTP Range 요청을 지원하는 파일 스트리밍을 처리합니다.</p>
 *
 * <h2>Range 요청 처리</h2>
 * <p>HTTP Range 헤더를 파싱하여 부분 콘텐츠를 반환합니다:</p>
 * <ul>
 *   <li>{@code bytes=0-1023} - 처음 1024바이트</li>
 *   <li>{@code bytes=1024-} - 1024바이트부터 끝까지</li>
 *   <li>{@code bytes=-500} - 마지막 500바이트</li>
 * </ul>
 *
 * <h2>의존성</h2>
 * <ul>
 *   <li>{@link FileStoragePort} - 파일 로드</li>
 *   <li>{@link FileMetadataPort} - 메타데이터 조회</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamFileUseCase
 * @see StreamableFile
 */
public class FileStreamService implements StreamFileUseCase {

  private static final Logger log = LoggerFactory.getLogger(FileStreamService.class);

  private final FileStoragePort storage;
  private final FileMetadataPort metadataRepository;

  /**
   * FileStreamService를 생성합니다.
   *
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   */
  public FileStreamService(FileStoragePort storage, FileMetadataPort metadataRepository) {
    this.storage = storage;
    this.metadataRepository = metadataRepository;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Range 헤더가 있으면 해당 범위만, 없으면 전체 파일을 스트리밍합니다.</p>
   */
  @Override
  public StreamableFile stream(StreamCommand command) {
    log.debug("Streaming file: {}, range: {}", command.fileId(), command.rangeHeader());

    FileMetadata metadata = findMetadataOrThrow(command.fileId());

    if (command.hasRange()) {
      return streamPartial(metadata, command.rangeHeader());
    }

    return streamFull(metadata);
  }

  /**
   * 전체 파일을 스트리밍합니다.
   *
   * @param metadata 파일 메타데이터
   * @return 전체 파일의 StreamableFile
   */
  private StreamableFile streamFull(FileMetadata metadata) {
    InputStream inputStream = storage.load(metadata.storagePath());
    return StreamableFile.full(metadata, inputStream);
  }

  /**
   * 부분 파일을 스트리밍합니다 (Range 요청).
   *
   * @param metadata    파일 메타데이터
   * @param rangeHeader HTTP Range 헤더 값
   * @return 부분 콘텐츠의 StreamableFile
   */
  private StreamableFile streamPartial(FileMetadata metadata, String rangeHeader) {
    Range range = parseRange(rangeHeader, metadata.size());

    InputStream inputStream = storage.loadPartial(
        metadata.storagePath(),
        range.start,
        range.end
    );

    return StreamableFile.partial(metadata, inputStream, range.start, range.end);
  }

  /**
   * Range 헤더를 파싱합니다.
   *
   * <p>지원 형식:</p>
   * <ul>
   *   <li>{@code bytes=start-end} - start부터 end까지</li>
   *   <li>{@code bytes=start-} - start부터 끝까지</li>
   *   <li>{@code bytes=-suffix} - 마지막 suffix 바이트</li>
   * </ul>
   *
   * @param rangeHeader Range 헤더 값
   * @param fileSize    전체 파일 크기
   * @return 파싱된 Range
   * @throws IllegalArgumentException 잘못된 Range 형식
   */
  private Range parseRange(String rangeHeader, long fileSize) {
    if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
      return new Range(0, fileSize - 1);
    }

    String rangeValue = rangeHeader.substring(6).trim();
    String[] parts = rangeValue.split("-");

    long start;
    long end;

    if (rangeValue.startsWith("-")) {
      // bytes=-500 (마지막 500바이트)
      long suffix = Long.parseLong(parts[1]);
      start = Math.max(0, fileSize - suffix);
      end = fileSize - 1;
    } else if (parts.length == 1 || parts[1].isEmpty()) {
      // bytes=1024- (1024부터 끝까지)
      start = Long.parseLong(parts[0]);
      end = fileSize - 1;
    } else {
      // bytes=0-1023
      start = Long.parseLong(parts[0]);
      end = Long.parseLong(parts[1]);
    }

    // 범위 검증
    start = Math.max(0, start);
    end = Math.min(end, fileSize - 1);

    if (start > end) {
      throw new IllegalArgumentException("Invalid range: " + rangeHeader);
    }

    return new Range(start, end);
  }

  /**
   * 메타데이터를 조회하거나 예외를 던집니다.
   *
   * @param fileId 파일 ID
   * @return 파일 메타데이터
   * @throws FileNotFoundException 파일이 존재하지 않는 경우
   */
  private FileMetadata findMetadataOrThrow(UUID fileId) {
    return metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));
  }

  /**
   * Range 정보를 담는 내부 레코드.
   *
   * @param start 시작 바이트 (inclusive)
   * @param end   종료 바이트 (inclusive)
   */
  private record Range(long start, long end) {}
}