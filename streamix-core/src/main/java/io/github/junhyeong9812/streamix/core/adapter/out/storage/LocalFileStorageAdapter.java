package io.github.junhyeong9812.streamix.core.adapter.out.storage;

import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException;
import io.github.junhyeong9812.streamix.core.domain.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 로컬 파일 시스템 기반 저장소 어댑터입니다.
 *
 * <p>{@link FileStoragePort}를 구현하며, 지정된 베이스 디렉토리에 파일을 저장하고 관리합니다.
 * 개발/테스트 환경이나 단일 서버 배포에 적합합니다.</p>
 *
 * <h2>기능</h2>
 * <ul>
 *   <li>파일 저장/로드/삭제</li>
 *   <li>Range 요청을 위한 부분 파일 로드</li>
 *   <li>Path Traversal 공격 방지</li>
 *   <li>자동 디렉토리 생성</li>
 * </ul>
 *
 * <h2>디렉토리 구조</h2>
 * <pre>
 * {basePath}/
 * ├── {uuid}.jpg           # 원본 이미지
 * ├── {uuid}_thumb.jpg     # 썸네일
 * ├── {uuid}.mp4           # 원본 비디오
 * └── {uuid}_thumb.jpg     # 비디오 썸네일
 * </pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 생성자에서 베이스 경로 지정
 * FileStoragePort storage = new LocalFileStorageAdapter("./data/streamix");
 *
 * // 파일 저장
 * String path = storage.save("video.mp4", inputStream, size);
 *
 * // 파일 로드
 * InputStream is = storage.load(path);
 *
 * // Range 로드
 * InputStream partial = storage.loadPartial(path, 0, 1023);
 * }</pre>
 *
 * <h2>보안</h2>
 * <p>Path Traversal 공격(예: "../../../etc/passwd")을 방지하기 위해
 * 모든 경로는 베이스 디렉토리 내로 제한됩니다.</p>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileStoragePort
 */
public class LocalFileStorageAdapter implements FileStoragePort {

  private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

  private final Path basePath;

  /**
   * 문자열 경로로 어댑터를 생성합니다.
   *
   * <p>경로가 존재하지 않으면 자동으로 생성됩니다.</p>
   *
   * @param basePath 파일 저장 베이스 경로
   * @throws StorageException 디렉토리 생성 실패 시
   */
  public LocalFileStorageAdapter(String basePath) {
    this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    initializeDirectory();
  }

  /**
   * Path 객체로 어댑터를 생성합니다.
   *
   * <p>경로가 존재하지 않으면 자동으로 생성됩니다.</p>
   *
   * @param basePath 파일 저장 베이스 경로
   * @throws StorageException 디렉토리 생성 실패 시
   */
  public LocalFileStorageAdapter(Path basePath) {
    this.basePath = basePath.toAbsolutePath().normalize();
    initializeDirectory();
  }

  /**
   * 베이스 디렉토리를 초기화합니다.
   */
  private void initializeDirectory() {
    try {
      if (!Files.exists(basePath)) {
        Files.createDirectories(basePath);
        log.info("Created storage directory: {}", basePath);
      }
    } catch (IOException e) {
      throw StorageException.directoryCreationFailed(basePath.toString(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>파일명에 경로가 포함되어 있으면 해당 서브디렉토리도 생성합니다.</p>
   */
  @Override
  public String save(String fileName, InputStream inputStream, long size) {
    Path filePath = resolveAndValidatePath(fileName);

    try {
      // 부모 디렉토리 생성
      Files.createDirectories(filePath.getParent());

      // 파일 저장
      Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

      log.debug("File saved: {}", filePath);
      return filePath.toString();

    } catch (IOException e) {
      throw StorageException.saveFailed(fileName, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream load(String storagePath) {
    Path filePath = Path.of(storagePath);

    if (!Files.exists(filePath)) {
      throw new FileNotFoundException(storagePath);
    }

    try {
      return Files.newInputStream(filePath);
    } catch (IOException e) {
      throw StorageException.loadFailed(storagePath, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@link RandomAccessFile}을 사용하여 지정된 범위만 읽습니다.</p>
   */
  @Override
  public InputStream loadPartial(String storagePath, long start, long end) {
    Path filePath = Path.of(storagePath);

    if (!Files.exists(filePath)) {
      throw new FileNotFoundException(storagePath);
    }

    try {
      return new RangeInputStream(filePath, start, end);
    } catch (IOException e) {
      throw StorageException.loadFailed(storagePath, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>파일이 존재하지 않아도 예외를 발생시키지 않습니다 (멱등성).</p>
   */
  @Override
  public void delete(String storagePath) {
    Path filePath = Path.of(storagePath);

    try {
      if (Files.exists(filePath)) {
        Files.delete(filePath);
        log.debug("File deleted: {}", filePath);
      }
    } catch (IOException e) {
      throw StorageException.deleteFailed(storagePath, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean exists(String storagePath) {
    return Files.exists(Path.of(storagePath));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize(String storagePath) {
    Path filePath = Path.of(storagePath);

    if (!Files.exists(filePath)) {
      throw new FileNotFoundException(storagePath);
    }

    try {
      return Files.size(filePath);
    } catch (IOException e) {
      throw StorageException.loadFailed(storagePath, e);
    }
  }

  /**
   * 경로를 검증하고 절대 경로로 해석합니다.
   *
   * <p>Path Traversal 공격을 방지합니다.</p>
   *
   * @param fileName 파일명 (경로 포함 가능)
   * @return 검증된 절대 경로
   * @throws IllegalArgumentException 베이스 디렉토리 외부로의 접근 시도 시
   */
  private Path resolveAndValidatePath(String fileName) {
    Path resolved = basePath.resolve(fileName).normalize();

    // Path Traversal 공격 방지
    if (!resolved.startsWith(basePath)) {
      throw new IllegalArgumentException("Invalid file path: " + fileName);
    }

    return resolved;
  }

  /**
   * Range 요청을 위한 InputStream 구현체.
   *
   * <p>{@link RandomAccessFile}을 사용하여 파일의 특정 범위만 읽습니다.
   * HTTP Range 요청 처리에 사용됩니다.</p>
   */
  private static class RangeInputStream extends InputStream {

    private final RandomAccessFile file;
    private final long end;
    private long position;

    /**
     * RangeInputStream을 생성합니다.
     *
     * @param path  파일 경로
     * @param start 시작 바이트 (inclusive)
     * @param end   종료 바이트 (inclusive)
     * @throws IOException 파일 열기 실패 시
     */
    public RangeInputStream(Path path, long start, long end) throws IOException {
      this.file = new RandomAccessFile(path.toFile(), "r");
      this.file.seek(start);
      this.position = start;
      this.end = end;
    }

    @Override
    public int read() throws IOException {
      if (position > end) {
        return -1;
      }
      position++;
      return file.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (position > end) {
        return -1;
      }

      long remaining = end - position + 1;
      int toRead = (int) Math.min(len, remaining);

      int bytesRead = file.read(b, off, toRead);
      if (bytesRead > 0) {
        position += bytesRead;
      }

      return bytesRead;
    }

    @Override
    public void close() throws IOException {
      file.close();
    }
  }
}