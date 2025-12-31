package io.github.junhyeong9812.streamix.core.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreamixException 통합 테스트")
class StreamixExceptionTest {

  @Nested
  @DisplayName("예외 계층 구조 테스트")
  class HierarchyTest {

    @Test
    @DisplayName("모든 예외는 StreamixException을 상속한다")
    void allExceptionsExtendStreamixException() {
      assertThat(new FileNotFoundException(UUID.randomUUID()))
          .isInstanceOf(StreamixException.class);
      assertThat(new InvalidFileTypeException("xyz"))
          .isInstanceOf(StreamixException.class);
      assertThat(new StorageException("error"))
          .isInstanceOf(StreamixException.class);
      assertThat(new ThumbnailGenerationException("error"))
          .isInstanceOf(StreamixException.class);
      assertThat(new FileSizeExceededException(100L, 50L))
          .isInstanceOf(StreamixException.class);
    }

    @Test
    @DisplayName("모든 예외는 RuntimeException이다")
    void allExceptionsAreRuntimeException() {
      assertThat(new FileNotFoundException(UUID.randomUUID()))
          .isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("FileNotFoundException 테스트")
  class FileNotFoundExceptionTest {

    @Test
    @DisplayName("UUID로 생성한다")
    void createWithUuid() {
      UUID id = UUID.randomUUID();
      FileNotFoundException ex = new FileNotFoundException(id);

      assertThat(ex.getFileId()).isEqualTo(id);
      assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    @DisplayName("경로로 생성한다")
    void createWithPath() {
      FileNotFoundException ex = new FileNotFoundException("/path/to/file.jpg");

      assertThat(ex.getFileId()).isNull();
      assertThat(ex.getMessage()).contains("/path/to/file.jpg");
    }
  }

  @Nested
  @DisplayName("InvalidFileTypeException 테스트")
  class InvalidFileTypeExceptionTest {

    @Test
    @DisplayName("확장자만으로 생성한다")
    void createWithExtensionOnly() {
      InvalidFileTypeException ex = new InvalidFileTypeException("exe");

      assertThat(ex.getExtension()).isEqualTo("exe");
      assertThat(ex.getContentType()).isNull();
      assertThat(ex.getMessage()).contains("exe");
    }

    @Test
    @DisplayName("확장자와 Content-Type으로 생성한다")
    void createWithExtensionAndContentType() {
      InvalidFileTypeException ex = new InvalidFileTypeException("exe", "application/x-executable");

      assertThat(ex.getExtension()).isEqualTo("exe");
      assertThat(ex.getContentType()).isEqualTo("application/x-executable");
      assertThat(ex.getMessage()).contains("exe");
      assertThat(ex.getMessage()).contains("application/x-executable");
    }
  }

  @Nested
  @DisplayName("StorageException 테스트")
  class StorageExceptionTest {

    @Test
    @DisplayName("메시지로 생성한다")
    void createWithMessage() {
      StorageException ex = new StorageException("Storage error");

      assertThat(ex.getMessage()).isEqualTo("Storage error");
      assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("메시지와 원인으로 생성한다")
    void createWithMessageAndCause() {
      IOException cause = new IOException("IO error");
      StorageException ex = new StorageException("Storage error", cause);

      assertThat(ex.getMessage()).isEqualTo("Storage error");
      assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("saveFailed 팩토리 메서드")
    void saveFailedFactory() {
      IOException cause = new IOException("Disk full");
      StorageException ex = StorageException.saveFailed("video.mp4", cause);

      assertThat(ex.getMessage()).contains("video.mp4");
      assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("loadFailed 팩토리 메서드")
    void loadFailedFactory() {
      IOException cause = new IOException("Read error");
      StorageException ex = StorageException.loadFailed("/path/file.jpg", cause);

      assertThat(ex.getMessage()).contains("/path/file.jpg");
    }

    @Test
    @DisplayName("deleteFailed 팩토리 메서드")
    void deleteFailedFactory() {
      IOException cause = new IOException("Permission denied");
      StorageException ex = StorageException.deleteFailed("/path/file.jpg", cause);

      assertThat(ex.getMessage()).contains("/path/file.jpg");
    }

    @Test
    @DisplayName("directoryCreationFailed 팩토리 메서드")
    void directoryCreationFailedFactory() {
      IOException cause = new IOException("No space");
      StorageException ex = StorageException.directoryCreationFailed("/path/dir", cause);

      assertThat(ex.getMessage()).contains("/path/dir");
    }
  }

  @Nested
  @DisplayName("ThumbnailGenerationException 테스트")
  class ThumbnailGenerationExceptionTest {

    @Test
    @DisplayName("메시지로 생성한다")
    void createWithMessage() {
      ThumbnailGenerationException ex = new ThumbnailGenerationException("Unsupported format");

      assertThat(ex.getFileId()).isNull();
      assertThat(ex.getMessage()).isEqualTo("Unsupported format");
    }

    @Test
    @DisplayName("메시지와 원인으로 생성한다")
    void createWithMessageAndCause() {
      IOException cause = new IOException("Read error");
      ThumbnailGenerationException ex = new ThumbnailGenerationException("Failed", cause);

      assertThat(ex.getFileId()).isNull();
      assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("파일 ID와 원인으로 생성한다")
    void createWithFileIdAndCause() {
      UUID id = UUID.randomUUID();
      IOException cause = new IOException("FFmpeg error");
      ThumbnailGenerationException ex = new ThumbnailGenerationException(id, cause);

      assertThat(ex.getFileId()).isEqualTo(id);
      assertThat(ex.getMessage()).contains(id.toString());
      assertThat(ex.getCause()).isEqualTo(cause);
    }
  }

  @Nested
  @DisplayName("FileSizeExceededException 테스트")
  class FileSizeExceededExceptionTest {

    @Test
    @DisplayName("크기만으로 생성한다")
    void createWithSizeOnly() {
      FileSizeExceededException ex = new FileSizeExceededException(
          200_000_000L,  // 200MB
          100_000_000L   // 100MB
      );

      assertThat(ex.getActualSize()).isEqualTo(200_000_000L);
      assertThat(ex.getMaxSize()).isEqualTo(100_000_000L);
      assertThat(ex.getFileName()).isNull();
    }

    @Test
    @DisplayName("파일명과 함께 생성한다")
    void createWithFileName() {
      FileSizeExceededException ex = new FileSizeExceededException(
          "large-video.mp4",
          500_000_000L,
          100_000_000L
      );

      assertThat(ex.getFileName()).isEqualTo("large-video.mp4");
      assertThat(ex.getMessage()).contains("large-video.mp4");
    }

    @Test
    @DisplayName("초과 크기를 계산한다")
    void calculatesExceededBy() {
      FileSizeExceededException ex = new FileSizeExceededException(150L, 100L);

      assertThat(ex.getExceededBy()).isEqualTo(50L);
    }

    @Test
    @DisplayName("크기를 사람이 읽기 쉽게 포맷한다")
    void formatsSizeInMessage() {
      // Bytes
      FileSizeExceededException bytesEx = new FileSizeExceededException(500L, 100L);
      assertThat(bytesEx.getMessage()).contains("500 B").contains("100 B");

      // KB
      FileSizeExceededException kbEx = new FileSizeExceededException(2048L, 1024L);
      assertThat(kbEx.getMessage()).contains("KB");

      // MB
      FileSizeExceededException mbEx = new FileSizeExceededException(15_728_640L, 10_485_760L);
      assertThat(mbEx.getMessage()).contains("MB");

      // GB
      FileSizeExceededException gbEx = new FileSizeExceededException(2_147_483_648L, 1_073_741_824L);
      assertThat(gbEx.getMessage()).contains("GB");
    }
  }

  @Nested
  @DisplayName("switch 표현식 테스트")
  class SwitchExpressionTest {

    @Test
    @DisplayName("sealed class로 타입별 처리가 가능하다")
    void switchExpressionByType() {
      StreamixException ex = new FileNotFoundException(UUID.randomUUID());

      String result = switch (ex) {
        case FileNotFoundException fnf -> "NOT_FOUND";
        case InvalidFileTypeException ift -> "INVALID_TYPE";
        case StorageException se -> "STORAGE_ERROR";
        case ThumbnailGenerationException tge -> "THUMBNAIL_ERROR";
        case FileSizeExceededException fse -> "SIZE_EXCEEDED";
        default -> "UNKNOWN";  // StreamixException 직접 인스턴스 대비
      };

      assertThat(result).isEqualTo("NOT_FOUND");
    }
  }
}