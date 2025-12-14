package io.github.junhyeong9812.streamix.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UploadResult 테스트")
class UploadResultTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("정상적인 값으로 생성된다")
    void createWithValidValues() {
      UUID id = UUID.randomUUID();

      UploadResult result = new UploadResult(
          id,
          "test.jpg",
          FileType.IMAGE,
          "image/jpeg",
          1024L,
          true
      );

      assertThat(result.id()).isEqualTo(id);
      assertThat(result.originalName()).isEqualTo("test.jpg");
      assertThat(result.type()).isEqualTo(FileType.IMAGE);
      assertThat(result.contentType()).isEqualTo("image/jpeg");
      assertThat(result.size()).isEqualTo(1024L);
      assertThat(result.thumbnailGenerated()).isTrue();
    }

    @Test
    @DisplayName("id가 null이면 예외가 발생한다")
    void throwsWhenIdIsNull() {
      assertThatThrownBy(() -> new UploadResult(
          null, "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, false
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("originalName이 null이면 예외가 발생한다")
    void throwsWhenOriginalNameIsNull() {
      assertThatThrownBy(() -> new UploadResult(
          UUID.randomUUID(), null, FileType.IMAGE, "image/jpeg", 1024L, false
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("originalName");
    }

    @Test
    @DisplayName("type이 null이면 예외가 발생한다")
    void throwsWhenTypeIsNull() {
      assertThatThrownBy(() -> new UploadResult(
          UUID.randomUUID(), "test.jpg", null, "image/jpeg", 1024L, false
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("type");
    }

    @Test
    @DisplayName("contentType이 null이면 예외가 발생한다")
    void throwsWhenContentTypeIsNull() {
      assertThatThrownBy(() -> new UploadResult(
          UUID.randomUUID(), "test.jpg", FileType.IMAGE, null, 1024L, false
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("contentType");
    }
  }

  @Nested
  @DisplayName("from 정적 메서드 테스트")
  class FromTest {

    @Test
    @DisplayName("FileMetadata에서 UploadResult를 생성한다")
    void createsFromMetadata() {
      // given
      UUID id = UUID.randomUUID();
      FileMetadata metadata = new FileMetadata(
          id,
          "video.mp4",
          FileType.VIDEO,
          "video/mp4",
          2048L,
          "/storage/video.mp4",
          null,
          LocalDateTime.now(),
          LocalDateTime.now()
      );

      // when
      UploadResult result = UploadResult.from(metadata);

      // then
      assertThat(result.id()).isEqualTo(id);
      assertThat(result.originalName()).isEqualTo("video.mp4");
      assertThat(result.type()).isEqualTo(FileType.VIDEO);
      assertThat(result.contentType()).isEqualTo("video/mp4");
      assertThat(result.size()).isEqualTo(2048L);
      assertThat(result.thumbnailGenerated()).isFalse();
    }

    @Test
    @DisplayName("썸네일이 있는 FileMetadata에서 생성하면 thumbnailGenerated가 true이다")
    void thumbnailGeneratedIsTrueWhenThumbnailExists() {
      // given
      FileMetadata metadata = new FileMetadata(
          UUID.randomUUID(),
          "image.jpg",
          FileType.IMAGE,
          "image/jpeg",
          1024L,
          "/storage/image.jpg",
          "/storage/image_thumb.jpg",  // 썸네일 있음
          LocalDateTime.now(),
          LocalDateTime.now()
      );

      // when
      UploadResult result = UploadResult.from(metadata);

      // then
      assertThat(result.thumbnailGenerated()).isTrue();
    }

    @Test
    @DisplayName("썸네일이 없는 FileMetadata에서 생성하면 thumbnailGenerated가 false이다")
    void thumbnailGeneratedIsFalseWhenNoThumbnail() {
      // given
      FileMetadata metadata = new FileMetadata(
          UUID.randomUUID(),
          "image.jpg",
          FileType.IMAGE,
          "image/jpeg",
          1024L,
          "/storage/image.jpg",
          null,  // 썸네일 없음
          LocalDateTime.now(),
          LocalDateTime.now()
      );

      // when
      UploadResult result = UploadResult.from(metadata);

      // then
      assertThat(result.thumbnailGenerated()).isFalse();
    }

    @Test
    @DisplayName("썸네일 경로가 blank이면 thumbnailGenerated가 false이다")
    void thumbnailGeneratedIsFalseWhenThumbnailPathIsBlank() {
      // given
      FileMetadata metadata = new FileMetadata(
          UUID.randomUUID(),
          "image.jpg",
          FileType.IMAGE,
          "image/jpeg",
          1024L,
          "/storage/image.jpg",
          "   ",  // blank 썸네일 경로
          LocalDateTime.now(),
          LocalDateTime.now()
      );

      // when
      UploadResult result = UploadResult.from(metadata);

      // then
      assertThat(result.thumbnailGenerated()).isFalse();
    }

    @Test
    @DisplayName("null metadata는 예외가 발생한다")
    void throwsWhenMetadataIsNull() {
      assertThatThrownBy(() -> UploadResult.from(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("metadata");
    }
  }

  @Nested
  @DisplayName("Record 동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 값을 가진 UploadResult는 동등하다")
    void equalResultsAreEqual() {
      UUID id = UUID.randomUUID();

      UploadResult result1 = new UploadResult(
          id, "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, true
      );
      UploadResult result2 = new UploadResult(
          id, "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, true
      );

      assertThat(result1).isEqualTo(result2);
      assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 UploadResult는 동등하지 않다")
    void differentResultsAreNotEqual() {
      UploadResult result1 = new UploadResult(
          UUID.randomUUID(), "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, true
      );
      UploadResult result2 = new UploadResult(
          UUID.randomUUID(), "test.jpg", FileType.IMAGE, "image/jpeg", 1024L, true
      );

      assertThat(result1).isNotEqualTo(result2);
    }
  }
}