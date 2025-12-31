package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileMetadataService 테스트")
class FileMetadataServiceTest {

  @Mock
  private FileMetadataPort metadataRepository;

  private FileMetadataService metadataService;

  @BeforeEach
  void setUp() {
    metadataService = new FileMetadataService(metadataRepository);
  }

  @Nested
  @DisplayName("getById 테스트")
  class GetByIdTest {

    @Test
    @DisplayName("ID로 메타데이터를 조회한다")
    void getById() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));

      // when
      FileMetadata result = metadataService.getById(fileId);

      // then
      assertThat(result).isEqualTo(metadata);
      assertThat(result.id()).isEqualTo(fileId);
    }

    @Test
    @DisplayName("존재하지 않는 ID는 예외가 발생한다")
    void throwsWhenNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      given(metadataRepository.findById(fileId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> metadataService.getById(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getAll 테스트")
  class GetAllTest {

    @Test
    @DisplayName("페이징으로 전체 목록을 조회한다")
    void getAll() {
      // given
      List<FileMetadata> metadataList = List.of(
          createMetadata(UUID.randomUUID()),
          createMetadata(UUID.randomUUID()),
          createMetadata(UUID.randomUUID())
      );

      given(metadataRepository.findAll(0, 10)).willReturn(metadataList);

      // when
      List<FileMetadata> result = metadataService.getAll(0, 10);

      // then
      assertThat(result).hasSize(3);
      verify(metadataRepository).findAll(0, 10);
    }

    @Test
    @DisplayName("page가 음수이면 예외가 발생한다")
    void throwsWhenPageIsNegative() {
      assertThatThrownBy(() -> metadataService.getAll(-1, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page");
    }

    @Test
    @DisplayName("size가 0이면 예외가 발생한다")
    void throwsWhenSizeIsZero() {
      assertThatThrownBy(() -> metadataService.getAll(0, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Size");
    }

    @Test
    @DisplayName("size가 100을 초과하면 예외가 발생한다")
    void throwsWhenSizeExceedsLimit() {
      assertThatThrownBy(() -> metadataService.getAll(0, 101))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Size");
    }
  }

  private FileMetadata createMetadata(UUID id) {
    return new FileMetadata(
        id,
        "test.jpg",
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/storage/test.jpg",
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}