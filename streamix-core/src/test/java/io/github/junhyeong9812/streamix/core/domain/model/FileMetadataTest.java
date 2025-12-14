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
    @DisplayName("기본값으로 메타데이터를 생성한다")
    void createWithDefaults() {
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
  }

  @Nested
  @DisplayName("hasThumbnail 테스트")
  class HasThumbnailTest {

    @Test
    @DisplayName("썸네일 경로가 있으면 true를 반환한다")
    void returnsTrueWhenThumbnailExists() {
      FileMetadata metadata = createMetadata("/thumb.jpg");
      assertThat(metadata.hasThumbnail()).isTrue();
    }

    @Test
    @DisplayName("썸네일 경로가 null이면 false를 반환한다")
    void returnsFalseWhenThumbnailIsNull() {
      FileMetadata metadata = createMetadata(null);
      assertThat(metadata.hasThumbnail()).isFalse();
    }

    @Test
    @DisplayName("썸네일 경로가 blank이면 false를 반환한다")
    void returnsFalseWhenThumbnailIsBlank() {
      FileMetadata metadata = createMetadata("  ");
      assertThat(metadata.hasThumbnail()).isFalse();
    }
  }

  @Nested
  @DisplayName("isVideo/isImage 테스트")
  class TypeCheckTest {

    @Test
    @DisplayName("VIDEO 타입은 isVideo가 true이다")
    void videoTypeReturnsTrue() {
      FileMetadata metadata = FileMetadata.create(
          "video.mp4", FileType.VIDEO, "video/mp4", 1024L, "/path"
      );
      assertThat(metadata.isVideo()).isTrue();
      assertThat(metadata.isImage()).isFalse();
    }

    @Test
    @DisplayName("IMAGE 타입은 isImage가 true이다")
    void imageTypeReturnsTrue() {
      FileMetadata metadata = FileMetadata.create(
          "image.jpg", FileType.IMAGE, "image/jpeg", 1024L, "/path"
      );
      assertThat(metadata.isImage()).isTrue();
      assertThat(metadata.isVideo()).isFalse();
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
          "noextension", FileType.IMAGE, "image/jpeg", 1024L, "/path"
      );
      assertThat(metadata.getExtension()).isEmpty();
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
      assertThat(original.thumbnailPath()).isNull(); // 원본은 변경 안됨
    }
  }

  private FileMetadata createMetadata(String thumbnailPath) {
    return new FileMetadata(
        UUID.randomUUID(),
        "test.jpg",
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/path/to/file.jpg",
        thumbnailPath,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}