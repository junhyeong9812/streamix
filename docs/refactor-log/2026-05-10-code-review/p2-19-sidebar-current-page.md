# [P2-19] Sidebar currentPage 모델 누락 — 메뉴 활성화 표시 안 됨

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — UI 결함** |
| 카테고리 | 프론트엔드 / 템플릿 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamixDashboardController.java`, `templates/streamix/layout.html` |

## 문제 분석

### 현재 동작 — layout.html
```html
<!-- layout.html line 38-62 -->
<ul class="nav flex-column mt-3">
    <li class="nav-item">
        <a class="nav-link"
           th:classappend="${currentPage == 'dashboard' ? 'active' : ''}"
           th:href="@{${@streamixProperties.dashboard.path}}">
            <i class="bi bi-speedometer2"></i>
            <span>대시보드</span>
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link"
           th:classappend="${currentPage == 'files' ? 'active' : ''}"
           th:href="@{${@streamixProperties.dashboard.path} + '/files'}">
            ...
        </a>
    </li>
    <li class="nav-item">
        <a class="nav-link"
           th:classappend="${currentPage == 'sessions' ? 'active' : ''}"
           th:href="@{${@streamixProperties.dashboard.path} + '/sessions'}">
            ...
        </a>
    </li>
</ul>
```

### 현재 컨트롤러 — `currentPage` 모델 추가 안 됨
```java
// StreamixDashboardController.dashboard()
model.addAttribute("stats", stats);
model.addAttribute("totalFiles", totalFiles);
model.addAttribute("recentFiles", recentFiles);
model.addAttribute("activeSessions", activeSessions);
model.addAttribute("recentSessions", recentSessions);
model.addAttribute("popularFiles", popularFiles);
model.addAttribute("apiBasePath", properties.api().basePath());
// ⚠ currentPage 누락
```

`fileList`, `fileDetail`, `sessionList`도 동일.

### 결과
- 모든 페이지에서 `currentPage` 변수가 null → SpEL에서 `null == 'dashboard'` → false
- 사이드바 메뉴 항목 **항상 비활성화**
- 사용자가 현재 어느 페이지에 있는지 시각적 피드백 없음

### 원인 분석
- 템플릿은 의도적으로 작성되었으나 컨트롤러 변경 시 모델 추가 누락
- `@ControllerAdvice` + `@ModelAttribute`로 통합하지 않음
- 테스트 부재로 발견 안 됨

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. 각 핸들러에서 model.addAttribute("currentPage", ...) | 명시적 | 단순 | 4곳 추가 |
| B. `@ControllerAdvice` + `@ModelAttribute` | 1곳에서 자동 | DRY | 매핑 정의 필요 |
| C. URI에서 추론하는 fragment | 템플릿 자체 처리 | 컨트롤러 변경 X | SpEL 복잡 |

### 채택: 옵션 A (단순)
이유:
1. 컨트롤러가 4개 메서드만 → 4곳 추가 부담 작음
2. 명시적이라 가독성 좋음
3. ControllerAdvice는 [P2-15](p2-15-properties-alias.md)에서 v3 검토 항목이라 일관성

### Step 1: 각 핸들러에 currentPage 추가
```java
@GetMapping("${streamix.dashboard.path:/streamix}")
public String dashboard(Model model) {
    model.addAttribute("currentPage", "dashboard");
    // 기존 코드...
}

@GetMapping("${streamix.dashboard.path:/streamix}/files")
public String fileList(...) {
    model.addAttribute("currentPage", "files");
    // ...
}

@GetMapping("${streamix.dashboard.path:/streamix}/files/{id}")
public String fileDetail(...) {
    model.addAttribute("currentPage", "files");  // 파일 상세도 'files' 활성
    // ...
}

@GetMapping("${streamix.dashboard.path:/streamix}/sessions")
public String sessionList(...) {
    model.addAttribute("currentPage", "sessions");
    // ...
}
```

### Step 2 (개선): 상수 추출
```java
private static final String ATTR_CURRENT_PAGE = "currentPage";
private static final String PAGE_DASHBOARD = "dashboard";
private static final String PAGE_FILES = "files";
private static final String PAGE_SESSIONS = "sessions";

model.addAttribute(ATTR_CURRENT_PAGE, PAGE_DASHBOARD);
```

오타 방지.

## Before / After

### Before — StreamixDashboardController.dashboard()
```java
@GetMapping("${streamix.dashboard.path:/streamix}")
public String dashboard(Model model) {
    log.debug("Rendering dashboard");
    var stats = monitoringService.getDashboardStats();
    model.addAttribute("stats", stats);
    long totalFiles = fileMetadataPort.count();
    model.addAttribute("totalFiles", totalFiles);
    var recentFiles = getFileMetadataUseCase.getAll(0, 5);
    model.addAttribute("recentFiles", recentFiles);
    var activeSessions = monitoringService.getActiveSessions();
    model.addAttribute("activeSessions", activeSessions);
    var recentSessions = monitoringService.getRecentSessions(10);
    model.addAttribute("recentSessions", recentSessions);
    var popularFiles = monitoringService.getMostStreamedFiles(5);
    model.addAttribute("popularFiles", popularFiles);
    model.addAttribute("apiBasePath", properties.api().basePath());
    return "streamix/dashboard";
}
```

### After — StreamixDashboardController.dashboard()
```java
private static final String ATTR_CURRENT_PAGE = "currentPage";
private static final String PAGE_DASHBOARD = "dashboard";
private static final String PAGE_FILES = "files";
private static final String PAGE_SESSIONS = "sessions";

@GetMapping("${streamix.dashboard.path:/streamix}")
public String dashboard(Model model) {
    log.debug("Rendering dashboard");
    model.addAttribute(ATTR_CURRENT_PAGE, PAGE_DASHBOARD);
    var stats = monitoringService.getDashboardStats();
    model.addAttribute("stats", stats);
    // ... (나머지 기존 코드)
    return "streamix/dashboard";
}

@GetMapping("${streamix.dashboard.path:/streamix}/files")
public String fileList(int page, int size, Model model) {
    model.addAttribute(ATTR_CURRENT_PAGE, PAGE_FILES);
    // ...
}

@GetMapping("${streamix.dashboard.path:/streamix}/files/{id}")
public String fileDetail(UUID id, Model model) {
    model.addAttribute(ATTR_CURRENT_PAGE, PAGE_FILES);
    // ...
}

@GetMapping("${streamix.dashboard.path:/streamix}/sessions")
public String sessionList(int limit, Model model) {
    model.addAttribute(ATTR_CURRENT_PAGE, PAGE_SESSIONS);
    // ...
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 수동 검증
1. 대시보드 접속 → 사이드바 "대시보드" 항목 active 표시
2. 파일 관리 클릭 → "파일 관리" 항목 active
3. 세션 클릭 → "스트리밍 세션" 항목 active
4. 파일 상세 페이지 → "파일 관리" 항목 active 유지

### 3. (선택) Thymeleaf 단위 테스트
Spring MVC test로 모델 검증:
```java
@Test
void dashboardSetsCurrentPage() throws Exception {
    mockMvc.perform(get("/streamix"))
        .andExpect(model().attribute("currentPage", "dashboard"));
}
```

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/dashboard/StreamixDashboardController.java`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/layout.html` (변경 없음)

## 참고
- Spring MVC `Model` API
- 파일 상세 페이지(`fileDetail`)에서 `PAGE_FILES`로 설정한 이유: 사용자 mental model 상 "파일 관리" 섹션 안에 있는 것
