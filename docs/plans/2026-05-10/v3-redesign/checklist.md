# 체크리스트: v3-redesign

## Pre-구현: 사용자 검토/승인
- [ ] plan.md 사용자 검토 + 5가지 결정 항목 답변
  - [ ] Tailwind 빌드 옵션 (C vs D)
  - [ ] Lucide 통합 방식
  - [ ] 다크모드 기본값
  - [ ] sessions API endpoint 추가 OK?
  - [ ] tailwind.config.js 위치

## Phase α — 디자인 시스템 + 인프라
- [ ] α1. `package.json` 작성 (tailwindcss + @tailwindcss/cli)
- [ ] α2. `tailwind.config.js` (content paths, theme.extend)
- [ ] α3. `streamix-spring-boot-starter/src/main/resources/static/streamix/css/streamix.css` 작성
  - [ ] OKLCH 토큰 (light + dark)
  - [ ] base styles
  - [ ] 컴포넌트 클래스 (.btn, .card, .badge, .toast, .modal, .skeleton)
- [ ] α4. `npm install` + `npm run build:css` → `tailwind.css` 생성 + commit
- [ ] α5. `streamix-spring-boot-starter/build.gradle`에서 WebJars Bootstrap 제거
- [ ] α6. 검증: `./gradlew build` BUILD SUCCESSFUL

## Phase β — JS 인프라
- [ ] β1. `static/streamix/js/event-bus.js` (EventBus + EVENTS)
- [ ] β2. `static/streamix/js/store.js` (Store + state)
- [ ] β3. `static/streamix/js/api.js` (Api 클라이언트 + ApiError)
- [ ] β4. `static/streamix/js/theme.js` (다크모드 토글)
- [ ] β5. `static/streamix/js/components/icons.js` (Lucide SVG sprite ~30개)
- [ ] β6. `static/streamix/js/main.js` (진입점, init)
- [ ] β7. 임시: layout에 새 모듈 추가하되 dashboard.js는 유지 → 충돌 검증

## Phase γ — 페이지별 재작성
- [ ] γ1. `templates/streamix/layout.html` 재작성
  - [ ] Tailwind classes
  - [ ] Theme inline script (FOUC 방지)
  - [ ] ES Module imports
  - [ ] 사이드바 + 다크모드 토글 + 모바일 햄버거
- [ ] γ2. `templates/streamix/dashboard.html` 재작성 (stats cards + recent files + active sessions + popular files)
- [ ] γ3. `templates/streamix/files.html` + `components/upload.js` + `components/modal.js`
- [ ] γ4. `templates/streamix/file-detail.html` (미디어 플레이어 + 정보 패널)
- [ ] γ5. `templates/streamix/sessions.html` + `components/sessions-poller.js`
- [ ] γ6. `components/toast.js` (XSS-safe)

## Phase δ — 통합/정리
- [ ] δ1. `StreamixSessionsApiController` 신규 (`/api/streamix/sessions/active`)
- [ ] δ2. `StreamixDashboardController` model attribute 갱신 (필요시)
- [ ] δ3. 기존 `dashboard.js` 삭제
- [ ] δ4. 기존 `dashboard.css` 삭제
- [ ] δ5. `build.gradle` version 3.0.0-SNAPSHOT
- [ ] δ6. `README.md` v3.0.0 changelog + 마이그레이션 가이드
- [ ] δ7. `docs/refactor-log/README.md`에 v3.0.0 항목 추가

## 최종 검증
- [ ] `./gradlew clean build` BUILD SUCCESSFUL
- [ ] JAR 확인 (`streamix-spring-boot-starter-3.0.0-SNAPSHOT.jar`)
- [ ] 수동 검증
  - [ ] 대시보드 메인 페이지
  - [ ] 파일 목록 + 업로드 + 삭제
  - [ ] 파일 상세 (이미지/비디오/오디오/PDF/기타)
  - [ ] 세션 페이지 + 자동 새로고침
  - [ ] 다크모드 토글
  - [ ] 모바일 반응형
- [ ] 학습 기록 (`learned.md`) 작성
- [ ] `project-overview.md` 업데이트

## Breaking Changes 기록
- [ ] dashboard.css → streamix.css + tailwind.css
- [ ] Bootstrap 의존성 제거
- [ ] window.Streamix API 재구성
- [ ] 다크모드 기본 활성

## 진행 상황 기록
| Phase | 시작 | 완료 | 메모 |
|-------|------|------|------|
| Pre-구현 검토 | 2026-05-10 | 대기 (사용자 검토) | plan.md 작성 완료 |
| α | (대기) | | |
| β | (대기) | | |
| γ | (대기) | | |
| δ | (대기) | | |
| 검증 | (대기) | | |

## 수정 기록
*(작업 진행하면서 추가)*

| Phase | 파일 | 변경 요약 |
|-------|------|----------|
| | | |
