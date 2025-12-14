package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.InvalidFileTypeException;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.core.domain.model.FileType;
import io.github.junhyeong9812.streamix.core.domain.model.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUploadService 테스트")
class FileUploadServiceTest {

  @Mock
  private FileStoragePort storage;

  @Mock
  private FileMetadataPort metadataRepository;

  @Mock
  private ThumbnailGeneratorPort thumbnailGenerator;

  private FileUploadService uploadService;

  @BeforeEach
  void setUp() {
    uploadService = new FileUploadService(
        storage,
        metadataRepository,
        List.of(thumbnailGenerator),
        true,  // thumbnailEnabled
        320,   // thumbnailWidth
        180    // thumbnailHeight
    );
  }

  @Nested
  @DisplayName("upload 테스트")
  class UploadTest {

    @Test
    @DisplayName("이미지 파일을 업로드한다")
    void uploadImageFile() {
      // given
      byte[] content = "test image content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "test.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/test.jpg");

      given(thumbnailGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailGenerator.generate(any(), anyInt(), anyInt()))
          .willReturn(new byte[100]);
      given(storage.load(anyString()))
          .willReturn(new ByteArrayInputStream(content));

      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.originalName()).isEqualTo("test.jpg");
      assertThat(result.type()).isEqualTo(FileType.IMAGE);
      assertThat(result.contentType()).isEqualTo("image/jpeg");
      assertThat(result.size()).isEqualTo(content.length);

      verify(storage).save(anyString(), any(InputStream.class), eq((long) content.length));
      verify(metadataRepository).save(any(FileMetadata.class));
    }

    @Test
    @DisplayName("비디오 파일을 업로드한다")
    void uploadVideoFile() {
      // given
      byte[] content = "test video content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "video.mp4",
          "video/mp4",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/video.mp4");

      given(thumbnailGenerator.supports(FileType.VIDEO)).willReturn(false);

      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.originalName()).isEqualTo("video.mp4");
      assertThat(result.type()).isEqualTo(FileType.VIDEO);
    }

    @Test
    @DisplayName("지원하지 않는 파일 타입은 예외가 발생한다")
    void throwsForUnsupportedFileType() {
      // given
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "document.pdf",
          "application/pdf",
          content.length,
          inputStream
      );

      // when & then
      assertThatThrownBy(() -> uploadService.upload(command))
          .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("메타데이터가 올바르게 저장된다")
    void savesMetadataCorrectly() {
      // given
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "photo.png",
          "image/png",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/photo.png");
      given(thumbnailGenerator.supports(FileType.IMAGE)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      uploadService.upload(command);

      // then
      ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
      verify(metadataRepository).save(captor.capture());

      FileMetadata saved = captor.getValue();
      assertThat(saved.originalName()).isEqualTo("photo.png");
      assertThat(saved.type()).isEqualTo(FileType.IMAGE);
      assertThat(saved.contentType()).isEqualTo("image/png");
      assertThat(saved.size()).isEqualTo(content.length);
      assertThat(saved.storagePath()).isEqualTo("/storage/photo.png");
    }
  }

  @Nested
  @DisplayName("썸네일 생성 테스트")
  class ThumbnailGenerationTest {

    @Test
    @DisplayName("썸네일이 비활성화되면 썸네일을 생성하지 않는다")
    void doesNotGenerateThumbnailWhenDisabled() {
      // given
      FileUploadService serviceWithoutThumbnail = new FileUploadService(
          storage,
          metadataRepository,
          List.of(thumbnailGenerator),
          false, // thumbnailEnabled = false
          320,
          180
      );

      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "test.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/test.jpg");
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = serviceWithoutThumbnail.upload(command);

      // then
      assertThat(result.thumbnailGenerated()).isFalse();
      verify(thumbnailGenerator, never()).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("썸네일 생성 실패해도 업로드는 성공한다")
    void uploadSucceedsEvenIfThumbnailFails() {
      // given
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "test.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/test.jpg");
      given(storage.load(anyString()))
          .willReturn(new ByteArrayInputStream(content));
      given(thumbnailGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailGenerator.generate(any(), anyInt(), anyInt()))
          .willThrow(new RuntimeException("Thumbnail generation failed"));
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.thumbnailGenerated()).isFalse();
    }
  }
}