package io.github.junhyeong9812.streamix.core.application.service;

import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.application.port.out.ThumbnailGeneratorPort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.exception.ThumbnailGenerationException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ThumbnailService 테스트")
class ThumbnailServiceTest {

  @Mock
  private FileStoragePort storage;

  @Mock
  private FileMetadataPort metadataRepository;

  @Mock
  private ThumbnailGeneratorPort imageGenerator;

  @Mock
  private ThumbnailGeneratorPort videoGenerator;

  @Mock
  private ThumbnailGeneratorPort documentGenerator;

  @BeforeEach
  void setUpMocks() {
    // 모든 mock에 기본 order 설정 (생성자에서 정렬 시 필요)
    lenient().when(imageGenerator.getOrder()).thenReturn(500);
    lenient().when(imageGenerator.getName()).thenReturn("ImageGenerator");
    lenient().when(videoGenerator.getOrder()).thenReturn(500);
    lenient().when(videoGenerator.getName()).thenReturn("VideoGenerator");
    lenient().when(documentGenerator.getOrder()).thenReturn(500);
    lenient().when(documentGenerator.getName()).thenReturn("DocumentGenerator");
  }

  @Nested
  @DisplayName("GetThumbnailUseCase 테스트")
  class GetThumbnailTest {

    private ThumbnailService thumbnailService;

    @BeforeEach
    void setUp() {
      thumbnailService = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );
    }

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
      StreamableFile result = thumbnailService.getThumbnail(fileId);

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
      FileMetadata metadata = createMetadata(fileId); // 썸네일 없음

      given(metadataRepository.findById(fileId)).willReturn(Optional.of(metadata));

