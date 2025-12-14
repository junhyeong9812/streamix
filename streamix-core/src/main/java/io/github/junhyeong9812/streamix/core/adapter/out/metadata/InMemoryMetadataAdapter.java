package io.github.junhyeong9812.streamix.core.adapter.out.metadata;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 메모리 기반 메타데이터 저장소 어댑터입니다.
 *
 * <p>{@link FileMetadataPort}를 구현하며, 테스트나 개발 환경에서
 * 데이터베이스 없이 사용할 수 있습니다.</p>
 *
 * <h2>특징</h2>
 * <ul>
 *   <li>애플리케이션 재시작 시 데이터 소실</li>
 *   <li>{@link ConcurrentHashMap}을 사용한 스레드 안전성</li>
 *   <li>생성일 역순 정렬 지원</li>
 *   <li>테스트를 위한 {@link #clear()} 메서드 제공</li>
 * </ul>
 *
 * <h2>사용 시나리오</h2>
 * <ul>
 *   <li>단위 테스트 / 통합 테스트</li>
 *   <li>개발 환경 (DB 설정 없이 빠른 시작)</li>
 *   <li>데모 / 프로토타입</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 직접 사용
 * FileMetadataPort metadataPort = new InMemoryMetadataAdapter();
 *
 * // Spring Bean 등록 (개발 환경)
 * @Bean
 * @Profile("dev")
 * public FileMetadataPort fileMetadataPort() {
 *     return new InMemoryMetadataAdapter();
 * }
 * }</pre>
 *
 * <h2>주의사항</h2>
 * <p>프로덕션 환경에서는 JPA 기반 어댑터 등 영속성이 보장되는
 * 구현체를 사용해야 합니다.</p>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadataPort
 */
public class InMemoryMetadataAdapter implements FileMetadataPort {

  /**
   * 메모리 저장소. ConcurrentHashMap으로 스레드 안전성을 보장합니다.
   */
  private final Map<UUID, FileMetadata> store = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   *
   * <p>동일한 ID가 이미 존재하면 덮어씁니다.</p>
   */
  @Override
  public FileMetadata save(FileMetadata metadata) {
    store.put(metadata.id(), metadata);
    return metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<FileMetadata> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  /**
   * {@inheritDoc}
   *
   * <p>생성일 역순(최신순)으로 정렬됩니다.</p>
   */
  @Override
  public List<FileMetadata> findAll(int page, int size) {
    return store.values().stream()
        .sorted(Comparator.comparing(FileMetadata::createdAt).reversed())
        .skip((long) page * size)
        .limit(size)
        .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   *
   * <p>존재하지 않는 ID여도 예외를 발생시키지 않습니다.</p>
   */
  @Override
  public void deleteById(UUID id) {
    store.remove(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean existsById(UUID id) {
    return store.containsKey(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long count() {
    return store.size();
  }

  /**
   * 저장소를 초기화합니다.
   *
   * <p>테스트에서 각 테스트 케이스 간 격리를 위해 사용합니다.</p>
   *
   * <pre>{@code
   * @BeforeEach
   * void setUp() {
   *     inMemoryAdapter.clear();
   * }
   * }</pre>
   */
  public void clear() {
    store.clear();
  }
}