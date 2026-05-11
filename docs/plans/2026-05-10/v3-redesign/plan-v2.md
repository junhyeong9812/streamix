# 계획서 v2: v3.0.0 — Brutalist Cinema Dashboard (Zero-Dependency)

> v1(plan.md)에서 사용자의 추가 지시(2026-05-11)를 반영한 개정판.
> **v1은 보존**하고 v2를 본 계획으로 한다.

## v1 → v2 변경 사유 (사용자 지시 2026-05-11)
1. **"순수 CSS/JS/HTML 기반, 프론트 의존성 최소화"** — Tailwind 제거.
2. **"다양한 라이브러리를 가져오면 의미가 없다"** — 외부 폰트(Google Fonts), Lucide WebJars 등 모든 외부 다운로드 제거.
3. **디자인 방향: A. Cinema/Editorial Brutalist** — 미디어/영화관 미감, 큰 세리프 + sans body, 단일 액센트.

## 핵심 원칙 (v2 추가)
- **외부 네트워크 의존성 0**: 폰트·CSS 프레임워크·아이콘 라이브러리·CDN 모두 사용 안 함.
- **모든 자산 self-host**: 작은 SVG sprite와 plain CSS 한 묶음으로 끝.
- **시스템 폰트 스택**: macOS·Windows·Linux 어디서 열어도 distinctive하게 보이도록 OS 기본 폰트만 사용.
- **빌드 도구 0**: starter 빌드는 Gradle Java만. npm/node 없음.

## v1 → v2 변경 항목

| 항목 | v1 | v2 |
|------|----|----|
| CSS 프레임워크 | Tailwind CSS v4 | ❌ **없음** — 순수 CSS (`streamix.css` 1개) |
| 빌드 도구 | npm + Tailwind CLI (`build:css`) | ❌ **전부 제거** |
| 생성 파일 (빌드 관련) | `package.json`, `tailwind.config.js`, `tailwind.css` | ❌ **작성 안 함** |
| 컬러 토큰 시스템 | shadcn OKLCH (외부 디자인 차용) | ✅ OKLCH 유지 (자체 정의, 디자인 컨텍스트 재조정) |
| 아이콘 | Lucide WebJars 또는 inline | ✅ **자체 inline SVG sprite** (`icons.svg`) — Bootstrap Icons MIT 라이센스 SVG self-host |
| WebJars Bootstrap | 제거 | ✅ 제거 |
| WebJars Bootstrap Icons | 제거 | ✅ 제거 |
| WebJars Locator | 검토 | ❌ **제거** |
| 외부 폰트 (Google Fonts 등) | (미정) | ❌ **없음** — 시스템 폰트 스택만 |
| 디자인 방향 | shadcn-style 일반 SaaS (모던 indigo 액센트) | 🎬 **Cinema/Editorial Brutalist** |
| 기본 테마 | 다크 기본 | **system** (OS 따름) + localStorage 토글 |
| sessions API | 검토 | ✅ `GET /api/streamix/sessions/active` 추가 |
| Event Bus | Custom Map<event, Set> | ✅ 유지 |

## 사용자 결정 (2026-05-11)

| # | 항목 | 결정 |
|---|------|------|
| 1 | 디자인 미적 방향 | **A. Cinema/Editorial Brutalist** |
| 2 | 기본 테마 | **system** (OS 따름) |
| 3 | WebJars 처리 | **전부 제거** (Bootstrap + Icons + Locator) |
| 4 | 새 sessions API | `GET /api/streamix/sessions/active` 추가 |
| 5 | 외부 폰트 | **없음 — 시스템 폰트 스택만** |
| 6 | SVG sprite 출처 | Bootstrap Icons MIT 라이센스 self-host (~30개) |

## 디자인 시스템

### 컨셉 — Cinema/Editorial Brutalist
- 영화관 객석 · 잡지 헤드라인 · 인쇄물 그리드의 분위기.
- **큰 세리프 디스플레이 폰트**(시스템 세리프 — macOS의 New York, Windows의 Cambria, Linux의 시스템 세리프)로 핵심 숫자/제목.
- **모노스페이스**(ui-monospace)로 ID, IP, 파일 ID, 코드형 메타데이터.
- **sans-serif**(system-ui)로 일반 본문.
- 라이트: 종이 톤(off-white) + 잉크 검정 + 시그널 단일 액센트(amber).
- 다크: 영화관 어둠(near-black) + 페이퍼 화이트 + 라임/amber 시그널.
- 카드 윤곽은 가는 1px 검정/회색 선, 그림자 거의 없음 (인쇄 페이지 느낌).
- 메타 텍스트(`MEDIA OPERATIONS · ROOM 04`)로 컨텍스트 라벨링 — 잡지 마스트헤드 감성.

