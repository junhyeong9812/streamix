package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileDeleteService 테스트")
class FileDeleteServiceTest {

  @Mock
  private FileMetadataPort metadataRepository;

  @Mock
  private FileStoragePort storage;

  private FileDeleteService deleteService;

  @BeforeEach
  void setUp() {
    deleteService = new FileDeleteService(metadataRepository, storage);
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("파일을 삭제한다")
    void deleteFile() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));

      // when
      deleteService.delete(fileId);

      // then
      verify(storage).delete(metadata.storagePath());
      verify(metadataRepository).deleteById(fileId);
    }

    @Test
    @DisplayName("썸네일이 있으면 함께 삭제한다")
    void deleteFileWithThumbnail() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadataWithThumbnail(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));

      // when
      deleteService.delete(fileId);

      // then
      verify(storage).delete(metadata.storagePath());
      verify(storage).delete(metadata.thumbnailPath());
      verify(metadataRepository).deleteById(fileId);
    }

    @Test
    @DisplayName("존재하지 않는 파일 삭제는 예외가 발생한다")
    void throwsWhenFileNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      given(metadataRepository.findById(fileId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> deleteService.delete(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("실제 파일 삭제 실패해도 메타데이터는 삭제된다")
    void deletesMetadataEvenIfStorageFails() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      doThrow(new RuntimeException("Storage error")).when(storage).delete(anyString());

      // when - 예외가 발생하지 않음
      deleteService.delete(fileId);

      // then
      verify(metadataRepository).deleteById(fileId);
    }

    @Test
    @DisplayName("썸네일 삭제 실패해도 메타데이터는 삭제된다")
    void deletesMetadataEvenIfThumbnailDeleteFails() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadataWithThumbnail(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      // 파일 삭제는 성공, 썸네일 삭제는 실패
      doNothing().when(storage).delete(metadata.storagePath());
      doThrow(new RuntimeException("Thumbnail delete error"))
          .when(storage).delete(metadata.thumbnailPath());

      // when
      deleteService.delete(fileId);

      // then
      verify(metadataRepository).deleteById(fileId);
    }

    @Test
    @DisplayName("파일과 썸네일 모두 삭제 실패해도 메타데이터는 삭제된다")
    void deletesMetadataEvenIfAllStorageDeletesFail() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadataWithThumbnail(fileId);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      doThrow(new RuntimeException("Storage error")).when(storage).delete(anyString());

      // when
      deleteService.delete(fileId);

      // then
      verify(metadataRepository).deleteById(fileId);
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

  private FileMetadata createMetadataWithThumbnail(UUID id) {
    return new FileMetadata(
        id,
        "test.jpg",
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/storage/test.jpg",
        "/storage/test_thumb.jpg",
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}