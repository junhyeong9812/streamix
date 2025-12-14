package io.github.junhyeong9812.streamix.core.adapter.out.storage;

import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.exception.StorageException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalFileStorageAdapter 테스트")
class LocalFileStorageAdapterTest {

  @TempDir
  Path tempDir;

  private LocalFileStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new LocalFileStorageAdapter(tempDir);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("파일을 저장한다")
    void saveFile() throws IOException {
      // given
      byte[] content = "test content".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      // when
      String savedPath = adapter.save("test.txt", inputStream, content.length);

      // then
      assertThat(savedPath).isNotNull();
      assertThat(Files.exists(Path.of(savedPath))).isTrue();
      assertThat(Files.readAllBytes(Path.of(savedPath))).isEqualTo(content);
    }

    @Test
    @DisplayName("하위 디렉토리에 파일을 저장한다")
    void saveFileInSubdirectory() {
      // given
      byte[] content = "test".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      // when
      String savedPath = adapter.save("sub/dir/test.txt", inputStream, content.length);

      // then
      assertThat(Files.exists(Path.of(savedPath))).isTrue();
    }

    @Test
    @DisplayName("Path Traversal 공격을 방지한다")
    void preventsPathTraversal() {
      // given
      byte[] content = "malicious".getBytes();
      InputStream inputStream = new ByteArrayInputStream(content);

      // when & then
      assertThatThrownBy(() -> adapter.save("../../../etc/passwd", inputStream, content.length))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid file path");
    }
  }

  @Nested
  @DisplayName("load 테스트")
  class LoadTest {

    @Test
    @DisplayName("파일을 로드한다")
    void loadFile() throws IOException {
      // given
      byte[] content = "test content".getBytes();
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, content);

      // when
      InputStream result = adapter.load(filePath.toString());

      // then
      assertThat(result.readAllBytes()).isEqualTo(content);
      result.close();
    }

    @Test
    @DisplayName("존재하지 않는 파일은 예외가 발생한다")
    void throwsWhenFileNotFound() {
      assertThatThrownBy(() -> adapter.load("/nonexistent/file.txt"))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("loadPartial 테스트")
  class LoadPartialTest {

    @Test
    @DisplayName("파일의 일부를 로드한다")
    void loadPartialFile() throws IOException {
      // given
      byte[] content = "0123456789".getBytes(); // 10 bytes
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, content);

      // when
      InputStream result = adapter.loadPartial(filePath.toString(), 2, 5);
      byte[] partialContent = result.readAllBytes();
      result.close();

      // then
      assertThat(new String(partialContent)).isEqualTo("2345");
    }

    @Test
    @DisplayName("시작부터 중간까지 로드한다")
    void loadFromStart() throws IOException {
      // given
      byte[] content = "0123456789".getBytes();
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, content);

      // when
      InputStream result = adapter.loadPartial(filePath.toString(), 0, 4);
      byte[] partialContent = result.readAllBytes();
      result.close();

      // then
      assertThat(new String(partialContent)).isEqualTo("01234");
    }

    @Test
    @DisplayName("중간부터 끝까지 로드한다")
    void loadToEnd() throws IOException {
      // given
      byte[] content = "0123456789".getBytes();
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, content);

      // when
      InputStream result = adapter.loadPartial(filePath.toString(), 7, 9);
      byte[] partialContent = result.readAllBytes();
      result.close();

      // then
      assertThat(new String(partialContent)).isEqualTo("789");
    }

    @Test
    @DisplayName("존재하지 않는 파일은 예외가 발생한다")
    void throwsWhenFileNotFound() {
      assertThatThrownBy(() -> adapter.loadPartial("/nonexistent/file.txt", 0, 10))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("파일을 삭제한다")
    void deleteFile() throws IOException {
      // given
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, "content".getBytes());
      assertThat(Files.exists(filePath)).isTrue();

      // when
      adapter.delete(filePath.toString());

      // then
      assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 파일 삭제는 예외가 발생하지 않는다")
    void doesNotThrowWhenFileNotExists() {
      // when & then - no exception
      adapter.delete("/nonexistent/file.txt");
    }
  }

  @Nested
  @DisplayName("exists 테스트")
  class ExistsTest {

    @Test
    @DisplayName("파일이 존재하면 true를 반환한다")
    void returnsTrueWhenExists() throws IOException {
      // given
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, "content".getBytes());

      // when & then
      assertThat(adapter.exists(filePath.toString())).isTrue();
    }

    @Test
    @DisplayName("파일이 없으면 false를 반환한다")
    void returnsFalseWhenNotExists() {
      assertThat(adapter.exists("/nonexistent/file.txt")).isFalse();
    }
  }

  @Nested
  @DisplayName("getSize 테스트")
  class GetSizeTest {

    @Test
    @DisplayName("파일 크기를 반환한다")
    void returnsFileSize() throws IOException {
      // given
      byte[] content = "test content".getBytes();
      Path filePath = tempDir.resolve("test.txt");
      Files.write(filePath, content);

      // when
      long size = adapter.getSize(filePath.toString());

      // then
      assertThat(size).isEqualTo(content.length);
    }

    @Test
    @DisplayName("존재하지 않는 파일은 예외가 발생한다")
    void throwsWhenFileNotFound() {
      assertThatThrownBy(() -> adapter.getSize("/nonexistent/file.txt"))
          .isInstanceOf(FileNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("생성자 테스트")
  class ConstructorTest {

    @Test
    @DisplayName("디렉토리가 없으면 생성한다")
    void createsDirectoryIfNotExists() {
      // given
      Path newDir = tempDir.resolve("new/storage/path");
      assertThat(Files.exists(newDir)).isFalse();

      // when
      new LocalFileStorageAdapter(newDir);

      // then
      assertThat(Files.exists(newDir)).isTrue();
    }

    @Test
    @DisplayName("String 경로로 생성할 수 있다")
    void createsWithStringPath() {
      // given
      String pathStr = tempDir.resolve("string-path").toString();

      // when
      LocalFileStorageAdapter stringAdapter = new LocalFileStorageAdapter(pathStr);

      // then
      assertThat(Files.exists(Path.of(pathStr))).isTrue();
    }
  }
}