### 컬러 토큰 (OKLCH)

```css
:root {
  /* Paper / Ink — Light theme */
  --background: oklch(0.985 0.005 85);          /* off-white paper */
  --foreground: oklch(0.15 0.01 60);            /* near-black ink */
  --card: oklch(1 0 0);                          /* pure white card */
  --card-foreground: oklch(0.15 0.01 60);
  --muted: oklch(0.94 0.005 85);
  --muted-foreground: oklch(0.45 0.01 60);      /* secondary ink */
  --border: oklch(0.85 0.005 60);
  --border-strong: oklch(0.20 0.01 60);         /* brutalist 1px ink line */
  --input: oklch(1 0 0);
  --ring: oklch(0.15 0.01 60);
  /* Signal accent (single bold mark, like editorial color highlight) */
  --accent: oklch(0.82 0.18 85);                /* amber/marigold */
  --accent-foreground: oklch(0.15 0.01 60);
  --destructive: oklch(0.55 0.22 25);           /* signal red */
  --destructive-foreground: oklch(0.985 0 0);
  --live: oklch(0.65 0.21 25);                  /* on-air red */
  /* FileType marks */
  --type-image: oklch(0.62 0.16 145);
  --type-video: oklch(0.58 0.16 250);
  --type-audio: oklch(0.55 0.20 295);
  --type-document: oklch(0.65 0.16 65);
  --type-archive: oklch(0.55 0.15 30);
  --type-other: oklch(0.55 0.02 60);
  /* Radii — brutalist: minimal */
  --radius: 0;
  --radius-sm: 2px;
  --radius-card: 0;
}

.dark {
  --background: oklch(0.12 0.005 60);           /* cinema black */
  --foreground: oklch(0.96 0.005 85);           /* paper white */
  --card: oklch(0.17 0.005 60);
  --card-foreground: oklch(0.96 0.005 85);
  --muted: oklch(0.22 0.005 60);
  --muted-foreground: oklch(0.65 0.005 85);
  --border: oklch(0.28 0.005 60);
  --border-strong: oklch(0.85 0.005 85);
  --input: oklch(0.17 0.005 60);
  --ring: oklch(0.96 0.005 85);
  --accent: oklch(0.88 0.18 95);                /* lime/amber neon */
  --accent-foreground: oklch(0.12 0.005 60);
  --destructive: oklch(0.65 0.24 25);
  --destructive-foreground: oklch(0.96 0.005 85);
  --live: oklch(0.70 0.23 25);
}
```

### 타이포그래피 (시스템 폰트 스택)

```css
:root {
  /* 디스플레이: 세리프 (헤드라인 + 큰 숫자) */
  --font-display:
    ui-serif,
    "Iowan Old Style",
    "Apple Garamond",
    Baskerville,
    "Times New Roman",
    "Droid Serif",
    Times,
    "Source Serif Pro",
    serif;

  /* 본문: sans-serif + 한국어 Pretendard fallback */
  --font-sans:
    system-ui,
    -apple-system,
    "Segoe UI",
    "Pretendard Variable",
    Pretendard,
    Roboto,
    "Helvetica Neue",
    "Noto Sans KR",
    sans-serif;

  /* 데이터: 모노스페이스 (ID, IP, 숫자형 메타) */
  --font-mono:
    ui-monospace,
    "SF Mono",
    "Cascadia Mono",
    "Roboto Mono",
    Menlo,
    Consolas,
    monospace;
}
```

**스케일**:
- `h1` 대시보드 타이틀: `clamp(2.5rem, 5vw, 4rem)`, font-display, weight 400, letter-spacing -0.02em
- `h2` 섹션: `1.5rem`, font-sans, weight 600, 대문자, letter-spacing 0.08em
- 큰 숫자(stat-value): `clamp(3rem, 6vw, 5rem)`, font-display, weight 400
- 메타 라벨: `0.7rem`, font-sans, 대문자, letter-spacing 0.15em, muted-foreground
- 본문: `0.875rem`, font-sans
- 데이터(파일 ID, IP, 사이즈): `0.8125rem`, font-mono

