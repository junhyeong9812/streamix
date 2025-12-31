package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.in.UploadFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileSizeExceededException;
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
import java.util.Set;

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
  private ThumbnailService thumbnailService;

  private FileUploadService uploadService;

  @BeforeEach
  void setUp() {
    uploadService = new FileUploadService(
        storage,
        metadataRepository,
        thumbnailService,
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
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailService.generate(eq(FileType.IMAGE), anyString(), anyInt(), anyInt()))
          .willReturn(new byte[100]);
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

      verify(storage, atLeast(1)).save(anyString(), any(InputStream.class), anyLong());
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
      given(thumbnailService.supports(FileType.VIDEO)).willReturn(true);
      given(thumbnailService.generate(eq(FileType.VIDEO), anyString(), anyInt(), anyInt()))
          .willReturn(new byte[100]);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.originalName()).isEqualTo("video.mp4");
      assertThat(result.type()).isEqualTo(FileType.VIDEO);
      assertThat(result.thumbnailGenerated()).isTrue();

      verify(thumbnailService).generate(eq(FileType.VIDEO), anyString(), eq(320), eq(180));
    }

    @Test
    @DisplayName("문서 파일을 업로드한다")
    void uploadDocumentFile() {
      // given
      byte[] content = "test document content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "report.pdf",
          "application/pdf",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/report.pdf");
      given(thumbnailService.supports(FileType.DOCUMENT)).willReturn(true);
      given(thumbnailService.generate(eq(FileType.DOCUMENT), anyString(), anyInt(), anyInt()))
          .willReturn(new byte[100]);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.type()).isEqualTo(FileType.DOCUMENT);
    }

    @Test
    @DisplayName("오디오 파일을 업로드한다")
    void uploadAudioFile() {
      // given
      byte[] content = "test audio content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "music.mp3",
          "audio/mpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/music.mp3");
      given(thumbnailService.supports(FileType.AUDIO)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.type()).isEqualTo(FileType.AUDIO);
      assertThat(result.thumbnailGenerated()).isFalse();
    }

    @Test
    @DisplayName("알 수 없는 확장자는 OTHER 타입으로 업로드된다")
    void uploadUnknownTypeAsOther() {
      // given
      byte[] content = "unknown content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "unknown.xyz",
          "application/octet-stream",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/unknown.xyz");
      given(thumbnailService.supports(FileType.OTHER)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.type()).isEqualTo(FileType.OTHER);
      assertThat(result.thumbnailGenerated()).isFalse();
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
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailService.generate(any(), anyString(), anyInt(), anyInt()))
          .willThrow(new RuntimeException("Skip thumbnail"));
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
  @DisplayName("파일 크기 검증 테스트")
  class FileSizeValidationTest {

    @Test
    @DisplayName("최대 크기를 초과하면 예외가 발생한다")
    void throwsWhenFileSizeExceeded() {
      // given
      FileUploadService serviceWithLimit = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          1024,  // maxFileSize = 1KB
          null   // 모든 타입 허용
      );

      byte[] content = new byte[2048];  // 2KB (초과)
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "large.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      // when & then
      assertThatThrownBy(() -> serviceWithLimit.upload(command))
          .isInstanceOf(FileSizeExceededException.class)
          .satisfies(ex -> {
            FileSizeExceededException fse = (FileSizeExceededException) ex;
            assertThat(fse.getActualSize()).isEqualTo(2048);
            assertThat(fse.getMaxSize()).isEqualTo(1024);
            assertThat(fse.getFileName()).isEqualTo("large.jpg");
          });
    }

    @Test
    @DisplayName("최대 크기 이하면 업로드 성공한다")
    void succeedsWhenFileSizeWithinLimit() {
      // given
      FileUploadService serviceWithLimit = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          2048,  // maxFileSize = 2KB
          null
      );

      byte[] content = new byte[1024];  // 1KB
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "small.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/small.jpg");
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = serviceWithLimit.upload(command);

      // then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("maxFileSize가 0이면 크기 제한이 없다")
    void noLimitWhenMaxFileSizeIsZero() {
      // given
      FileUploadService serviceNoLimit = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          0,  // 제한 없음
          null
      );

      byte[] content = new byte[10_000_000];  // 10MB
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "huge.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/huge.jpg");
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = serviceNoLimit.upload(command);

      // then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("파일 타입 검증 테스트")
  class FileTypeValidationTest {

    @Test
    @DisplayName("허용되지 않은 타입은 예외가 발생한다")
    void throwsWhenTypeNotAllowed() {
      // given
      FileUploadService serviceWithTypeLimit = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          0,
          Set.of(FileType.IMAGE, FileType.VIDEO)  // IMAGE, VIDEO만 허용
      );

      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "report.pdf",
          "application/pdf",
          content.length,
          inputStream
      );

      // when & then
      assertThatThrownBy(() -> serviceWithTypeLimit.upload(command))
          .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("허용된 타입은 업로드 성공한다")
    void succeedsWhenTypeAllowed() {
      // given
      FileUploadService serviceWithTypeLimit = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          0,
          Set.of(FileType.IMAGE, FileType.VIDEO)
      );

      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "photo.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/photo.jpg");
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = serviceWithTypeLimit.upload(command);

      // then
      assertThat(result.type()).isEqualTo(FileType.IMAGE);
    }

    @Test
    @DisplayName("allowedTypes가 비어있으면 모든 타입 허용")
    void allowsAllTypesWhenEmpty() {
      // given - 기본 생성자 사용 (allowedTypes = empty)
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "unknown.xyz",
          "application/octet-stream",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/unknown.xyz");
      given(thumbnailService.supports(FileType.OTHER)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.type()).isEqualTo(FileType.OTHER);
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
          metadataRepository
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
      verifyNoInteractions(thumbnailService);
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
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailService.generate(any(), anyString(), anyInt(), anyInt()))
          .willThrow(new RuntimeException("Thumbnail generation failed"));
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.thumbnailGenerated()).isFalse();
    }

    @Test
    @DisplayName("썸네일 생성 성공 시 썸네일 경로가 저장된다")
    void thumbnailPathSavedOnSuccess() {
      // given
      byte[] content = "test content".getBytes();
      byte[] thumbnailBytes = new byte[50];
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "test.jpg",
          "image/jpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/test.jpg")
          .willReturn("/storage/test_thumb.jpg");
      given(thumbnailService.supports(FileType.IMAGE)).willReturn(true);
      given(thumbnailService.generate(eq(FileType.IMAGE), anyString(), eq(320), eq(180)))
          .willReturn(thumbnailBytes);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.thumbnailGenerated()).isTrue();

      ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
      verify(metadataRepository).save(captor.capture());
      assertThat(captor.getValue().hasThumbnail()).isTrue();
    }

    @Test
    @DisplayName("썸네일 미지원 타입은 썸네일을 생성하지 않는다")
    void doesNotGenerateThumbnailForUnsupportedType() {
      // given
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      UploadFileUseCase.UploadCommand command = new UploadFileUseCase.UploadCommand(
          "music.mp3",
          "audio/mpeg",
          content.length,
          inputStream
      );

      given(storage.save(anyString(), any(InputStream.class), anyLong()))
          .willReturn("/storage/music.mp3");
      given(thumbnailService.supports(FileType.AUDIO)).willReturn(false);
      given(metadataRepository.save(any(FileMetadata.class)))
          .willAnswer(inv -> inv.getArgument(0));

      // when
      UploadResult result = uploadService.upload(command);

      // then
      assertThat(result.thumbnailGenerated()).isFalse();
      verify(thumbnailService, never()).generate(any(), anyString(), anyInt(), anyInt());
    }
  }

  @Nested
  @DisplayName("Getter 테스트")
  class GetterTest {

    @Test
    @DisplayName("maxFileSize를 반환한다")
    void getMaxFileSize() {
      // given
      FileUploadService service = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          100_000_000L,  // 100MB
          null
      );

      // when & then
      assertThat(service.getMaxFileSize()).isEqualTo(100_000_000L);
    }

    @Test
    @DisplayName("allowedTypes를 반환한다")
    void getAllowedTypes() {
      // given
      Set<FileType> allowed = Set.of(FileType.IMAGE, FileType.VIDEO);
      FileUploadService service = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180,
          0,
          allowed
      );

      // when & then
      assertThat(service.getAllowedTypes()).containsExactlyInAnyOrder(FileType.IMAGE, FileType.VIDEO);
    }

    @Test
    @DisplayName("모든 타입 허용 여부를 확인한다")
    void isAllTypesAllowed() {
      // given
      FileUploadService serviceAllTypes = new FileUploadService(storage, metadataRepository);
      FileUploadService serviceLimitedTypes = new FileUploadService(
          storage, metadataRepository, thumbnailService,
          true, 320, 180, 0,
          Set.of(FileType.IMAGE)
      );

      // when & then
      assertThat(serviceAllTypes.isAllTypesAllowed()).isTrue();
      assertThat(serviceLimitedTypes.isAllTypesAllowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("간편 생성자 테스트")
  class SimpleConstructorTest {

    @Test
    @DisplayName("간편 생성자로 썸네일 비활성화 서비스를 생성한다")
    void createServiceWithSimpleConstructor() {
      // given
      FileUploadService simpleService = new FileUploadService(storage, metadataRepository);

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
      UploadResult result = simpleService.upload(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.thumbnailGenerated()).isFalse();
      assertThat(simpleService.getMaxFileSize()).isEqualTo(0);
      assertThat(simpleService.isAllTypesAllowed()).isTrue();
    }
  }
}