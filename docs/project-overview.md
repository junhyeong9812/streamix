# 프로젝트 개요 (Project Overview)

> Streamix — `@EnableStreamix` 한 줄로 활성화하는 Spring Boot 4 미디어 스트리밍 라이브러리.

## 프로젝트 정보
- **프로젝트명**: streamix
- **모듈 구성**: 멀티 모듈 Gradle (`streamix-core`, `streamix-spring-boot-starter`)
- **현재 버전**: 3.0.0-SNAPSHOT (v3.0.0 대시보드 재설계 완료)
- **기술 스택**: Java 25, Spring Boot 4.0, Spring Framework 7.0, Spring Data JPA 4.0, Thymeleaf 3.1, Thumbnailator 0.4.20, FFmpeg 6.x
- **프론트엔드**: 순수 CSS + ES Modules (외부 의존성 0 — Tailwind/Bootstrap/Lucide/Google Fonts 없음)
- **아키텍처 패턴**: Hexagonal (Ports & Adapters)
- **배포 채널**: Maven Central (`io.github.junhyeong9812:streamix-core`, `streamix-spring-boot-starter`)

## 디렉토리 구조

```
streamix/
├── build.gradle                          # 루트: 공통 toolchain(Java 25), 테스트 의존성
├── settings.gradle                       # 멀티 모듈 등록
├── gradle/wrapper/                       # Gradle 9.2.1
├── .github/workflows/
│   ├── ci.yml                            # PR/푸시 시 build + test
│   └── publish.yml                       # main 푸시 시 Maven Central publish
│
├── streamix-core/                        # 순수 Java, Spring 의존성 없음
│   └── src/main/java/.../core/
│       ├── domain/
│       │   ├── model/                    # FileMetadata, FileType, StreamableFile, UploadResult
│       │   ├── exception/                # sealed StreamixException 계층 5종
│       │   └── service/                  # FileTypeDetector
│       ├── application/
│       │   ├── port/in/                  # UseCase 인터페이스 5종 (Upload/Stream/Delete/GetMetadata/GetThumbnail)
│       │   ├── port/out/                 # FileMetadataPort, FileStoragePort, ThumbnailGeneratorPort
│       │   └── service/                  # 5개 Service (Upload/Stream/Delete/Metadata/Thumbnail)
│       └── adapter/out/
│           ├── metadata/                 # InMemoryMetadataAdapter
│           ├── storage/                  # LocalFileStorageAdapter (Range stream 포함)
│           └── thumbnail/                # ImageThumbnailAdapter (Thumbnailator)
│
├── streamix-spring-boot-starter/         # Spring Boot 자동 설정
│   └── src/main/
│       ├── java/.../starter/
│       │   ├── annotation/               # @EnableStreamix
│       │   ├── autoconfigure/            # 6개 *Configuration
│       │   ├── properties/               # StreamixProperties (record)
│       │   ├── adapter/in/web/           # REST Controller, Dashboard Controller, GlobalExceptionHandler, DTO 4종
│       │   ├── adapter/out/persistence/  # JPA Entity/Repository/Adapter, StreamingSession
│       │   ├── adapter/out/thumbnail/    # FFmpegThumbnailAdapter
│       │   └── service/                  # StreamingMonitoringService
│       └── resources/
│           ├── application.yml           # ⚠ 라이브러리 측 설정 — 사용자 설정 오염 위험
│           ├── static/streamix/
│           │   ├── css/streamix.css      # 단일 디자인 시스템 (OKLCH 토큰 + brutalist 컴포넌트, v3 신규)
│           │   ├── svg/icons.svg         # Bootstrap Icons MIT self-host sprite (v3 신규)
│           │   └── js/                   # ES Module 모듈: event-bus, store, api, theme, components/*, main (v3 신규)
│           └── templates/streamix/       # layout / dashboard / files / file-detail / sessions (v3 brutalist 재작성)
│
└── docs/
    ├── concepts/                         # 학습용 개념 문서 (architecture, java, spring-boot-starter, streaming)
    ├── implement/                        # 구현 계획 문서
    ├── refactor-log/                     # 변경 이력 (README.md + 2026-05-10-code-review/ 신설)
    └── plans/                            # 오케스트레이션 산출물 (2026-05-10/code-review-fix/ 신설)
```

## 핵심 모듈/컴포넌트

