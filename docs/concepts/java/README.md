# Java 25 & 관련 개념

Streamix에서 활용하는 Java 기능들입니다.

---

## 1. Record (Java 16+)

### 개념

불변(immutable) 데이터 클래스를 간결하게 선언하는 기능입니다.
Lombok의 `@Value`나 `@Data`를 대체할 수 있습니다.

### 기본 문법

```java
// 기존 방식 (30줄 이상)
public class FileMetadata {
    private final UUID id;
    private final String name;
    private final long size;
    
    public FileMetadata(UUID id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public long getSize() { return size; }
    
    @Override
    public boolean equals(Object o) { ... }
    
    @Override
    public int hashCode() { ... }
    
    @Override
    public String toString() { ... }
}

// Record (1줄)
public record FileMetadata(UUID id, String name, long size) {}
```

### 자동 생성되는 것들

- 모든 필드에 대한 private final 선언
- 모든 필드를 받는 생성자 (Canonical Constructor)
- 각 필드에 대한 접근자 메서드 (id(), name(), size())
- equals(), hashCode(), toString()

### Compact Constructor

```java
public record FileMetadata(
    UUID id,
    String originalName,
    FileType type,
    long size
) {
    // Compact Constructor - 검증 로직 추가
    public FileMetadata {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(originalName, "originalName must not be null");
        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
```

### 메서드 추가

```java
public record FileMetadata(UUID id, String name, FileType type) {
    
    // 인스턴스 메서드
    public boolean isVideo() {
        return type == FileType.VIDEO;
    }
    
    public boolean isImage() {
        return type == FileType.IMAGE;
    }
    
    // 정적 팩토리 메서드
    public static FileMetadata of(String name, FileType type) {
        return new FileMetadata(UUID.randomUUID(), name, type);
    }
}
```

### 주의사항

- Record는 다른 클래스를 상속할 수 없음 (암묵적으로 java.lang.Record 상속)
- 인터페이스 구현은 가능
- 필드는 모두 final (불변)
- JPA Entity로 사용 불가 (기본 생성자 없음)

---

## 2. Sealed Classes (Java 17+)

### 개념

클래스의 상속을 제한하는 기능입니다.
어떤 클래스가 자신을 상속할 수 있는지 명시적으로 선언합니다.

### 문법

```java
// StreamixException을 상속할 수 있는 클래스 제한
public sealed class StreamixException extends RuntimeException
    permits FileNotFoundException, InvalidFileTypeException, StorageException {
    
    public StreamixException(String message) {
        super(message);
    }
}

// final: 더 이상 상속 불가
public final class FileNotFoundException extends StreamixException {
    public FileNotFoundException(UUID fileId) {
        super("File not found: " + fileId);
    }
}

// non-sealed: 상속 제한 해제
public non-sealed class StorageException extends StreamixException {
    public StorageException(String message) {
        super(message);
    }
}
```

### 장점

- 컴파일 타임에 상속 계층 검증
- Pattern Matching에서 exhaustive check 가능

---

## 3. Pattern Matching (Java 21+)

### instanceof 패턴 매칭

```java
// 기존 방식
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// 패턴 매칭
if (obj instanceof String s) {
    System.out.println(s.length());
}
```

### switch 패턴 매칭

```java
public String getFileDescription(Object file) {
    return switch (file) {
        case FileMetadata m when m.isVideo() -> "Video: " + m.name();
        case FileMetadata m when m.isImage() -> "Image: " + m.name();
        case FileMetadata m -> "Unknown type: " + m.name();
        case String s -> "Path: " + s;
        case null -> "No file";
        default -> "Unknown";
    };
}
```

### Record 패턴 (Java 21+)

```java
public record Point(int x, int y) {}
public record Rectangle(Point topLeft, Point bottomRight) {}

// Record 분해
public int calculateArea(Rectangle rect) {
    return switch (rect) {
        case Rectangle(Point(int x1, int y1), Point(int x2, int y2)) ->
            Math.abs(x2 - x1) * Math.abs(y2 - y1);
    };
}
```

---

