# [P2-21] dashboard.js maxSize 500MB 하드코딩 — 서버 설정과 동기화

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 일관성 / UX** |
| 카테고리 | 프론트엔드 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `static/streamix/js/dashboard.js`, `templates/streamix/layout.html`, `files.html` |

## 문제 분석

### 현재 동작
```javascript
// dashboard.js line 259-264
function handleFileUpload(file) {
    // ...
    // 파일 크기 제한 (500MB - 기본값, 실제로는 서버 설정 따름)
    var maxSize = 500 * 1024 * 1024;
    if (file.size > maxSize) {
        showToast('파일 크기는 500MB를 초과할 수 없습니다.', 'error');
        return;
    }
    // ...
}
```

### 서버 설정 vs JS 하드코딩
- 서버 default: `streamix.storage.max-file-size = 104857600` (100MB)
- JS 하드코딩: `500MB`

### 결과 시나리오
1. **사용자가 200MB 파일 업로드 시도** (서버 100MB 설정)
   - JS 검증 통과 (500MB 이하)
   - 서버에 전송 시작 → 100MB 즈음 fail
   - **불필요한 네트워크 트래픽** + 사용자 대기시간

2. **사용자가 1GB 파일 업로드 시도**
   - JS에서 "500MB 초과" 메시지 — 서버 실제 제한과 무관

### `files.html`의 안내 텍스트
```html
<!-- files.html line 152 -->
<div class="text-muted small">
    <span th:if="${@streamixProperties.storage.isAllTypesAllowed()}">모든 파일 지원</span>
    <span th:if="${!@streamixProperties.storage.isAllTypesAllowed()}"
          th:text="${T(java.lang.String).join(', ', @streamixProperties.storage.allowedTypes)} + ' 파일 지원'"></span>
    (최대 500MB)              <!-- ⚠ 하드코딩 -->
</div>
```

→ "(최대 500MB)" 안내문도 하드코딩.

### 기대 동작
- 서버 설정값을 JS와 안내문에 전달
- JS는 서버 limit보다 큰 파일 업로드 시도 차단

## 변경 프로세스

### Step 1: layout.html에 max-file-size data attribute 추가
이미 `data-api-base-path`, `data-allowed-types` 패턴이 있음. 같은 방식으로:

```html
<!-- layout.html line 17-18 변경 -->
<body th:data-api-base-path="${@streamixProperties.api.basePath}"
      th:data-allowed-types="${T(java.lang.String).join(',', @streamixProperties.storage.allowedTypes)}"
      th:data-max-file-size="${@streamixProperties.storage.maxFileSize}">
```

### Step 2: dashboard.js — body data attribute 읽기
```javascript
function handleFileUpload(file) {
    // ...
    var maxSizeAttr = document.body.dataset.maxFileSize;
    var maxSize = maxSizeAttr ? parseInt(maxSizeAttr, 10) : 0;
    // 0 또는 음수면 제한 없음 (서버 설정과 일치)
    if (maxSize > 0 && file.size > maxSize) {
        showToast('파일 크기는 ' + Streamix.formatFileSize(maxSize) + '를 초과할 수 없습니다.', 'error');
        return;
    }
    // ...
}
```

`Streamix.formatFileSize`는 이미 노출된 글로벌 — 같은 포맷으로 표시.

### Step 3: files.html 안내문에 동적 값
```html
<!-- files.html -->
<div class="text-muted small">
    <span th:if="${@streamixProperties.storage.isAllTypesAllowed()}">모든 파일 지원</span>
    <span th:unless="${@streamixProperties.storage.isAllTypesAllowed()}"
          th:text="${T(java.lang.String).join(', ', @streamixProperties.storage.allowedTypes)} + ' 파일 지원'"></span>
    (최대 <span data-format="filesize"
                th:data-value="${@streamixProperties.storage.maxFileSize}"
                th:text="${#numbers.formatDecimal(@streamixProperties.storage.maxFileSize / 1024.0 / 1024.0, 1, 0)} + ' MB'"></span>)
</div>
```

설정값이 100MB면 "(최대 100 MB)", 500MB면 "(최대 500 MB)".

> 주의: `T(java.lang.String).join(...)`는 SpEL `T()` 식으로 Spring Security가 차단할 수 있음. 그러나 이번 작업 범위 외이므로 그대로 유지. 별도 issue로 검토 가능.

### Step 4: maxSize 0 처리 (제한 없음)
서버에서 maxFileSize = 0 → 제한 없음.
- JS에서 maxSize 0이면 검증 skip
- 안내문도 "최대" 부분 숨김 또는 "제한 없음" 표시

