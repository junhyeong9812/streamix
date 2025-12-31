package io.github.junhyeong9812.streamix.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileMetadata 테스트")
class FileMetadataTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("정상적인 값으로 생성된다")
    void createWithValidValues() {
      UUID id = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      FileMetadata metadata = new FileMetadata(
          id,
          "test.jpg",
          FileType.IMAGE,
          "image/jpeg",
          1024L,
          "/path/to/file.jpg",
          "/path/to/thumb.jpg",
          now,
          now
      );

      assertThat(metadata.id()).isEqualTo(id);
      assertThat(metadata.originalName()).isEqualTo("test.jpg");
      assertThat(metadata.type()).isEqualTo(FileType.IMAGE);
      assertThat(metadata.contentType()).isEqualTo("image/jpeg");
      assertThat(metadata.size()).isEqualTo(1024L);
      assertThat(metadata.storagePath()).isEqualTo("/path/to/file.jpg");
      assertThat(metadata.thumbnailPath()).isEqualTo("/path/to/thumb.jpg");
    }

    @Test
    @DisplayName("id가 null이면 예외가 발생한다")
    void throwsWhenIdIsNull() {
      assertThatThrownBy(() -> new FileMetadata(
          null, "test.jpg", FileType.IMAGE, "image/jpeg",
          1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("originalName이 null이면 예외가 발생한다")
    void throwsWhenOriginalNameIsNull() {
      assertThatThrownBy(() -> new FileMetadata(
          UUID.randomUUID(), null, FileType.IMAGE, "image/jpeg",
          1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("originalName");
    }

    @Test
    @DisplayName("originalName이 blank이면 예외가 발생한다")
    void throwsWhenOriginalNameIsBlank() {
      assertThatThrownBy(() -> new FileMetadata(
          UUID.randomUUID(), "  ", FileType.IMAGE, "image/jpeg",
          1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("originalName");
    }

    @Test
    @DisplayName("size가 음수이면 예외가 발생한다")
    void throwsWhenSizeIsNegative() {
      assertThatThrownBy(() -> new FileMetadata(
          UUID.randomUUID(), "test.jpg", FileType.IMAGE, "image/jpeg",
          -1L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("size");
    }
  }

  @Nested
  @DisplayName("create 정적 메서드 테스트")
  class StaticCreateTest {

    @Test
    @DisplayName("5파라미터로 메타데이터를 생성한다")
    void createWithFiveParams() {
      FileMetadata metadata = FileMetadata.create(
          "video.mp4",
          FileType.VIDEO,
          "video/mp4",
          2048L,
          "/storage/video.mp4"
      );

      assertThat(metadata.id()).isNotNull();
      assertThat(metadata.originalName()).isEqualTo("video.mp4");
      assertThat(metadata.type()).isEqualTo(FileType.VIDEO);
      assertThat(metadata.thumbnailPath()).isNull();
      assertThat(metadata.createdAt()).isNotNull();
      assertThat(metadata.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("4파라미터로 메타데이터를 생성한다 (타입 자동 추론)")
    void createWithFourParams() {
      FileMetadata metadata = FileMetadata.create(
          "report.pdf",
          "application/pdf",
          4096L,
          "/storage/report.pdf"
      );

      assertThat(metadata.type()).isEqualTo(FileType.DOCUMENT);
      assertThat(metadata.originalName()).isEqualTo("report.pdf");
    }

    @Test
    @DisplayName("Content-Type으로 타입 추론 실패 시 확장자로 재시도한다")
    void fallsBackToExtensionWhenContentTypeIsOther() {
      FileMetadata metadata = FileMetadata.create(
          "music.mp3",
          "application/octet-stream",  // OTHER로 판단됨
          1024L,
          "/storage/music.mp3"
      );

      assertThat(metadata.type()).isEqualTo(FileType.AUDIO);
    }
  }

  @Nested
  @DisplayName("hasThumbnail 테스트")
  class HasThumbnailTest {

    @Test
    @DisplayName("썸네일 경로가 있으면 true를 반환한다")
    void returnsTrueWhenThumbnailExists() {
      FileMetadata metadata = createMetadata(FileType.IMAGE, "/thumb.jpg");
      assertThat(metadata.hasThumbnail()).isTrue();
    }

    @Test
    @DisplayName("썸네일 경로가 null이면 false를 반환한다")
    void returnsFalseWhenThumbnailIsNull() {
      FileMetadata metadata = createMetadata(FileType.IMAGE, null);
      assertThat(metadata.hasThumbnail()).isFalse();
    }

    @Test
    @DisplayName("썸네일 경로가 blank이면 false를 반환한다")
    void returnsFalseWhenThumbnailIsBlank() {
      FileMetadata metadata = createMetadata(FileType.IMAGE, "  ");
      assertThat(metadata.hasThumbnail()).isFalse();
    }
  }

  @Nested
  @DisplayName("타입 체크 메서드 테스트")
  class TypeCheckTest {

    @Test
    @DisplayName("IMAGE 타입은 isImage가 true이다")
    void imageTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.IMAGE);
      assertThat(metadata.isImage()).isTrue();
      assertThat(metadata.isVideo()).isFalse();
      assertThat(metadata.isAudio()).isFalse();
      assertThat(metadata.isDocument()).isFalse();
      assertThat(metadata.isArchive()).isFalse();
      assertThat(metadata.isOther()).isFalse();
    }

    @Test
    @DisplayName("VIDEO 타입은 isVideo가 true이다")
    void videoTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.VIDEO);
      assertThat(metadata.isVideo()).isTrue();
      assertThat(metadata.isImage()).isFalse();
    }

    @Test
    @DisplayName("AUDIO 타입은 isAudio가 true이다")
    void audioTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.AUDIO);
      assertThat(metadata.isAudio()).isTrue();
    }

    @Test
    @DisplayName("DOCUMENT 타입은 isDocument가 true이다")
    void documentTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.DOCUMENT);
      assertThat(metadata.isDocument()).isTrue();
    }

    @Test
    @DisplayName("ARCHIVE 타입은 isArchive가 true이다")
    void archiveTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.ARCHIVE);
      assertThat(metadata.isArchive()).isTrue();
    }

    @Test
    @DisplayName("OTHER 타입은 isOther가 true이다")
    void otherTypeCheck() {
      FileMetadata metadata = createMetadataWithType(FileType.OTHER);
      assertThat(metadata.isOther()).isTrue();
    }

    @Test
    @DisplayName("PDF 파일은 isPdf가 true이다")
    void pdfCheck() {
      FileMetadata metadata = new FileMetadata(
          UUID.randomUUID(), "doc.pdf", FileType.DOCUMENT, "application/pdf",
          1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      );
      assertThat(metadata.isPdf()).isTrue();
    }

    @Test
    @DisplayName("PDF가 아닌 DOCUMENT는 isPdf가 false이다")
    void nonPdfDocumentCheck() {
      FileMetadata metadata = new FileMetadata(
          UUID.randomUUID(), "doc.docx", FileType.DOCUMENT,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now()
      );
      assertThat(metadata.isPdf()).isFalse();
    }
  }

  @Nested
  @DisplayName("isMedia 테스트")
  class IsMediaTest {

    @Test
    @DisplayName("IMAGE, VIDEO, AUDIO는 미디어이다")
    void mediaTypes() {
      assertThat(createMetadataWithType(FileType.IMAGE).isMedia()).isTrue();
      assertThat(createMetadataWithType(FileType.VIDEO).isMedia()).isTrue();
      assertThat(createMetadataWithType(FileType.AUDIO).isMedia()).isTrue();
    }

    @Test
    @DisplayName("DOCUMENT, ARCHIVE, OTHER는 미디어가 아니다")
    void nonMediaTypes() {
      assertThat(createMetadataWithType(FileType.DOCUMENT).isMedia()).isFalse();
      assertThat(createMetadataWithType(FileType.ARCHIVE).isMedia()).isFalse();
      assertThat(createMetadataWithType(FileType.OTHER).isMedia()).isFalse();
    }
  }

  @Nested
  @DisplayName("isStreamable 테스트")
  class IsStreamableTest {

    @Test
    @DisplayName("VIDEO, AUDIO는 스트리밍 가능하다")
    void streamableTypes() {
      assertThat(createMetadataWithType(FileType.VIDEO).isStreamable()).isTrue();
      assertThat(createMetadataWithType(FileType.AUDIO).isStreamable()).isTrue();
    }

    @Test
    @DisplayName("나머지 타입은 스트리밍 대상이 아니다")
    void nonStreamableTypes() {
      assertThat(createMetadataWithType(FileType.IMAGE).isStreamable()).isFalse();
      assertThat(createMetadataWithType(FileType.DOCUMENT).isStreamable()).isFalse();
    }
  }

  @Nested
  @DisplayName("canGenerateThumbnail 테스트")
  class CanGenerateThumbnailTest {

    @Test
    @DisplayName("IMAGE, VIDEO, DOCUMENT는 썸네일 생성 가능하다")
    void thumbnailSupportedTypes() {
      assertThat(createMetadataWithType(FileType.IMAGE).canGenerateThumbnail()).isTrue();
      assertThat(createMetadataWithType(FileType.VIDEO).canGenerateThumbnail()).isTrue();
      assertThat(createMetadataWithType(FileType.DOCUMENT).canGenerateThumbnail()).isTrue();
    }

    @Test
    @DisplayName("AUDIO, ARCHIVE, OTHER는 썸네일 생성 불가하다")
    void thumbnailUnsupportedTypes() {
      assertThat(createMetadataWithType(FileType.AUDIO).canGenerateThumbnail()).isFalse();
      assertThat(createMetadataWithType(FileType.ARCHIVE).canGenerateThumbnail()).isFalse();
      assertThat(createMetadataWithType(FileType.OTHER).canGenerateThumbnail()).isFalse();
    }
  }

  @Nested
  @DisplayName("getExtension 테스트")
  class GetExtensionTest {

    @Test
    @DisplayName("파일명에서 확장자를 추출한다")
    void extractsExtension() {
      FileMetadata metadata = FileMetadata.create(
          "photo.PNG", FileType.IMAGE, "image/png", 1024L, "/path"
      );
      assertThat(metadata.getExtension()).isEqualTo("png");
    }

    @Test
    @DisplayName("확장자가 없으면 빈 문자열을 반환한다")
    void returnsEmptyWhenNoExtension() {
      FileMetadata metadata = FileMetadata.create(
          "noextension", FileType.OTHER, "application/octet-stream", 1024L, "/path"
      );
      assertThat(metadata.getExtension()).isEmpty();
    }
  }

  @Nested
  @DisplayName("getBaseName 테스트")
  class GetBaseNameTest {

    @Test
    @DisplayName("파일명에서 확장자를 제외한 이름을 반환한다")
    void returnsBaseName() {
      FileMetadata metadata = FileMetadata.create(
          "photo.jpg", FileType.IMAGE, "image/jpeg", 1024L, "/path"
      );
      assertThat(metadata.getBaseName()).isEqualTo("photo");
    }

    @Test
    @DisplayName("확장자가 없으면 전체 파일명을 반환한다")
    void returnsFullNameWhenNoExtension() {
      FileMetadata metadata = FileMetadata.create(
          "noextension", FileType.OTHER, "application/octet-stream", 1024L, "/path"
      );
      assertThat(metadata.getBaseName()).isEqualTo("noextension");
    }

    @Test
    @DisplayName("여러 점이 있으면 마지막 점 이전까지 반환한다")
    void handlesMultipleDots() {
      FileMetadata metadata = FileMetadata.create(
          "my.file.name.jpg", FileType.IMAGE, "image/jpeg", 1024L, "/path"
      );
      assertThat(metadata.getBaseName()).isEqualTo("my.file.name");
    }
  }

  @Nested
  @DisplayName("getFormattedSize 테스트")
  class GetFormattedSizeTest {

    @Test
    @DisplayName("바이트 단위로 표시한다")
    void formatsBytes() {
      FileMetadata metadata = createMetadataWithSize(500L);
      assertThat(metadata.getFormattedSize()).isEqualTo("500 B");
    }

    @Test
    @DisplayName("KB 단위로 표시한다")
    void formatsKilobytes() {
      FileMetadata metadata = createMetadataWithSize(2048L);
      assertThat(metadata.getFormattedSize()).isEqualTo("2.0 KB");
    }

    @Test
    @DisplayName("MB 단위로 표시한다")
    void formatsMegabytes() {
      FileMetadata metadata = createMetadataWithSize(15_728_640L);  // 15 MB
      assertThat(metadata.getFormattedSize()).isEqualTo("15.0 MB");
    }

    @Test
    @DisplayName("GB 단위로 표시한다")
    void formatsGigabytes() {
      FileMetadata metadata = createMetadataWithSize(2_147_483_648L);  // 2 GB
      assertThat(metadata.getFormattedSize()).isEqualTo("2.00 GB");
    }
  }

  @Nested
  @DisplayName("withThumbnailPath 테스트")
  class WithThumbnailPathTest {

    @Test
    @DisplayName("썸네일 경로를 추가한 새 객체를 반환한다")
    void returnsNewInstanceWithThumbnail() {
      FileMetadata original = FileMetadata.create(
          "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, "/path"
      );

      FileMetadata withThumb = original.withThumbnailPath("/thumb.jpg");

      assertThat(withThumb).isNotSameAs(original);
      assertThat(withThumb.thumbnailPath()).isEqualTo("/thumb.jpg");
      assertThat(withThumb.id()).isEqualTo(original.id());
      assertThat(original.thumbnailPath()).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private FileMetadata createMetadata(FileType type, String thumbnailPath) {
    return new FileMetadata(
        UUID.randomUUID(),
        "test.jpg",
        type,
        "image/jpeg",
        1024L,
        "/path/to/file.jpg",
        thumbnailPath,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  private FileMetadata createMetadataWithType(FileType type) {
    return new FileMetadata(
        UUID.randomUUID(),
        "test.file",
        type,
        "application/octet-stream",
        1024L,
        "/path/to/file",
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }

  private FileMetadata createMetadataWithSize(long size) {
    return new FileMetadata(
        UUID.randomUUID(),
        "test.file",
        FileType.OTHER,
        "application/octet-stream",
        size,
        "/path/to/file",
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}