package io.github.junhyeong9812.streamix.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StreamableFile 테스트")
class StreamableFileTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("정상적인 값으로 생성된다")
    void createWithValidValues() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = new StreamableFile(
          metadata, stream, 100L, null, null
      );

      assertThat(file.metadata()).isEqualTo(metadata);
      assertThat(file.inputStream()).isEqualTo(stream);
      assertThat(file.contentLength()).isEqualTo(100L);
      assertThat(file.rangeStart()).isNull();
      assertThat(file.rangeEnd()).isNull();
    }

    @Test
    @DisplayName("metadata가 null이면 예외가 발생한다")
    void throwsWhenMetadataIsNull() {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      assertThatThrownBy(() -> new StreamableFile(
          null, stream, 100L, null, null
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("metadata");
    }

    @Test
    @DisplayName("inputStream이 null이면 예외가 발생한다")
    void throwsWhenInputStreamIsNull() {
      FileMetadata metadata = createMetadata();

      assertThatThrownBy(() -> new StreamableFile(
          metadata, null, 100L, null, null
      )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("inputStream");
    }

    @Test
    @DisplayName("contentLength가 음수이면 예외가 발생한다")
    void throwsWhenContentLengthIsNegative() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      assertThatThrownBy(() -> new StreamableFile(
          metadata, stream, -1L, null, null
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("contentLength");
    }
  }

  @Nested
  @DisplayName("full 정적 메서드 테스트")
  class FullTest {

    @Test
    @DisplayName("전체 파일 스트림을 생성한다")
    void createsFullStream() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.full(metadata, stream);

      assertThat(file.contentLength()).isEqualTo(metadata.size());
      assertThat(file.isPartialContent()).isFalse();
      assertThat(file.rangeStart()).isNull();
      assertThat(file.rangeEnd()).isNull();
    }
  }

  @Nested
  @DisplayName("partial 정적 메서드 테스트")
  class PartialTest {

    @Test
    @DisplayName("부분 파일 스트림을 생성한다")
    void createsPartialStream() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.partial(metadata, stream, 0L, 99L);

      assertThat(file.contentLength()).isEqualTo(100L);
      assertThat(file.isPartialContent()).isTrue();
      assertThat(file.rangeStart()).isEqualTo(0L);
      assertThat(file.rangeEnd()).isEqualTo(99L);
    }

    @Test
    @DisplayName("Range 범위에 맞는 contentLength를 계산한다")
    void calculatesContentLength() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[50]);

      StreamableFile file = StreamableFile.partial(metadata, stream, 100L, 149L);

      assertThat(file.contentLength()).isEqualTo(50L); // 149 - 100 + 1
    }
  }

  @Nested
  @DisplayName("isPartialContent 테스트")
  class IsPartialContentTest {

    @Test
    @DisplayName("rangeStart가 있으면 true를 반환한다")
    void returnsTrueWhenRangeExists() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.partial(metadata, stream, 0L, 99L);

      assertThat(file.isPartialContent()).isTrue();
    }

    @Test
    @DisplayName("rangeStart가 null이면 false를 반환한다")
    void returnsFalseWhenNoRange() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.full(metadata, stream);

      assertThat(file.isPartialContent()).isFalse();
    }
  }

  @Nested
  @DisplayName("getContentRange 테스트")
  class GetContentRangeTest {

    @Test
    @DisplayName("부분 콘텐츠의 Content-Range 헤더 값을 반환한다")
    void returnsContentRangeHeader() {
      FileMetadata metadata = createMetadata(); // size = 1024
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.partial(metadata, stream, 0L, 99L);

      assertThat(file.getContentRange()).isEqualTo("bytes 0-99/1024");
    }

    @Test
    @DisplayName("전체 콘텐츠는 null을 반환한다")
    void returnsNullForFullContent() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.full(metadata, stream);

      assertThat(file.getContentRange()).isNull();
    }
  }

  @Nested
  @DisplayName("getContentType 테스트")
  class GetContentTypeTest {

    @Test
    @DisplayName("메타데이터의 contentType을 반환한다")
    void returnsContentType() {
      FileMetadata metadata = createMetadata();
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      StreamableFile file = StreamableFile.full(metadata, stream);

      assertThat(file.getContentType()).isEqualTo("image/jpeg");
    }
  }

  private FileMetadata createMetadata() {
    return new FileMetadata(
        UUID.randomUUID(),
        "test.jpg",
        FileType.IMAGE,
        "image/jpeg",
        1024L,
        "/path/to/file.jpg",
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
  }
}