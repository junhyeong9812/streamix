# 맥락 노트: v3-redesign

## 작업 배경
사용자 요청 (2026-05-10): "버전 올리고 대시보드 디자인이랑 상태관리도 깔끔하게 수정해보자. 현재는 디자인이 상당히 아쉬운데 모던 디자인 형태로 재구성해보자."

## 사전 작업
직전(2026-05-10)에 코드 리뷰 24개 이슈 일괄 개선 완료 — `BUILD SUCCESSFUL`. v2.0.1-SNAPSHOT 상태에서 시작.

이 작업은 v2.0.1 → v3.0.0으로의 major release.

## 사용자 결정 (4지선다, 2026-05-10)
| 질문 | 답 |
|------|----|
| 버전 | 3.0.0 |
| 디자인 | ~~Tailwind + shadcn-style 테마~~ → v2: **순수 CSS + Cinema/Editorial Brutalist** |
| 상태관리 | Vanilla JS + Custom Event Bus |
| 진행 | plan 우선 → 검토 후 구현 |

## v2 추가 결정 (2026-05-11)
사용자 지시: "순수 CSS/JS/HTML 기반, 프론트 의존성 최소화" + "다양한 라이브러리는 의미 없다" + "디자인 옵션 A로 진행"

| 항목 | v2 결정 |
|------|--------|
| 디자인 미적 방향 | A. Cinema/Editorial Brutalist |
| CSS 프레임워크 | ❌ 없음 (순수 CSS — streamix.css 1개) |
| 빌드 도구 | ❌ 없음 (npm/Tailwind CLI 제거) |
| 외부 폰트 | ❌ 없음 (시스템 폰트 스택만) |
| 아이콘 | Bootstrap Icons MIT SVG self-host sprite |
| WebJars | 전부 제거 (Bootstrap + Icons + Locator) |
| 기본 테마 | system (OS 따름) |
| sessions API | `GET /api/streamix/sessions/active` 추가 |
| 본 계획 문서 | **plan-v2.md** (plan.md는 v1으로 보존) |

## 코드베이스 현황 (v2.0.1-SNAPSHOT)
- 24개 이슈 개선 완료
- 빌드: BUILD SUCCESSFUL
- JAR: `streamix-spring-boot-starter-2.0.1-SNAPSHOT.jar`
- 대시보드 현재 상태:
  - Bootstrap 5 (WebJars 5.3.2) + Bootstrap Icons (1.11.1)
  - dashboard.css 822줄 — 색상/그라디언트/카드 스타일 모놀리식
  - dashboard.js 515줄 — IIFE 패턴, FileTypes 정의 + 업로드 + delete + 새로고침 + 토스트 등 한 파일
  - 4개 Thymeleaf 템플릿 (layout, dashboard, files, file-detail, sessions)
  - sidebar `currentMenu` 모델 (P2-19로 추가)

## 금지 영역 (절대 수정 금지)
- `streamix-core/` 전체
- `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixApiController.java` (REST API 시그니처)
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/GlobalExceptionHandler.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/dto/*.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/*.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/service/StreamingMonitoringService.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/properties/StreamixProperties.java`
- `streamix-spring-boot-starter/src/main/java/.../starter/annotation/EnableStreamix.java`

## 변경 영역 (v2)
- `streamix-spring-boot-starter/src/main/resources/static/streamix/css/` — `streamix.css` 1개
- `streamix-spring-boot-starter/src/main/resources/static/streamix/svg/icons.svg` — 신규 sprite
- `streamix-spring-boot-starter/src/main/resources/static/streamix/js/` — 모듈 분리 (event-bus, store, api, theme, components/*, main)
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/` — 5개 템플릿 재작성
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixSessionsApiController.java` (신규 — JSON API)
- `streamix-spring-boot-starter/build.gradle` (WebJars 3개 제거)
- `build.gradle` (version 3.0.0-SNAPSHOT)
- ~~`package.json`, `tailwind.config.js`, `postcss.config.js`~~ — **v2에서 작성 안 함**
- `README.md` (changelog v3.0.0 + 마이그레이션 + Bootstrap Icons MIT 고지)

## 컨벤션 (관찰 + 결정)

### 기존 컨벤션
- Java: 한글 javadoc + Korean DisplayName
- ~~Tailwind utility 우선~~ → v2: **순수 CSS 컴포넌트 클래스 (BEM-lite)**
- ~~shadcn naming~~ → v2: **OKLCH 토큰 + streamix-* 접두사 (사용자 페이지와 충돌 회피)**

### 신규 컨벤션
- JS 모듈: ES Module import/export (modern browser ES2020+)
- CSS class: `streamix-` 접두사 (사용자 페이지와 충돌 회피)
- Event 이름: `'streamix:domain:action'` 형식
- HTML data attribute: `data-streamix-*`

### 다크모드 토글 위치
- 사이드바 footer (브랜드 로고 옆)
- 또는 우측 상단 고정 (사용자 선호)
- → plan 검토 시 결정

## 외부 큐레이션 결과 (요약)
- Tailwind v4 Play CDN은 dev 전용 — production은 prebuilt CSS
- shadcn은 React 컴포넌트지만 디자인 토큰만 차용 가능 (OKLCH + .dark 토글)
- Vanilla EventBus: Map<event, Set<handler>> + 명명 상수 + cleanup function

## 전체 빌드 명령어 (v2 — 단순)
```bash
# Java만 — 이게 전부
./gradlew build

# 부분 검증
./gradlew :streamix-spring-boot-starter:compileJava
./gradlew :streamix-core:test
```
v2는 npm/node 사용 안 함.

## 마이그레이션 가이드 (v2 → v3)

### Breaking Changes
1. **CSS 파일명/구조 변경**:
   - `streamix/css/dashboard.css` → `streamix/css/streamix.css` + `streamix/css/tailwind.css`
   - 사용자가 직접 link한 경우 갱신 필요
2. **Bootstrap 의존성 제거**:
   - 사용자가 starter의 Bootstrap을 자기 페이지에서 사용했다면 따로 추가 필요
3. **JS API 변경**:
   - `window.Streamix.formatFileSize` → `window.Streamix.format.fileSize`
   - 새 globals: `window.Streamix.events`, `window.Streamix.store`, `window.Streamix.api`
4. **다크모드 기본 활성**:
   - 페이지 로드 시 OS 설정 따름 (system mode)
   - 사용자가 light로 강제하려면 `<html class="light">` 또는 toggle

### Non-breaking
- REST API (`/api/streamix/files`) 시그니처 동일
- `@EnableStreamix` 동일
- StreamixProperties 동일
- 모든 도메인/어플리케이션 로직 동일

## 사용자가 검토 시 결정할 항목 (v2 — 전부 해결됨)
| # | v1 항목 | v2 처리 |
|---|---------|--------|
| 1 | Tailwind 빌드 옵션 (C vs D) | **소거** — Tailwind 자체 안 씀 |
| 2 | Lucide WebJars vs inline SVG | **Bootstrap Icons MIT self-host sprite** |
| 3 | 다크모드 기본값 | **system** |
| 4 | sessions API endpoint | **추가** (`GET /api/streamix/sessions/active`) |
| 5 | tailwind.config.js 위치 | **소거** — Tailwind 안 씀 |

v2에서 남은 미결정 사항 없음 — plan-v2.md 승인 시 Phase α 즉시 시작 가능.
