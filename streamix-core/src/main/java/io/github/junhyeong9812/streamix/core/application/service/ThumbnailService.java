package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.GetThumbnailUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 썸네일 관리 서비스 구현체입니다.
 *
 * <p>{@link GetThumbnailUseCase}를 구현하며, 썸네일 조회와 생성을 담당합니다.
 * 여러 {@link ThumbnailGeneratorPort} 구현체를 관리하고, 파일 타입에 맞는
 * 적절한 생성기를 선택하여 처리합니다.</p>
 *
 * <h2>역할</h2>
 * <ul>
 *   <li>썸네일 조회 (GetThumbnailUseCase)</li>
 *   <li>썸네일 생성 (FileUploadService에서 호출)</li>
 *   <li>다중 ThumbnailGeneratorPort 관리</li>
 * </ul>
 *
 * <h2>생성기 선택 로직</h2>
 * <ol>
 *   <li>{@link ThumbnailGeneratorPort#supports(FileType)}가 true인 생성기 필터링</li>
 *   <li>{@link ThumbnailGeneratorPort#getOrder()}가 가장 낮은 생성기 선택</li>
 * </ol>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 여러 생성기 등록
 * List<ThumbnailGeneratorPort> generators = List.of(
 *     new ImageThumbnailAdapter(),      // order=500, IMAGE 지원
 *     new VideoThumbnailAdapter(),      // order=500, VIDEO 지원
 *     new DefaultThumbnailGenerator()   // order=1000, 모든 타입 지원 (폴백)
 * );
 *
 * ThumbnailService service = new ThumbnailService(generators, storage, metadataPort);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see GetThumbnailUseCase
 * @see ThumbnailGeneratorPort
 */
public class ThumbnailService implements GetThumbnailUseCase {

  private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

  private final List<ThumbnailGeneratorPort> generators;
  private final FileStoragePort storage;
  private final FileMetadataPort metadataRepository;

  /**
   * ThumbnailService를 생성합니다.
   *
   * @param generators         썸네일 생성기 목록
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   */
  public ThumbnailService(
      List<ThumbnailGeneratorPort> generators,
      FileStoragePort storage,
      FileMetadataPort metadataRepository
  ) {
    this.generators = generators != null ? new ArrayList<>(generators) : new ArrayList<>();
    this.storage = storage;
    this.metadataRepository = metadataRepository;

    // 우선순위로 정렬
    this.generators.sort(Comparator.comparingInt(ThumbnailGeneratorPort::getOrder));

    if (log.isDebugEnabled()) {
      log.debug("Registered {} thumbnail generators:", this.generators.size());
      this.generators.forEach(g ->
          log.debug("  - {} (order={})", g.getName(), g.getOrder()));
    }
  }

  /**
   * 생성기 없이 ThumbnailService를 생성합니다.
   *
   * <p>이 생성자는 썸네일 기능이 비활성화된 경우에 사용됩니다.</p>
   *
   * @param storage            파일 저장소 포트
   * @param metadataRepository 메타데이터 저장소 포트
   */
  public ThumbnailService(FileStoragePort storage, FileMetadataPort metadataRepository) {
    this(null, storage, metadataRepository);
  }

  // ==================== GetThumbnailUseCase 구현 ====================

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamableFile getThumbnail(UUID fileId) {
    log.debug("Getting thumbnail for file: {}", fileId);

    FileMetadata metadata = metadataRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));

    if (!metadata.hasThumbnail()) {
      throw new FileNotFoundException("Thumbnail not found for file: " + fileId);
    }

    InputStream thumbnailStream = storage.load(metadata.thumbnailPath());
    long thumbnailSize = storage.getSize(metadata.thumbnailPath());

    // 썸네일용 메타데이터 생성
    FileMetadata thumbnailMetadata = createThumbnailMetadata(metadata, thumbnailSize);

    return StreamableFile.full(thumbnailMetadata, thumbnailStream);
  }

  // ==================== 썸네일 생성 (FileUploadService에서 호출) ====================

  /**
   * 파일 경로에서 썸네일을 생성합니다.
   *
   * @param fileType   파일 타입
   * @param sourcePath 원본 파일 경로
   * @param width      썸네일 너비 (픽셀)
   * @param height     썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws ThumbnailGenerationException 생성기가 없거나 생성 실패 시
   */
  public byte[] generate(FileType fileType, String sourcePath, int width, int height) {
    log.debug("Generating thumbnail: type={}, path={}, size={}x{}", fileType, sourcePath, width, height);

    ThumbnailGeneratorPort generator = findGenerator(fileType)
        .orElseThrow(() -> new ThumbnailGenerationException(
            "No thumbnail generator available for type: " + fileType));

    log.debug("Using generator: {} for type: {}", generator.getName(), fileType);
    return generator.generateFromPath(sourcePath, width, height);
  }

  /**
   * InputStream에서 썸네일을 생성합니다.
   *
   * <p><b>주의:</b> 비디오 파일은 InputStream에서 썸네일 생성을 지원하지 않습니다.</p>
   *
   * @param fileType    파일 타입
   * @param inputStream 원본 파일 스트림
   * @param width       썸네일 너비 (픽셀)
   * @param height      썸네일 높이 (픽셀)
   * @return 썸네일 이미지 데이터 (JPEG 형식)
   * @throws ThumbnailGenerationException 비디오/오디오 타입이거나 생성 실패 시
   */
  public byte[] generateFromStream(FileType fileType, InputStream inputStream, int width, int height) {
    if (fileType == FileType.VIDEO || fileType == FileType.AUDIO) {
      throw new ThumbnailGenerationException(
          fileType + " thumbnail generation from InputStream is not supported. " +
              "Use generate(FileType, String, int, int) with file path instead.");
    }

    ThumbnailGeneratorPort generator = findGenerator(fileType)
        .orElseThrow(() -> new ThumbnailGenerationException(
            "No thumbnail generator available for type: " + fileType));

    log.debug("Generating thumbnail from stream: type={}, generator={}", fileType, generator.getName());
    return generator.generate(inputStream, width, height);
  }

  // ==================== 유틸리티 ====================

  /**
   * 주어진 파일 타입의 썸네일 생성을 지원하는지 확인합니다.
   *
   * @param fileType 파일 타입
   * @return 지원 여부
   */
  public boolean supports(FileType fileType) {
    return findGenerator(fileType).isPresent();
  }

  /**
   * 등록된 모든 생성기 목록을 반환합니다.
   *
   * @return 생성기 목록 (우선순위 순)
   */
  public List<ThumbnailGeneratorPort> getGenerators() {
    return List.copyOf(generators);
  }

  /**
   * 파일 타입에 맞는 생성기를 찾습니다.
   *
   * @param fileType 파일 타입
   * @return 적합한 생성기 (없으면 empty)
   */
  private Optional<ThumbnailGeneratorPort> findGenerator(FileType fileType) {
    return generators.stream()
        .filter(g -> g.supports(fileType))
        .findFirst();  // 이미 우선순위로 정렬되어 있음
  }

  /**
   * 썸네일용 메타데이터를 생성합니다.
   */
  private FileMetadata createThumbnailMetadata(FileMetadata original, long thumbnailSize) {
    return new FileMetadata(
        original.id(),
        original.originalName() + "_thumb.jpg",
        original.type(),
        "image/jpeg",
        thumbnailSize,
        original.thumbnailPath(),
        null,
        original.createdAt(),
        original.updatedAt()
    );
  }
}