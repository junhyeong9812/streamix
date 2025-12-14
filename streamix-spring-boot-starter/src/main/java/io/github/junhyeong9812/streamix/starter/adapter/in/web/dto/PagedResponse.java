package io.github.junhyeong9812.streamix.starter.adapter.in.web.dto;

import java.util.List;

/**
 * 페이징된 목록 응답을 위한 제네릭 DTO입니다.
 *
 * <p>목록 조회 API에서 페이징 정보와 함께 데이터를 반환할 때 사용합니다.
 * 타입 파라미터 {@code T}를 통해 다양한 타입의 목록을 감쌀 수 있습니다.</p>
 *
 * <h2>응답 필드</h2>
 * <table border="1">
 *   <tr><th>필드</th><th>타입</th><th>설명</th></tr>
 *   <tr><td>content</td><td>List&lt;T&gt;</td><td>데이터 목록</td></tr>
 *   <tr><td>page</td><td>int</td><td>현재 페이지 번호 (0부터 시작)</td></tr>
 *   <tr><td>size</td><td>int</td><td>페이지 크기</td></tr>
 *   <tr><td>totalElements</td><td>long</td><td>전체 항목 수</td></tr>
 *   <tr><td>totalPages</td><td>int</td><td>전체 페이지 수</td></tr>
 *   <tr><td>hasNext</td><td>boolean</td><td>다음 페이지 존재 여부</td></tr>
 *   <tr><td>hasPrevious</td><td>boolean</td><td>이전 페이지 존재 여부</td></tr>
 * </table>
 *
 * <h2>JSON 응답 예시</h2>
 * <pre>{@code
 * {
 *   "content": [
 *     { "id": "...", "originalName": "photo1.jpg", ... },
 *     { "id": "...", "originalName": "video1.mp4", ... }
 *   ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "hasNext": true,
 *   "hasPrevious": false
 * }
 * }</pre>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @GetMapping("/files")
 * public PagedResponse<FileInfoResponse> listFiles(
 *         @RequestParam(defaultValue = "0") int page,
 *         @RequestParam(defaultValue = "20") int size) {
 *
 *     List<FileMetadata> files = getFileMetadataUseCase.getAll(page, size);
 *     long total = fileMetadataPort.count();
 *
 *     List<FileInfoResponse> content = files.stream()
 *         .map(m -> FileInfoResponse.from(m, apiBasePath))
 *         .toList();
 *
 *     return PagedResponse.of(content, page, size, total);
 * }
 * }</pre>
 *
 * @param <T> 목록 항목의 타입
 * @author junhyeong9812
 * @since 1.0.0
 */
public record PagedResponse<T>(
    /**
     * 데이터 목록.
     *
     * <p>현재 페이지에 해당하는 항목들입니다.</p>
     */
    List<T> content,

    /**
     * 현재 페이지 번호 (0부터 시작).
     */
    int page,

    /**
     * 페이지 크기 (한 페이지당 항목 수).
     */
    int size,

    /**
     * 전체 항목 수.
     */
    long totalElements,

    /**
     * 전체 페이지 수.
     */
    int totalPages,

    /**
     * 다음 페이지 존재 여부.
     */
    boolean hasNext,

    /**
     * 이전 페이지 존재 여부.
     */
    boolean hasPrevious
) {
  /**
   * 페이징된 응답을 생성합니다.
   *
   * <p>전체 항목 수와 페이지 크기를 기반으로 전체 페이지 수,
   * 다음/이전 페이지 존재 여부를 자동으로 계산합니다.</p>
   *
   * @param content       데이터 목록
   * @param page          현재 페이지 번호 (0부터 시작)
   * @param size          페이지 크기
   * @param totalElements 전체 항목 수
   * @param <T>           목록 항목의 타입
   * @return 페이징된 응답 DTO
   */
  public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    boolean hasNext = page < totalPages - 1;
    boolean hasPrevious = page > 0;

    return new PagedResponse<>(
        content,
        page,
        size,
        totalElements,
        totalPages,
        hasNext,
        hasPrevious
    );
  }

  /**
   * 빈 응답을 생성합니다.
   *
   * <p>데이터가 없을 때 사용합니다.</p>
   *
   * @param page 현재 페이지 번호
   * @param size 페이지 크기
   * @param <T>  목록 항목의 타입
   * @return 빈 페이징 응답
   */
  public static <T> PagedResponse<T> empty(int page, int size) {
    return new PagedResponse<>(
        List.of(),
        page,
        size,
        0,
        0,
        false,
        false
    );
  }
}