### 레이아웃 원칙
- **사이드바**: 240px 고정, 좌측. 다크/라이트 그림자 없이 가는 1px 보더.
- **메인**: max-width 1280px, 좌우 padding 2rem (모바일 1rem). 큰 네거티브 스페이스.
- **카드**: 그림자 0. 보더 1px solid var(--border). 라운드 0. brutalist 사각 박스.
- **stat 카드**: 4-grid → 모바일에서 1-grid. 안에 거대한 숫자가 카드 사이즈 절반 이상 차지.
- **테이블**: 행 패딩 1rem, 상하 보더만, hover 시 배경 muted.
- **마스트헤드**: 상단에 `MEDIA OPERATIONS · v3.0.0` 같은 메타 라벨.
- **모바일**: 햄버거로 사이드바 오버레이, 메인은 col 1.

### 컴포넌트 클래스 (streamix-* 접두사)

| 클래스 | 역할 |
|--------|------|
| `.streamix-app` | 최상위 wrapper (data-theme 속성으로 light/dark) |
| `.streamix-sidebar` | 좌측 네비게이션 |
| `.streamix-main` | 콘텐츠 영역 |
| `.streamix-card`, `.streamix-card-header`, `.streamix-card-body`, `.streamix-card-footer` | 카드 |
| `.streamix-stat`, `.streamix-stat-value`, `.streamix-stat-label`, `.streamix-stat-meta` | 통계 카드 |
| `.streamix-btn`, `.streamix-btn-primary`, `.streamix-btn-ghost`, `.streamix-btn-danger`, `.streamix-btn-icon` | 버튼 |
| `.streamix-input`, `.streamix-input-group` | 폼 |
| `.streamix-badge`, `.streamix-badge--{type}` | 배지 (FileType 별) |
| `.streamix-table`, `.streamix-th`, `.streamix-td` | 테이블 |
| `.streamix-modal`, `.streamix-modal-overlay`, `.streamix-modal-content` | 모달 |
| `.streamix-toast`, `.streamix-toast-container` | 토스트 |
| `.streamix-pulse` (live dot), `.streamix-progress`, `.streamix-skeleton` | 상태/로딩 |
| `.streamix-meta` | 라벨 메타 텍스트 (대문자 작은 글씨) |
| `.streamix-mono` | 데이터 메타 (font-mono) |
| `.streamix-display` | 디스플레이 헤드라인 (font-display) |

