# 학습 기록: v3-redesign (Brutalist + Zero-Dependency)

> 본 작업(2026-05-11)에서 새로 만들거나 익힌 모든 기술을 정리한 문서.

## 1. 사용된 라이브러리

| 라이브러리 | 버전 | 용도 | 선택 이유 |
|-----------|------|------|----------|
| Bootstrap Icons SVG (self-host) | 최신 raw SVG | 25개 아이콘 sprite | MIT 라이센스, 중립적 stroke. 외부 의존성 0 원칙 유지하면서 직접 그리는 부담 회피 |
| Spring Boot Starter Thymeleaf | 4.0.0 | 템플릿 엔진 | 기존 유지 — layout decorator(`thymeleaf-layout-dialect`) 활용 |
| Thymeleaf Layout Dialect | 3.4.0 | `layout:decorate` 패턴 | 기존 유지 |
| Spring Web MVC | 4.0.0 | REST API + 정적 자원 서빙 | 기존 유지, 새 JSON 컨트롤러 추가 |
| Spring Data JPA | 4.0.0 | `StreamingMonitoringService` 의존 | 기존 유지 |

### 제거된 라이브러리 (v2 → v3)
| 라이브러리 | 이유 |
|-----------|------|
| `org.webjars:bootstrap:5.3.2` | 사용자 의존성 최소화 방침 + 자체 CSS로 대체 |
| `org.webjars.npm:bootstrap-icons:1.11.1` | SVG sprite를 self-host로 차용 |
| `org.webjars:webjars-locator-lite:1.0.1` | WebJars 자체 제거 → 불필요 |

### 도입을 고려했다가 거부한 라이브러리
| 라이브러리 | 거부 이유 |
|-----------|---------|
| Tailwind CSS v4 | "라이브러리를 가져오면 의미가 없다" — npm 빌드 단계 추가 회피 |
| shadcn/ui (React 컴포넌트) | 프로젝트는 Thymeleaf SSR. React stack 도입 불필요 |
| Lucide icons | Bootstrap Icons MIT로 충분, 별도 라이브러리 추가 회피 |
| Google Fonts (Pretendard, Inter 등) | 외부 네트워크 의존 — 시스템 폰트 스택만 사용 |

## 2. 핵심 함수/메서드

### 2.1 EventBus (Pub/Sub)

**파일**: `static/streamix/js/event-bus.js`

```javascript
const subs = new Map();

export const EventBus = {
  on(event, handler) {
    if (!subs.has(event)) subs.set(event, new Set());
    subs.get(event).add(handler);
    return () => subs.get(event)?.delete(handler);
  },
  off(event, handler) {
    subs.get(event)?.delete(handler);
  },
  emit(event, payload) {
    const handlers = subs.get(event);
    if (!handlers) return;
    handlers.forEach((h) => {
      try { h(payload); }
      catch (e) { console.error(`[Streamix] handler error for ${event}:`, e); }
    });
  }
};

export const EVENTS = Object.freeze({
  FILE_UPLOAD_STARTED:  'file:upload:started',
  FILE_UPLOAD_PROGRESS: 'file:upload:progress',
  FILE_UPLOAD_SUCCESS:  'file:upload:success',
  FILE_UPLOAD_ERROR:    'file:upload:error',
  FILE_DELETED:         'file:deleted',
  SESSIONS_REFRESHED:   'sessions:refreshed',
  THEME_CHANGED:        'theme:changed',
  TOAST:                'ui:toast',
  MODAL_OPEN:           'ui:modal:open',
  MODAL_CLOSE:          'ui:modal:close',
  STORE_CHANGED:        'store:changed'
});
```

**설명**: `Map<event, Set<handler>>` 자료구조로 framework-agnostic Pub/Sub. `on()`이 cleanup function을 반환해 unsubscribe가 명확하고, `emit()`은 try/catch로 하나의 handler 실패가 전체를 멈추지 않게 한다. EVENTS 상수는 magic string 회피.

### 2.2 Store (단순 경로 기반 상태)

**파일**: `static/streamix/js/store.js`

```javascript
import { EventBus, EVENTS } from './event-bus.js';

const state = {
  theme: 'system',
  activeSessions: [],
  files: [],
  pagination: { page: 0, size: 20, total: 0 },
  upload: { active: false, progress: 0 }
};

function resolve(path) {
  return path.split('.').reduce((o, k) => (o == null ? o : o[k]), state);
}

function set(path, value) {
  const parts = path.split('.');
  let target = state;
  for (let i = 0; i < parts.length - 1; i++) {
    if (target[parts[i]] == null || typeof target[parts[i]] !== 'object') {
      target[parts[i]] = {};
    }
    target = target[parts[i]];
  }
  target[parts.at(-1)] = value;
  EventBus.emit(EVENTS.STORE_CHANGED, { path, value });
}

export const Store = { get: resolve, set, state };
```

**설명**: `'pagination.page'` 같은 dot path로 read/write. `set` 호출 시 `STORE_CHANGED` 이벤트 발행 — 구독자는 path를 보고 필요한 경우만 반응.

### 2.3 Api (fetch wrapper + ApiError)

**파일**: `static/streamix/js/api.js`

```javascript
export class ApiError extends Error {
  constructor(status, body) {
    super((body && body.message) || `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body || {};
  }
}

const baseUrl = (document.body && document.body.dataset.apiBasePath) || '/api/streamix';