| 모듈 | 경로 | 역할 |
|------|------|------|
| `FileMetadata` (record) | core/domain/model | 파일 메타데이터 도메인 모델 (불변) |
| `FileType` (enum) | core/domain/model | IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER + 확장자 매핑 |
| `StreamableFile` (record) | core/domain/model | HTTP Range 스트리밍 응답 객체 |
| `StreamixException` (sealed) | core/domain/exception | 5개 final 하위 예외 계층 |
| `FileUploadService` | core/application/service | 검증 → 저장 → 썸네일 → 메타데이터 |
| `FileStreamService` | core/application/service | Range 헤더 파싱 + 부분 스트리밍 |
| `FileDeleteService` | core/application/service | 파일/썸네일/메타데이터 일괄 삭제 |
| `LocalFileStorageAdapter` | core/adapter/out/storage | RandomAccessFile 기반 부분 로드 |
| `ImageThumbnailAdapter` | core/adapter/out/thumbnail | Thumbnailator로 JPEG 썸네일 |
| `FFmpegThumbnailAdapter` | starter/adapter/out/thumbnail | FFmpeg 외부 프로세스로 비디오 썸네일 |
| `JpaFileMetadataAdapter` | starter/adapter/out/persistence | Spring Data JPA 어댑터 |
| `StreamingMonitoringService` | starter/service | 세션 기록 + 통계 |
| `StreamixApiController` | starter/adapter/in/web | `/api/streamix/files` REST API |
| `StreamixSessionsApiController` | starter/adapter/in/web | `/api/streamix/sessions/active` JSON API (v3 신규 — 폴링용) |
| `StreamixDashboardController` | starter/adapter/in/web | `/streamix` Thymeleaf UI |
| `StreamixAutoConfiguration` | starter/autoconfigure | 핵심 Bean 등록 |
| `StreamixRepositoryConfiguration` | starter/autoconfigure | EntityScan + EnableJpaRepositories |

## 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| spring-boot-starter | 4.0.0 | 코어 |
| spring-boot-starter-web | 4.0.0 | REST + servlet |
| spring-boot-starter-data-jpa | 4.0.0 | 메타데이터 영속화 |
| spring-boot-starter-thymeleaf | 4.0.0 | 대시보드 템플릿 |
| spring-boot-starter-validation | 4.0.0 | 입력 검증 |
| thymeleaf-layout-dialect | 3.4.0 | layout decorator |
| thumbnailator | 0.4.20 | 이미지 썸네일 |
| postgresql | 42.7.4 | compileOnly (사용자 선택) |
| mysql-connector-j | 9.1.0 | compileOnly (사용자 선택) |
| h2 | 2.3.232 | testRuntimeOnly |
| junit-jupiter | 5.11.3 | 테스트 |
| assertj-core | 3.26.3 | 테스트 assertion |
| mockito-core | 5.14.2 | 테스트 mock |
| byte-buddy | 1.17.5 | Java 25 mock 호환 |
| slf4j-api | 2.0.16 | 로깅 |

## 설정 파일

| 파일 | 역할 |
|------|------|
| `streamix.storage.base-path` | 파일 저장 경로 (기본 `./streamix-data`) |
| `streamix.storage.max-file-size` | 업로드 제한 (기본 100MB) |
| `streamix.storage.allowed-types` | IMAGE,VIDEO,... 콤마 구분 (빈 값=전체) |
| `streamix.thumbnail.enabled/width/height/ffmpeg-path` | 썸네일 |
| `streamix.api.enabled/base-path` | REST API |
| `streamix.dashboard.enabled/path` | 대시보드 UI |

## 테스트 구조
- 프레임워크: JUnit 5 (`useJUnitPlatform()`) + AssertJ + Mockito 5 + ApplicationContextRunner
- 실행: `./gradlew test` (전체) / `./gradlew :streamix-core:test`
- 위치: 각 모듈의 `src/test/java/...`
- 현황(2026-05-10 시점): core 12개 테스트 클래스 양호, starter는 `StreamixAutoConfigurationTest` 1개로 빈약

## 외부 시스템 연동

| 시스템 | 용도 | 접속 방식 |
|--------|------|----------|
| H2 | 개발/테스트 메타데이터 | application.yml 기본 |
| PostgreSQL/MySQL | 프로덕션 메타데이터 | 사용자 의존성 추가 |
| FFmpeg | 비디오 썸네일 | 외부 프로세스(`ProcessBuilder`) |
| 로컬 디스크 | 파일 저장소 | `LocalFileStorageAdapter` |
| Maven Central | 배포 | `tech.yanand.maven-central-publish` Gradle 플러그인 |

## 현재 상태 (2026-05-11)
- **단계**: 운영중 (v3.0.0-SNAPSHOT — 대시보드 전면 재설계 완료)
- **완료 작업**:
  - v2.0.1: 전체 코드 리뷰 24개 이슈 일괄 개선 ✅
  - **v3.0.0: 대시보드 재설계 (Cinema/Editorial Brutalist) + 외부 의존성 0** ✅ `BUILD SUCCESSFUL`
- **v3 산출물**:
  - `docs/plans/2026-05-10/v3-redesign/plan-v2.md` — 본 계획
  - `docs/plans/2026-05-10/v3-redesign/checklist-v2.md` — 진행 트래커
  - `docs/plans/2026-05-10/v3-redesign/learned.md` — 학습 기록

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-10 | project-overview.md 신규 작성 |
| 2026-05-10 | 코드 리뷰 24개 이슈 (P0 5 / P1 7 / P2 12) 일괄 개선 완료 — `BUILD SUCCESSFUL` |
| 2026-05-11 | v3.0.0 대시보드 재설계: Tailwind/WebJars 모두 제거, 순수 CSS + ES Modules + Brutalist 미감, system 폰트 스택만 사용, Sessions API 추가 — `BUILD SUCCESSFUL` |
