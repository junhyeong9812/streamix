# [P1-10] FileType.isPreviewable() — DOCUMENT가 항상 true (논리 버그)

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — 논리 버그 / UI 잘못된 동작** |
| 카테고리 | 도메인 / 정확성 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `FileType.java`, `FileMetadata.java` |

## 문제 분석

### 현재 동작
```java
// FileType.java:203-206
public boolean isPreviewable() {
    return this == IMAGE || this == VIDEO || this == AUDIO ||
        (this == DOCUMENT && extensions.contains("pdf"));
}
```

### 버그 메커니즘
`DOCUMENT` enum의 `extensions` Set 정의:
```java
DOCUMENT("document", Set.of(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "rtf", "odt", "ods", "odp", "csv", "md", "json", "xml", "html", "htm"
), true),
```

`extensions.contains("pdf")` → DOCUMENT 타입 자체에 항상 "pdf"가 포함되어 있음 → **항상 true**

결과:
- `FileType.DOCUMENT.isPreviewable()` → 항상 `true`
- 즉 docx, xlsx, pptx, txt, csv 등 **모든 DOCUMENT가 미리보기 가능으로 분류**

### 의도된 동작
PDF만 미리보기 가능. 다른 DOCUMENT는 다운로드만.

### 의도가 코드에 반영되지 못한 이유
`FileType` enum은 **타입 단위** 정보만 가짐. 특정 인스턴스의 contentType이 `application/pdf`인지 여부는 `FileMetadata` 인스턴스 단위로 결정됨. 따라서 `FileType.isPreviewable()` 시그니처로는 PDF 판별 불가.

### 호출자
`FileMetadata.isPreviewable()` (line 208-210):
```java
public boolean isPreviewable() {
    return type.isPreviewable();
}
```
→ FileMetadata가 contentType을 가지고 있는데도 활용 안 함.

또한 템플릿(`file-detail.html`)은 `${file.type.name() == 'DOCUMENT'}`로 분기하여 모든 DOCUMENT를 다운로드 모드로 표시 → **현재 UI는 우연히 정상 동작**. 하지만 외부에서 `isPreviewable()`을 호출하는 사용자 코드는 잘못된 결과를 받음.

### 영향 범위
- `FileMetadata.isPreviewable()` — 외부에 노출된 API. 잘못된 결과
- `FileType.isPreviewable()` — 의미 없는 메서드 (항상 true 또는 무관)
- 향후 PDF 인라인 뷰어 추가 시 모든 DOCUMENT가 PDF 뷰어로 잘못 분기됨

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. `FileType.isPreviewable()` 삭제 + `FileMetadata.isPreviewable()`만 contentType으로 판별 | 정확한 책임 분리 | 깔끔 | breaking change |
| B. `FileType.isPreviewable(String contentType)` 시그니처 변경 | 명시적 | 유연 | enum에 contentType 인자가 어색 |
| C. `FileType.isPreviewable()` deprecate + `FileMetadata.isPreviewable()` 정확 구현 | 점진적 | 호환성 | API 두 개 |

### 채택: 옵션 A — 삭제 + FileMetadata로 책임 이전
이유:
1. `FileType.isPreviewable()`은 의미가 없음 (typed information으로 결정 불가)
2. `FileMetadata`가 contentType을 가지고 있으므로 자연스러운 위치
3. v2.0.x 마이너 릴리스이지만 잘못된 동작 수정이므로 breaking change 정당화 가능

### Step 1: `FileType.isPreviewable()` 삭제 + 마이그레이션 javadoc
```java
// 삭제
// public boolean isPreviewable() { ... }
```

### Step 2: `FileMetadata.isPreviewable()` 정확 구현
```java
/**
 * 브라우저에서 미리보기 가능한지 확인합니다.
 *
 * <p>미리보기 가능 조건:</p>
 * <ul>
 *   <li>IMAGE, VIDEO, AUDIO 타입</li>
 *   <li>또는 DOCUMENT 타입이면서 contentType이 application/pdf</li>
 * </ul>
 *
 * @return 미리보기 가능 시 {@code true}
 */
public boolean isPreviewable() {
    if (type == FileType.IMAGE || type == FileType.VIDEO || type == FileType.AUDIO) {
        return true;
    }
    return type == FileType.DOCUMENT && "application/pdf".equalsIgnoreCase(contentType);
}
```

