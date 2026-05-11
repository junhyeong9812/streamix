# 커밋 분할 가이드: v3.0.0 Brutalist 재설계

> v2.0.1 → v3.0.0 변경 사항을 합리적인 커밋 단위로 나누기 위한 가이드.
> 작성: 2026-05-11. 적용 대상: 현재 master 브랜치의 working tree.

---

## 변경 사항 요약 (커밋 전 git status)

| 상태 | 카운트 | 비고 |
|------|--------|------|
| Modified | 9 | README, build.gradle×2, project-overview, MonitoringConfig, 4 templates |
| Deleted | 2 | dashboard.css, dashboard.js |
| Untracked (신규) | 18+ | streamix.css, icons.svg, fragments.html, 9 JS 모듈, Sessions API Controller, plan-v2/checklist-v2/learned/commits |

**제외 (별도 처리)**: `gradlew` (이전 작업에서 이미 M 상태 — 본 작업과 무관)

---

## 권장 옵션 — 9개 커밋 (영역별 + 의미별)

다음 순서를 권장한다. 각 커밋이 독립적으로 빌드 가능하지는 않지만(중간 단계에서 일부 깨질 수 있음), 리뷰 단위로 의미가 명확하다.

### Commit 1 — `chore(deps): remove WebJars Bootstrap/Icons/Locator from starter`
**목적**: v3는 외부 프론트 라이브러리 의존성을 0으로 두는 방향. WebJars 3개 정리.

```bash
git add streamix-spring-boot-starter/build.gradle
git commit -m "$(cat <<'EOF'
chore(deps): WebJars Bootstrap / Icons / Locator 의존성 제거

- v3.0.0 대시보드 재설계의 일환으로 프론트엔드 외부 의존성 0 방침 적용
- bootstrap 5.3.2, bootstrap-icons 1.11.1, webjars-locator-lite 1.0.1 제거
- 이후 커밋에서 순수 CSS / Self-host SVG sprite로 대체

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 2 — `feat(static): brutalist CSS 디자인 시스템 (streamix.css)`
**목적**: dashboard.css(822줄)를 cinema/editorial brutalist 미감의 단일 디자인 시스템(streamix.css)으로 교체.

```bash
git add streamix-spring-boot-starter/src/main/resources/static/streamix/css/streamix.css
git rm streamix-spring-boot-starter/src/main/resources/static/streamix/css/dashboard.css
git commit -m "$(cat <<'EOF'
feat(static): Cinema/Editorial Brutalist CSS 디자인 시스템 추가

- streamix.css 1개 파일로 OKLCH 토큰(라이트/다크) + 30개 섹션
- 시스템 폰트 스택만 사용 — 외부 폰트 다운로드 없음
- streamix-* 접두사 (사용자 페이지 클래스와 충돌 회피)
- :focus-visible + prefers-reduced-motion + OKLCH @supports fallback 포함
- 기존 dashboard.css (Bootstrap 기반 822줄) 삭제

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 3 — `feat(static): Bootstrap Icons MIT SVG sprite 추가 (self-host)`
**목적**: WebJars Bootstrap Icons 폰트 대신 self-host SVG sprite (45 symbols).

```bash
git add streamix-spring-boot-starter/src/main/resources/static/streamix/svg/icons.svg
git commit -m "$(cat <<'EOF'
feat(static): Bootstrap Icons MIT SVG sprite self-host 추가

- /streamix/svg/icons.svg (45 symbols, <use href>로 참조)
- 출처: https://github.com/twbs/icons (MIT License)
- WebJars Bootstrap Icons 의존성 대체
- 모든 페이지에서 <link rel="preload">로 priority fetch

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 4 — `feat(static): ES Module JS 인프라 (event-bus / store / api / theme / utils)`
**목적**: 모놀리식 dashboard.js를 framework-agnostic 코어 모듈로 분리.

```bash
git add streamix-spring-boot-starter/src/main/resources/static/streamix/js/event-bus.js \
        streamix-spring-boot-starter/src/main/resources/static/streamix/js/store.js \
        streamix-spring-boot-starter/src/main/resources/static/streamix/js/api.js \
        streamix-spring-boot-starter/src/main/resources/static/streamix/js/theme.js \
        streamix-spring-boot-starter/src/main/resources/static/streamix/js/utils.js
git commit -m "$(cat <<'EOF'
feat(static): ES Module 코어 JS 인프라 추가

- event-bus.js: Pub/Sub (Map<event, Set<handler>>) + EVENTS 상수
- store.js: path 기반 set/get + STORE_CHANGED 이벤트
- api.js: fetch wrapper + ApiError (files/sessions 엔드포인트)
- theme.js: light/dark/system 토글 + matchMedia + storage 이벤트
- utils.js: spriteBase/spriteIcon + formatFileSize/Duration/DateTime 공유

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 5 — `feat(static): UI 컴포넌트 모듈 (toast / modal / upload / sessions-poller)`
**목적**: XSS-safe toast, focus-trap modal, drag-drop upload, visibility-aware polling.

