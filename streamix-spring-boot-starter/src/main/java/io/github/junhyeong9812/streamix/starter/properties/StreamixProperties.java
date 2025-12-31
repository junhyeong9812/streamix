package io.github.junhyeong9812.streamix.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Streamix 설정 프로퍼티를 담는 불변 레코드입니다.
 *
 * <p>Spring Boot의 {@code @ConfigurationProperties}를 사용하여
 * {@code streamix.*} 접두사의 설정값을 바인딩합니다.
 * 모든 설정은 합리적인 기본값을 가지며, 필요에 따라 오버라이드할 수 있습니다.</p>
 *
 * <h2>설정 계층 구조</h2>
 * <pre>
 * streamix
 * ├── storage              # 파일 저장소 설정
 * │   ├── base-path        # 저장 경로 (기본: ./streamix-data)
 * │   ├── max-file-size    # 최대 파일 크기 (기본: 100MB)
 * │   └── allowed-types    # 허용 파일 타입 (기본: 전체 허용)
 * ├── thumbnail            # 썸네일 설정
 * │   ├── enabled          # 활성화 여부 (기본: true)
 * │   ├── width            # 너비 (기본: 320)
 * │   ├── height           # 높이 (기본: 180)
 * │   └── ffmpeg-path      # FFmpeg 경로 (기본: ffmpeg)
 * ├── api                  # REST API 설정
 * │   ├── enabled          # 활성화 여부 (기본: true)
 * │   └── base-path        # API 경로 (기본: /api/streamix)
 * └── dashboard            # 대시보드 설정
 *     ├── enabled          # 활성화 여부 (기본: true)
 *     └── path             # 대시보드 경로 (기본: /streamix)
 * </pre>
 *
 * <h2>YAML 설정 예시</h2>
 * <pre>{@code
 * streamix:
 *   storage:
 *     base-path: /var/lib/streamix/uploads
 *     max-file-size: 524288000  # 500MB
 *     allowed-types: IMAGE,VIDEO,DOCUMENT  # 빈 값 = 전체 허용
 *   thumbnail:
 *     enabled: true
 *     width: 320
 *     height: 180
 *     ffmpeg-path: /usr/bin/ffmpeg
 *   api:
 *     enabled: true
 *     base-path: /api/v1/media
 *   dashboard:
 *     enabled: false  # 프로덕션에서 비활성화
 * }</pre>
 *
 * <h2>프로퍼티 파일 설정 예시</h2>
 * <pre>{@code
 * streamix.storage.base-path=/var/lib/streamix/uploads
 * streamix.storage.max-file-size=524288000
 * streamix.storage.allowed-types=IMAGE,VIDEO
 * streamix.thumbnail.enabled=true
 * streamix.thumbnail.width=320
 * streamix.thumbnail.height=180
 * streamix.api.enabled=true
 * streamix.api.base-path=/api/v1/media
 * }</pre>
 *
 * <h2>환경 변수 설정</h2>
 * <p>Spring Boot의 relaxed binding을 통해 환경 변수로도 설정 가능합니다:</p>
 * <pre>{@code
 * STREAMIX_STORAGE_BASE_PATH=/data/uploads
 * STREAMIX_STORAGE_MAX_FILE_SIZE=524288000
 * STREAMIX_STORAGE_ALLOWED_TYPES=IMAGE,VIDEO
 * STREAMIX_THUMBNAIL_ENABLED=true
 * }</pre>
 *
 * @param storage   파일 저장소 설정
 * @param thumbnail 썸네일 생성 설정
 * @param api       REST API 설정
 * @param dashboard 대시보드 UI 설정
 * @author junhyeong9812
 * @since 1.0.0
 * @see io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixAutoConfiguration
 */
