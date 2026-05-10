# [P1-06] LocalFileStorageAdapter — Path Traversal 검증 누락

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — 보안 / 임의 파일 액세스 가능** |
| 카테고리 | 보안 / 파일 시스템 |
| 발견 위치 | `streamix-core` |
| 영향 파일 | `LocalFileStorageAdapter.java` |
| OWASP | A01:2021 Broken Access Control / Path Traversal (CWE-22) |

## 문제 분석

### 현재 동작
```java
// LocalFileStorageAdapter.java
@Override
public String save(String fileName, InputStream inputStream, long size) {
    Path filePath = resolveAndValidatePath(fileName);  // ✓ 검증
    ...
}

@Override
public InputStream load(String storagePath) {
//    Path filePath = Path.of(storagePath);              // ⚠ 주석 처리된 코드 잔존
    Path filePath = storagePath.startsWith("/")           // ⚠ 검증 없이 그대로 사용
        ? Path.of(storagePath)
        : basePath.resolve(storagePath);
    ...
}

@Override
public InputStream loadPartial(String storagePath, long start, long end) {
    Path filePath = Path.of(storagePath);                 // ⚠ 검증 없이 그대로 사용
    ...
}

@Override
public void delete(String storagePath) {
    Path filePath = Path.of(storagePath);                 // ⚠ 검증 없이 그대로 사용
    ...
}

@Override
public boolean exists(String storagePath) {
    return Files.exists(Path.of(storagePath));            // ⚠ 검증 없이 그대로 사용
}

@Override
public long getSize(String storagePath) {
    Path filePath = Path.of(storagePath);                 // ⚠ 검증 없이 그대로 사용
    ...
}
```

### 위협 시나리오

#### 시나리오 1 — 메타데이터 변조
공격자가 `FileMetadata.storagePath`를 `/etc/passwd`로 변조한 후 다음 호출:
```bash
curl http://server/api/streamix/files/{id}/stream
```
→ `FileStreamService` → `storage.load(metadata.storagePath())` → `LocalFileStorageAdapter.load("/etc/passwd")` → `/etc/passwd` 내용 노출.

공격자가 어떻게 메타데이터를 변조하나? 직접 SQL 변조는 어렵지만:
- 사용자가 `JpaFileMetadataAdapter` 외 다른 어댑터(예: 사용자 직접 구현 MongoDB 어댑터)를 사용하고 그 어댑터에 검증 결함이 있으면
- 사용자 코드에서 메타데이터를 수동 생성하면

#### 시나리오 2 — 멱등 삭제 호출 오용
사용자 코드가 path를 직접 받아 `storage.delete(userInput)` 호출하면 임의 파일 삭제. (라이브러리가 위험한 API를 안전하게 노출하지 않음)

### 기대 동작
모든 storage 메서드(load, loadPartial, delete, exists, getSize)가 path를 basePath 기준으로 검증해야 함.

### 원인 분석
- `save`는 신규 작성 시 path traversal 위험을 인식하여 검증 추가
- `load` 등은 "어차피 우리가 저장한 path니까"라는 신뢰 기반 가정
- 신뢰 경계가 코드로 명확히 표현되지 않음

### 영향 범위
- 메타데이터 신뢰 모델: 라이브러리는 메타데이터를 신뢰한다는 가정이지만 그 가정이 코드에 명시되지 않음
- 외부 어댑터 사용자: 결함 있는 메타데이터 어댑터 사용 시 즉시 RCE급 위험으로 격상
- 기존 테스트: `save`의 path traversal 테스트만 있음 (`LocalFileStorageAdapterTest.preventsPathTraversal`)

## 변경 프로세스

### 옵션 비교
| 옵션 | 동작 | 장점 | 단점 |
|------|------|------|------|
| A. 모든 메서드에 검증 추가 (절대경로/상대경로 통일 처리) | basePath 외부 거부 | 일관성, 가장 안전 | 절대경로로 저장된 기존 데이터 호환성 |
| B. storagePath를 항상 상대경로로 저장 + load 시 basePath.resolve | 저장 시 변환, 로드 시 검증 | 명확한 신뢰 경계 | 마이그레이션 필요 (기존 메타데이터) |
| C. 외부에 검증 책임 위임 | 인터페이스 javadoc 명시 | 코드 변경 최소 | 사용자 실수 시 깨짐 |