      // when & then
      assertThatThrownBy(() -> thumbnailService.getThumbnail(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 파일의 썸네일 조회는 예외가 발생한다")
    void throwsWhenFileNotFound() {
      // given
      UUID fileId = UUID.randomUUID();
      given(metadataRepository.findById(fileId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> thumbnailService.getThumbnail(fileId))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("generate 테스트")
  class GenerateTest {

    @Test
    @DisplayName("이미지 생성기를 사용하여 썸네일을 생성한다")
    void generateImageThumbnail() {
      // given
      given(imageGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(imageGenerator.generateFromPath(anyString(), anyInt(), anyInt()))
          .willReturn(new byte[100]);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      // when
      byte[] result = service.generate(FileType.IMAGE, "/path/to/image.jpg", 320, 180);

      // then
      assertThat(result).hasSize(100);
      verify(imageGenerator).generateFromPath("/path/to/image.jpg", 320, 180);
    }

    @Test
    @DisplayName("비디오 생성기를 사용하여 썸네일을 생성한다")
    void generateVideoThumbnail() {
      // given
      given(imageGenerator.supports(FileType.VIDEO)).willReturn(false);
      given(videoGenerator.supports(FileType.VIDEO)).willReturn(true);
      given(videoGenerator.generateFromPath(anyString(), anyInt(), anyInt()))
          .willReturn(new byte[200]);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator, videoGenerator),
          storage,
          metadataRepository
      );

      // when
      byte[] result = service.generate(FileType.VIDEO, "/path/to/video.mp4", 320, 180);

      // then
      assertThat(result).hasSize(200);
      verify(videoGenerator).generateFromPath("/path/to/video.mp4", 320, 180);
    }

    @Test
    @DisplayName("생성기가 없으면 예외가 발생한다")
    void throwsWhenNoGeneratorAvailable() {
      // given
      given(imageGenerator.supports(FileType.AUDIO)).willReturn(false);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      // when & then
      assertThatThrownBy(() -> service.generate(FileType.AUDIO, "/path/to/audio.mp3", 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class)
          .hasMessageContaining("No thumbnail generator available");
    }
  }

  @Nested
  @DisplayName("우선순위 테스트")
  class OrderTest {

    @Test
    @DisplayName("낮은 order를 가진 생성기가 우선 선택된다")
    void selectsGeneratorWithLowestOrder() {
      // given
      ThumbnailGeneratorPort highPriorityGenerator = createMockGenerator(FileType.IMAGE, 100);
      ThumbnailGeneratorPort lowPriorityGenerator = createMockGenerator(FileType.IMAGE, 1000);

      given(highPriorityGenerator.generateFromPath(anyString(), anyInt(), anyInt()))
          .willReturn(new byte[]{1, 2, 3});

      ThumbnailService service = new ThumbnailService(
          List.of(lowPriorityGenerator, highPriorityGenerator),  // 순서 상관없이
          storage,
          metadataRepository
      );

      // when
      byte[] result = service.generate(FileType.IMAGE, "/path/to/image.jpg", 320, 180);

      // then
      assertThat(result).containsExactly(1, 2, 3);
      verify(highPriorityGenerator).generateFromPath(anyString(), anyInt(), anyInt());
    }

    private ThumbnailGeneratorPort createMockGenerator(FileType supportedType, int order) {
      ThumbnailGeneratorPort mock = org.mockito.Mockito.mock(ThumbnailGeneratorPort.class);
      given(mock.supports(supportedType)).willReturn(true);
      given(mock.getOrder()).willReturn(order);
      given(mock.getName()).willReturn("MockGenerator-" + order);
      return mock;
    }
  }

  @Nested
  @DisplayName("supports 테스트")
  class SupportsTest {

    @Test
    @DisplayName("지원하는 파일 타입을 확인한다")
    void checkSupports() {
      // given
      given(imageGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(imageGenerator.supports(FileType.VIDEO)).willReturn(false);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      // when & then
      assertThat(service.supports(FileType.IMAGE)).isTrue();
      assertThat(service.supports(FileType.VIDEO)).isFalse();
    }

    @Test
    @DisplayName("여러 생성기가 있으면 하나라도 지원하면 true")
    void supportsWithMultipleGenerators() {
      // given
      given(imageGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(imageGenerator.supports(FileType.VIDEO)).willReturn(false);
      given(imageGenerator.supports(FileType.AUDIO)).willReturn(false);
      given(videoGenerator.supports(FileType.IMAGE)).willReturn(false);
      given(videoGenerator.supports(FileType.VIDEO)).willReturn(true);
      given(videoGenerator.supports(FileType.AUDIO)).willReturn(false);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator, videoGenerator),
          storage,
          metadataRepository
      );

      // when & then
      assertThat(service.supports(FileType.IMAGE)).isTrue();
      assertThat(service.supports(FileType.VIDEO)).isTrue();
      assertThat(service.supports(FileType.AUDIO)).isFalse();
    }
  }

  @Nested
  @DisplayName("generateFromStream 테스트")
  class GenerateFromStreamTest {

    @Test
    @DisplayName("InputStream에서 이미지 썸네일을 생성한다")
    void generateFromStream() {
      // given
      given(imageGenerator.supports(FileType.IMAGE)).willReturn(true);
      given(imageGenerator.generate(any(InputStream.class), anyInt(), anyInt()))
          .willReturn(new byte[50]);

      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      InputStream inputStream = new ByteArrayInputStream(new byte[100]);

      // when
      byte[] result = service.generateFromStream(FileType.IMAGE, inputStream, 100, 100);

      // then
      assertThat(result).hasSize(50);
      verify(imageGenerator).generate(any(InputStream.class), eq(100), eq(100));
    }

    @Test
    @DisplayName("VIDEO는 InputStream에서 썸네일 생성을 지원하지 않는다")
    void throwsForVideoInputStream() {
      // given
      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      InputStream inputStream = new ByteArrayInputStream(new byte[100]);

      // when & then
      assertThatThrownBy(() -> service.generateFromStream(FileType.VIDEO, inputStream, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("AUDIO는 InputStream에서 썸네일 생성을 지원하지 않는다")
    void throwsForAudioInputStream() {
      // given
      ThumbnailService service = new ThumbnailService(
          List.of(imageGenerator),
          storage,
          metadataRepository
      );

      InputStream inputStream = new ByteArrayInputStream(new byte[100]);

      // when & then
      assertThatThrownBy(() -> service.generateFromStream(FileType.AUDIO, inputStream, 320, 180))
          .isInstanceOf(ThumbnailGenerationException.class)
          .hasMessageContaining("not supported");
    }
  }

  @Nested
  @DisplayName("getGenerators 테스트")
  class GetGeneratorsTest {

    @Test
    @DisplayName("등록된 생성기 목록을 반환한다")
    void getGenerators() {
      // given - order 설정 (imageGenerator: 500, videoGenerator: 600)
      given(imageGenerator.getOrder()).willReturn(500);
      given(videoGenerator.getOrder()).willReturn(600);

      ThumbnailService service = new ThumbnailService(
          List.of(videoGenerator, imageGenerator),  // 역순으로 등록
          storage,
          metadataRepository
      );

      // when
      List<ThumbnailGeneratorPort> generators = service.getGenerators();

      // then
      assertThat(generators).hasSize(2);
      // order 순으로 정렬되어 있어야 함
      assertThat(generators.get(0)).isEqualTo(imageGenerator);
      assertThat(generators.get(1)).isEqualTo(videoGenerator);
    }

    @Test
    @DisplayName("빈 생성기 목록으로 생성할 수 있다")
    void createWithEmptyGenerators() {
      // given & when
      ThumbnailService service = new ThumbnailService(storage, metadataRepository);

      // then
      assertThat(service.getGenerators()).isEmpty();
      assertThat(service.supports(FileType.IMAGE)).isFalse();
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