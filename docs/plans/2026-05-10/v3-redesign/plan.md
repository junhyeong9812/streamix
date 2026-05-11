# 계획서: v3.0.0 대시보드 모던 재설계

## 작업 메타
- **작업명**: v3-redesign
- **시작일**: 2026-05-10
- **유형**: 기능 구현 (대시보드 UI 재설계) + 리팩토링 (상태관리)
- **규모**: **대** (4 페이지 + layout + CSS 822줄 → 새 시스템 + JS 515줄 → Event Bus 재구성)
- **목표 버전**: v3.0.0 (semver major bump — Bootstrap → Tailwind 교체로 사용자 커스텀 CSS와의 호환성 깨짐)

## 사용자 결정 사항 (2026-05-10)

| 항목 | 결정 | 근거 |
|------|------|------|
| 버전 | **3.0.0** | major bump — UI 프레임워크 전면 교체 (Bootstrap → Tailwind), 기존 .css 커스터마이징 사용자 영향 |
| 디자인 | **Tailwind + shadcn-style 테마** | 모던 SaaS 감성, OKLCH 토큰, 다크모드 기본 |
| 상태관리 | **Vanilla JS + Custom Event Bus** | 프레임워크 의존 없음, framework-agnostic |
| 진행 | **plan 우선 → 사용자 검토 → 구현** | 대규모 디자인 작업 — 방향 합의 필수 |

## 외부 큐레이션 (B1.5)

### Tailwind CSS v4 — production 통합 옵션
| 옵션 | 장점 | 단점 | 채택 |
|------|------|------|------|
| A. Play CDN (`@tailwindcss/browser@4`) | 설치 0, 즉시 사용 | dev 전용. JIT 컴파일이 사용자 브라우저에서 → 성능/CPU 부담 | ❌ |
| B. WebJars `tailwindcss` | 자동 의존성 관리 | 메인테이너가 prebuilt CSS만 제공, JIT 안 됨. 우리가 사용하는 클래스만 포함 안 됨 | △ 검토 |
| C. **Tailwind CLI로 starter 빌드 시 CSS 생성** | 우리가 사용하는 클래스만 → ~10-25KB. 사용자 환경 빌드 불필요 | starter Gradle에 Node.js 빌드 단계 추가 (build task) | ✅ |
| D. **prebuilt 정적 CSS 파일 직접 포함** | C와 거의 같지만 npm 사용자 측 빌드만 → starter 빌드 환경 단순 | 클래스 추가/삭제 시 매뉴얼 재빌드 필요 | ✅ (대안) |

→ **C + D 혼합**: 개발 시 `npm run build:css` 수동 실행으로 정적 파일 생성, 그 결과(`tailwind.css`)를 git commit. starter Gradle 빌드는 Java만. 단순 + 빠름.