### 채택: 옵션 A — 모든 메서드 검증
이유:
1. 라이브러리 사용자에게 책임 떠넘기지 않음 — defense in depth
2. 기존 데이터 호환: 절대경로면 basePath 시작 여부만 검증 (실제 파일은 그대로)
3. 마이그레이션 비용 없음

### Step 1: `resolveAndValidatePath` 통합 메서드 개선
현재 `save`만 사용하는 private 메서드를 모든 메서드가 사용할 수 있도록 확장.

```java
/**
 * 입력 path를 검증하고 절대 경로로 정규화한다.
 * 
 * - 절대 경로면 그대로 normalize
 * - 상대 경로면 basePath 기준으로 resolve
 * - 결과가 basePath 외부면 IllegalArgumentException
 */
private Path resolveAndValidatePath(String path) {
    Path resolved = path.startsWith("/") || hasWindowsDriveLetter(path)
        ? Path.of(path).normalize()
        : basePath.resolve(path).normalize();

    if (!resolved.startsWith(basePath)) {
        throw new IllegalArgumentException("Path is outside base directory: " + path);
    }
    return resolved;
}

private static boolean hasWindowsDriveLetter(String path) {
    return path.length() >= 3
        && Character.isLetter(path.charAt(0))
        && path.charAt(1) == ':'
        && (path.charAt(2) == '/' || path.charAt(2) == '\\');
}
```

### Step 2: 모든 메서드에서 호출
```java
@Override
public InputStream load(String storagePath) {
    Path filePath = resolveAndValidatePath(storagePath);
    if (!Files.exists(filePath)) {
        throw new FileNotFoundException(storagePath);
    }
    try {
        return Files.newInputStream(filePath);
    } catch (IOException e) {
        throw StorageException.loadFailed(storagePath, e);
    }
}
// loadPartial, delete, exists, getSize 모두 동일하게
```

### Step 3: 주석 처리된 코드 제거
```java
// 삭제: //    Path filePath = Path.of(storagePath);
```

### Step 4: 검증 실패 시 메시지 — 정보 누출 최소화
- "Path is outside base directory: " 뒤에 사용자 입력을 그대로 노출하면 path 전체가 로그/응답에 노출됨
- → 입력 path는 로그에만 남기고 사용자 응답에는 일반 메시지만:
```java
log.warn("Rejected path outside base directory: {}", path);
throw new IllegalArgumentException("Invalid storage path");
```

### Step 5: 테스트 추가
```java
@Test
void load_rejectsPathOutsideBaseDir() {
    assertThatThrownBy(() -> adapter.load("/etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void delete_rejectsPathOutsideBaseDir() {
    assertThatThrownBy(() -> adapter.delete("/etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void getSize_rejectsPathOutsideBaseDir() {
    assertThatThrownBy(() -> adapter.getSize("/etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void loadPartial_rejectsPathOutsideBaseDir() {
    assertThatThrownBy(() -> adapter.loadPartial("/etc/passwd", 0, 100))
        .isInstanceOf(IllegalArgumentException.class);
}
```

기존 테스트 `LocalFileStorageAdapterTest.LoadTest.throwsWhenFileNotFound`가 `/nonexistent/file.txt`로 호출하는데 이는 basePath 외부 → **이제 IllegalArgumentException 발생**. 테스트 갱신 필요:
```java
@Test
void throwsWhenFileNotFound() {
    Path nonExistent = tempDir.resolve("nonexistent.txt");
    assertThatThrownBy(() -> adapter.load(nonExistent.toString()))
        .isInstanceOf(FileNotFoundException.class);
}
```

## Before / After

