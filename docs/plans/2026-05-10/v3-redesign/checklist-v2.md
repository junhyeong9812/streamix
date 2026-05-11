# 체크리스트 v2: v3-redesign (Brutalist + Zero-Dep)

> v1 checklist.md는 보존. 본 파일이 v2 기준 진행 트래커.

## Pre-구현: 사용자 검토/승인
- [ ] plan-v2.md 사용자 검토 + 승인

## Phase α — CSS + 아이콘 + 의존성 정리
- [ ] α1. `static/streamix/css/streamix.css` 작성
  - [ ] OKLCH 토큰 (light + dark)
  - [ ] 시스템 폰트 스택 (display/sans/mono)
  - [ ] base styles (reset + body + 타이포 스케일)
  - [ ] `.streamix-sidebar`, `.streamix-main`, `.streamix-mast`
  - [ ] `.streamix-card`, `.streamix-stat`
  - [ ] `.streamix-btn` 변형 4종 (primary/ghost/danger/icon)
  - [ ] `.streamix-table`, `.streamix-th`, `.streamix-td`
  - [ ] `.streamix-badge--{image,video,audio,document,archive,other}`
  - [ ] `.streamix-modal`, `.streamix-modal-overlay`, `.streamix-modal-content`
  - [ ] `.streamix-toast`, `.streamix-toast-container`
  - [ ] `.streamix-pulse`, `.streamix-progress`, `.streamix-skeleton`, `.streamix-spinner`
  - [ ] `.streamix-icon` (use href sprite)
  - [ ] `.streamix-meta`, `.streamix-mono`, `.streamix-display`
  - [ ] `.streamix-upload-area`, `.streamix-empty`, `.streamix-input`
  - [ ] 반응형 (`@media max-width: 768px` 사이드바 오버레이)
  - [ ] dark mode 전환 (`html.dark` 셀렉터)
  - [ ] page load staggered fade-in animations
- [ ] α2. `static/streamix/svg/icons.svg` sprite 작성 (~25개)
  - [ ] 네비: speedometer2, folder, broadcast
  - [ ] 액션: play-fill, eye, trash, cloud-upload, download, clipboard, check-circle, x-circle
  - [ ] 파일타입: image, camera-video, music-note-beamed, file-earmark-text, file-earmark-zip, file-earmark
  - [ ] UI: sun, moon, list, chevron-left, chevron-right, exclamation-triangle, info-circle, wifi-off, broadcast-pin
- [ ] α3. `streamix-spring-boot-starter/build.gradle` WebJars 3줄 제거
  - [ ] bootstrap:5.3.2
  - [ ] bootstrap-icons:1.11.1
  - [ ] webjars-locator-lite:1.0.1
- [ ] α4. 검증: `./gradlew :streamix-spring-boot-starter:compileJava` PASS

## Phase β — JS 인프라
- [ ] β1. `static/streamix/js/event-bus.js` (EventBus + EVENTS 상수)
- [ ] β2. `static/streamix/js/store.js` (Store + state shape)
- [ ] β3. `static/streamix/js/api.js` (Api + ApiError)
- [ ] β4. `static/streamix/js/theme.js` (light/dark/system 토글 + localStorage)
- [ ] β5. `static/streamix/js/components/upload.js` (드래그앤드롭 + XHR + 진행률)
- [ ] β6. `static/streamix/js/components/toast.js` (XSS-safe DOM API)
- [ ] β7. `static/streamix/js/components/modal.js` (삭제 확인 + 업로드 모달)
- [ ] β8. `static/streamix/js/components/sessions-poller.js` (5초 폴링 + visibility-aware)
- [ ] β9. `static/streamix/js/main.js` (DOMContentLoaded init + 모듈 wiring)

## Phase γ — 페이지별 재작성
- [ ] γ1. `templates/streamix/layout.html` 재작성
  - [ ] `<head>` inline theme script (FOUC 방지)
  - [ ] SVG sprite preload
  - [ ] streamix.css 링크 (Bootstrap WebJars 링크 제거)
  - [ ] ES Module 진입점 `main.js`
  - [ ] 사이드바 brutalist 재구성 (마스트헤드 + nav + footer + theme toggle)
  - [ ] 모바일 햄버거 + 오버레이
- [ ] γ2. `templates/streamix/dashboard.html` 재작성
  - [ ] page-mast (제목 + 메타 라벨)
  - [ ] 4-grid stats (큰 숫자 + 라벨 + 메타)
  - [ ] 최근 업로드 테이블
  - [ ] 활성 세션 패널 (live dot)
  - [ ] 인기 파일 그리드
- [ ] γ3. `templates/streamix/files.html` 재작성
  - [ ] 파일 테이블 + 페이지네이션
  - [ ] 업로드 모달 (드래그앤드롭 + 진행률)
  - [ ] 삭제 확인 모달
- [ ] γ4. `templates/streamix/file-detail.html` 재작성
  - [ ] breadcrumb
  - [ ] 미디어 플레이어 (video/audio/image/document/archive/other)
  - [ ] 정보 패널 (file-info-grid)
  - [ ] API URL 복사 박스
  - [ ] 스트리밍 통계
- [ ] γ5. `templates/streamix/sessions.html` 재작성
  - [ ] 자동 새로고침 토글 (sessions-poller 연결)
  - [ ] 활성 세션 + 최근 세션 테이블
  - [ ] 통계 카드

## Phase δ — 통합/정리
- [ ] δ1. `StreamixSessionsApiController.java` 신규 작성
  - [ ] `GET /api/streamix/sessions/active` JSON 응답
  - [ ] StreamingMonitoringService.getActiveSessions() 호출
- [ ] δ2. 기존 `static/streamix/js/dashboard.js` 삭제
- [ ] δ3. 기존 `static/streamix/css/dashboard.css` 삭제
- [ ] δ4. 루트 `build.gradle` version 2.0.1-SNAPSHOT → 3.0.0-SNAPSHOT
- [ ] δ5. `README.md` v3.0.0 changelog
  - [ ] Breaking changes 명시
  - [ ] 마이그레이션 가이드 (v2 → v3)
  - [ ] Bootstrap Icons MIT 라이센스 고지
- [ ] δ6. `docs/refactor-log/README.md`에 v3.0.0 항목

## 최종 검증
- [ ] `./gradlew clean build` BUILD SUCCESSFUL
- [ ] JAR 확인 (`streamix-spring-boot-starter-3.0.0-SNAPSHOT.jar`)
- [ ] 수동 검증 (사용자가 실행)
  - [ ] 대시보드 메인
  - [ ] 파일 목록 + 업로드 + 삭제
  - [ ] 파일 상세 (이미지/비디오/오디오/PDF/기타)
  - [ ] 세션 페이지 + 자동 새로고침 (5초 폴링 동작)
  - [ ] 다크/라이트 토글
  - [ ] system 테마 OS 동기화
  - [ ] 모바일 반응형
- [ ] `learned.md` 작성
- [ ] `docs/project-overview.md` v3.0.0 반영

## Breaking Changes 기록 (v2 → v3)
- [ ] `dashboard.css` → `streamix.css` (파일명 변경)
- [ ] `dashboard.js` → ES Modules 분리
- [ ] WebJars Bootstrap/Icons/Locator 의존성 제거 → 사용자가 자체 Bootstrap 사용 중이라면 별도 추가 필요
- [ ] `window.Streamix` API 재구성 (`window.Streamix.events`, `window.Streamix.store`, `window.Streamix.api`)
- [ ] 다크모드 system 기본 활성
- [ ] CSS class 접두사 `streamix-*` 정착

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
