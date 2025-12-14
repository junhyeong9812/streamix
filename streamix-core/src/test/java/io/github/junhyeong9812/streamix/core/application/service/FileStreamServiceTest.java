package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.StreamFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.StreamableFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStreamService 테스트")
class FileStreamServiceTest {

  @Mock
  private FileStoragePort storage;

  @Mock
  private FileMetadataPort metadataRepository;

  private FileStreamService streamService;

  @BeforeEach
  void setUp() {
    streamService = new FileStreamService(storage, metadataRepository);
  }

  @Nested
  @DisplayName("stream 테스트")
  class StreamTest {

    @Test
    @DisplayName("전체 파일을 스트리밍한다")
    void streamFullFile() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId, 1024L);
      InputStream mockStream = new ByteArrayInputStream(new byte[1024]);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      given(storage.load(metadata.storagePath())).willReturn(mockStream);

      StreamFileUseCase.StreamCommand command = StreamFileUseCase.StreamCommand.of(fileId);

      // when
      StreamableFile result = streamService.stream(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.isPartialContent()).isFalse();
      assertThat(result.contentLength()).isEqualTo(1024L);
      assertThat(result.metadata()).isEqualTo(metadata);

      verify(storage).load(metadata.storagePath());
    }

    @Test
    @DisplayName("Range 요청으로 부분 파일을 스트리밍한다")
    void streamPartialFile() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId, 10000L);
      InputStream mockStream = new ByteArrayInputStream(new byte[1024]);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      given(storage.loadPartial(anyString(), anyLong(), anyLong())).willReturn(mockStream);

      StreamFileUseCase.StreamCommand command =
          StreamFileUseCase.StreamCommand.withRange(fileId, "bytes=0-1023");

      // when
      StreamableFile result = streamService.stream(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.isPartialContent()).isTrue();
      assertThat(result.rangeStart()).isEqualTo(0L);
      assertThat(result.rangeEnd()).isEqualTo(1023L);
      assertThat(result.contentLength()).isEqualTo(1024L);

      verify(storage).loadPartial(metadata.storagePath(), 0L, 1023L);
    }

    @Test
    @DisplayName("bytes=1024- 형식의 Range 요청을 처리한다")
    void streamWithOpenEndRange() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId, 5000L);
      InputStream mockStream = new ByteArrayInputStream(new byte[3976]);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      given(storage.loadPartial(anyString(), anyLong(), anyLong())).willReturn(mockStream);

      StreamFileUseCase.StreamCommand command =
          StreamFileUseCase.StreamCommand.withRange(fileId, "bytes=1024-");

      // when
      StreamableFile result = streamService.stream(command);

      // then
      assertThat(result.rangeStart()).isEqualTo(1024L);
      assertThat(result.rangeEnd()).isEqualTo(4999L); // fileSize - 1
    }

    @Test
    @DisplayName("bytes=-500 형식의 Range 요청을 처리한다 (마지막 500바이트)")
    void streamWithSuffixRange() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId, 1000L);
      InputStream mockStream = new ByteArrayInputStream(new byte[500]);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      given(storage.loadPartial(anyString(), anyLong(), anyLong())).willReturn(mockStream);

      StreamFileUseCase.StreamCommand command =
          StreamFileUseCase.StreamCommand.withRange(fileId, "bytes=-500");

      // when
      StreamableFile result = streamService.stream(command);

      // then
      assertThat(result.rangeStart()).isEqualTo(500L); // 1000 - 500
      assertThat(result.rangeEnd()).isEqualTo(999L);
    }

    @Test
    @DisplayName("존재하지 않는 파일은 예외가 발생한다")
    void throwsWhenFileNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      given(metadataRepository.findById(fileId)).willReturn(Optional.empty());

      StreamFileUseCase.StreamCommand command = StreamFileUseCase.StreamCommand.of(fileId);

      // when & then
      assertThatThrownBy(() -> streamService.stream(command))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getThumbnail 테스트")
  class GetThumbnailTest {

    @Test
    @DisplayName("썸네일을 조회한다")
    void getThumbnail() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadataWithThumbnail(fileId);
      InputStream mockStream = new ByteArrayInputStream(new byte[100]);

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
      given(storage.load(metadata.thumbnailPath())).willReturn(mockStream);
      given(storage.getSize(metadata.thumbnailPath())).willReturn(100L);

      // when
      StreamableFile result = streamService.getThumbnail(fileId);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getContentType()).isEqualTo("image/jpeg");
      assertThat(result.contentLength()).isEqualTo(100L);
    }

    @Test
    @DisplayName("썸네일이 없으면 예외가 발생한다")
    void throwsWhenThumbnailNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      FileMetadata metadata = createMetadata(fileId, 1024L); // 썸네일 없음

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));

      // when & then
      assertThatThrownBy(() -> streamService.getThumbnail(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 파일의 썸네일 조회는 예외가 발생한다")
    void throwsWhenFileNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      given(metadataRepository.findById(fileId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> streamService.getThumbnail(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  private FileMetadata createMetadata(UUID id, long size) {
    return new FileMetadata(
        id,
        "test.mp4",
        FileType.VIDEO,
        "video/mp4",
        size,
        "/storage/test.mp4",
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  private FileMetadata createMetadataWithThumbnail(UUID id) {
    return new FileMetadata(
        id,
        "test.mp4",
        FileType.VIDEO,
        "video/mp4",
        1024L,
        "/storage/test.mp4",
        "/storage/test_thumb.jpg",
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}