### Step 3: 기존 테스트 업데이트
`FileMetadataTest`에 isPreviewable 관련 테스트가 있는지 확인 → **없음** (검증 시점). 새 테스트 추가:
```java
@Nested
@DisplayName("isPreviewable 테스트")
class IsPreviewableTest {

    @Test
    @DisplayName("IMAGE는 미리보기 가능하다")
    void imagePreviewable() {
        FileMetadata m = createMetadataWithType(FileType.IMAGE);
        assertThat(m.isPreviewable()).isTrue();
    }

    @Test
    @DisplayName("PDF DOCUMENT는 미리보기 가능하다")
    void pdfPreviewable() {
        FileMetadata m = new FileMetadata(
            UUID.randomUUID(), "report.pdf", FileType.DOCUMENT, "application/pdf",
            1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now());
        assertThat(m.isPreviewable()).isTrue();
    }

    @Test
    @DisplayName("PDF가 아닌 DOCUMENT는 미리보기 불가")
    void nonPdfDocumentNotPreviewable() {
        FileMetadata m = new FileMetadata(
            UUID.randomUUID(), "doc.docx", FileType.DOCUMENT,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            1024L, "/path", null, LocalDateTime.now(), LocalDateTime.now());
        assertThat(m.isPreviewable()).isFalse();
    }

    @Test
    @DisplayName("ARCHIVE는 미리보기 불가")
    void archiveNotPreviewable() {
        FileMetadata m = createMetadataWithType(FileType.ARCHIVE);
        assertThat(m.isPreviewable()).isFalse();
    }
}
```

### Step 4: `FileTypeTest`의 `isPreviewable` 관련 테스트 정리
`FileTypeTest`에 `isPreviewable` 테스트는 없음 (검증 시점). 변경 불필요.

### Step 5: `FileType.java`의 `Arrays` 미사용 import 정리 (덤)
```java
import java.util.Arrays;  // 미사용 — 삭제
```

## Before / After

### Before — FileType.java
```java
/**
 * 브라우저에서 미리보기 가능한지 확인합니다.
 *
 * <p>IMAGE, VIDEO, AUDIO, PDF 파일이 미리보기 가능합니다.</p>
 *
 * @return 미리보기 가능 시 {@code true}
 * @since 1.0.7
 */
public boolean isPreviewable() {
    return this == IMAGE || this == VIDEO || this == AUDIO ||
        (this == DOCUMENT && extensions.contains("pdf"));
}
```

### After — FileType.java
```java
// isPreviewable() 메서드 삭제
// → FileMetadata.isPreviewable()로 이전 (contentType 정보 필요)
```

### Before — FileMetadata.java
```java
/**
 * 브라우저에서 미리보기 가능한지 확인합니다.
 *
 * <p>IMAGE, VIDEO, AUDIO, PDF 파일이 미리보기 가능합니다.</p>
 *
 * @return 미리보기 가능 시 {@code true}
 * @since 1.0.7
 */
public boolean isPreviewable() {
    return type.isPreviewable();
}
```

### After — FileMetadata.java
```java
/**
 * 브라우저에서 미리보기 가능한지 확인합니다.
 *
 * <p>미리보기 가능 조건:</p>
 * <ul>
 *   <li>IMAGE, VIDEO, AUDIO — 항상 가능</li>
 *   <li>DOCUMENT — contentType이 {@code application/pdf}일 때만</li>
 *   <li>ARCHIVE, OTHER — 불가</li>
 * </ul>
 *
 * @return 미리보기 가능 시 {@code true}
 * @since 1.0.7
 */
public boolean isPreviewable() {
    if (type == FileType.IMAGE || type == FileType.VIDEO || type == FileType.AUDIO) {
        return true;
    }
    return type == FileType.DOCUMENT && "application/pdf".equalsIgnoreCase(contentType);
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-core:compileJava
```

### 2. 신규 테스트 + 기존 테스트
```bash
./gradlew :streamix-core:test --tests FileMetadataTest
./gradlew :streamix-core:test --tests FileTypeTest
```

### 3. 사용자 코드 영향 확인
프로젝트 내 `isPreviewable` 호출처:
```bash
grep -rn 'isPreviewable' streamix-core streamix-spring-boot-starter
```
- 템플릿(`file-detail.html`)은 `${file.type.name() == 'DOCUMENT'}` 패턴 사용 → 영향 없음
- 외부 사용자가 `FileType.X.isPreviewable()` 직접 호출했다면 컴파일 에러 (의도된 breaking)

### 4. CHANGELOG에 breaking change 기록
v2.0.1 changelog:
```markdown
### Breaking Changes
- `FileType.isPreviewable()` 삭제 — contentType 정보가 필요하므로 `FileMetadata.isPreviewable()` 사용
```

## 관련 파일
- `streamix-core/src/main/java/.../core/domain/model/FileType.java`
- `streamix-core/src/main/java/.../core/domain/model/FileMetadata.java`
- `streamix-core/src/test/java/.../core/domain/model/FileMetadataTest.java` (테스트 추가)

## 참고
- 단일 책임 원칙 (SRP): enum은 타입 단위 정보만, 인스턴스 단위 판단은 인스턴스가 가진 데이터로
- enum-based polymorphism의 한계: enum value는 정적 — instance attribute로 결정해야 하는 로직은 enum에 두지 않는다