### Before — load (대표)
```java
@Override
public InputStream load(String storagePath) {
//    Path filePath = Path.of(storagePath);
    // 상대 경로면 basePath 기준으로 해석
    Path filePath = storagePath.startsWith("/")
        ? Path.of(storagePath)
        : basePath.resolve(storagePath);

    if (!Files.exists(filePath)) {
        throw new FileNotFoundException(storagePath);
    }

    try {
        return Files.newInputStream(filePath);
    } catch (IOException e) {
        throw StorageException.loadFailed(storagePath, e);
    }
}
```

### After — load
```java
@Override
public InputStream load(String storagePath) {
    Path filePath = resolveAndValidatePath(storagePath);

    if (!Files.exists(filePath)) {
        throw new FileNotFoundException(storagePath);
    }

    try {
        return Files.newInputStream(filePath);
    } catch (IOException e) {
        throw StorageException.loadFailed(storagePath, e);
    }
}
```

### Before — resolveAndValidatePath
```java
private Path resolveAndValidatePath(String fileName) {
    Path resolved = basePath.resolve(fileName).normalize();

    // Path Traversal 공격 방지
    if (!resolved.startsWith(basePath)) {
        throw new IllegalArgumentException("Invalid file path: " + fileName);
    }

    return resolved;
}
```

### After — resolveAndValidatePath (확장)
```java
/**
 * 입력 path를 검증하고 basePath 기준 절대 경로로 정규화합니다.
 *
 * <p>다음 동작을 보장합니다:</p>
 * <ul>
 *   <li>상대 경로 → basePath와 결합하여 정규화</li>
 *   <li>절대 경로 → 그대로 정규화</li>
 *   <li>결과가 basePath 외부면 {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <p>이 메서드는 모든 파일 시스템 작업의 path 검증 진입점입니다.
 * Path Traversal 공격(예: {@code ../../etc/passwd})을 방어합니다.</p>
 *
 * @param path 검증할 path (상대/절대)
 * @return basePath 내부의 정규화된 절대 경로
 * @throws IllegalArgumentException basePath 외부로 접근 시도 시
 */
private Path resolveAndValidatePath(String path) {
    Path resolved = isAbsolutePath(path)
        ? Path.of(path).normalize()
        : basePath.resolve(path).normalize();

    if (!resolved.startsWith(basePath)) {
        log.warn("Rejected path outside base directory: {}", path);
        throw new IllegalArgumentException("Invalid storage path");
    }
    return resolved;
}

private static boolean isAbsolutePath(String path) {
    if (path == null || path.isEmpty()) return false;
    if (path.startsWith("/")) return true;
    // Windows: C:\, D:/ 등
    return path.length() >= 3
        && Character.isLetter(path.charAt(0))
        && path.charAt(1) == ':'
        && (path.charAt(2) == '/' || path.charAt(2) == '\\');
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-core:compileJava
```

### 2. 기존 테스트 회귀
```bash
./gradlew :streamix-core:test --tests LocalFileStorageAdapterTest
```
- `preventsPathTraversal` (기존) — 통과 유지
- `throwsWhenFileNotFound` (load) — 메시지 갱신 필요
- `throwsWhenFileNotFound` (loadPartial) — 메시지 갱신 필요
- `doesNotThrowWhenFileNotExists` (delete) — 갱신 필요

### 3. 신규 보안 테스트
```bash
./gradlew :streamix-core:test --tests LocalFileStorageAdapterTest.SecurityTest
```

### 4. 메시지 누출 확인
검증 실패 시 응답에 입력 path가 노출되지 않는지 확인:
```bash
curl -X DELETE 'http://localhost:8080/api/streamix/files/00000000-0000-0000-0000-000000000000'
# 메타데이터가 변조된 가짜 path를 가진 경우 응답에 path 미노출 확인
```

## 관련 파일
- `streamix-core/src/main/java/.../core/adapter/out/storage/LocalFileStorageAdapter.java`
- `streamix-core/src/test/java/.../core/adapter/out/storage/LocalFileStorageAdapterTest.java`

## 참고
- OWASP Path Traversal: https://owasp.org/www-community/attacks/Path_Traversal
- CWE-22: Improper Limitation of a Pathname to a Restricted Directory
- Java NIO `Path.normalize()` 동작 — `..` 경로 정리 후 검증해야 우회 방지