### 아이콘 — Self-host SVG Sprite
- 출처: [Bootstrap Icons (MIT)](https://github.com/twbs/icons) — `<symbol>` 형식으로 한 파일 sprite.
- 사용 아이콘 (~25개):
  - 네비: `speedometer2`, `folder`, `broadcast`
  - 액션: `play-fill`, `eye`, `trash`, `cloud-upload`, `download`, `clipboard`, `check-circle`, `x-circle`
  - 파일타입: `image`, `camera-video`, `music-note-beamed`, `file-earmark-text`, `file-earmark-zip`, `file-earmark`
  - UI: `sun`, `moon`, `list` (햄버거), `chevron-left`, `chevron-right`, `exclamation-triangle`, `info-circle`, `wifi-off`
- 파일 위치: `streamix-spring-boot-starter/src/main/resources/static/streamix/svg/icons.svg`
- 사용: `<svg class="streamix-icon"><use href="/streamix/svg/icons.svg#icon-name"/></svg>`
- 라이센스: README에 Bootstrap Icons MIT 라이센스 고지 명시.

### 인터랙션/모션
- CSS transition만 사용 (200ms ease-out 표준).
- live dot: pulse 애니메이션 (이미 v1 dashboard.css에 있음 — 재활용).
- 페이지 로드 시 사이드바·메인 staggered fade-in (animation-delay).
- modal: fade + scale.
- hover: 카드는 살짝 보더 강화(border-strong)로 상호작용. translateY 없음 (brutalist는 깔끔하게).

## 상태 관리 (v1 동일, 단순화 재확인)

### EventBus + EVENTS (v1 동일)
[v1 plan.md 참조]

### Store
```javascript
const store = {
  state: {
    theme: 'system',         // 'system' | 'light' | 'dark'
    activeSessions: [],
    files: [],
    upload: { active: false, progress: 0 }
  },
  // ...
};
```

### Api (fetch wrapper)
```javascript
const Api = {
  baseUrl: document.body.dataset.apiBasePath,
  async request(method, path, opts = {}) {
    const res = await fetch(`${this.baseUrl}${path}`, { method, ...opts });
    if (!res.ok) throw new ApiError(res.status, await res.json().catch(() => ({})));
    return res.status === 204 ? null : res.json();
  },
  files: {
    list: (page, size) => Api.request('GET', `/files?page=${page}&size=${size}`),
    get:  (id) => Api.request('GET', `/files/${id}`),
    delete: (id) => Api.request('DELETE', `/files/${id}`),
  },
  sessions: {
    active: () => Api.request('GET', '/sessions/active')
  }
};
```

### Theme toggle (FOUC 방지)
```html
<!-- layout.html <head> 안 inline script — 첫 paint 전 즉시 적용 -->
<script>
  (function() {
    const stored = localStorage.getItem('streamix.theme');
    const sys = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    const theme = stored && stored !== 'system' ? stored : sys;
    document.documentElement.classList.toggle('dark', theme === 'dark');
  })();
</script>
```

### Sessions polling
```javascript
const SessionsPoller = {
  intervalId: null,
  start() {
    if (this.intervalId) return;
    this.intervalId = setInterval(async () => {
      try {
        const data = await Api.sessions.active();
        EventBus.emit(EVENTS.SESSIONS_REFRESHED, data);
      } catch (e) { /* silent */ }
    }, 5000);
  },
  stop() { clearInterval(this.intervalId); this.intervalId = null; }
};
```

## 변경 대상 파일 (v2)

### 생성

| 파일 | 이유 |
|------|------|
| `static/streamix/css/streamix.css` | **단일 디자인 시스템** (토큰 + base + 컴포넌트 + 유틸 — 한 파일) |
| `static/streamix/svg/icons.svg` | Bootstrap Icons MIT SVG sprite ~25개 |
| `static/streamix/js/event-bus.js` | EventBus + EVENTS |
| `static/streamix/js/store.js` | Store + state |
| `static/streamix/js/api.js` | Api + ApiError |
| `static/streamix/js/theme.js` | Theme toggle (light/dark/system) |
| `static/streamix/js/components/upload.js` | 업로드 로직 |
| `static/streamix/js/components/toast.js` | XSS-safe 토스트 |
| `static/streamix/js/components/modal.js` | 모달 (delete 확인 등) |
| `static/streamix/js/components/sessions-poller.js` | 5초 폴링 |
| `static/streamix/js/main.js` | 진입점 (init) |
| `starter/adapter/in/web/StreamixSessionsApiController.java` | `/api/streamix/sessions/active` JSON 엔드포인트 |
| `docs/plans/2026-05-10/v3-redesign/plan-v2.md` | **이 파일** |

### 삭제

| 파일 | 이유 |
|------|------|
| `static/streamix/css/dashboard.css` | 완전 재작성 (streamix.css로 대체) |
| `static/streamix/js/dashboard.js` | 모놀리식 → 모듈 분리 |
| `build.gradle`의 `org.webjars:bootstrap:5.3.2` | Bootstrap 제거 |
| `build.gradle`의 `org.webjars.npm:bootstrap-icons:1.11.1` | Icons 제거 |
| `build.gradle`의 `org.webjars:webjars-locator-lite:1.0.1` | 사용처 없음 |

### 수정

| 파일 | 변경 |
|------|------|
| `templates/streamix/layout.html` | Bootstrap class → streamix-* class + inline theme script + 모듈 import |
| `templates/streamix/dashboard.html` | brutalist 레이아웃 재작성 |
| `templates/streamix/files.html` | 업로드 모달 + 테이블 재작성 |
| `templates/streamix/file-detail.html` | 미디어 플레이어 + 정보 패널 재작성 |
| `templates/streamix/sessions.html` | 폴링 + 자동 새로고침 정상화 |
| `streamix-spring-boot-starter/build.gradle` | WebJars 3개 제거 |
| `build.gradle` (루트) | version `2.0.1-SNAPSHOT` → `3.0.0-SNAPSHOT` |
| `README.md` | v3.0.0 changelog + 마이그레이션 가이드 + Bootstrap Icons MIT 고지 |

## 구현 순서 (Phase) — v2 단순화

### Phase α — CSS + 아이콘 + 의존성 정리
1. `streamix.css` 작성 (OKLCH 토큰 + 시스템 폰트 + 컴포넌트 클래스 + brutalist 베이스)
2. `icons.svg` sprite 작성 (Bootstrap Icons SVG 자체 복사 ~25개)
3. `build.gradle` WebJars 3줄 제거
4. 검증: `./gradlew build` BUILD SUCCESSFUL

→ **v1의 npm/Tailwind 단계 4개가 1단계로 축소**.

### Phase β — JS 인프라
5. `event-bus.js`, `store.js`, `api.js`, `theme.js`, `main.js`
6. `components/{upload,toast,modal,sessions-poller}.js`
7. 임시: 새 모듈 추가하되 기존 `dashboard.js`는 유지 (충돌 검증)

### Phase γ — 페이지별 재작성
8. `layout.html` 재작성 (inline theme script + svg sprite preload + 모듈 import)
9. `dashboard.html` (brutalist stats + recent + active + popular)
10. `files.html` (테이블 + 업로드 모달 + 삭제 모달)
11. `file-detail.html` (미디어 플레이어 + 정보 패널)
12. `sessions.html` (실시간 모니터 + 폴링)

### Phase δ — 통합/정리
13. `StreamixSessionsApiController` 신규 (`/api/streamix/sessions/active`)
14. `dashboard.js`, `dashboard.css` 삭제
15. 루트 `build.gradle` version 3.0.0-SNAPSHOT
16. README v3.0.0 changelog + 마이그레이션 가이드 + Bootstrap Icons MIT 라이센스 고지

### 검증
17. `./gradlew clean build` BUILD SUCCESSFUL
18. 수동 검증 (5 페이지 + 다크/라이트 + 모바일 + 업로드/삭제 + 폴링)
19. `learned.md` + `project-overview.md` 갱신

## 절대 변경하지 않는 영역 (v1 동일)
- `streamix-core/` 전체
- `starter/autoconfigure/`
- `starter/adapter/in/web/StreamixApiController.java` (기존 REST API 시그니처)
- `starter/adapter/in/web/GlobalExceptionHandler.java`
- `starter/adapter/in/web/dto/*.java`
- `starter/adapter/out/persistence/*.java`
- `starter/service/StreamingMonitoringService.java` (비즈니스 로직)
- `starter/properties/StreamixProperties.java`
- `starter/annotation/EnableStreamix.java`
- 코드 리뷰 24개 이슈 개선 결과

## 트레이드오프 / 리스크

### 리스크 1: 시스템 폰트의 OS별 차이
- macOS는 디스플레이에서 New York (Apple Garamond fallback), Windows는 Cambria, Linux는 시스템 세리프.
- **이는 의도다.** 각 환경에서 distinctive하게 보이는 게 brutalist 컨셉과 맞음.
- 약점: 인쇄물 같은 일관된 룩앤필을 원하면 self-host 폰트 필요 → 거부함 (의존성 0 원칙).

### 리스크 2: Bootstrap Icons SVG self-host
- 25개 SVG를 직접 sprite로 만드는 작업.
- 라이센스: MIT — 허용. README에 고지.
- 미세한 디자인 부조화는 거의 없음 (Bootstrap Icons는 중립적 stroke).

### 리스크 3: 사용자 커스텀 CSS 호환성 (v1과 동일)
- v2 사용자가 `dashboard.css`를 override했다면 깨짐 → CHANGELOG 명시.

### 리스크 4: brutalist 미감의 호불호
- 영화관 컨셉이 미디어 라이브러리에 적합 — 그러나 보수적 admin tool 사용자에게는 unconventional.
- 사용자가 명시적으로 옵션 A를 선택 — 진행.

### 리스크 5: ES Module 브라우저 지원
- 모든 모던 브라우저 (Chrome 89+, FF 89+, Safari 14+) 지원. legacy 브라우저는 미지원 (Streamix는 admin tool이라 OK).

### 리스크 6: 다크모드 FOUC
- inline script로 첫 paint 전 적용 — 표준 패턴, 안전.

## 외부 큐레이션 (B1.5) — v1 자료 재평가

v1에 정리된 출처 중:
- ❌ Tailwind CSS v4 production — **무관해짐** (Tailwind 안 씀)
- △ shadcn/ui Theming OKLCH — 디자인 토큰 구조만 차용
- ✅ EventBus in Vanilla JS — 그대로 유효
- ✅ State Management 2026 — 그대로 유효

v2에서 추가:
- 시스템 폰트 스택 best practice: [system-ui 폰트 스택 (modern-font-stacks)](https://modernfontstacks.com/) — 시스템 폰트 카테고리별 정리.
- Bootstrap Icons MIT 라이센스 SVG: [twbs/icons](https://github.com/twbs/icons).

## 승인 게이트 (B2)

이 plan-v2 + 새 checklist 사용자 검토 후 승인 → Phase α 시작.

### 사용자 검토 시점에 결정할 항목 (남은 사항 없음)
v2에서 모든 선결정 사항이 해결됨. 큰 변경 없으면 Phase α 진행.

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-10 | v1 초기 작성 |
| 2026-05-11 | v2 작성 — 순수 CSS, 외부 의존성 0, Cinema/Editorial Brutalist 방향 |
