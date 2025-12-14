package io.github.junhyeong9812.streamix.core.application.port.out;

import java.io.InputStream;

/**
 * 파일 저장소 포트 인터페이스입니다 (Output Port / SPI).
 *
 * <p>헥사고날 아키텍처의 Output Port로, 실제 파일 저장소와의 상호작용을 정의합니다.
 * Driven Adapter(예: LocalFileStorageAdapter, S3StorageAdapter)가 이 인터페이스를 구현합니다.</p>
 *
 * <h2>구현체 예시</h2>
 * <ul>
 *   <li>{@link io.github.junhyeong9812.streamix.core.adapter.out.storage.LocalFileStorageAdapter}
 *       - 로컬 파일 시스템</li>
 *   <li>S3StorageAdapter - AWS S3 (별도 구현 필요)</li>
 *   <li>GcsStorageAdapter - Google Cloud Storage (별도 구현 필요)</li>
 * </ul>
 *
 * <h2>커스텀 구현 예시</h2>
 * <pre>{@code
 * @Component
 * public class S3StorageAdapter implements FileStoragePort {
 *     private final S3Client s3Client;
 *     private final String bucketName;
 *
 *     @Override
 *     public String save(String fileName, InputStream inputStream, long size) {
 *         s3Client.putObject(PutObjectRequest.builder()
 *             .bucket(bucketName)
 *             .key(fileName)
 *             .build(), RequestBody.fromInputStream(inputStream, size));
 *         return "s3://" + bucketName + "/" + fileName;
 *     }
 *
 *     // ... 나머지 메서드 구현
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see io.github.junhyeong9812.streamix.core.adapter.out.storage.LocalFileStorageAdapter
 */
public interface FileStoragePort {

  /**
   * 파일을 저장합니다.
   *
   * @param fileName    저장할 파일명 (경로 포함 가능)
   * @param inputStream 파일 데이터 스트림
   * @param size        파일 크기 (바이트)
   * @return 저장된 파일의 경로 또는 식별자
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         저장 실패 시
   */
  String save(String fileName, InputStream inputStream, long size);

  /**
   * 파일 전체를 로드합니다.
   *
   * @param storagePath 저장 경로
   * @return 파일 데이터 스트림
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         로드 실패 시
   */
  InputStream load(String storagePath);

  /**
   * 파일의 일부를 로드합니다 (Range 요청용).
   *
   * <p>start와 end는 inclusive입니다. 예: loadPartial(path, 0, 99)는 처음 100바이트.</p>
   *
   * @param storagePath 저장 경로
   * @param start       시작 바이트 (0-based, inclusive)
   * @param end         종료 바이트 (inclusive)
   * @return 지정된 범위의 데이터 스트림
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         로드 실패 시
   */
  InputStream loadPartial(String storagePath, long start, long end);

  /**
   * 파일을 삭제합니다.
   *
   * <p>파일이 존재하지 않아도 예외를 발생시키지 않습니다 (멱등성).</p>
   *
   * @param storagePath 삭제할 파일의 경로
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.StorageException
   *         삭제 실패 시
   */
  void delete(String storagePath);

  /**
   * 파일 존재 여부를 확인합니다.
   *
   * @param storagePath 확인할 파일의 경로
   * @return 파일이 존재하면 {@code true}
   */
  boolean exists(String storagePath);

  /**
   * 파일 크기를 조회합니다.
   *
   * @param storagePath 파일 경로
   * @return 파일 크기 (바이트)
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   */
  long getSize(String storagePath);
}