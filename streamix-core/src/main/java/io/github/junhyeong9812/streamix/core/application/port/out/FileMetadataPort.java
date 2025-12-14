package io.github.junhyeong9812.streamix.core.application.port.out;

import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 파일 메타데이터 저장소 포트 인터페이스입니다 (Output Port / SPI).
 *
 * <p>헥사고날 아키텍처의 Output Port로, 파일 메타데이터의 영속성을 담당합니다.
 * Driven Adapter(예: JpaFileMetadataAdapter, InMemoryMetadataAdapter)가 이 인터페이스를 구현합니다.</p>
 *
 * <h2>구현체 예시</h2>
 * <ul>
 *   <li>{@link io.github.junhyeong9812.streamix.core.adapter.out.metadata.InMemoryMetadataAdapter}
 *       - 메모리 기반 (테스트/개발용)</li>
 *   <li>JpaFileMetadataAdapter - JPA/Hibernate (Spring Boot Starter에서 제공)</li>
 *   <li>MongoFileMetadataAdapter - MongoDB (별도 구현 필요)</li>
 * </ul>
 *
 * <h2>커스텀 구현 예시</h2>
 * <pre>{@code
 * @Repository
 * public class JpaFileMetadataAdapter implements FileMetadataPort {
 *     private final FileMetadataRepository repository;
 *
 *     @Override
 *     public FileMetadata save(FileMetadata metadata) {
 *         FileMetadataEntity entity = FileMetadataEntity.from(metadata);
 *         return repository.save(entity).toDomain();
 *     }
 *
 *     // ... 나머지 메서드 구현
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see io.github.junhyeong9812.streamix.core.adapter.out.metadata.InMemoryMetadataAdapter
 */
public interface FileMetadataPort {

  /**
   * 메타데이터를 저장합니다.
   *
   * <p>ID가 이미 존재하면 업데이트, 없으면 신규 생성합니다.</p>
   *
   * @param metadata 저장할 메타데이터
   * @return 저장된 메타데이터
   */
  FileMetadata save(FileMetadata metadata);

  /**
   * ID로 메타데이터를 조회합니다.
   *
   * @param id 파일 ID
   * @return 메타데이터 Optional, 없으면 empty
   */
  Optional<FileMetadata> findById(UUID id);

  /**
   * 전체 메타데이터를 페이징하여 조회합니다.
   *
   * <p>생성일 역순(최신순)으로 정렬됩니다.</p>
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @return 메타데이터 목록
   */
  List<FileMetadata> findAll(int page, int size);

  /**
   * ID로 메타데이터를 삭제합니다.
   *
   * <p>존재하지 않는 ID여도 예외를 발생시키지 않습니다 (멱등성).</p>
   *
   * @param id 삭제할 파일 ID
   */
  void deleteById(UUID id);

  /**
   * ID의 존재 여부를 확인합니다.
   *
   * @param id 확인할 파일 ID
   * @return 존재하면 {@code true}
   */
  boolean existsById(UUID id);

  /**
   * 전체 파일 개수를 조회합니다.
   *
   * @return 파일 개수
   */
  long count();
}