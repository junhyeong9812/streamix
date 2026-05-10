# [P1-07] dashboard.js showToast — innerHTML 기반 XSS

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — 보안 / Stored XSS** |
| 카테고리 | 보안 / 프론트엔드 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `static/streamix/js/dashboard.js` |
| OWASP | A03:2021 Injection / XSS (CWE-79) |

## 문제 분석

### 현재 동작
```javascript
// dashboard.js line 459-486
function showToast(message, type) {
    var toastContainer = getOrCreateToastContainer();
    var toastEl = document.createElement('div');
    // ...
    toastEl.innerHTML =
        '<div class="d-flex">' +
        '<div class="toast-body">' +
        '<i class="bi ' + (type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle') + ' me-2"></i>' +
        message +                                              // ⚠️ 사용자 입력이 innerHTML에
        '</div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
        '</div>';
    // ...
}
```

### 호출 경로 (Stored XSS)
1. 공격자가 파일명을 `<img src=x onerror=alert(document.cookie)>.jpg`로 만들어 업로드
2. 서버는 `originalName`을 그대로 메타데이터에 저장
3. 다른 사용자가 대시보드에서 같은 파일을 다시 업로드 시도하다가 실패 → 서버가 `INVALID_FILE_TYPE` 응답에 파일명을 echo
4. `dashboard.js`의 `xhr.onload`:
   ```javascript
   var error = JSON.parse(xhr.responseText);
   showToast(error.message || '업로드에 실패했습니다.', 'error');
   ```
5. `error.message`에 포함된 파일명이 innerHTML로 렌더 → **JS 실행**

추가 호출 경로:
```javascript
// 같은 파일 line 107-108
showToast('복사에 실패했습니다.', 'error');     // ✓ 안전 (라이브러리 정의 문자열)

// line 254-255
showToast('허용되지 않는 파일 타입입니다. 허용: ' + allowedStr, 'error');  // ⚠️ allowedStr은 server data attribute에서 옴 — 부분 위험

// line 296, 307, 315
showToast('네트워크 오류가 발생했습니다.', 'error');     // ✓ 안전
showToast('파일이 업로드되었습니다.', 'success');        // ✓ 안전
showToast(error.message || '업로드에 실패했습니다.', 'error');  // ⚠️ error.message가 위험
```

### 기대 동작
사용자 입력 경유 텍스트는 항상 `textContent`로 처리. 또는 escape 후 innerHTML.

### 원인 분석
- 토스트 UI 빠르게 만들기 위해 innerHTML로 한 번에 구성
- 문자열 연결 방식은 친숙하지만 안전하지 않음
- 서버에서 사용자 입력을 응답에 echo하는 것 자체는 디버깅용으로 자연스러움 — 클라이언트 렌더 시 escape 책임

### 영향 범위
- 대시보드 사용자 (관리자) 세션 탈취 가능
- 라이브러리 사용자가 추가 컨텍스트(쿠키 인증) 두면 더 위험
- 파일 메타데이터 자체가 영구 저장이므로 Stored XSS

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. textContent + DOM API | createElement로 노드 구성 | 가장 안전 | 코드 길이 ↑ |
| B. escape 함수 + innerHTML | message만 escape | 코드 변경 적음 | escape 누락 위험, < > & " ' 외에도 신경 |
| C. Bootstrap Toast 컴포넌트 직접 활용 | Bootstrap docs 권장 패턴 | 표준 | 큰 리팩토링 |

### 채택: 옵션 A — textContent + DOM API
이유:
1. **default-safe** — 향후 메시지 추가에서도 자동으로 안전
2. 추가 의존성 없음
3. 코드 길이 차이 미미 (10줄 정도)

### Step 1: showToast 재작성
```javascript
function showToast(message, type) {
    var toastContainer = getOrCreateToastContainer();
    var isError = type === 'error';

    var toastEl = document.createElement('div');
    toastEl.className = 'toast align-items-center border-0 text-white';
    toastEl.classList.add(isError ? 'bg-danger' : 'bg-success');
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    var dFlex = document.createElement('div');
    dFlex.className = 'd-flex';

    var body = document.createElement('div');
    body.className = 'toast-body';

    var icon = document.createElement('i');
    icon.className = 'bi me-2 ' + (isError ? 'bi-exclamation-circle' : 'bi-check-circle');
    body.appendChild(icon);

    // ⭐ 사용자 입력은 textContent로 안전하게
    body.appendChild(document.createTextNode(' ' + (message || '')));

    var closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'btn-close btn-close-white me-2 m-auto';
    closeBtn.setAttribute('data-bs-dismiss', 'toast');
    closeBtn.setAttribute('aria-label', 'Close');

    dFlex.appendChild(body);
    dFlex.appendChild(closeBtn);
    toastEl.appendChild(dFlex);

    toastContainer.appendChild(toastEl);

    var toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}
```