@ConfigurationProperties(prefix = "streamix")
public record StreamixProperties(
    Storage storage,
    Thumbnail thumbnail,
    Api api,
    Dashboard dashboard
) {
  /**
   * Compact Constructor - null 값에 기본값을 적용합니다.
   */
  public StreamixProperties {
    storage = storage != null ? storage : new Storage(null, 0, null);
    thumbnail = thumbnail != null ? thumbnail : new Thumbnail(true, 0, 0, null);
    api = api != null ? api : new Api(true, null);
    dashboard = dashboard != null ? dashboard : new Dashboard(true, null);
  }

  /**
   * 파일 저장소 관련 설정을 담는 레코드입니다.
   *
   * <p>업로드된 파일과 썸네일이 저장되는 위치와 제한을 설정합니다.</p>
   *
   * <h2>설정 항목</h2>
   * <table border="1">
   *   <caption>Storage 설정 항목</caption>
   *   <tr><th>속성</th><th>기본값</th><th>설명</th></tr>
   *   <tr><td>base-path</td><td>./streamix-data</td><td>파일 저장 기본 경로</td></tr>
   *   <tr><td>max-file-size</td><td>104857600 (100MB)</td><td>최대 업로드 파일 크기 (바이트)</td></tr>
   *   <tr><td>allowed-types</td><td>(빈 값 = 전체 허용)</td><td>허용 파일 타입 (콤마 구분)</td></tr>
   * </table>
   *
   * <h2>허용 파일 타입</h2>
   * <p>다음 값들을 콤마로 구분하여 지정할 수 있습니다:</p>
   * <ul>
   *   <li>IMAGE - 이미지 파일 (jpg, png, gif, webp 등)</li>
   *   <li>VIDEO - 비디오 파일 (mp4, webm, avi 등)</li>
   *   <li>AUDIO - 오디오 파일 (mp3, wav, flac 등)</li>
   *   <li>DOCUMENT - 문서 파일 (pdf, doc, xlsx 등)</li>
   *   <li>ARCHIVE - 압축 파일 (zip, tar, gz 등)</li>
   *   <li>OTHER - 기타 파일</li>
   * </ul>
   *
   * @param basePath     파일 저장 기본 경로 (상대/절대 경로 모두 가능)
   * @param maxFileSize  최대 업로드 파일 크기 (바이트 단위)
   * @param allowedTypes 허용 파일 타입 (콤마 구분, 빈 값이면 전체 허용)
   * @since 1.0.7 allowedTypes 추가
   */
  public record Storage(
      String basePath,
      long maxFileSize,
      Set<String> allowedTypes
  ) {
    /**
     * Compact Constructor - 기본값을 적용합니다.
     */
    public Storage {
      basePath = basePath != null && !basePath.isBlank() ? basePath : "./streamix-data";
      maxFileSize = maxFileSize > 0 ? maxFileSize : 104857600L; // 100MB
      allowedTypes = allowedTypes != null ? Set.copyOf(allowedTypes) : Set.of();
    }

    /**
     * 절대 경로로 변환된 저장소 경로를 반환합니다.
     *
     * <p>상대 경로인 경우 현재 작업 디렉토리({@code user.dir})를 기준으로 절대 경로를 생성합니다.
     * 절대 경로이거나 Windows 드라이브 문자가 포함된 경우 그대로 반환합니다.</p>
     *
     * @return 절대 경로로 변환된 저장소 경로
     */
    public String getResolvedBasePath() {
      // 이미 절대 경로이거나 Windows 드라이브 문자 포함 시 그대로 반환
      if (basePath.startsWith("/") || basePath.contains(":")) {
        return basePath;
      }
      return System.getProperty("user.dir") + "/" + basePath;
    }

    /**
     * 모든 파일 타입이 허용되는지 확인합니다.
     *
     * @return 빈 Set이면 true (전체 허용)
     * @since 1.0.7
     */
    public boolean isAllTypesAllowed() {
      return allowedTypes.isEmpty();
    }
  }

  /**
   * 썸네일 생성 관련 설정을 담는 레코드입니다.
   *
   * <p>이미지와 비디오의 썸네일 생성 동작을 제어합니다.
   * 비디오 썸네일 생성에는 시스템에 FFmpeg가 설치되어 있어야 합니다.</p>
   *
   * <h2>설정 항목</h2>
   * <table border="1">
   *   <caption>Thumbnail 설정 항목</caption>
   *   <tr><th>속성</th><th>기본값</th><th>설명</th></tr>
   *   <tr><td>enabled</td><td>true</td><td>썸네일 생성 활성화 여부</td></tr>
   *   <tr><td>width</td><td>320</td><td>썸네일 너비 (픽셀)</td></tr>
   *   <tr><td>height</td><td>180</td><td>썸네일 높이 (픽셀)</td></tr>
   *   <tr><td>ffmpeg-path</td><td>ffmpeg</td><td>FFmpeg 실행 파일 경로</td></tr>
   * </table>
   *
   * <h2>FFmpeg 설치</h2>
   * <p>비디오 썸네일 생성을 위해서는 FFmpeg가 필요합니다:</p>
   * <ul>
   *   <li>Ubuntu/Debian: {@code sudo apt install ffmpeg}</li>
   *   <li>macOS: {@code brew install ffmpeg}</li>
   *   <li>Windows: FFmpeg 다운로드 후 PATH에 추가</li>
   * </ul>
   *
   * @param enabled    썸네일 생성 활성화 여부
   * @param width      썸네일 너비 (픽셀)
   * @param height     썸네일 높이 (픽셀)
   * @param ffmpegPath FFmpeg 실행 파일 경로 (PATH에 있으면 "ffmpeg")
   */
  public record Thumbnail(
      boolean enabled,
      int width,
      int height,
      String ffmpegPath
  ) {
    /**
     * Compact Constructor - 기본값을 적용합니다.
     */
    public Thumbnail {
      width = width > 0 ? width : 320;
      height = height > 0 ? height : 180;
      ffmpegPath = ffmpegPath != null && !ffmpegPath.isBlank() ? ffmpegPath : "ffmpeg";
    }
  }

  /**
   * REST API 관련 설정을 담는 레코드입니다.
   *
   * <p>Streamix가 제공하는 REST API 엔드포인트를 제어합니다.</p>
   *
   * <h2>설정 항목</h2>
   * <table border="1">
   *   <caption>Api 설정 항목</caption>
   *   <tr><th>속성</th><th>기본값</th><th>설명</th></tr>
   *   <tr><td>enabled</td><td>true</td><td>API 활성화 여부</td></tr>
   *   <tr><td>base-path</td><td>/api/streamix</td><td>API 기본 경로</td></tr>
   * </table>
   *
   * <h2>제공되는 엔드포인트</h2>
   * <ul>
   *   <li>{@code POST {base-path}/files} - 파일 업로드</li>
   *   <li>{@code GET {base-path}/files} - 파일 목록 조회</li>
   *   <li>{@code GET {base-path}/files/{id}} - 파일 정보 조회</li>
   *   <li>{@code GET {base-path}/files/{id}/stream} - 파일 스트리밍</li>
   *   <li>{@code GET {base-path}/files/{id}/thumbnail} - 썸네일 조회</li>
   *   <li>{@code DELETE {base-path}/files/{id}} - 파일 삭제</li>
   * </ul>
   *
   * @param enabled  API 활성화 여부
   * @param basePath API 기본 경로 (슬래시로 시작해야 함)
   */
  public record Api(
      boolean enabled,
      String basePath
  ) {
    /**
     * Compact Constructor - 기본값을 적용합니다.
     */
    public Api {
      basePath = basePath != null && !basePath.isBlank() ? basePath : "/api/streamix";
    }
  }

  /**
   * 대시보드 UI 관련 설정을 담는 레코드입니다.
   *
   * <p>웹 기반 관리 대시보드의 활성화 및 경로를 제어합니다.
   * 대시보드에서는 파일 목록 조회, 업로드, 미리보기, 삭제 등이 가능합니다.</p>
   *
   * <h2>설정 항목</h2>
   * <table border="1">
   *   <caption>Dashboard 설정 항목</caption>
   *   <tr><th>속성</th><th>기본값</th><th>설명</th></tr>
   *   <tr><td>enabled</td><td>true</td><td>대시보드 활성화 여부</td></tr>
   *   <tr><td>path</td><td>/streamix</td><td>대시보드 접근 경로</td></tr>
   * </table>
   *
   * <h2>제공되는 페이지</h2>
   * <ul>
   *   <li>{@code {path}} - 메인 대시보드 (통계, 최근 업로드)</li>
   *   <li>{@code {path}/files} - 파일 목록 (그리드/리스트 뷰)</li>
   *   <li>{@code {path}/files/{id}} - 파일 미리보기</li>
   *   <li>{@code {path}/monitor} - 스트리밍 모니터</li>
   * </ul>
   *
   * <h2>보안 고려사항</h2>
   * <p>프로덕션 환경에서는 대시보드를 비활성화하거나
   * Spring Security로 인증을 추가하는 것을 권장합니다.</p>
   *
   * @param enabled 대시보드 활성화 여부
   * @param path    대시보드 접근 경로 (슬래시로 시작해야 함)
   */
  public record Dashboard(
      boolean enabled,
      String path
  ) {
    /**
     * Compact Constructor - 기본값을 적용합니다.
     */
    public Dashboard {
      path = path != null && !path.isBlank() ? path : "/streamix";
    }
  }
}