async function request(method, path, options = {}) {
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers: { Accept: 'application/json', ...(options.headers || {}) },
    body: options.body,
    credentials: 'same-origin'
  });
  if (!res.ok) {
    let body = {};
    try { body = await res.json(); } catch { body = { message: res.statusText }; }
    throw new ApiError(res.status, body);
  }
  if (res.status === 204) return null;
  const ctype = res.headers.get('content-type') || '';
  return ctype.includes('json') ? res.json() : res.text();
}

export const Api = {
  baseUrl, request,
  files: {
    list:   (page = 0, size = 20) => request('GET', `/files?page=${page}&size=${size}`),
    get:    (id) => request('GET', `/files/${id}`),
    delete: (id) => request('DELETE', `/files/${id}`)
  },
  sessions: {
    active: () => request('GET', '/sessions/active')
  }
};
```

**설명**: `document.body.dataset.apiBasePath`를 base URL로 사용 — Thymeleaf layout이 `${@streamixProperties.api.basePath}`를 data attribute로 주입. `204 No Content`와 비-JSON 응답을 분기 처리. 에러는 `ApiError`로 status + body를 보존.

### 2.4 Theme (light/dark/system)

**파일**: `static/streamix/js/theme.js`

```javascript
import { EventBus, EVENTS } from './event-bus.js';

const KEY = 'streamix.theme';
const VALID = new Set(['system', 'light', 'dark']);

function getStored() {
  const v = localStorage.getItem(KEY);
  return VALID.has(v) ? v : 'system';
}

