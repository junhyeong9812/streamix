package io.github.junhyeong9812.streamix.core.adapter.out.metadata;

import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryMetadataAdapter 테스트")
class InMemoryMetadataAdapterTest {

  private InMemoryMetadataAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryMetadataAdapter();
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("메타데이터를 저장한다")
    void saveMetadata() {
      // given
      FileMetadata metadata = createMetadata(UUID.randomUUID());

      // when
      FileMetadata saved = adapter.save(metadata);

      // then
      assertThat(saved).isEqualTo(metadata);
      assertThat(adapter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 ID로 저장하면 덮어쓴다")
    void overwritesWithSameId() {
      // given
      UUID id = UUID.randomUUID();
      FileMetadata original = createMetadata(id, "original.jpg");
      FileMetadata updated = createMetadata(id, "updated.jpg");

      // when
      adapter.save(original);
      adapter.save(updated);

      // then
      assertThat(adapter.count()).isEqualTo(1);
      assertThat(adapter.findById(id).get().originalName()).isEqualTo("updated.jpg");
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("ID로 메타데이터를 조회한다")
    void findById() {
      // given
      UUID id = UUID.randomUUID();
      FileMetadata metadata = createMetadata(id);
      adapter.save(metadata);

      // when
      Optional<FileMetadata> result = adapter.findById(id);

      // then
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("존재하지 않는 ID는 빈 Optional을 반환한다")
    void returnsEmptyWhenNotFound() {
      // when
      Optional<FileMetadata> result = adapter.findById(UUID.randomUUID());

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findAll 테스트")
  class FindAllTest {

    @Test
    @DisplayName("페이징으로 목록을 조회한다")
    void findAllWithPaging() {
      // given
      for (int i = 0; i < 25; i++) {
        adapter.save(createMetadata(UUID.randomUUID(), "file" + i + ".jpg"));
      }

      // when
      List<FileMetadata> page0 = adapter.findAll(0, 10);
      List<FileMetadata> page1 = adapter.findAll(1, 10);
      List<FileMetadata> page2 = adapter.findAll(2, 10);

      // then
      assertThat(page0).hasSize(10);
      assertThat(page1).hasSize(10);
      assertThat(page2).hasSize(5);
    }

    @Test
    @DisplayName("빈 저장소에서는 빈 목록을 반환한다")
    void returnsEmptyListWhenEmpty() {
      // when
      List<FileMetadata> result = adapter.findAll(0, 10);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("생성일 역순으로 정렬된다")
    void sortsByCreatedAtDescending() throws InterruptedException {
      // given
      FileMetadata first = createMetadataWithTime(UUID.randomUUID(), LocalDateTime.now().minusHours(2));
      FileMetadata second = createMetadataWithTime(UUID.randomUUID(), LocalDateTime.now().minusHours(1));
      FileMetadata third = createMetadataWithTime(UUID.randomUUID(), LocalDateTime.now());

      adapter.save(first);
      adapter.save(second);
      adapter.save(third);

      // when
      List<FileMetadata> result = adapter.findAll(0, 10);

      // then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).createdAt()).isAfter(result.get(1).createdAt());
      assertThat(result.get(1).createdAt()).isAfter(result.get(2).createdAt());
    }
  }

  @Nested
  @DisplayName("deleteById 테스트")
  class DeleteByIdTest {

    @Test
    @DisplayName("ID로 메타데이터를 삭제한다")
    void deleteById() {
      // given
      UUID id = UUID.randomUUID();
      adapter.save(createMetadata(id));
      assertThat(adapter.existsById(id)).isTrue();

      // when
      adapter.deleteById(id);

      // then
      assertThat(adapter.existsById(id)).isFalse();
      assertThat(adapter.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 ID 삭제는 예외가 발생하지 않는다")
    void doesNotThrowWhenNotExists() {
      // when & then - no exception
      adapter.deleteById(UUID.randomUUID());
    }
  }

  @Nested
  @DisplayName("existsById 테스트")
  class ExistsByIdTest {

    @Test
    @DisplayName("존재하면 true를 반환한다")
    void returnsTrueWhenExists() {
      // given
      UUID id = UUID.randomUUID();
      adapter.save(createMetadata(id));

      // when & then
      assertThat(adapter.existsById(id)).isTrue();
    }

    @Test
    @DisplayName("존재하지 않으면 false를 반환한다")
    void returnsFalseWhenNotExists() {
      assertThat(adapter.existsById(UUID.randomUUID())).isFalse();
    }
  }

  @Nested
  @DisplayName("count 테스트")
  class CountTest {

    @Test
    @DisplayName("저장된 개수를 반환한다")
    void returnsCount() {
      // given
      adapter.save(createMetadata(UUID.randomUUID()));
      adapter.save(createMetadata(UUID.randomUUID()));
      adapter.save(createMetadata(UUID.randomUUID()));

      // when & then
      assertThat(adapter.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("빈 저장소는 0을 반환한다")
    void returnsZeroWhenEmpty() {
      assertThat(adapter.count()).isZero();
    }
  }

  @Nested
  @DisplayName("clear 테스트")
  class ClearTest {

    @Test
    @DisplayName("저장소를 초기화한다")
    void clearsStorage() {
      // given
      adapter.save(createMetadata(UUID.randomUUID()));
      adapter.save(createMetadata(UUID.randomUUID()));
      assertThat(adapter.count()).isEqualTo(2);

      // when
      adapter.clear();

      // then
      assertThat(adapter.count()).isZero();
    }
  }

  private FileMetadata createMetadata(UUID id) {
    return createMetadata(id, "test.jpg");
  }

  private FileMetadata createMetadata(UUID id, String name) {
    return new FileMetadata(
        id,
        name,
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/storage/" + name,
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  private FileMetadata createMetadataWithTime(UUID id, LocalDateTime createdAt) {
    return new FileMetadata(
        id,
        "test.jpg",
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/storage/test.jpg",
        null,
        createdAt,
        createdAt
    );
  }
}