## 4. Virtual Threads (Java 21+)

### 개념

경량 스레드로, 기존 플랫폼 스레드보다 훨씬 적은 리소스로 대량의 동시 작업을 처리합니다.
Project Loom의 결과물입니다.

### 기존 스레드 vs Virtual Thread

| 항목 | Platform Thread | Virtual Thread |
|------|-----------------|----------------|
| 메모리 | ~1MB 스택 | ~수 KB |
| 생성 비용 | 높음 | 낮음 |
| 컨텍스트 스위칭 | OS 레벨 | JVM 레벨 |
| 개수 제한 | 수천 개 | 수백만 개 가능 |

### 사용법

```java
// 직접 생성
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Virtual Thread");
});

// ExecutorService
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        // 작업
    });
}

// Spring Boot에서 활성화
// application.yml
spring:
  threads:
    virtual:
      enabled: true
```

### 주의사항

- synchronized 블록에서 블로킹 시 pinning 발생 가능
- ReentrantLock 사용 권장
- CPU 집약적 작업에는 적합하지 않음

---

## 5. InputStream.transferTo (Java 9+)

### 개념

InputStream의 모든 내용을 OutputStream으로 전송하는 편의 메서드입니다.

### 사용법

```java
// 기존 방식
byte[] buffer = new byte[8192];
int bytesRead;
while ((bytesRead = inputStream.read(buffer)) != -1) {
    outputStream.write(buffer, 0, bytesRead);
}

// transferTo 사용
inputStream.transferTo(outputStream);
```

### 스트리밍에서 활용

```java
@GetMapping("/stream/{id}")
public StreamingResponseBody stream(@PathVariable UUID id) {
    return outputStream -> {
        try (InputStream is = storageService.load(id)) {
            is.transferTo(outputStream);
        }
    };
}
```

---

## 6. try-with-resources 개선 (Java 9+)

### effectively final 변수 사용 가능

```java
// Java 8
InputStream is = getInputStream();
try (InputStream wrapped = is) {
    // 사용
}

// Java 9+
InputStream is = getInputStream();
try (is) {  // effectively final이면 직접 사용 가능
    // 사용
}
```

---

## 7. Optional 개선

### or() (Java 9+)

```java
Optional<FileMetadata> findById(UUID id) {
    return primaryStorage.findById(id)
        .or(() -> secondaryStorage.findById(id))
        .or(() -> archiveStorage.findById(id));
}
```

### ifPresentOrElse() (Java 9+)

```java
fileMetadata.ifPresentOrElse(
    meta -> processFile(meta),
    () -> throw new FileNotFoundException(id)
);
```

### stream() (Java 9+)

```java
// Optional을 Stream으로 변환
List<String> names = fileIds.stream()
    .map(this::findById)           // Stream<Optional<FileMetadata>>
    .flatMap(Optional::stream)     // Stream<FileMetadata>
    .map(FileMetadata::name)
    .toList();
```

---

## 8. Java 25 신기능

### Compact Source Files (JEP 512)

클래스 선언 없이 바로 main 메서드 작성 가능 (스크립트용):

```java
// Hello.java
void main() {
    System.out.println("Hello, Java 25!");
}
```

### Module Import (JEP 511)

모듈 전체를 한 번에 import:

```java
import module java.base;  // java.util, java.io 등 포함

public class Main {
    void main() {
        List<String> list = new ArrayList<>();  // import 없이 사용
    }
}
```

### Scoped Values (JEP 506)

ThreadLocal의 대안으로, 불변 값을 스레드/가상 스레드 간 공유:

```java
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void handleRequest(User user) {
    ScopedValue.where(CURRENT_USER, user).run(() -> {
        processRequest();  // CURRENT_USER.get()으로 접근 가능
    });
}
```

---

## 9. 참고 자료

- [JDK 25 Release Notes](https://openjdk.org/projects/jdk/25/)
- [Java Record](https://docs.oracle.com/en/java/javase/21/language/records.html)
- [Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [Pattern Matching](https://docs.oracle.com/en/java/javase/21/language/pattern-matching.html)