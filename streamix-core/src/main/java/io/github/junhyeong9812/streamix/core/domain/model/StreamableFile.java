package io.github.junhyeong9812.streamix.core.domain.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 스트리밍 가능한 파일을 표현하는 불변 레코드입니다.
 *
 * <p>HTTP Range 요청을 지원하여 전체 파일 또는 부분 콘텐츠를 스트리밍할 수 있습니다.
 * {@link Closeable}을 구현하여 try-with-resources 문에서 사용할 수 있습니다.</p>
 *
 * <h2>Range 요청 지원</h2>
 * <p>비디오 스트리밍에서 사용되는 HTTP Range 요청을 지원합니다:</p>
 * <ul>
 *   <li><b>전체 요청</b>: Range 헤더 없음 → 200 OK</li>
 *   <li><b>부분 요청</b>: Range: bytes=0-1023 → 206 Partial Content</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * // 전체 파일 스트리밍
 * StreamableFile file = StreamableFile.full(metadata, inputStream);
 *
 * // 부분 파일 스트리밍 (Range 요청)
 * StreamableFile partial = StreamableFile.partial(metadata, inputStream, 0, 1023);
 *
 * // try-with-resources 사용
 * try (StreamableFile file = streamService.stream(command)) {
 *     file.inputStream().transferTo(outputStream);
 * }
 * }</pre>
 *
 * @param metadata      파일 메타데이터
 * @param inputStream   파일 데이터 스트림 (전체 또는 Range 해당 부분)
 * @param contentLength 콘텐츠 길이 (바이트). 전체 요청시 파일 크기, Range 요청시 범위 크기
 * @param rangeStart    Range 시작 바이트 (0-based). 전체 요청시 null
 * @param rangeEnd      Range 종료 바이트 (inclusive). 전체 요청시 null
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see io.github.junhyeong9812.streamix.core.application.port.in.StreamFileUseCase
 */
public record StreamableFile(
    FileMetadata metadata,
    InputStream inputStream,
    long contentLength,
    Long rangeStart,
    Long rangeEnd
) implements Closeable {

  /**
   * Compact Constructor - 유효성 검증을 수행합니다.
   *
   * @throws NullPointerException     metadata나 inputStream이 null인 경우
   * @throws IllegalArgumentException contentLength가 음수인 경우
   */
  public StreamableFile {
    Objects.requireNonNull(metadata, "metadata must not be null");
    Objects.requireNonNull(inputStream, "inputStream must not be null");

    if (contentLength < 0) {
      throw new IllegalArgumentException("contentLength must be positive");
    }
  }

  /**
   * 부분 콘텐츠(Range 요청) 여부를 확인합니다.
   *
   * @return Range 요청이면 {@code true}
   */
  public boolean isPartialContent() {
    return rangeStart != null;
  }

  /**
   * Content-Range 헤더 값을 생성합니다.
   *
   * <p>형식: "bytes {start}-{end}/{total}"</p>
   * <p>예시: "bytes 0-1023/15728640"</p>
   *
   * @return Content-Range 헤더 값, 전체 요청이면 {@code null}
   */
  public String getContentRange() {
    if (!isPartialContent()) {
      return null;
    }
    return String.format("bytes %d-%d/%d", rangeStart, rangeEnd, metadata.size());
  }

  /**
   * 파일의 Content-Type을 반환합니다.
   *
   * @return MIME Content-Type (예: "video/mp4", "image/jpeg")
   */
  public String getContentType() {
    return metadata.contentType();
  }

  /**
   * 전체 파일 스트리밍용 StreamableFile을 생성합니다.
   *
   * <p>HTTP 200 OK 응답에 사용됩니다.</p>
   *
   * @param metadata    파일 메타데이터
   * @param inputStream 파일 전체 데이터 스트림
   * @return 전체 파일을 담은 StreamableFile
   */
  public static StreamableFile full(FileMetadata metadata, InputStream inputStream) {
    return new StreamableFile(
        metadata,
        inputStream,
        metadata.size(),
        null,
        null
    );
  }

  /**
   * 부분 콘텐츠(Range) 스트리밍용 StreamableFile을 생성합니다.
   *
   * <p>HTTP 206 Partial Content 응답에 사용됩니다.</p>
   *
   * @param metadata    파일 메타데이터
   * @param inputStream Range에 해당하는 부분 데이터 스트림
   * @param rangeStart  시작 바이트 (0-based, inclusive)
   * @param rangeEnd    종료 바이트 (inclusive)
   * @return 부분 콘텐츠를 담은 StreamableFile
   */
  public static StreamableFile partial(
      FileMetadata metadata,
      InputStream inputStream,
      long rangeStart,
      long rangeEnd
  ) {
    long contentLength = rangeEnd - rangeStart + 1;
    return new StreamableFile(
        metadata,
        inputStream,
        contentLength,
        rangeStart,
        rangeEnd
    );
  }

  /**
   * 내부 InputStream을 닫습니다.
   *
   * <p>try-with-resources 문에서 자동으로 호출됩니다.</p>
   *
   * @throws IOException 스트림 닫기 실패 시
   */
  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }
}