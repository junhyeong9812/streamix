# [P2-16] FileTypeDetector — 무상태 클래스를 인스턴스로 사용

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P2 — 효율성 / 설계** |
| 카테고리 | 리팩토링 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `FileTypeDetector.java`, `FileUploadService.java` |

## 문제 분석

### 현재 동작
```java
// FileTypeDetector.java
public class FileTypeDetector {
    public FileTypeDetector() {}
    
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(...);
    
    public String extractExtension(String fileName) { ... }
    public FileType detect(String fileName) { ... }
    public FileType detect(String fileName, String contentType) { ... }
    public String getContentType(String extension) { ... }
    public boolean isSupported(String fileName) { ... }
    public boolean isSupported(String fileName, String contentType) { ... }
    public FileType detectAllowingAll(String fileName) { return detect(fileName); }  // wrapper
}
```

```java
// FileUploadService.java:111
this.fileTypeDetector = new FileTypeDetector();   // 매 인스턴스 생성마다 new
```

### 발견
1. **모든 메서드가 무상태** — 인스턴스 필드 없음, 정적 데이터 `EXTENSION_TO_CONTENT_TYPE`만 사용
2. `FileUploadService`는 매번 `new FileTypeDetector()` — 사실상 의미 없는 인스턴스 생성
3. `detectAllowingAll(String)`은 `detect(String)`과 동일 동작 — 의미 없는 wrapper

### 영향 범위
- **메모리/CPU**: 미미 (인스턴스가 필드 없으므로 매우 작음)
- **테스트**: 매번 `new FileTypeDetector()` 호출 (`FileTypeDetectorTest.setUp`)
- **API**: 사용자 코드에서 `new FileTypeDetector()` 호출이 가능 — breaking change 시 영향

### 옵션 비교
| 옵션 | 변경 | 장점 | 단점 |
|------|------|------|------|
| A. 모든 메서드 static | 인스턴스 메서드 → static | 의도 명확, GC 부담 0 | breaking change |
| B. 싱글톤 INSTANCE 노출 + 인스턴스 메서드 유지 | `public static final FileTypeDetector INSTANCE` | 호환성 유지 | 인스턴스 메서드와 static 혼재 |
| C. 그대로 유지 + javadoc | 변경 없음 | 호환성 100% | 부채 유지 |

### 채택: 옵션 A (static + utility)
이유:
1. v2.0.x 마이너지만 의미 있는 정리. 사용자 영향 작음 (직접 호출자 적을 것)
2. `final class` + `private constructor`로 instantiation 차단
3. detector 변수도 정적 메서드 호출로 변경

## 변경 프로세스

### Step 1: FileTypeDetector를 utility class로
```java
public final class FileTypeDetector {

    private FileTypeDetector() {
        throw new AssertionError("Utility class");
    }

    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(...);

    public static String extractExtension(String fileName) { ... }
    public static FileType detect(String fileName) { ... }
    public static FileType detect(String fileName, String contentType) { ... }
    public static String getContentType(String extension) { ... }
    public static boolean isSupported(String fileName) { ... }
    public static boolean isSupported(String fileName, String contentType) { ... }
    
    // detectAllowingAll 삭제 — detect()와 동일
    
    // P1-12에서 추가된 메서드도 static
    public static boolean isValidContentType(String contentType) { ... }
    public static String safeContentType(String contentType, String fileName) { ... }
}
```

### Step 2: FileUploadService 수정
```java
// 필드 제거
// private final FileTypeDetector fileTypeDetector;

// 호출 변경
FileType fileType = FileTypeDetector.detect(command.originalName(), command.contentType());
String extension = FileTypeDetector.extractExtension(command.originalName());
```

### Step 3: 테스트 갱신
```java
// Before
private FileTypeDetector detector;

@BeforeEach
void setUp() {
    detector = new FileTypeDetector();
}

@Test
void test() {
    assertThat(detector.detect("x.jpg")).isEqualTo(FileType.IMAGE);
}

// After
@Test
void test() {
    assertThat(FileTypeDetector.detect("x.jpg")).isEqualTo(FileType.IMAGE);
}
```

