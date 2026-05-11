# 체크리스트 v2: v3-redesign (Brutalist + Zero-Dep)

> v1 checklist.md는 보존. 본 파일이 v2 기준 진행 트래커.

## Pre-구현: 사용자 검토/승인
- [x] plan-v2.md 사용자 검토 + 승인

## Phase α — CSS + 아이콘 + 의존성 정리
- [x] α1. `static/streamix/css/streamix.css` 작성
  - [x] OKLCH 토큰 (light + dark)
  - [x] 시스템 폰트 스택 (display/sans/mono)
  - [x] base styles (reset + body + 타이포 스케일)
  - [x] `.streamix-sidebar`, `.streamix-main`, `.streamix-mast`
  - [x] `.streamix-card`, `.streamix-stat`
  - [x] `.streamix-btn` 변형 4종 (primary/ghost/danger/icon)
  - [x] `.streamix-table`, `.streamix-th`, `.streamix-td`
  - [x] `.streamix-badge--{image,video,audio,document,archive,other}`
  - [x] `.streamix-modal`, `.streamix-modal-overlay`, `.streamix-modal-content`
  - [x] `.streamix-toast`, `.streamix-toast-container`
  - [x] `.streamix-pulse`, `.streamix-progress`, `.streamix-skeleton`, `.streamix-spinner`
  - [x] `.streamix-icon` (use href sprite)
  - [x] `.streamix-meta`, `.streamix-mono`, `.streamix-display`
  - [x] `.streamix-upload-area`, `.streamix-empty`, `.streamix-input`
  - [x] 반응형 (`@media max-width: 768px` 사이드바 오버레이)
  - [x] dark mode 전환 (`html.dark` 셀렉터)
  - [x] page load staggered fade-in animations
- [x] α2. `static/streamix/svg/icons.svg` sprite 작성 (~25개)
  - [x] 네비: speedometer2, folder, broadcast
  - [x] 액션: play-fill, eye, trash, cloud-upload, download, clipboard, check-circle, x-circle
  - [x] 파일타입: image, camera-video, music-note-beamed, file-earmark-text, file-earmark-zip, file-earmark
  - [x] UI: sun, moon, list, chevron-left, chevron-right, exclamation-triangle, info-circle, wifi-off, broadcast-pin
- [x] α3. `streamix-spring-boot-starter/build.gradle` WebJars 3줄 제거
  - [x] bootstrap:5.3.2
  - [x] bootstrap-icons:1.11.1
  - [x] webjars-locator-lite:1.0.1
- [x] α4. 검증: `./gradlew :streamix-spring-boot-starter:compileJava` PASS

## Phase β — JS 인프라
- [x] β1. `static/streamix/js/event-bus.js` (EventBus + EVENTS 상수)
- [x] β2. `static/streamix/js/store.js` (Store + state shape)
- [x] β3. `static/streamix/js/api.js` (Api + ApiError)
- [x] β4. `static/streamix/js/theme.js` (light/dark/system 토글 + localStorage)
- [x] β5. `static/streamix/js/components/upload.js` (드래그앤드롭 + XHR + 진행률)
- [x] β6. `static/streamix/js/components/toast.js` (XSS-safe DOM API)
- [x] β7. `static/streamix/js/components/modal.js` (삭제 확인 + 업로드 모달)
- [x] β8. `static/streamix/js/components/sessions-poller.js` (5초 폴링 + visibility-aware)
- [x] β9. `static/streamix/js/main.js` (DOMContentLoaded init + 모듈 wiring)

## Phase γ — 페이지별 재작성
- [x] γ1. `templates/streamix/layout.html` 재작성
  - [x] `<head>` inline theme script (FOUC 방지)
  - [x] SVG sprite preload
  - [x] streamix.css 링크 (Bootstrap WebJars 링크 제거)
  - [x] ES Module 진입점 `main.js`
  - [x] 사이드바 brutalist 재구성 (마스트헤드 + nav + footer + theme toggle)
  - [x] 모바일 햄버거 + 오버레이
- [x] γ2. `templates/streamix/dashboard.html` 재작성
  - [x] page-mast (제목 + 메타 라벨)
  - [x] 4-grid stats (큰 숫자 + 라벨 + 메타)
  - [x] 최근 업로드 테이블
  - [x] 활성 세션 패널 (live dot)
  - [x] 인기 파일 그리드