function resolve(theme) {
  if (theme === 'system') {
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return theme;
}

function apply(theme) {
  const actual = resolve(theme);
  document.documentElement.classList.toggle('dark', actual === 'dark');
  EventBus.emit(EVENTS.THEME_CHANGED, { theme, resolved: actual });
}

export const Theme = {
  current: getStored(),
  resolved() { return resolve(this.current); },
  set(theme) {
    if (!VALID.has(theme)) return;
    this.current = theme;
    localStorage.setItem(KEY, theme);
    apply(theme);
  },
  toggle() {
    const next = this.current === 'light' ? 'dark'
               : this.current === 'dark'  ? 'system' : 'light';
    this.set(next);
  },
  init() {
    apply(this.current);
    const mq = matchMedia('(prefers-color-scheme: dark)');
    const listener = () => { if (this.current === 'system') apply('system'); };
    if (mq.addEventListener) mq.addEventListener('change', listener);
    else if (mq.addListener) mq.addListener(listener);
  }
};
```

**설명**: 3-state 순환 토글(light → dark → system → light). system mode일 때 `prefers-color-scheme` 미디어 쿼리에 listener 등록 — OS 설정 변경 시 자동 갱신. FOUC 방지를 위해 `layout.html` `<head>`에 inline script가 별도로 첫 paint 전에 동일 로직을 실행.

### 2.5 SessionsPoller (visibility-aware polling)

**파일**: `static/streamix/js/components/sessions-poller.js`

```javascript
import { Api } from '../api.js';
import { EventBus, EVENTS } from '../event-bus.js';

let intervalId = null;
let lastError = null;

async function tick() {
  if (document.visibilityState !== 'visible') return;
  try {
    const sessions = await Api.sessions.active();
    lastError = null;
    EventBus.emit(EVENTS.SESSIONS_REFRESHED, sessions);
  } catch (e) {
    lastError = e;
  }
}

export const SessionsPoller = {
  start(periodMs = 5000) {
    if (intervalId) return;
    tick();
    intervalId = setInterval(tick, periodMs);
  },
  stop() {
    if (intervalId) { clearInterval(intervalId); intervalId = null; }
  },
  isRunning() { return intervalId !== null; },
  lastError() { return lastError; }
};
```

**설명**: `document.visibilityState !== 'visible'`일 때 fetch 자체를 건너뜀 — 탭이 백그라운드일 때 API 호출 낭비 회피. 시작 시 즉시 한 번 tick + 이후 5초마다.

### 2.6 Inline Theme Bootstrap Script (FOUC 방지)

**파일**: `templates/streamix/layout.html` `<head>`

```html
<script th:inline="none">
  (function () {
    try {
      var stored = localStorage.getItem('streamix.theme');
      var theme = (stored === 'light' || stored === 'dark') ? stored
        : (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
      if (theme === 'dark') document.documentElement.classList.add('dark');
    } catch (e) { /* localStorage 비활성 환경 */ }
  })();
</script>
```

**설명**: ES Module은 deferred. CSS 적용 전 다크모드 결정이 필요해서 `<head>`에서 일반 IIFE로 즉시 `html.dark` 클래스를 부여. `th:inline="none"`로 Thymeleaf의 JS inline 처리 회피.

### 2.7 SVG Sprite `<use>` 패턴

**Thymeleaf 표현**:
```html
<svg class="streamix-icon" aria-hidden="true">
  <use th:attr="href=|@{/streamix/svg/icons.svg}#icon-broadcast-pin|"></use>
</svg>
```

**JS 동적 swap (theme toggle 아이콘)**:
```javascript
const use = btn.querySelector('use');
if (use) use.setAttribute('href', `${spriteBase()}#icon-${isDark ? 'sun' : 'moon'}`);
```

**설명**: 외부 sprite 파일(`/streamix/svg/icons.svg`)에 `<symbol id="icon-xxx">`로 정의된 아이콘을 `<use href="...#icon-xxx">`로 참조. `xlink:href`는 deprecate, 모던 브라우저는 `href`만으로 충분. Thymeleaf URL fragment는 `|...|` literal substitution으로 합쳐서 안전.

### 2.8 StreamixSessionsApiController (Java)

**파일**: `starter/adapter/in/web/StreamixSessionsApiController.java`

```java
@RestController
public class StreamixSessionsApiController {

  private final StreamingMonitoringService monitoringService;

  public StreamixSessionsApiController(StreamingMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @GetMapping("${streamix.api.base-path:/api/streamix}/sessions/active")
  public ResponseEntity<List<ActiveSessionResponse>> activeSessions() {
    List<StreamingSessionEntity> sessions = monitoringService.getActiveSessions();
    log.debug("Active sessions polled: count={}", sessions.size());
    return ResponseEntity.ok(sessions.stream().map(ActiveSessionResponse::from).toList());
  }

  public record ActiveSessionResponse(
      Long id,
      UUID fileId,
      String clientIp,
      LocalDateTime startedAt,
      long bytesSent,
      String status
  ) {
    public static ActiveSessionResponse from(StreamingSessionEntity entity) {
      return new ActiveSessionResponse(
          entity.getId(), entity.getFileId(), entity.getClientIp(),
          entity.getStartedAt(), entity.getBytesSent(), entity.getStatus().name()
      );
    }
  }
}
```

**설명**: JPA 엔티티를 직접 노출하지 않고 `record`로 명시 변환 — LAZY/순환참조/필요 없는 필드 노출 회피. `${streamix.api.base-path:/api/streamix}` placeholder 패턴으로 사용자 base path 설정 존중.

### 2.9 등록 패턴 (StreamixMonitoringConfiguration)

```java
@Bean
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "streamix.api.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(StreamixSessionsApiController.class)
public StreamixSessionsApiController streamixSessionsApiController(
    StreamingMonitoringService monitoringService
) {
  log.info("Creating StreamixSessionsApiController for active sessions polling");
  return new StreamixSessionsApiController(monitoringService);
}
```

**설명**: Sessions API는 **모니터링 의존** + **API enabled** + **Servlet 환경** 3중 조건. `@ConditionalOnMissingBean`으로 사용자가 자체 Bean을 정의했을 경우 override 허용.

## 3. 어노테이션 / 데코레이터

### Spring 어노테이션 (Java)
| 어노테이션 | 위치 | 동작 원리 |
|-----------|------|----------|
| `@RestController` | StreamixSessionsApiController | `@Controller` + `@ResponseBody` 메타. 반환 객체를 HttpMessageConverter(Jackson)로 JSON 직렬화 |
| `@GetMapping("${...}")` | activeSessions() | property placeholder 해석 후 URL 매핑. `streamix.api.base-path` 누락 시 `/api/streamix` fallback |
| `@Bean` | streamixSessionsApiController() | 메서드 반환을 Spring Context Bean으로 등록 |
| `@Configuration(proxyBeanMethods = false)` | StreamixMonitoringConfiguration | proxy 생성 없이 빠른 lite-mode configuration |
| `@ConditionalOnWebApplication(SERVLET)` | Bean 메서드 | 서블릿 스택일 때만 등록 (Reactive WebFlux 환경 회피) |
| `@ConditionalOnProperty(matchIfMissing = true)` | Bean 메서드 | 속성 미설정 시 기본 활성 |
| `@ConditionalOnMissingBean` | Bean 메서드 | 사용자가 자체 Bean을 정의했다면 자동 등록 skip |
| `@ConditionalOnBean(DataSource.class)` | Configuration | DataSource Bean 존재 시에만 활성 (JPA 환경 검사) |
| `@AutoConfigureAfter(...)` | Configuration | 다른 Configuration 처리 후에 평가 |

### Thymeleaf 디렉티브
| 디렉티브 | 동작 |
|---------|------|
| `layout:decorate="~{streamix/layout}"` | layout-dialect로 layout 템플릿의 fragment 슬롯에 콘텐츠 삽입 |
| `layout:fragment="content"` | layout에서 콘텐츠가 들어갈 자리 |
| `layout:title-pattern="$CONTENT_TITLE — Streamix"` | 자식 페이지 title을 부모 패턴에 삽입 |
| `th:href`, `th:src` | URL 표현 (contextPath 자동 prefix) |
| `th:text` | 텍스트 콘텐츠 (HTML escape) |
| `th:if`, `th:unless` | 조건 렌더 |
| `th:each="x, stat : ${list}"` | 반복 + stat(index, last, ...) |
| `th:classappend` | 기존 class 유지하고 추가 클래스 부여 |
| `th:attr="href=|...|"` | 일반 속성 값 표현. `|...|` 안에서 Thymeleaf 표현식 inline 삽입 가능 |
| `th:data-...` | data attribute. 그대로 `data-...`로 렌더 |
| `th:inline="none"` | 해당 영역 안에서 `[[...]]` inline 표현 처리 안 함 (theme bootstrap script에 사용) |

### CSS 표기
| 표기 | 의미 |
|------|------|
| `oklch(L C H)` | OKLCH 컬러 (perceptually uniform) — L: 0~1 명도, C: chroma, H: 0~360 hue |
| `oklch(0.985 0.005 85)` | 거의 흰색, 채도 0.005, 85° hue (warm paper) |
| `oklch(0 0 0 / 0.45)` | rgba에 해당하는 alpha 표기 (slash 후 0~1) |
| `clamp(min, fluid, max)` | 반응형 fluid 값. `clamp(2.5rem, 5vw, 4rem)` = 뷰포트 5% 변화 + 클램프 |

## 4. 수정 전/후 코드 비교

### 4.1 layout.html — Bootstrap → streamix-* 클래스

**v2 (이전)**:
```html
<link th:href="@{/webjars/bootstrap/css/bootstrap.min.css}" rel="stylesheet">
<link th:href="@{/webjars/bootstrap-icons/font/bootstrap-icons.css}" rel="stylesheet">
<link th:href="@{/streamix/css/dashboard.css}" rel="stylesheet">
...
<nav class="sidebar">
  <a class="brand"><i class="bi bi-play-circle-fill"></i> Streamix</a>
  ...
</nav>
<script th:src="@{/webjars/bootstrap/js/bootstrap.bundle.min.js}"></script>
<script th:src="@{/streamix/js/dashboard.js}"></script>
```

**v3 (현재)**:
```html
<link rel="stylesheet" th:href="@{/streamix/css/streamix.css}">
<link rel="preload" as="image" type="image/svg+xml" th:href="@{/streamix/svg/icons.svg}">
...
<aside class="streamix-sidebar">
  <a class="streamix-sidebar-brand">
    <svg class="streamix-sidebar-brand-mark"><use href="...#icon-broadcast-pin"/></svg>
    <span>Streamix</span>
  </a>
  ...
</aside>
<script type="module" th:src="@{/streamix/js/main.js}"></script>
```

**변경 이유**:
- WebJars Bootstrap CSS/JS 의존성 제거
- Bootstrap Icons font → SVG sprite (네트워크 1회 fetch + 캐시)
- `<i class="bi bi-...">` (Bootstrap Icons font) → `<svg><use href=".../icons.svg#icon-...">` (self-host sprite)
- 모놀리식 `dashboard.js` → ES Module 진입점 `main.js`

### 4.2 dashboard.css 822줄 → streamix.css (단일)

**v2 (이전, 일부)**:
```css
:root {
  --streamix-primary: #6366f1;
  --streamix-primary-dark: #4f46e5;
  --streamix-bg: #f1f5f9;
  --streamix-card-bg: #ffffff;
  ...
}
body {
  font-family: 'Pretendard', -apple-system, ...;
}
.sidebar {
  background: linear-gradient(180deg, var(--streamix-primary) 0%, var(--streamix-secondary) 100%);
  ...
}
```

**v3 (현재, 일부)**:
```css
:root {
  --streamix-background: oklch(0.985 0.005 85);
  --streamix-foreground: oklch(0.15 0.01 60);
  --streamix-card: oklch(1 0 0);
  --streamix-border-strong: oklch(0.20 0.01 60);
  --streamix-accent: oklch(0.82 0.18 85);
  --radius: 0; /* brutalist */
  --streamix-font-display: ui-serif, "Iowan Old Style", "Apple Garamond", ...;
  --streamix-font-sans: system-ui, -apple-system, "Segoe UI", "Pretendard Variable", ...;
}
html.dark { ... 다크 토큰 ... }
.streamix-sidebar {
  border-right: 1px solid var(--streamix-border);
  background-color: var(--streamix-card);
}
```

**변경 이유**:
- HEX/RGBA → OKLCH (perceptually uniform, 다크/라이트 paired tokens)
- 라이트 only → 라이트/다크 듀얼 토큰
- 보라색 gradient sidebar → 종이 톤 카드 + 보더 only (cinema/editorial)
- 외부 폰트('Pretendard') → 시스템 폰트 스택
- 라운드 0.5~1rem → 0 (brutalist 사각 박스)
- 그림자 강조 → 보더 강조

### 4.3 dashboard.js IIFE 모놀리식 → ES Modules

**v2 (이전)**:
```javascript
(function() {
  'use strict';
  var FileTypes = { ... };
  document.addEventListener('DOMContentLoaded', function() {
    initCopyButtons();
    initDeleteConfirmation();
    initTooltips();        // Bootstrap Tooltip 의존
    initFileUpload();
    initAutoRefresh();
    initSidebarToggle();
    initFormatters();
  });
  function initFileUpload() { ... handleFileUpload(file) ... }
  function showToast(message, type) {
    var toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();
  }
  window.Streamix = { formatFileSize, formatDateTime, showToast, ... };
})();
```

**v3 (현재)**:
```javascript
// main.js
import { EventBus, EVENTS } from './event-bus.js';
import { Store } from './store.js';
import { Api, ApiError } from './api.js';
import { Theme } from './theme.js';
import { showToast } from './components/toast.js';
import { Modal } from './components/modal.js';
import { initUpload, detectFileType } from './components/upload.js';
import { SessionsPoller } from './components/sessions-poller.js';

function init() {
  Theme.init();
  initThemeToggle();
  initMobileNav();
  initCopyButtons();
  initFormatters();
  initDeleteModal();
  initAutoRefresh();
  initFlashDismiss();
  initUpload();
}

window.Streamix = {
  events: EventBus, EVENTS, store: Store, api: Api, ApiError,
  theme: Theme, toast: showToast, modal: Modal, sessionsPoller: SessionsPoller,
  format: { fileSize: formatFileSize, duration: formatDuration, dateTime: formatDateTime },
  detectFileType
};
```

**변경 이유**:
- Bootstrap Toast/Tooltip 의존 제거 → custom toast + native `title` attribute
- 모놀리식 → 8개 모듈로 분리 (책임 분리, lazy loading 가능, 테스트 용이)
- `window.Streamix.foo` flat → `window.Streamix.format.foo`, `.api.files.list` 등 hierarchical namespace
- DOMContentLoaded 의존 → `readyState` 검사로 ES Module 환경 대응

### 4.4 build.gradle 의존성 변경

**v2 (이전)**:
```groovy
// WebJars — Bootstrap UI (CDN 의존 제거)
implementation 'org.webjars:bootstrap:5.3.2'
implementation 'org.webjars.npm:bootstrap-icons:1.11.1'
implementation 'org.webjars:webjars-locator-lite:1.0.1'
```

**v3 (현재)**: 3줄 모두 제거.

**변경 이유**: 사용자 방침 "프론트 의존성 최소화"에 따라 starter JAR 크기 감소 + 클래스패스 단순화.

### 4.5 루트 build.gradle 버전

**v2 (이전)**:
```groovy
.getOrElse('2.1.0-SNAPSHOT')
```

**v3 (현재)**:
```groovy
.getOrElse('3.0.0-SNAPSHOT')
```

**변경 이유**: Tailwind/Bootstrap 교체는 사용자 커스텀 CSS 호환성을 깨므로 semver major bump.

### 4.6 StreamixMonitoringConfiguration — 새 Bean 추가

**v3에서 추가된 부분**:
```java
@Bean
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "streamix.api.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(StreamixSessionsApiController.class)
public StreamixSessionsApiController streamixSessionsApiController(
    StreamingMonitoringService monitoringService
) {
  log.info("Creating StreamixSessionsApiController for active sessions polling");
  return new StreamixSessionsApiController(monitoringService);
}
```

**변경 이유**: sessions 페이지의 5초 폴링 정상화를 위해 `/sessions/active` JSON 엔드포인트가 필요. StreamingMonitoringService가 활성화된 환경에서만 의미가 있으므로 Monitoring Configuration에 동거.

## 5. 동작 구조

### 5.1 페이지 로드 흐름
```
1. 브라우저가 layout.html 받음
2. <head> inline IIFE: localStorage 또는 prefers-color-scheme 검사 → html.dark 클래스 부여 (FOUC 방지)
3. streamix.css 로드 → :root 토큰 + html.dark 변형 토큰 적용
4. SVG sprite preload (browser hint)
5. <body> 렌더 시작 → Thymeleaf로 data-* attribute 주입
6. fade-in[data-delay] CSS 애니메이션 (60ms~240ms 차등)
7. main.js (type="module") 로드 — deferred
8. EventBus / Store / Api / Theme import + main.js init():
   - Theme.init() — system mode prefers-color-scheme listener 등록
   - 각 컴포넌트 wiring (theme toggle / mobile nav / copy / formatters / delete modal / auto refresh / upload)
9. 첫 paint 완료 — 다크/라이트 깜빡임 없음
```

### 5.2 자동 새로고침 흐름 (Sessions 페이지)
```
[사용자] 'Auto Refresh' switch ON
   ↓
main.js initAutoRefresh() → SessionsPoller.start(5000)
   ↓
즉시 tick() — Api.sessions.active() → GET /api/streamix/sessions/active
   ↓
StreamixSessionsApiController.activeSessions()
   ↓
StreamingMonitoringService.getActiveSessions() (JPA query)
   ↓
ActiveSessionResponse[]를 JSON으로 응답
   ↓
EventBus.emit(EVENTS.SESSIONS_REFRESHED, sessions)
   ↓
[향후 확장] 구독자가 DOM 갱신 — 현재는 데이터만 dispatch (Phase γ에서는 토글만 정상화)
   ↓
5초 후 다시 tick() — document.visibilityState !== 'visible' 시 건너뜀
```

### 5.3 모달 흐름 (삭제 확인)
```
[사용자] 파일 행의 휴지통 버튼 클릭
   ↓
data-streamix-modal-target="streamix-delete-modal" 트리거
   ↓
modal.js의 document-level click delegator가 감지
   ↓
Modal.open(overlay, { trigger, dataset })
   ↓
overlay.dispatchEvent(new CustomEvent('streamix:modal:open', { detail }))
   ↓
main.js initDeleteModal()의 listener:
  - trigger.dataset.fileId, fileName 추출
  - modal 안 [data-streamix-delete-name] 텍스트 갱신
  - [data-streamix-delete-form] action 동적 설정 (`{basePath}/files/{id}/delete`)
   ↓
[사용자] '삭제' 버튼 클릭 → form submit → 서버 redirect
   ↓
'취소' 또는 Esc → Modal.close(overlay)
```

### 5.4 SVG Sprite 로딩 흐름
```
1. layout.html <head>:
   <link rel="preload" as="image" type="image/svg+xml" href="/streamix/svg/icons.svg">
   → 브라우저가 sprite 파일 hint를 받아 priority fetch 시작
2. body 안의 모든 <use href="/streamix/svg/icons.svg#icon-xxx"/>:
   → 동일 URL이라 추가 fetch 없음. 캐시에서 즉시 symbol 추출 후 렌더
3. JS에서 동적 swap (theme toggle moon/sun):
   use.setAttribute('href', `${spriteBase()}#icon-${isDark ? 'sun' : 'moon'}`);
   → 같은 sprite 안의 다른 symbol로 즉시 교체
```

### 5.5 컴포넌트 책임 분리
```
event-bus.js  ← 다른 모듈의 communication backbone (Pub/Sub)
store.js      ← 공유 state (path 기반 set/get + STORE_CHANGED 이벤트)
api.js        ← HTTP 추상화 (fetch + ApiError)
theme.js      ← 테마 토글 + persistence + prefers-color-scheme

components/
  toast.js    ← (subscribes EVENTS.TOAST 또는 직접 showToast)
  modal.js    ← (overlay open/close + delegated click)
  upload.js   ← (drag-drop + XHR + EVENTS.FILE_UPLOAD_*)
  sessions-poller.js ← (Api.sessions.active + EVENTS.SESSIONS_REFRESHED + visibility-aware)

main.js       ← 진입점: 모듈 import + DOM wiring + window.Streamix 노출
```

## 6. 디자인 패턴

### 6.1 Pub/Sub (EventBus)
- **위치**: `event-bus.js`
- **이유**: 컴포넌트 간 직접 의존 회피 → 한 컴포넌트가 다른 컴포넌트의 존재를 몰라도 통신.
- **구조**: `Map<EventName, Set<Handler>>` + 명명 상수 + cleanup 반환 함수.

### 6.2 Module Script
- **위치**: 모든 JS (`type="module"`)
- **이유**: 의존성 명시(import/export), tree-shaking 가능성, 자동 strict mode, deferred 로드.
- **장점**: IIFE보다 깔끔, 모듈 단위 테스트 가능, 브라우저 native 지원.

### 6.3 Static Factory (record + `from()`)
- **위치**: `ActiveSessionResponse.from(entity)`
- **이유**: 엔티티 → DTO 변환을 명시적 named factory로. constructor 직접 호출보다 의도가 분명.

### 6.4 Auto-configuration with Conditionals
- **위치**: `StreamixMonitoringConfiguration`, `StreamixWebConfiguration`
- **이유**: 사용자 환경(JPA 없음, REST 비활성, override Bean)에 따라 Bean을 선택적으로 등록. Spring Boot starter의 핵심 패턴.

### 6.5 Decorator (Thymeleaf layout-dialect)
- **위치**: `layout:decorate="~{streamix/layout}"`
- **이유**: 공통 셸(사이드바, 마스트헤드, 토스트 컨테이너)을 layout.html 하나에 두고, 각 페이지는 `layout:fragment="content"`만 채움. DRY.

### 6.6 Brutalist UI Pattern
- **개념**: 인쇄물(잡지/포스터) 미감을 디지털 UI에 적용. 그림자/그라데이션 최소, 굵은 보더, 큰 타이포그래피, 거대한 네거티브 스페이스.
- **이유**: 미디어 스트리밍 admin 도구의 컨텍스트(영화관, 미디어 오퍼레이션 룸)와 자연 매칭.

### 6.7 Token-driven Theming (CSS Variables)
- **구조**: `:root`에 light 토큰, `html.dark`에 dark 토큰 — 컴포넌트 클래스는 `var(--streamix-foo)`만 참조.
- **장점**: 컴포넌트 코드 변경 없이 테마 전환 완료. 사용자 커스텀도 토큰만 override.

### 6.8 Visibility-Aware Polling
- **위치**: `sessions-poller.js`
- **이유**: 백그라운드 탭에서 API 호출 낭비 회피. `document.visibilityState`를 매 tick 시작 시 확인.

## 7. 설정 / 컨벤션

### 7.1 CSS 클래스 명명
- **prefix**: `streamix-*` (사용자 페이지 클래스와 충돌 회피)
- **modifier**: BEM-lite `--variant` 또는 `is-state`
  - 변형: `.streamix-btn--primary`, `.streamix-badge--video`, `.streamix-toast--error`
  - 상태: `.is-open`, `.is-active`, `.is-uploading`, `.is-disabled`, `.is-drag`, `.is-copied`, `.is-leaving`
- **유틸**: `.streamix-mono`, `.streamix-display`, `.streamix-meta`, `.streamix-truncate`, `.streamix-flex-between`

### 7.2 HTML data attribute
- **JS hook**: `data-streamix-*` (사용자 attribute와 충돌 회피)
  - `data-streamix-modal-target`, `data-streamix-modal-close`, `data-streamix-modal="delete"`
  - `data-streamix-theme-toggle`, `data-streamix-theme-label`
  - `data-streamix-upload`, `data-streamix-upload-input`, `data-streamix-upload-progress`
  - `data-streamix-copy="targetId"`
  - `data-streamix-auto-refresh`
  - `data-streamix-alert-close`
  - `data-streamix-delete-name`, `data-streamix-delete-form`
  - `data-streamix-mobile-toggle`, `data-streamix-mobile-overlay`
- **포맷 hint**: `data-format="filesize|datetime|duration"` + `data-value="..."`
- **컨텍스트 주입**: body 자체에 `data-api-base-path`, `data-allowed-types`, `data-max-file-size`, `data-streamix-sprite`

### 7.3 EventBus 이벤트 이름
- **prefix**: 도메인:액션:상태
  - `file:upload:started/progress/success/error`
  - `file:deleted`
  - `sessions:refreshed`
  - `theme:changed`
  - `ui:toast`, `ui:modal:open`, `ui:modal:close`
  - `store:changed`

### 7.4 OKLCH 토큰 분류
| prefix | 의미 |
|--------|------|
| `--streamix-background/foreground` | 페이지 표면 |
| `--streamix-card/card-foreground` | 카드 표면 |
| `--streamix-muted/muted-foreground` | 부드러운 보조 표면 |
| `--streamix-border/border-strong` | 보더 (강조 보더는 brutalist 1px 잉크 선) |
| `--streamix-accent/accent-foreground` | 단일 시그널 액센트 (amber light / lime dark) |
| `--streamix-destructive/destructive-foreground` | 위험 액션 |
| `--streamix-live` | 방송 중 빨강 |
| `--streamix-success` | 성공 그린 |
| `--streamix-type-{image|video|audio|document|archive|other}` | FileType 라벨 컬러 |

### 7.5 Java 컨벤션 (기존 유지)
- Korean Javadoc + Korean DisplayName (테스트)
- Constructor injection (no field injection)
- `record` for DTOs
- `proxyBeanMethods = false` for lite Configuration

## 8. 테스트 관련

이번 작업은 **UI 위주 변경**이라 새 자동 테스트는 추가하지 않았다. 기존 테스트는 그대로 통과 (BUILD SUCCESSFUL).

### 회귀 테스트로 통과한 기존 테스트
- streamix-core: 12개 테스트 클래스 (단위 테스트)
- streamix-spring-boot-starter: `StreamixAutoConfigurationTest`

### UI 검증 (수동 — Phase 검증 단계에서 사용자가 수행 권장)
- 5 페이지(dashboard, files, file-detail, sessions, 모달) × 라이트/다크 × 모바일/데스크탑
- 업로드 (드래그 + 클릭)
- 삭제 모달 (파일명 동적 주입, form action 동적 설정)
- 자동 새로고침 (5초 폴링, /api/streamix/sessions/active 호출)
- URL 복사 (clipboard API)
- 다크/라이트/system 토글 순환

## 9. 새로 알게 된 것

### 9.1 OKLCH 컬러 공간
- LCH의 변형으로, perceptually uniform → 같은 L 값이면 인식 명도가 같음.
- HEX/HSL의 함정(파랑이 노랑보다 어두워 보임) 해소.
- 다크 토큰을 만들 때 같은 hue를 유지하면서 L만 반전하면 자연스러운 다크 페어 생성.
- 브라우저 지원: Chrome 111+, Safari 15.4+, Firefox 113+ — admin tool 컨텍스트에서 충분.

### 9.2 시스템 폰트 스택의 강점
- 외부 네트워크 의존 0 → 첫 paint 빠름, 차단된 환경(intranet)에서도 정상.
- OS별 distinctive 룩 (macOS의 New York Serif vs Windows의 Cambria) — brutalist 컨셉에 부합.
- `ui-serif`, `ui-monospace`, `system-ui`는 표준 keyword로 OS의 기본 폰트를 골라준다.

### 9.3 SVG Sprite + `<use href>`
- 한 sprite 파일 안에 `<symbol id="icon-name" viewBox="0 0 16 16">` 정의.
- `<use href="path.svg#icon-name">` 또는 `<use href="#icon-name">` (inline일 때)으로 참조.
- 캐시 효율 좋음 (한 번 다운로드, 모든 페이지 재사용).
- `fill="currentColor"`로 CSS color를 통한 색상 제어.
- `<link rel="preload" as="image" type="image/svg+xml">`로 priority fetch hint.

### 9.4 ES Modules in Browser
- `<script type="module">`는 자동 deferred — DOM 파싱 완료 후 실행.
- `import`/`export`로 의존성 그래프 명시.
- strict mode 자동 적용.
- relative path: `./foo.js`, `../bar.js` — 명시적 `./` 필수.

### 9.5 `prefers-color-scheme` Media Query
- `matchMedia('(prefers-color-scheme: dark)').matches` → 즉시 OS 설정 알 수 있음.
- `addEventListener('change', ...)`로 OS 설정 변경 실시간 감지 (system mode 구현 핵심).

### 9.6 Spring Boot `@ConditionalOnWebApplication`
- 서블릿 vs 리액티브 스택 구분 — `Type.SERVLET` 또는 `Type.REACTIVE`.
- WebFlux 환경에서 `@Controller`가 무의미하니 조건적 등록 필수.

### 9.7 Spring Record DTO + Jackson
- Java `record`는 immutable + canonical constructor + getter 자동.
- Jackson은 record를 즉시 직렬화/역직렬화 (모듈 없이도 OK).
- JPA 엔티티 노출 회피 + LAZY 함정 회피.

### 9.8 Thymeleaf `|...|` literal substitution
- `th:attr="href=|@{/path/file.svg}#fragment|"` — 표현식 결과를 inline 문자열로 합침.
- URL fragment(`#xxx`)를 안전하게 붙일 때 매우 유용.

## 10. 더 공부할 것

| 주제 | 왜 깊이 공부 필요한가 |
|------|---------------------|
| OKLCH gamut mapping | 의도한 채도가 sRGB에서 표현 불가일 때의 fallback 메커니즘 |
| Container Queries (`@container`) | 사이드바 collapsible 시 컴포넌트 단위 반응형 |
| View Transitions API | 페이지 간 부드러운 전환 — 다크/라이트 토글에도 활용 가능 |
| `<dialog>` element + `::backdrop` | 현재는 커스텀 modal-overlay로 구현, native `<dialog>`가 더 깔끔 (focus trap 자동) |
| HTMX | sessions polling을 client-side fetch + EventBus 대신 HTMX `hx-trigger="every 5s"`로 단순화 가능 |
| Shadow DOM 캡슐화 | 사용자 페이지 CSS와 격리 — Web Component 도입 검토 |
| Server-Sent Events (SSE) | 폴링 대신 push — 5초 latency 줄이고 서버 부하 감소 |
| Spring Boot `@AutoConfiguration` (4.0 새 어노테이션) | 기존 `@Configuration` + `META-INF/.imports` 패턴의 진화 |
| 폼 검증 데코레이터 패턴 | 업로드 시 client + server 검증 일관화 |
| Lighthouse + Web Vitals | brutalist 미감의 perceived performance 측정 |

## 11. 배포 자동화 패턴 (Tag-driven Maven Central Publish)

### 11.1 워크플로우 트리거 구조

**`.github/workflows/publish.yml`**:
```yaml
on:
  push:
    tags:
      - 'v*'
  release:
    types: [ created ]
  workflow_dispatch:
    inputs:
      version: { description: 'Release version (e.g. 2.0.1)', required: true }
```

세 가지 트리거가 모두 등록되어 있다:
- `push: tags: 'v*'` — `v3.0.0` 등 버전 태그 push 시
- `release: created` — GitHub Release 생성 시
- `workflow_dispatch` — Actions 탭에서 수동 실행 (`version` 입력 받음)

### 11.2 버전 추출 패턴

```yaml
- name: Resolve release version
  run: |
    if [ "${{ github.event_name }}" = "push" ] || [ "${{ github.event_name }}" = "release" ]; then
      echo "RELEASE_VERSION=${GITHUB_REF#refs/tags/v}" >> "$GITHUB_ENV"
    else
      echo "RELEASE_VERSION=${{ github.event.inputs.version }}" >> "$GITHUB_ENV"
    fi
```

- `${GITHUB_REF#refs/tags/v}`: `refs/tags/v3.0.0` → `3.0.0` (bash parameter expansion `#prefix`로 prefix 제거)
- 그 후 `./gradlew build -PreleaseVersion=$RELEASE_VERSION`로 주입

### 11.3 루트 build.gradle의 fallback 패턴

```groovy
version = providers.gradleProperty('releaseVersion')
    .orElse(providers.environmentVariable('RELEASE_VERSION'))
    .getOrElse('3.0.0-SNAPSHOT')
```

우선순위:
1. `-PreleaseVersion=3.0.0` CLI 프로퍼티 (CI에서 주입)
2. `RELEASE_VERSION` 환경변수
3. fallback: `3.0.0-SNAPSHOT` (로컬 개발용)

### 11.4 Maven Central Portal Publishing

`tech.yanand.maven-central-publish` 플러그인 사용:
```groovy
mavenCentral {
    authToken = System.getenv("MAVEN_CENTRAL_TOKEN")
    publishingType = "AUTOMATIC"
}
```
- `AUTOMATIC`: staging → release를 자동 진행 (수동 portal 확인 없음)
- `USER_MANAGED`: staging만, release는 portal UI에서 직접

GPG 서명:
```groovy
signing {
    def signingKey = System.getenv("GPG_SIGNING_KEY")
    def signingPassword = System.getenv("SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign publishing.publications.mavenJava
}
```
- in-memory GPG → CI 환경에서 keyring 파일 없이 secret env로만 서명 가능

### 11.5 트리거 중복 주의

`push:tags`와 `release:created` 둘 다 있을 때 `gh release create v3.0.0 --target master` 같은 명령은:
1. GitHub이 서버사이드에서 태그를 생성/push → `push:tags` trigger 발동
2. Release를 생성 → `release:created` trigger 발동

→ publish 워크플로우가 두 번 동시 실행될 수 있음. Maven Central은 같은 GAV(GroupId:ArtifactId:Version)를 두 번 publish하려고 하면 거부하므로 두 번째 run은 실패. 이번 사이클은 **태그 push만 수행하여 1회 발동**으로 처리.

### 11.6 의미 단위 커밋 분할 전략 (commits.md 옵션 A)

v3-redesign처럼 18+ 파일 변경 / CSS+JS+템플릿+Java가 섞인 대규모 변경에서, "단일 거대 커밋"이나 "Phase별 5개 커밋"보다 **영역별/의미별 9개 커밋**을 선택한 이유:
1. **리뷰 단위**: 각 커밋이 한 가지 의미를 담아 PR 리뷰 시 한 번에 파악 가능
2. **bisect 친화적**: 회귀 발생 시 `git bisect`로 9개 후보 중 원인 좁히기 쉬움
3. **롤백 단위**: 한 영역(예: 컴포넌트 모듈만)만 revert 가능
4. **단점 수용**: 중간 커밋(1~7번)에서는 UI가 깨지지만 컴파일/테스트는 통과 → CI bisect 시 빌드 기준으로 충분

커밋 메시지 컨벤션 (commits.md):
- `chore(deps):` 의존성 변경
- `feat(static):` 정적 자원 (CSS/JS/SVG)
- `feat(templates):` Thymeleaf 페이지
- `feat(api):` REST/JSON API
- `chore:` 버전 bump / 문서
- BREAKING CHANGE footer는 conventional commits 규약 따름 (v3.0.0 major bump 정당화)

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-11 | learned.md 초안 작성 (v3-redesign 완료 시점) |
| 2026-05-11 | 11절 "배포 자동화 패턴" 추가 — tag push → Maven Central, 트리거 중복 주의, 9-커밋 분할 사유 |
