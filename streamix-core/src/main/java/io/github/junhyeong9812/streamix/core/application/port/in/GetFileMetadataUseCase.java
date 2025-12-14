package io.github.junhyeong9812.streamix.core.application.port.in;

import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;

import java.util.List;
import java.util.UUID;

/**
 * 파일 메타데이터 조회 유스케이스 인터페이스입니다.
 *
 * <p>헥사고날 아키텍처의 Input Port로, 파일 메타데이터 조회 기능을 정의합니다.
 * 단건 조회와 목록 조회(페이징)를 지원합니다.</p>
 *
 * <h2>조회 가능한 정보</h2>
 * <ul>
 *   <li>파일 ID, 원본 파일명</li>
 *   <li>파일 타입 (IMAGE/VIDEO)</li>
 *   <li>Content-Type, 파일 크기</li>
 *   <li>저장 경로, 썸네일 경로</li>
 *   <li>생성/수정 시각</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @GetMapping("/files/{id}")
 * public FileMetadata getFile(@PathVariable UUID id) {
 *     return getFileMetadataUseCase.getById(id);
 * }
 *
 * @GetMapping("/files")
 * public List<FileMetadata> listFiles(
 *         @RequestParam(defaultValue = "0") int page,
 *         @RequestParam(defaultValue = "20") int size) {
 *     return getFileMetadataUseCase.getAll(page, size);
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see FileMetadata
 * @see io.github.junhyeong9812.streamix.core.application.service.FileMetadataService
 */
public interface GetFileMetadataUseCase {

  /**
   * ID로 파일 메타데이터를 조회합니다.
   *
   * @param fileId 파일 ID
   * @return 파일 메타데이터
   * @throws io.github.junhyeong9812.streamix.core.domain.exception.FileNotFoundException
   *         파일이 존재하지 않는 경우
   */
  FileMetadata getById(UUID fileId);

  /**
   * 전체 파일 목록을 페이징하여 조회합니다.
   *
   * <p>생성일 역순(최신순)으로 정렬됩니다.</p>
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기 (1~100)
   * @return 파일 메타데이터 목록
   * @throws IllegalArgumentException page가 음수이거나 size가 범위를 벗어난 경우
   */
  List<FileMetadata> getAll(int page, int size);
}