```bash
git add streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/
git commit -m "$(cat <<'EOF'
feat(static): UI 컴포넌트 모듈 4개 추가

- components/toast.js: XSS-safe DOM API (textContent + createElementNS)
- components/modal.js: focus trap + previousFocus 복원 + Esc 닫기
- components/upload.js: 드래그앤드롭 + XHR 진행률 + 파일 타입/사이즈 검증
- components/sessions-poller.js: 5초 폴링 + visibility-aware

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 6 — `feat(static): main.js 진입점 + 기존 dashboard.js 제거`
**목적**: ES Module 진입점 및 `window.Streamix` 공개 namespace 신규. legacy IIFE dashboard.js 삭제.

```bash
git add streamix-spring-boot-starter/src/main/resources/static/streamix/js/main.js
git rm streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js
git commit -m "$(cat <<'EOF'
feat(static): main.js 진입점 추가 + 기존 dashboard.js 제거

- main.js가 모든 모듈을 wiring (Theme.init, mobile nav, copy, formatters,
  delete modal, auto refresh + SESSIONS_REFRESHED 구독, flash, upload)
- 공개 namespace 재구성:
  window.Streamix.{events, store, api, theme, toast, modal,
                   sessionsPoller, format.{fileSize, duration, dateTime},
                   detectFileType, spriteBase, spriteIcon}
- 기존 IIFE dashboard.js (515줄, Bootstrap Toast/Tooltip 의존) 삭제

BREAKING CHANGE:
- window.Streamix.formatFileSize → window.Streamix.format.fileSize
- window.Streamix.showToast → window.Streamix.toast

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 7 — `feat(templates): 5 페이지 brutalist 재작성 + 공통 fragment 추출`
**목적**: 모든 Thymeleaf 페이지를 streamix-* 클래스 + SVG sprite + ES Module로 재구성. deleteModal/fileTypeIcon fragment로 중복 제거.

```bash
git add streamix-spring-boot-starter/src/main/resources/templates/streamix/layout.html \
        streamix-spring-boot-starter/src/main/resources/templates/streamix/dashboard.html \
        streamix-spring-boot-starter/src/main/resources/templates/streamix/files.html \
        streamix-spring-boot-starter/src/main/resources/templates/streamix/file-detail.html \
        streamix-spring-boot-starter/src/main/resources/templates/streamix/sessions.html \
        streamix-spring-boot-starter/src/main/resources/templates/streamix/fragments.html
git commit -m "$(cat <<'EOF'
feat(templates): 5 페이지 brutalist 재작성 + 공통 fragment

- layout.html: <head> inline theme bootstrap (FOUC 방지) + SVG preload + ES Module
- dashboard.html: stat-grid 4 + recent + active sessions + popular tiles
- files.html: 테이블 + 업로드/삭제 모달 + 페이지네이션
- file-detail.html: 미디어 플레이어(video/audio/image/document/archive/other) +
                   정보 grid + API URL 복사 박스 + 스트리밍 통계
- sessions.html: 자동 새로고침 토글 + 활성/최근 테이블 + cumulative summary
- fragments.html: deleteModal(initialName) + fileTypeIcon(typeName) 공통 fragment
- 모든 페이지 aria-label / focus-visible 호환 + Korean a11y

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 8 — `feat(api): Sessions JSON API 컨트롤러 신규 + Bean 등록`
**목적**: 5초 폴링용 새 JSON 엔드포인트.

```bash
git add streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/in/web/StreamixSessionsApiController.java \
        streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/autoconfigure/StreamixMonitoringConfiguration.java
git commit -m "$(cat <<'EOF'
feat(api): StreamixSessionsApiController 신규 (sessions/active JSON)

- GET /api/streamix/sessions/active — 활성 세션 JSON 응답
- record DTO ActiveSessionResponse로 JPA 엔티티 노출 회피
  (id, fileId, clientIp, startedAt, bytesSent, status만 노출 — userAgent/Range 제외)
- StreamixMonitoringConfiguration에 @Bean 등록:
  * @ConditionalOnWebApplication(SERVLET)
  * @ConditionalOnProperty(streamix.api.enabled, default true)
  * @ConditionalOnBean(StreamingMonitoringService.class)
  * @ConditionalOnMissingBean(StreamixSessionsApiController.class)
- @GetMapping produces = APPLICATION_JSON_VALUE 명시

대시보드 sessions 페이지의 5초 폴링이 이 엔드포인트를 호출하여
활성 세션 카운트 변화 감지 → location.reload()

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Commit 9 — `chore: bump version 3.0.0-SNAPSHOT + docs (README / overview / plan / learned)`
**목적**: 버전 메이저 bump + 문서.