### Step 4: detectAllowingAll 삭제
- 사용처 없음 (검증 시점)
- 의미 없는 wrapper

## Before / After

### Before — FileTypeDetector
```java
public class FileTypeDetector {
    public FileTypeDetector() {}
    
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(...);
    
    public String extractExtension(String fileName) { ... }
    public FileType detect(String fileName) { ... }
    public FileType detect(String fileName, String contentType) { ... }
    public String getContentType(String extension) { ... }
    public boolean isSupported(String fileName) { ... }
    public boolean isSupported(String fileName, String contentType) { ... }
    public FileType detectAllowingAll(String fileName) { return detect(fileName); }
}
```

### After — FileTypeDetector
```java
/**
 * 파일 타입을 감지하는 유틸리티 클래스입니다.
 *
 * <p>모든 메서드는 static입니다. 인스턴스 생성 불가합니다.</p>
 */
public final class FileTypeDetector {

    private FileTypeDetector() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(...);

    public static String extractExtension(String fileName) { ... }
    public static FileType detect(String fileName) { ... }
    public static FileType detect(String fileName, String contentType) { ... }
    public static String getContentType(String extension) { ... }
    public static boolean isSupported(String fileName) { ... }
    public static boolean isSupported(String fileName, String contentType) { ... }
    public static boolean isValidContentType(String contentType) { ... }     // P1-12 추가
    public static String safeContentType(String contentType, String fileName) { ... }  // P1-12 추가

    // detectAllowingAll 삭제 — detect와 동일했음
}
```

### Before — FileUploadService
```java
private final FileTypeDetector fileTypeDetector;

public FileUploadService(...) {
    this.fileTypeDetector = new FileTypeDetector();
    ...
}

// 사용
FileType fileType = fileTypeDetector.detect(command.originalName(), command.contentType());
String extension = fileTypeDetector.extractExtension(command.originalName());
```

### After — FileUploadService
```java
// fileTypeDetector 필드 제거

public FileUploadService(...) {
    // FileTypeDetector 인스턴스 변수 초기화 코드 제거
    ...
}

// 사용
FileType fileType = FileTypeDetector.detect(command.originalName(), command.contentType());
String extension = FileTypeDetector.extractExtension(command.originalName());
```

### Before — FileTypeDetectorTest
```java
private FileTypeDetector detector;

@BeforeEach
void setUp() {
    detector = new FileTypeDetector();
}

@Test
void detectsImage() {
    assertThat(detector.detect("photo.jpg")).isEqualTo(FileType.IMAGE);
}
```

### After — FileTypeDetectorTest
```java
// detector 필드 + setUp 제거

@Test
void detectsImage() {
    assertThat(FileTypeDetector.detect("photo.jpg")).isEqualTo(FileType.IMAGE);
}
```

## Breaking Change
- 사용자가 `new FileTypeDetector()` 호출했다면 컴파일 에러
- 사용자가 인스턴스 메서드로 호출했다면 (`detector.detect(...)`) 컴파일 에러
- → CHANGELOG에 명시

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-core:compileJava
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 인스턴스화 차단 검증
```java
@Test
void cannotInstantiate() {
    assertThatThrownBy(() -> {
        Constructor<FileTypeDetector> c = FileTypeDetector.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    })
    .hasCauseInstanceOf(AssertionError.class);
}
```

### 3. 회귀 테스트
```bash
./gradlew test
```

### 4. 사용처 grep
```bash
grep -rn 'new FileTypeDetector\|fileTypeDetector\.' streamix-core/ streamix-spring-boot-starter/
# 결과: 모두 FileTypeDetector.method() 호출 형태
```

## 관련 파일
- `streamix-core/src/main/java/.../core/domain/service/FileTypeDetector.java`
- `streamix-core/src/main/java/.../core/application/service/FileUploadService.java`
- `streamix-core/src/test/java/.../core/domain/service/FileTypeDetectorTest.java`

## 참고
- Effective Java Item 4: "Enforce noninstantiability with a private constructor"
- `final class` + private 생성자 + AssertionError로 reflection 차단