```html
<!-- 옵션 -->
<span th:if="${@streamixProperties.storage.maxFileSize > 0}">
    (최대 <span th:text="${...}"></span>)
</span>
<span th:unless="${@streamixProperties.storage.maxFileSize > 0}">
    (크기 제한 없음)
</span>
```

## Before / After

### Before — dashboard.js handleFileUpload
```javascript
function handleFileUpload(file) {
    var uploadArea = document.getElementById('uploadArea');
    var progressBar = document.getElementById('uploadProgress');
    // ...
    
    // 파일 타입 검증
    if (!isAllowedType(file.type, allowedTypes)) {
        // ...
    }

    // 파일 크기 제한 (500MB - 기본값, 실제로는 서버 설정 따름)
    var maxSize = 500 * 1024 * 1024;
    if (file.size > maxSize) {
        showToast('파일 크기는 500MB를 초과할 수 없습니다.', 'error');
        return;
    }
    // ...
}
```

### After — dashboard.js handleFileUpload
```javascript
function handleFileUpload(file) {
    var uploadArea = document.getElementById('uploadArea');
    var progressBar = document.getElementById('uploadProgress');
    // ...
    
    // 파일 타입 검증
    if (!isAllowedType(file.type, allowedTypes)) {
        // ...
    }

    // 파일 크기 제한 — 서버 설정에서 동적으로 가져옴 (0 또는 미설정이면 무제한)
    var maxSizeAttr = document.body.dataset.maxFileSize;
    var maxSize = maxSizeAttr ? parseInt(maxSizeAttr, 10) : 0;
    if (maxSize > 0 && file.size > maxSize) {
        showToast(
            '파일 크기는 ' + formatFileSize(maxSize) + '를 초과할 수 없습니다.',
            'error'
        );
        return;
    }
    // ...
}
```

### Before — layout.html
```html
<body th:data-api-base-path="${@streamixProperties.api.basePath}"
      th:data-allowed-types="${T(java.lang.String).join(',', @streamixProperties.storage.allowedTypes)}">
```

### After — layout.html
```html
<body th:data-api-base-path="${@streamixProperties.api.basePath}"
      th:data-allowed-types="${T(java.lang.String).join(',', @streamixProperties.storage.allowedTypes)}"
      th:data-max-file-size="${@streamixProperties.storage.maxFileSize}">
```

### Before — files.html (upload 모달 안내문)
```html
<div class="text-muted small">
    <span th:if="${@streamixProperties.storage.isAllTypesAllowed()}">모든 파일 지원</span>
    <span th:if="${!@streamixProperties.storage.isAllTypesAllowed()}"
          th:text="${T(java.lang.String).join(', ', @streamixProperties.storage.allowedTypes)} + ' 파일 지원'"></span>
    (최대 500MB)
</div>
```

### After — files.html
```html
<div class="text-muted small">
    <span th:if="${@streamixProperties.storage.isAllTypesAllowed()}">모든 파일 지원</span>
    <span th:unless="${@streamixProperties.storage.isAllTypesAllowed()}"
          th:text="${T(java.lang.String).join(', ', @streamixProperties.storage.allowedTypes)} + ' 파일 지원'"></span>
    <span th:if="${@streamixProperties.storage.maxFileSize > 0}">
        (최대 <span th:text="${#numbers.formatDecimal(@streamixProperties.storage.maxFileSize / 1024.0 / 1024.0, 1, 0)} + ' MB'"></span>)
    </span>
    <span th:unless="${@streamixProperties.storage.maxFileSize > 0}">(크기 제한 없음)</span>
</div>
```

## 검증 방법

### 1. 서버 설정 변경 시 UI 반영
```yaml
streamix:
  storage:
    max-file-size: 52428800   # 50MB
```
- 안내문: "(최대 50 MB)"
- JS 검증: 60MB 파일 시도 → "파일 크기는 50.0 MB를 초과할 수 없습니다"

### 2. 서버 무제한 (0)
```yaml
streamix:
  storage:
    max-file-size: 0
```
- 안내문: "(크기 제한 없음)"
- JS 검증 skip

### 3. 회귀
```bash
./gradlew test
```

## 관련 파일
- `streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/layout.html`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/files.html`

## 참고
- HTML5 data attribute로 server → client 설정 전달 패턴
- 클라이언트 검증은 UX 개선 — 서버 검증이 1차 진실
