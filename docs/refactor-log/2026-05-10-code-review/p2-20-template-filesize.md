# [P2-20] 템플릿 file size MB 고정 표시

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — UX 결함** |
| 카테고리 | 프론트엔드 / 템플릿 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | 4개 Thymeleaf 템플릿 |

## 문제 분석

### 현재 동작
4개 템플릿에서 file.size를 모두 MB로만 표시:

```html
<!-- dashboard.html line 141 -->
<span data-format="filesize" th:data-value="${file.size}"
      th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 1)} + ' MB'"></span>

<!-- files.html line 76 -->
<span th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 2)} + ' MB'"></span>

<!-- file-detail.html line 191 -->
<div class="file-info-value"
     th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 2)} + ' MB'"></div>

<!-- sessions.html line 185 -->
<span th:text="${#numbers.formatDecimal(session.bytesSent / 1024 / 1024, 1, 2)} + ' MB'"></span>
```

### 결과
| 실제 크기 | 현재 표시 | 기대 |
|-----------|----------|------|
| 500 B | 0.0 MB | 500 B |
| 50 KB | 0.05 MB | 50 KB |
| 1.5 MB | 1.5 MB | 1.5 MB ✓ |
| 2.0 GB | 2048 MB | 2.0 GB |
| 1.5 TB | 1572864 MB | 1.5 TB |

→ KB 미만, GB 이상 케이스에서 가독성 매우 나쁨.

### 원인 분석
- `FileMetadata.getFormattedSize()`라는 도메인 메서드가 있지만 템플릿에서 활용 안 함
- `dashboard.js`의 `formatFileSize()` JS 함수도 있지만 사용 안 함
- 템플릿 작성자가 직접 계산식을 작성

### 영향 범위
- 모든 대시보드 페이지 — 사용자가 보는 모든 곳

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. `${file.formattedSize}` 활용 | record accessor | 도메인 메서드 활용 | record는 getXxx() 자동 생성 안 함 (P2-15 비슷한 이슈) |
| B. JS `data-format="filesize"` 처리 | JS 측에서 dynamic | 동적 정밀도 | JS 비활성 환경에서 깨짐 |
| C. 컨트롤러에서 미리 변환한 DTO 사용 | model에 formatted 값 추가 | SSR 호환 | 반복 코드 |

### 채택: **옵션 A + JS 보강** (record accessor 활용 + data 속성)
이유:
1. 도메인 메서드가 이미 존재(`FileMetadata.getFormattedSize()`)
2. record는 `getFormattedSize()`를 자동 생성하지 않지만, **메서드 이름이 `get...`이면 Thymeleaf는 자동으로 인식**
3. JS data-format="filesize"는 백업

검증: Thymeleaf SpEL은 record의 메서드 호출 시 다음 순서:
1. `formattedSize()` (record component accessor) — 우선
2. `getFormattedSize()` (JavaBean style) — fallback

`FileMetadata`는 record가 아닌 일반 클래스가 아니라 `record`로 선언됨. record의 component만 자동 accessor 생성. `getFormattedSize()`는 일반 메서드라 SpEL이 인식 OK.

### Step 1: 템플릿에서 `file.formattedSize` 사용
```html
<!-- dashboard.html, files.html, file-detail.html -->
<span th:text="${file.formattedSize}">0 B</span>
```

Thymeleaf의 property access:
- `file.formattedSize` → `file.getFormattedSize()` 호출
- record component accessor와 충돌 없음 (component name이 `formattedSize`가 아니므로)

### Step 2: sessions.html — bytesSent는 entity 속성
`StreamingSessionEntity.getBytesSent()` 반환 long. 템플릿에서 직접 포맷하려면:
```html
<!-- 옵션 1: entity에 메서드 추가 -->
public String getBytesSentFormatted() { return ByteSizeFormatter.format(bytesSent); }

<!-- 옵션 2: 템플릿에 utility expression -->
<!-- Thymeleaf 자체에 ByteSizeFormatter 호출 어려움 → option 1 채택 -->
```

→ `StreamingSessionEntity`에 `getBytesSentFormatted()` 추가:
```java
public String getBytesSentFormatted() {
    return ByteSizeFormatter.format(bytesSent);
}
```

→ sessions.html:
```html
<span th:text="${session.bytesSentFormatted}">0 B</span>
```

### Step 3: data-format JS는 그대로 유지 (대안)
JS 측에서 `formatFileSize`도 자동 단위 선택 — 백업 안전망.

### Step 4: dashboard.js의 formatFileSize 정밀도 통일
`ByteSizeFormatter.format`과 동일 정책:
```javascript
function formatFileSize(bytes) {
    if (bytes < 0) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    var units = ['KB', 'MB', 'GB', 'TB'];
    var k = 1024;
    var i = Math.floor(Math.log(bytes) / Math.log(k));
    if (i > units.length) i = units.length;
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i - 1];
}
```

(현재 코드는 단위 0 base — `'B'` 포함. 0 B 처리 + B 단위 정수 표시 등 micro 차이 정리)

## Before / After

### Before — files.html
```html
<td>
    <span th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 2)} + ' MB'"></span>
</td>
```

### After — files.html
```html
<td>
    <span th:text="${file.formattedSize}">0 B</span>
</td>
```

### Before — dashboard.html
```html
<td>
    <span data-format="filesize" th:data-value="${file.size}"
          th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 1)} + ' MB'"></span>
</td>
```

### After — dashboard.html
```html
<td>
    <span th:text="${file.formattedSize}">0 B</span>
</td>
```

### Before — file-detail.html
```html
<div class="file-info-value"
     th:text="${#numbers.formatDecimal(file.size / 1024 / 1024, 1, 2)} + ' MB'"></div>
```

### After — file-detail.html
```html
<div class="file-info-value" th:text="${file.formattedSize}">0 B</div>
```

### Before — sessions.html
```html
<td>
    <span th:text="${#numbers.formatDecimal(session.bytesSent / 1024 / 1024, 1, 2)} + ' MB'"></span>
</td>
```

### After — sessions.html
```html
<td>
    <span th:text="${session.bytesSentFormatted}">0 B</span>
</td>
```

### Before — StreamingSessionEntity (변경 없음)
```java
public long getBytesSent() { return bytesSent; }
```

### After — StreamingSessionEntity (메서드 추가)
```java
import io.github.junhyeong9812.streamix.core.domain.util.ByteSizeFormatter;

public long getBytesSent() { return bytesSent; }

/**
 * 전송 바이트 수를 사람이 읽기 쉬운 형식으로 반환합니다.
 * 
 * @return 예: "1.5 GB", "256 MB", "10 KB"
 */
public String getBytesSentFormatted() {
    return ByteSizeFormatter.format(bytesSent);
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew compileJava
```

### 2. 수동 검증
1. 작은 파일(500B), 큰 파일(2GB) 업로드
2. 대시보드 → 둘 다 적절한 단위로 표시 (`500 B`, `2.0 GB`)
3. 파일 상세, 파일 목록 페이지도 동일

### 3. 회귀 (Thymeleaf SSR)
브라우저 dev tools에서 HTML 출력 확인.

## 관련 파일
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/dashboard.html`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/files.html`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/file-detail.html`
- `streamix-spring-boot-starter/src/main/resources/templates/streamix/sessions.html`
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/StreamingSessionEntity.java`
- `streamix-spring-boot-starter/src/main/resources/static/streamix/js/dashboard.js`

## 참고
- Thymeleaf property access — record vs class
- 도메인 객체에 formatting 메서드 두는 것 vs DTO 변환 — 본 라이브러리는 단순성 위해 도메인 메서드 활용