> 출처: [Tailwind CSS v4](https://tailwindcss.com/blog/tailwindcss-v4), [Play CDN docs](https://tailwindcss.com/docs/installation/play-cdn) — "CDN is dev-only and should not be shipped to production"

### shadcn-style 디자인 토큰 (OKLCH)
shadcn/ui는 React 컴포넌트 라이브러리이지만, **디자인 토큰(CSS variables)만** 차용 가능. Tailwind v4와 자연 통합.

```css
/* shadcn New York style 기본 light theme — OKLCH */
:root {
  --background: oklch(1 0 0);
  --foreground: oklch(0.145 0 0);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0.145 0 0);
  --primary: oklch(0.205 0 0);            /* near-black */
  --primary-foreground: oklch(0.985 0 0); /* near-white */
  --secondary: oklch(0.97 0 0);
  --muted: oklch(0.97 0 0);
  --muted-foreground: oklch(0.556 0 0);
  --accent: oklch(0.97 0 0);
  --accent-foreground: oklch(0.205 0 0);
  --destructive: oklch(0.577 0.245 27.325);
  --border: oklch(0.922 0 0);
  --input: oklch(0.922 0 0);
  --ring: oklch(0.708 0 0);
  --radius: 0.5rem;
  /* Streamix 브랜드 액센트 */
  --brand: oklch(0.55 0.2 264);            /* indigo-ish */
  --brand-foreground: oklch(0.985 0 0);
}
.dark {
  --background: oklch(0.145 0 0);
  --foreground: oklch(0.985 0 0);
  --card: oklch(0.205 0 0);
  --card-foreground: oklch(0.985 0 0);
  --primary: oklch(0.985 0 0);
  --primary-foreground: oklch(0.205 0 0);
  --secondary: oklch(0.269 0 0);
  --muted: oklch(0.269 0 0);
  --muted-foreground: oklch(0.708 0 0);
  --accent: oklch(0.269 0 0);
  --accent-foreground: oklch(0.985 0 0);
  --destructive: oklch(0.704 0.191 22.216);
  --border: oklch(1 0 0 / 10%);
  --input: oklch(1 0 0 / 15%);
  --ring: oklch(0.556 0 0);
  --brand: oklch(0.65 0.22 264);
}
```

> 출처: [shadcn/ui Theming](https://ui.shadcn.com/docs/theming), [Variables Docs](https://www.shadcndesign.com/docs/variables)

### Event Bus 패턴
세 가지 옵션 비교:

| 옵션 | 구현 | 장점 | 단점 |
|------|------|------|------|
| A. 네이티브 CustomEvent | `window.dispatchEvent(new CustomEvent('streamix:upload', { detail }))` | 의존성 0, 디버거에서 추적 | global pollution, 이름 충돌 위험 |
| B. **Custom EventBus 클래스** | private subscribers Map + on/off/emit | 명시적 이름공간, cleanup 보장, 테스트 용이 | 추가 코드 |
| C. EventTarget 서브클래스 | `class EventBus extends EventTarget` | 표준 API + 캡슐화 | addEventListener 시그니처 약간 verbose |

→ **B + 명명 상수**: `EVENTS.FILE_UPLOADED`, `EVENTS.SESSION_UPDATED` 등 상수 객체. cleanup function 반환.

```javascript
// Streamix.eventBus
const EventBus = (() => {
  const subs = new Map();  // event → Set<handler>
  return {
    on(event, handler) {
      if (!subs.has(event)) subs.set(event, new Set());
      subs.get(event).add(handler);
      return () => subs.get(event)?.delete(handler);  // cleanup
    },
    emit(event, payload) {
      subs.get(event)?.forEach(h => {
        try { h(payload); } catch (e) { console.error(`[Streamix] handler error for ${event}:`, e); }
      });
    },
    off(event, handler) {
      subs.get(event)?.delete(handler);
    }
  };
})();

const EVENTS = Object.freeze({
  FILE_UPLOAD_STARTED: 'file:upload:started',
  FILE_UPLOAD_PROGRESS: 'file:upload:progress',
  FILE_UPLOAD_SUCCESS: 'file:upload:success',
  FILE_UPLOAD_ERROR: 'file:upload:error',
  FILE_DELETED: 'file:deleted',
  SESSIONS_REFRESHED: 'sessions:refreshed',
  THEME_CHANGED: 'theme:changed',
  TOAST: 'ui:toast'
});
```

> 출처: [EventBus in Vanilla JS](https://docs.bswen.com/blog/2026-04-30-event-bus-pattern-javascript/), [State Management 2026](https://medium.com/@chirag.dave/state-management-in-vanilla-js-2026-trends-f9baed7599de)

## 디자인 시스템 결정

### 1. 컬러 팔레트
- shadcn New York 스타일 기본 (neutral grayscale + 단일 액센트)
- 액센트: indigo `oklch(0.55 0.2 264)` (Streamix 브랜드)
- 다크모드 기본 활성 (`<html class="dark">`)
- 사용자 토글 가능 (localStorage 저장)

### 2. 타이포그래피
- 시스템 폰트 스택 (network 의존 없음): `system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif`
- 한국어 폰트: `"Pretendard Variable", Pretendard, ...` (선택적 — 사용자 설치 시 적용)
- 사이즈: `text-xs (12px)`, `text-sm (14px)`, `text-base (16px)`, `text-lg (18px)`, `text-xl (20px)`, `text-2xl (24px)`, `text-3xl (30px)`

### 3. 스페이싱/레이아웃
- 사이드바: 240px 고정 (모바일 collapsible)
- 콘텐츠: `max-w-7xl mx-auto p-6`
- 카드 radius: `--radius (0.5rem)`
- Grid: `grid-cols-1 md:grid-cols-2 lg:grid-cols-4` (반응형)

### 4. 컴포넌트 (CSS 클래스 패턴)
shadcn 컨벤션 따라 CSS variables + Tailwind utility 조합:
- `.btn` (`btn-primary`, `btn-secondary`, `btn-destructive`, `btn-ghost`)
- `.card` (`card-header`, `card-content`, `card-footer`)
- `.input` (`input-field`)
- `.badge` (`badge-success`, `badge-warning`, `badge-info`, `badge-destructive`)
- `.toast` (`toast-success`, `toast-error`)
- `.modal` (`modal-overlay`, `modal-content`)
- `.skeleton` (loading state)
- `.spinner` (loading)

### 5. 아이콘
- Bootstrap Icons 제거
- **Lucide icons** (shadcn 표준) — SVG inline 또는 Tailwind 통합 가능
- WebJars `lucide-static` (있으면) 또는 prebuilt sprite
- → 검토: 사용 아이콘 ~30개라 inline SVG sprite로 충분

### 6. 애니메이션
- Tailwind transition utilities (`transition-all`, `duration-200`, `ease-out`)
- shadcn 표준: `animate-in fade-in slide-in-from-top-2`
- skeleton pulse: `animate-pulse`

### 7. 다크모드 토글
- 사이드바 footer에 토글 버튼 (sun/moon icon)
- localStorage `streamix.theme: light | dark | system`
- system이면 `prefers-color-scheme` media query 따름
- 페이지 로드 시 첫 paint 전 적용 (FOUC 방지) — `<head>` 안 inline script

## 상태 관리 아키텍처

### 1. Event Bus (위 외부 큐레이션 참조)

### 2. Store (단순 객체)
```javascript
const store = {
  state: {
    theme: 'system',
    activeSessions: [],
    files: [],
    pagination: { page: 0, size: 20, total: 0 },
    upload: { active: false, progress: 0 }
  },
  set(path, value) {
    // path: 'pagination.page'
    const parts = path.split('.');
    let target = this.state;
    for (let i = 0; i < parts.length - 1; i++) target = target[parts[i]];
    target[parts.at(-1)] = value;
    EventBus.emit('store:changed', { path, value });
  },
  get(path) { /* ... */ },
  subscribe(path, handler) { /* ... */ }
};
```

### 3. API 클라이언트 (fetch 추상화)
```javascript
const Api = {
  baseUrl: document.body.dataset.apiBasePath,
  async request(method, path, options = {}) {
    const res = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers: { 'Accept': 'application/json', ...options.headers },
      body: options.body
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new ApiError(res.status, err);
    }
    return res.status === 204 ? null : await res.json();
  },
  files: {
    list: (page, size) => Api.request('GET', `/files?page=${page}&size=${size}`),
    get: (id) => Api.request('GET', `/files/${id}`),
    delete: (id) => Api.request('DELETE', `/files/${id}`),
    upload: (file, onProgress) => /* XHR for progress */
  }
};
```

### 4. 자동 새로고침 (sessions 페이지)
```javascript
const SessionsPolling = {
  intervalId: null,
  start() {
    this.intervalId = setInterval(async () => {
      try {
        const sessions = await fetch('/streamix/api/sessions/active').then(r => r.json());
        store.set('activeSessions', sessions);
        EventBus.emit(EVENTS.SESSIONS_REFRESHED, sessions);
      } catch (e) { /* silent */ }
    }, 5000);
  },
  stop() { clearInterval(this.intervalId); this.intervalId = null; }
};
```

⚠ **주의**: 현재 dashboard.js의 `refreshActiveSessions`는 시간만 업데이트하고 실제 fetch는 안 함. 이번에 정상 구현. 단, 새로운 server-side endpoint 필요할 수 있음 (현재는 SSR로만 제공).

→ **결정**: REST API에 `/api/streamix/sessions/active` 엔드포인트 추가 또는 기존 dashboard URL을 fetch + JSON으로 응답하는 컨트롤러 분리.

## 변경 대상 파일 (전체)

### 생성 (10+개)

| 파일 | 이유 |
|------|------|
| `streamix-spring-boot-starter/src/main/resources/static/streamix/css/tailwind.css` | Tailwind 빌드 결과 (prebuilt) |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/css/streamix.css` | shadcn 토큰 + 컴포넌트 클래스 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/event-bus.js` | EventBus + EVENTS 상수 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/store.js` | Store + state 정의 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/api.js` | API 클라이언트 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/theme.js` | 다크모드 토글 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/upload.js` | 업로드 로직 분리 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/toast.js` | Toast 컴포넌트 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/modal.js` | 삭제 확인 모달 등 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/sessions-poller.js` | 5초 폴링 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/components/icons.js` | Lucide SVG sprite |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/main.js` | 진입점 (init) |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixSessionsApiController.java` | `/api/streamix/sessions/active` JSON 엔드포인트 |
| `tailwind.config.js` (프로젝트 루트) | Tailwind 설정 |
| `package.json` (프로젝트 루트) | Tailwind CLI dev dependency |
| `docs/concepts/frontend-design/README.md` | 디자인 시스템 문서 |

### 삭제

| 파일 | 이유 |
|------|------|
| `streamix-spring-boot-starter/src/main/resources/static/streamix/css/dashboard.css` | Bootstrap 기반 — 전면 재작성 (streamix.css로 대체) |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js` | 모놀리식 → 모듈로 분리 (event-bus/store/api/components/...) |
| WebJars Bootstrap 의존성 (`build.gradle`) | Tailwind로 교체 |
| WebJars Bootstrap Icons | Lucide SVG로 교체 |

### 수정

| 파일 | 변경 |
|------|------|
| `templates/streamix/layout.html` | Bootstrap 클래스 → Tailwind 클래스 + JS 모듈 import + theme inline script |
| `templates/streamix/dashboard.html` | 카드/그리드 재설계 (shadcn 스타일) |
| `templates/streamix/files.html` | 테이블 + 업로드 모달 재설계 |
| `templates/streamix/file-detail.html` | 미디어 플레이어 + 정보 패널 재설계 |
| `templates/streamix/sessions.html` | 실시간 모니터 + 자동 새로고침 |
| `streamix-spring-boot-starter/build.gradle` | WebJars Bootstrap 제거 |
| `build.gradle` | version `2.0.1-SNAPSHOT` → `3.0.0-SNAPSHOT` |
| `README.md` | v3.0.0 changelog |

## 구현 순서 (Phase)

### Phase α — 디자인 시스템 + 인프라 (1단계 검증 가능)
1. `tailwind.config.js` + `package.json` 작성
2. `streamix.css` (shadcn 토큰 + 컴포넌트 클래스)
3. Tailwind CLI로 `tailwind.css` prebuilt 생성 + commit
4. WebJars Bootstrap 제거
5. 검증: 빌드 통과

### Phase β — JS 인프라
6. `event-bus.js`, `store.js`, `api.js`, `theme.js`
7. `components/icons.js` (Lucide SVG sprite)
8. `main.js` 진입점
9. 검증: `dashboard.js` 임시 두고 새 모듈만 추가

### Phase γ — 페이지별 재작성
10. `layout.html` 재작성 (Tailwind + theme toggle)
11. `dashboard.html`
12. `files.html` + `components/upload.js` + `components/modal.js`
13. `file-detail.html`
14. `sessions.html` + `components/sessions-poller.js`

### Phase δ — 통합/정리
15. 새 server-side 엔드포인트 `/api/streamix/sessions/active` (JSON)
16. 기존 `dashboard.js`, `dashboard.css` 삭제
17. version 3.0.0
18. README + Changelog
19. 검증: 빌드 + 모든 페이지 수동 확인

## 절대 변경하지 않는 영역 (금지)

- `streamix-core/` 전체 — 도메인/어플리케이션 로직 그대로
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/` — autoconfig 그대로
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixApiController.java` — 기존 REST API 시그니처 유지
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/` — JPA 그대로
- `streamix-spring-boot-starter/src/main/java/.../starter/service/StreamingMonitoringService.java` — 비즈니스 로직 그대로
- 기존 P0~P2 24개 이슈 작업 결과 — 모두 유지

## 트레이드오프 / 리스크

### 리스크 1: Tailwind 빌드 단계 추가
- prebuilt CSS 방식이라 사용자 빌드는 영향 없음
- 개발자(Streamix 메인테이너)는 새 클래스 추가 시 `npm run build:css` 수동 실행 필요
- → 명확한 README 안내 + 가능하면 Gradle task로 wrapping

### 리스크 2: 사용자 커스텀 CSS 호환성
- v2 사용자가 `dashboard.css`를 override했다면 v3에서 깨짐
- 해결: CHANGELOG breaking change 명시, 마이그레이션 가이드 작성

### 리스크 3: SVG sprite 관리
- Lucide 약 30개 아이콘만 사용 → 직접 inline 또는 sprite
- 사용 아이콘 결정 필요 (메뉴, 액션, 파일타입, 상태)

### 리스크 4: WebJars Locator
- v2.0.1에서 `webjars-locator-lite` 추가했지만 v3에선 WebJars Bootstrap 제거 → locator 불필요?
- → Lucide SVG도 WebJars로 가져오면 locator 유지. Tailwind는 자체 호스팅이므로 불필요. 일단 유지(작은 의존성)

### 리스크 5: 다크모드 FOUC (Flash of Unstyled Content)
- 페이지 로드 시 light → dark 깜빡임 방지 위해 `<head>` 안 inline script로 즉시 적용
- 이건 표준 패턴이라 안전

## 외부 큐레이션 (B1.5) — 출처
- [Tailwind CSS v4 Production](https://tailwindcss.com/blog/tailwindcss-v4)
- [shadcn/ui Theming](https://ui.shadcn.com/docs/theming)
- [shadcn/ui + Tailwind v4](https://ui.shadcn.com/docs/tailwind-v4)
- [EventBus in Vanilla JS](https://docs.bswen.com/blog/2026-04-30-event-bus-pattern-javascript/)
- [State Management 2026 Trends](https://medium.com/@chirag.dave/state-management-in-vanilla-js-2026-trends-f9baed7599de)

## 승인 게이트 (B2)
이 plan을 사용자가 검토 후 승인하면 Phase α부터 순차 구현.
사용자 검토 시점에 결정해야 할 항목:
1. **Tailwind 통합 방식**: C(Gradle build task wrap) vs D(수동 prebuilt + commit) — 어느 쪽?
2. **WebJars Locator 유지/제거**: Lucide도 WebJars 사용? 직접 inline?
3. **다크모드 기본**: 페이지 로드 시 light/dark/system 중 default?
4. **새 엔드포인트 필요**: `/api/streamix/sessions/active` 추가 OK?
5. **Tailwind 설정 위치**: `tailwind.config.js`를 프로젝트 루트 vs starter 모듈 안?

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-10 | 초기 작성 (사용자 결정: 3.0.0 + Tailwind + shadcn + Vanilla JS Event Bus) |