```bash
git add build.gradle README.md docs/project-overview.md docs/plans/2026-05-10/v3-redesign/
git commit -m "$(cat <<'EOF'
chore: v3.0.0-SNAPSHOT + 문서 (changelog / 마이그레이션 / 학습)

- 루트 build.gradle: fallback 버전 2.1.0-SNAPSHOT → 3.0.0-SNAPSHOT
- README.md: v3.0.0 changelog + 마이그레이션 가이드 (v2 → v3) +
            Bootstrap Icons MIT 라이센스 고지
- docs/project-overview.md: v3 반영 (디렉토리 구조 + 신규 컨트롤러)
- docs/plans/2026-05-10/v3-redesign/:
  * plan-v2.md (v1 보존, v2 본 계획)
  * checklist-v2.md (진행 상황)
  * learned.md (사용 라이브러리/패턴/디자인 결정 학습)
  * commits.md (본 분할 가이드)
  * context.md 갱신

Breaking Changes:
- CSS 파일명 dashboard.css → streamix.css
- CSS 클래스 prefix streamix-*
- JS 모듈 분리 + window.Streamix.* 네임스페이스 재구성
- Bootstrap WebJars 제거 — 사용자 페이지가 의존했다면 별도 추가 필요
- 다크모드 system 기본

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## 대안 옵션

### 옵션 B — Phase별 5개 커밋 (간단)

| 커밋 | 내용 |
|------|------|
| 1 | Phase α — CSS + SVG sprite + WebJars 제거 |
| 2 | Phase β — JS 인프라 9개 모듈 |
| 3 | Phase γ — 5 템플릿 재작성 |
| 4 | Phase δ — Sessions API Controller + 버전 + dashboard.js/css 삭제 |
| 5 | docs — README + project-overview + plan/learned |

**장점**: 작업 단계와 1:1 매핑, 직관적.
**단점**: 커밋 크기 큼, 회귀 발생 시 bisect 어려움.

### 옵션 C — 단일 커밋 (가장 단순)

```bash
git add -A
git commit -m "feat: v3.0.0 대시보드 Brutalist 재설계 (zero-dependency)"
```

**장점**: 빠름.
**단점**: 변경 범위 큼, 리뷰/롤백 어려움. 권장하지 않음.

---

## 실행 전 체크리스트

- [ ] 현재 working tree에 본 작업과 무관한 변경이 없는지 확인 (`git status`).
  - 예외: `gradlew`는 이전 작업의 M 상태 — 본 v3-redesign과 무관. **이 커밋 시리즈에 포함하지 않는다.** 별도 커밋이나 stash로 처리.
- [ ] 각 커밋 직후 `./gradlew :streamix-spring-boot-starter:compileJava`로 컴파일 통과 확인 (특히 8번 커밋 후).
- [ ] 모든 커밋 후 `./gradlew clean build`로 BUILD SUCCESSFUL 재확인.
- [ ] commit hook (pre-commit)가 있으면 통과 확인. 실패 시 `--no-verify` 사용하지 않고 원인 해결.

---

## 중간 빌드 통과 보장

각 커밋 직후의 빌드 상태:

| 커밋 후 | compile | test | UI 작동 |
|---------|---------|------|---------|
| 1 | ✓ | ✓ | ✗ (Bootstrap 클래스 참조 깨짐) |
| 2 | ✓ | ✓ | ✗ (CSS 클래스 안 매핑) |
| 3 | ✓ | ✓ | ✗ |
| 4 | ✓ | ✓ | ✗ (JS 진입점 없음) |
| 5 | ✓ | ✓ | ✗ |
| 6 | ✓ | ✓ | ✗ (템플릿이 옛 dashboard.js 참조 시 404) |
| 7 | ✓ | ✓ | ⚠ (StreamixSessionsApiController 미존재 시 sessions 폴링 404) |
| 8 | ✓ | ✓ | ✓ |
| 9 | ✓ | ✓ | ✓ |

**완전한 UI 작동은 8번 커밋 이후부터**. 따라서 1~8번을 빠르게 적용 후 push하는 것을 권장한다. 각 커밋이 의미적으로는 독립적이지만, 런타임 작동 측면에서는 시리즈가 완성되어야 한다.

---

## Push 전 최종 확인

```bash
# 변경 요약 검토
git log --oneline origin/master..HEAD

# 각 커밋의 변경 파일 수 확인
git log --stat origin/master..HEAD

# 최종 빌드 한 번 더
./gradlew clean build
```

문제 없으면 사용자 승인 후 `git push origin master` (또는 PR 브랜치 사용).

---

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-11 | 초기 작성 — 9개 커밋 권장 + 옵션 B/C |
