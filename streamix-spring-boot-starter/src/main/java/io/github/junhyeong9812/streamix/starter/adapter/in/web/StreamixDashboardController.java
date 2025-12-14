package io.github.junhyeong9812.streamix.starter.adapter.in.dashboard;

import io.github.junhyeong9812.streamix.core.application.port.in.DeleteFileUseCase;
import io.github.junhyeong9812.streamix.core.application.port.in.GetFileMetadataUseCase;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.domain.model.FileMetadata;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import io.github.junhyeong9812.streamix.starter.service.StreamingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Streamix 대시보드 컨트롤러입니다.
 *
 * <p>파일 관리, 스트리밍 모니터링, 통계를 위한 웹 UI를 제공합니다.
 * Thymeleaf 템플릿 엔진을 사용하여 뷰를 렌더링합니다.</p>
 *
 * <h2>제공 페이지</h2>
 * <table border="1">
 *   <caption>대시보드 페이지 목록</caption>
 *   <tr><th>경로</th><th>설명</th></tr>
 *   <tr><td>/streamix</td><td>메인 대시보드 (통계 요약)</td></tr>
 *   <tr><td>/streamix/files</td><td>파일 목록</td></tr>
 *   <tr><td>/streamix/files/{id}</td><td>파일 상세 정보</td></tr>
 *   <tr><td>/streamix/sessions</td><td>스트리밍 세션 목록</td></tr>
 * </table>
 *
 * <h2>템플릿 위치</h2>
 * <p>템플릿 파일은 {@code classpath:/templates/streamix/} 아래에 위치합니다:</p>
 * <ul>
 *   <li>dashboard.html - 메인 대시보드</li>
 *   <li>files.html - 파일 목록</li>
 *   <li>file-detail.html - 파일 상세</li>
 *   <li>sessions.html - 세션 목록</li>
 * </ul>
 *
 * <h2>설정</h2>
 * <pre>{@code
 * streamix:
 *   dashboard:
 *     enabled: true     # 대시보드 활성화 (기본: true)
 *     path: /streamix   # 대시보드 경로 (기본: /streamix)
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see StreamingMonitoringService
 */
@Controller
public class StreamixDashboardController {

  private static final Logger log = LoggerFactory.getLogger(StreamixDashboardController.class);

  private final StreamingMonitoringService monitoringService;
  private final GetFileMetadataUseCase getFileMetadataUseCase;
  private final DeleteFileUseCase deleteFileUseCase;
  private final FileMetadataPort fileMetadataPort;
  private final StreamixProperties properties;

  /**
   * StreamixDashboardController를 생성합니다.
   *
   * @param monitoringService      모니터링 서비스
   * @param getFileMetadataUseCase 메타데이터 조회 유스케이스
   * @param deleteFileUseCase      파일 삭제 유스케이스
   * @param fileMetadataPort       메타데이터 포트
   * @param properties             Streamix 설정
   */
  public StreamixDashboardController(
      StreamingMonitoringService monitoringService,
      GetFileMetadataUseCase getFileMetadataUseCase,
      DeleteFileUseCase deleteFileUseCase,
      FileMetadataPort fileMetadataPort,
      StreamixProperties properties
  ) {
    this.monitoringService = monitoringService;
    this.getFileMetadataUseCase = getFileMetadataUseCase;
    this.deleteFileUseCase = deleteFileUseCase;
    this.fileMetadataPort = fileMetadataPort;
    this.properties = properties;
  }

  /**
   * 메인 대시보드 페이지를 렌더링합니다.
   *
   * <p>통계 요약, 최근 파일, 활성 세션 등을 표시합니다.</p>
   *
   * @param model 뷰 모델
   * @return 템플릿 이름
   */
  @GetMapping("${streamix.dashboard.path:/streamix}")
  public String dashboard(Model model) {
    log.debug("Rendering dashboard");

    // 통계 데이터
    var stats = monitoringService.getDashboardStats();
    model.addAttribute("stats", stats);

    // 파일 통계
    long totalFiles = fileMetadataPort.count();
    model.addAttribute("totalFiles", totalFiles);

    // 최근 파일 (5개)
    var recentFiles = getFileMetadataUseCase.getAll(0, 5);
    model.addAttribute("recentFiles", recentFiles);

    // 활성 세션
    var activeSessions = monitoringService.getActiveSessions();
    model.addAttribute("activeSessions", activeSessions);

    // 최근 세션 (10개)
    var recentSessions = monitoringService.getRecentSessions(10);
    model.addAttribute("recentSessions", recentSessions);

    // 인기 파일 (5개)
    var popularFiles = monitoringService.getMostStreamedFiles(5);
    model.addAttribute("popularFiles", popularFiles);

    // API 기본 경로
    model.addAttribute("apiBasePath", properties.api().basePath());

    return "streamix/dashboard";
  }

  /**
   * 파일 목록 페이지를 렌더링합니다.
   *
   * @param page  페이지 번호 (기본: 0)
   * @param size  페이지 크기 (기본: 20)
   * @param model 뷰 모델
   * @return 템플릿 이름
   */
  @GetMapping("${streamix.dashboard.path:/streamix}/files")
  public String fileList(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      Model model
  ) {
    log.debug("Rendering file list: page={}, size={}", page, size);

    List<FileMetadata> files = getFileMetadataUseCase.getAll(page, size);
    long totalElements = fileMetadataPort.count();
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

    model.addAttribute("files", files);
    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", size);
    model.addAttribute("totalElements", totalElements);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("apiBasePath", properties.api().basePath());

    return "streamix/files";
  }

  /**
   * 파일 상세 정보 페이지를 렌더링합니다.
   *
   * @param id    파일 ID
   * @param model 뷰 모델
   * @return 템플릿 이름
   */
  @GetMapping("${streamix.dashboard.path:/streamix}/files/{id}")
  public String fileDetail(
      @PathVariable(name = "id") UUID id,
      Model model
  ) {
    log.debug("Rendering file detail: id={}", id);

    FileMetadata file = getFileMetadataUseCase.getById(id);
    model.addAttribute("file", file);

    // 파일별 스트리밍 통계
    var fileStats = monitoringService.getFileStats(id);
    model.addAttribute("fileStats", fileStats);

    model.addAttribute("apiBasePath", properties.api().basePath());

    return "streamix/file-detail";
  }

  /**
   * 스트리밍 세션 목록 페이지를 렌더링합니다.
   *
   * @param limit 조회 개수 (기본: 50)
   * @param model 뷰 모델
   * @return 템플릿 이름
   */
  @GetMapping("${streamix.dashboard.path:/streamix}/sessions")
  public String sessionList(
      @RequestParam(name = "limit", defaultValue = "50") int limit,
      Model model
  ) {
    log.debug("Rendering session list: limit={}", limit);

    var sessions = monitoringService.getRecentSessions(limit);
    model.addAttribute("sessions", sessions);

    var activeSessions = monitoringService.getActiveSessions();
    model.addAttribute("activeSessions", activeSessions);

    var stats = monitoringService.getDashboardStats();
    model.addAttribute("stats", stats);

    return "streamix/sessions";
  }

  /**
   * 파일을 삭제합니다.
   *
   * @param id                 파일 ID
   * @param redirectAttributes 리다이렉트 속성
   * @return 리다이렉트 URL
   */
  @PostMapping("${streamix.dashboard.path:/streamix}/files/{id}/delete")
  public String deleteFile(
      @PathVariable(name = "id") UUID id,
      RedirectAttributes redirectAttributes
  ) {
    log.info("Deleting file from dashboard: id={}", id);

    try {
      deleteFileUseCase.delete(id);
      redirectAttributes.addFlashAttribute("successMessage", "파일이 삭제되었습니다.");
    } catch (Exception e) {
      log.error("Failed to delete file: id={}", id, e);
      redirectAttributes.addFlashAttribute("errorMessage", "파일 삭제 실패: " + e.getMessage());
    }

    return "redirect:" + properties.dashboard().path() + "/files";
  }
}