- [x] γ3. `templates/streamix/files.html` 재작성
  - [x] 파일 테이블 + 페이지네이션
  - [x] 업로드 모달 (드래그앤드롭 + 진행률)
  - [x] 삭제 확인 모달
- [x] γ4. `templates/streamix/file-detail.html` 재작성
  - [x] breadcrumb
  - [x] 미디어 플레이어 (video/audio/image/document/archive/other)
  - [x] 정보 패널 (file-info-grid)
  - [x] API URL 복사 박스
  - [x] 스트리밍 통계
- [x] γ5. `templates/streamix/sessions.html` 재작성
  - [x] 자동 새로고침 토글 (sessions-poller 연결)
  - [x] 활성 세션 + 최근 세션 테이블
  - [x] 통계 카드

## Phase δ — 통합/정리
- [x] δ1. `StreamixSessionsApiController.java` 신규 작성
  - [x] `GET /api/streamix/sessions/active` JSON 응답
  - [x] StreamingMonitoringService.getActiveSessions() 호출
- [x] δ2. 기존 `static/streamix/js/dashboard.js` 삭제
- [x] δ3. 기존 `static/streamix/css/dashboard.css` 삭제
- [x] δ4. 루트 `build.gradle` version 2.0.1-SNAPSHOT → 3.0.0-SNAPSHOT
- [x] δ5. `README.md` v3.0.0 changelog
  - [x] Breaking changes 명시
  - [x] 마이그레이션 가이드 (v2 → v3)
  - [x] Bootstrap Icons MIT 라이센스 고지
- [x] δ6. `docs/refactor-log/README.md`에 v3.0.0 항목

## 최종 검증
- [x] `./gradlew clean build` BUILD SUCCESSFUL
- [x] JAR 확인 (`streamix-spring-boot-starter-3.0.0-SNAPSHOT.jar`)
- [x] 수동 검증 (사용자가 실행)
  - [x] 대시보드 메인
  - [x] 파일 목록 + 업로드 + 삭제
  - [x] 파일 상세 (이미지/비디오/오디오/PDF/기타)
  - [x] 세션 페이지 + 자동 새로고침 (5초 폴링 동작)
  - [x] 다크/라이트 토글
  - [x] system 테마 OS 동기화
  - [x] 모바일 반응형
- [x] `learned.md` 작성
- [x] `docs/project-overview.md` v3.0.0 반영

## Breaking Changes 기록 (v2 → v3)
- [x] `dashboard.css` → `streamix.css` (파일명 변경)
- [x] `dashboard.js` → ES Modules 분리
- [x] WebJars Bootstrap/Icons/Locator 의존성 제거 → 사용자가 자체 Bootstrap 사용 중이라면 별도 추가 필요
- [x] `window.Streamix` API 재구성 (`window.Streamix.events`, `window.Streamix.store`, `window.Streamix.api`)
- [x] 다크모드 system 기본 활성
- [x] CSS class 접두사 `streamix-*` 정착

## 진행 상황 기록
| Phase | 시작 | 완료 | 메모 |
|-------|------|------|------|
| Pre-구현 검토 | 2026-05-11 | 2026-05-11 | plan-v2 사용자 승인 — "진행하자" |
| α | 2026-05-11 | 2026-05-11 | streamix.css 1031줄 + icons.svg sprite 45개 + WebJars 3개 제거. `compileJava` PASS |
| β | 2026-05-11 | 2026-05-11 | event-bus / store / api / theme / components×4 / main 9개 ES 모듈 |
| γ | 2026-05-11 | 2026-05-11 | layout / dashboard / files / file-detail / sessions 5개 템플릿 brutalist 재작성 |
| δ | 2026-05-11 | 2026-05-11 | StreamixSessionsApiController + 버전 3.0.0-SNAPSHOT + dashboard.css/js 삭제 + README v3.0.0 |
| 검증 | 2026-05-11 | 2026-05-11 | `./gradlew clean build` BUILD SUCCESSFUL in 40s · JAR `streamix-spring-boot-starter-3.0.0-SNAPSHOT.jar` 100KB · learned.md + project-overview 갱신 |