### Step 2: 다른 호출부 점검
- `'허용되지 않는 파일 타입입니다. 허용: ' + allowedStr` — allowedStr은 `<body data-allowed-types>`에서 파싱한 enum 이름들. enum 이름은 정해진 알파벳 + 콤마. 그래도 안전한 textContent 처리됨 ✓
- 다른 모든 호출은 라이브러리 정의 상수 ✓

### Step 3: 추가 보호 — 서버 응답에 ETag/Content-Type 검사
이건 작업 범위 밖이지만, 추가 권장:
- 서버 응답이 JSON이 맞는지 Content-Type 확인 후 parse

## Before / After

### Before — showToast (line 460-486)
```javascript
function showToast(message, type) {
    var toastContainer = getOrCreateToastContainer();

    var toastEl = document.createElement('div');
    toastEl.className = 'toast align-items-center border-0';
    toastEl.classList.add(type === 'error' ? 'bg-danger' : 'bg-success');
    toastEl.classList.add('text-white');
    toastEl.setAttribute('role', 'alert');

    toastEl.innerHTML =
        '<div class="d-flex">' +
        '<div class="toast-body">' +
        '<i class="bi ' + (type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle') + ' me-2"></i>' +
        message +
        '</div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
        '</div>';

    toastContainer.appendChild(toastEl);

    var toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}
```

### After — showToast
```javascript
function showToast(message, type) {
    var toastContainer = getOrCreateToastContainer();
    var isError = type === 'error';

    var toastEl = document.createElement('div');
    toastEl.className = 'toast align-items-center border-0 text-white';
    toastEl.classList.add(isError ? 'bg-danger' : 'bg-success');
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    var dFlex = document.createElement('div');
    dFlex.className = 'd-flex';

    var body = document.createElement('div');
    body.className = 'toast-body';

    var icon = document.createElement('i');
    icon.className = 'bi me-2 ' + (isError ? 'bi-exclamation-circle' : 'bi-check-circle');
    body.appendChild(icon);

    // 사용자 입력은 textContent로 안전하게 처리 (XSS 방지)
    body.appendChild(document.createTextNode(' ' + (message || '')));

    var closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'btn-close btn-close-white me-2 m-auto';
    closeBtn.setAttribute('data-bs-dismiss', 'toast');
    closeBtn.setAttribute('aria-label', 'Close');

    dFlex.appendChild(body);
    dFlex.appendChild(closeBtn);
    toastEl.appendChild(dFlex);

    toastContainer.appendChild(toastEl);

    var toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}
```

## 검증 방법

### 1. 정적 검사 — innerHTML 잔존 확인
```bash
grep -n 'innerHTML' streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js
# 결과: showToast에서 innerHTML 사용 없어야 함
```

### 2. 수동 XSS 시도
1. 파일명을 `<img src=x onerror=alert(1)>.txt`로 만들어 업로드
2. 서버에서 `INVALID_FILE_TYPE`로 거부 응답
3. 대시보드에서 토스트 메시지가 텍스트로 표시되고 alert가 뜨지 않아야 함

### 3. DOM 검사
```javascript
// 브라우저 콘솔에서
Streamix.showToast('<script>alert(1)</script>', 'error');
// 화면에 텍스트 그대로 표시되어야 하고 script 실행 안 됨
document.querySelector('.toast').innerHTML
// "<script>"가 텍스트 노드로 렌더링되어 escape됨 확인
```

### 4. 기존 동작 유지
- 정상 메시지 ('파일이 업로드되었습니다') 표시 OK
- 자동 닫힘 동작 OK
- close 버튼 동작 OK

## 관련 파일
- `streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js`

## 참고
- OWASP XSS Prevention Cheat Sheet — Rule #1: HTML Escape Before Inserting Untrusted Data into HTML Element Content
- DOM `textContent` vs `innerHTML`: textContent는 자동 escape, innerHTML은 raw HTML로 해석
- WAI-ARIA `role="alert"` + `aria-live="assertive"`: 스크린 리더 지원 — 토스트 표준
