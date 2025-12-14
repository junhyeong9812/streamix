package io.github.junhyeong9812.streamix.starter.adapter.out.persistence;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA 기반 파일 메타데이터 어댑터입니다.
 *
 * <p>Core 모듈의 {@link FileMetadataPort} Output Port를 구현하는 Driven Adapter입니다.
 * Spring Data JPA를 사용하여 파일 메타데이터의 영속성을 담당합니다.</p>
 *
 * <h2>헥사고날 아키텍처</h2>
 * <pre>
 * [Core - Domain]
 *       ↓
 * [Core - Application Service]
 *       ↓
 * [Core - Output Port (FileMetadataPort)]
 *       ↓ implements
 * [Starter - JpaFileMetadataAdapter] ← 이 클래스
 *       ↓ uses
 * [Starter - FileMetadataJpaRepository]
 *       ↓
 * [Database]
 * </pre>
 *
 * <h2>도메인 변환</h2>
 * <p>Core의 도메인 모델({@link FileMetadata})과 JPA 엔티티({@link FileMetadataEntity})
 * 사이의 변환은 엔티티 클래스의 정적 팩토리 메서드를 통해 수행됩니다:</p>
 * <ul>
 *   <li>저장 시: {@code FileMetadataEntity.from(domain)}</li>
 *   <li>조회 시: {@code entity.toDomain()}</li>
 * </ul>
 *
 * <h2>트랜잭션</h2>
 * <p>이 어댑터는 트랜잭션을 직접 관리하지 않습니다.
 * 상위 계층(Service)에서 {@code @Transactional}을 통해 트랜잭션을 제어해야 합니다.</p>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // Spring Bean으로 자동 주입
 * @Autowired
 * private FileMetadataPort fileMetadataPort;
 *
 * // 저장
 * FileMetadata saved = fileMetadataPort.save(metadata);
 *
 * // 조회
 * Optional<FileMetadata> found = fileMetadataPort.findById(fileId);
 *
 * // 페이징 조회
 * List<FileMetadata> list = fileMetadataPort.findAll(0, 20);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadataPort
 * @see FileMetadataEntity
 * @see FileMetadataJpaRepository
 */
public class JpaFileMetadataAdapter implements FileMetadataPort {

  private static final Logger log = LoggerFactory.getLogger(JpaFileMetadataAdapter.class);

  private final FileMetadataJpaRepository repository;

  /**
   * JpaFileMetadataAdapter를 생성합니다.
   *
   * @param repository JPA 리포지토리
   */
  public JpaFileMetadataAdapter(FileMetadataJpaRepository repository) {
    this.repository = repository;
  }

  /**
   * {@inheritDoc}
   *
   * <p>도메인 모델을 JPA 엔티티로 변환하여 저장하고,
   * 저장된 엔티티를 다시 도메인 모델로 변환하여 반환합니다.</p>
   *
   * @param metadata 저장할 메타데이터
   * @return 저장된 메타데이터 (ID 포함)
   */
  @Override
  public FileMetadata save(FileMetadata metadata) {
    log.debug("Saving file metadata: id={}", metadata.id());

    FileMetadataEntity entity = FileMetadataEntity.from(metadata);
    FileMetadataEntity saved = repository.save(entity);

    log.debug("File metadata saved: id={}", saved.getId());
    return saved.toDomain();
  }

  /**
   * {@inheritDoc}
   *
   * <p>UUID로 엔티티를 조회하고, 존재하면 도메인 모델로 변환하여 반환합니다.</p>
   *
   * @param id 파일 ID
   * @return 메타데이터 Optional, 없으면 empty
   */
  @Override
  public Optional<FileMetadata> findById(UUID id) {
    log.debug("Finding file metadata by id: {}", id);

    return repository.findById(id)
        .map(FileMetadataEntity::toDomain);
  }

  /**
   * {@inheritDoc}
   *
   * <p>생성일 역순(최신순)으로 정렬하여 페이징 조회합니다.
   * Spring Data의 PageRequest를 사용하여 페이징을 구현합니다.</p>
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @return 메타데이터 목록
   */
  @Override
  public List<FileMetadata> findAll(int page, int size) {
    log.debug("Finding all file metadata: page={}, size={}", page, size);

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    return repository.findAll(pageRequest)
        .getContent()
        .stream()
        .map(FileMetadataEntity::toDomain)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * <p>ID에 해당하는 엔티티를 삭제합니다.
   * 존재하지 않는 ID여도 예외를 발생시키지 않습니다 (멱등성 보장).</p>
   *
   * @param id 삭제할 파일 ID
   */
  @Override
  public void deleteById(UUID id) {
    log.debug("Deleting file metadata: id={}", id);

    repository.deleteById(id);

    log.debug("File metadata deleted: id={}", id);
  }

  /**
   * {@inheritDoc}
   *
   * @param id 확인할 파일 ID
   * @return 존재하면 {@code true}
   */
  @Override
  public boolean existsById(UUID id) {
    return repository.existsById(id);
  }

  /**
   * {@inheritDoc}
   *
   * @return 전체 파일 개수
   */
  @Override
  public long count() {
    return repository.count();
  }
}