## 최종 산출물 요약 (2026-05-11)
- **CSS**: `static/streamix/css/streamix.css` (단일 파일, OKLCH 토큰 + brutalist 컴포넌트, 30개 섹션)
- **SVG**: `static/streamix/svg/icons.svg` (Bootstrap Icons MIT self-host sprite, 45 symbols)
- **JS**: 9개 ES Module — event-bus / store / api / theme / main + components/{toast, modal, upload, sessions-poller}
- **Java**: 신규 `StreamixSessionsApiController` (record DTO + Monitoring Configuration Bean 등록)
- **템플릿**: layout / dashboard / files / file-detail / sessions 5개 전면 재작성
- **빌드**: `./gradlew clean build` BUILD SUCCESSFUL · 모든 테스트 통과
- **버전**: 2.0.1-SNAPSHOT → **3.0.0-SNAPSHOT**
- **의존성 변화**: WebJars 3개 (Bootstrap + Bootstrap Icons + Locator) 제거, npm/Tailwind 도입 없음, 외부 폰트 사용 없음

## 수정 기록 (작업 진행하면서 추가)
| Phase | 파일 | 변경 요약 |
|-------|------|----------|
| | | |

## 릴리즈 & 배포 (2026-05-11)

### 커밋 분할 (commits.md 옵션 A 채택 — 9개 + gradlew 1개)
- [x] `1a62e6c` chore: gradlew 실행 권한 부여 (file mode 100644 → 100755, v3 작업과 무관 — 별도 분리)
- [x] `33e99f9` chore(deps): WebJars Bootstrap / Icons / Locator 의존성 제거 (Commit 1)
- [x] `4e1797e` feat(static): Cinema/Editorial Brutalist CSS 디자인 시스템 추가 (Commit 2)
- [x] `ef260df` feat(static): Bootstrap Icons MIT SVG sprite self-host 추가 (Commit 3)
- [x] `65f59d8` feat(static): ES Module 코어 JS 인프라 추가 (Commit 4)
- [x] `43fa41f` feat(static): UI 컴포넌트 모듈 4개 추가 (Commit 5)
- [x] `09fb364` feat(static): main.js 진입점 추가 + 기존 dashboard.js 제거 (Commit 6, BREAKING CHANGE footer)
- [x] `6ef3c14` feat(templates): 5 페이지 brutalist 재작성 + 공통 fragment (Commit 7)
- [x] `1a7c619` feat(api): StreamixSessionsApiController 신규 (sessions/active JSON) (Commit 8)
- [x] `982273f` chore: v3.0.0-SNAPSHOT + 문서 (changelog / 마이그레이션 / 학습) (Commit 9)

### Push & 배포 트리거
- [x] `git push origin master` — 11 commits 푸시 → `ci.yml` 발동
- [x] `git tag -a v3.0.0` — 어노테이티드 태그 생성
- [x] `git push origin v3.0.0` — 태그 푸시 → `publish.yml` 발동 (Maven Central 자동 배포)

### CI/CD 워크플로우 메모
- `ci.yml`: master push/PR 시 발동 — JDK 25 setup + `./gradlew build` (테스트 포함)
- `publish.yml`: **`v*` 태그 push** 또는 **release:created** 시 발동
  - `RELEASE_VERSION` 환경변수를 `${GITHUB_REF#refs/tags/v}`에서 추출 → `-PreleaseVersion=$RELEASE_VERSION`로 주입
  - 결과: `streamix-core` + `streamix-spring-boot-starter`가 정식 `3.0.0` 버전으로 Maven Central에 publish (`publishingType = "AUTOMATIC"`)
  - 트리거 중복 주의: `push:tags` + `release:created` 둘 다 있으므로 GitHub Release를 추가로 만들면 publish가 두 번 발동될 수 있음. 같은 버전 재 publish는 Maven Central에서 거부됨 — 이번에는 태그 push만 수행하여 1회만 발동

### 미수행 (선택사항)
- [ ] GitHub Release 생성 (`gh` CLI 미설치 — 필요 시 웹 UI에서 v3.0.0 태그 기반 release 생성)

