# [P1-09] FFmpegThumbnailAdapter — Process stderr drain 누락 (deadlock)

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P1 — Process Deadlock** |
| 카테고리 | 안정성 / 외부 프로세스 |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `FFmpegThumbnailAdapter.java` |

## 문제 분석

### 현재 동작
```java
// FFmpegThumbnailAdapter.java:230-247
processBuilder.redirectErrorStream(false);   // stderr 별도 처리

Process process = processBuilder.start();

// stdout에서 이미지 데이터 읽기 (block until EOF)
byte[] thumbnailData;
try (InputStream stdout = process.getInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = stdout.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
    }
    thumbnailData = baos.toByteArray();
}

// 프로세스 완료 대기
boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
```

### 위험 — Process Pipe Buffer Deadlock

#### OS pipe 버퍼 크기
- Linux 기본: 64KB (`/proc/sys/fs/pipe-max-size`)
- FFmpeg는 verbose 정보를 stderr로 출력 (인코딩 진행률, 코덱 정보, 경고 등)
- 64KB 초과하면 stderr write가 **block** (소비자가 읽을 때까지)

#### Deadlock 시퀀스
1. FFmpeg가 stdout에 일부 출력 (썸네일 JPEG 일부)
2. FFmpeg가 stderr에 verbose 출력 (예: 큰 비디오의 경우 64KB+ 발생)
3. JVM이 stdout만 읽고 있음 → stderr 버퍼 가득 → ffmpeg가 stderr write에서 block
4. ffmpeg가 멈춤 → stdout EOF 안 옴 → JVM도 stdout read에서 block
5. **양방향 deadlock**

#### 구현 의도 vs 실제
- `redirectErrorStream(false)` 명시 — stderr를 별도로 보겠다는 의도
- 하지만 stderr drain 시점이 stdout 다 읽고 waitFor 후 → 이미 늦음

#### 30초 timeout으로 부분 방어되지만
- 정상 동작이어도 30초 timeout에 걸려 실패 가능
- timeout 후 `destroyForcibly` 호출하지만 이미 파일 system에 partial output 남음

### 기대 동작
stdout/stderr를 **동시에 읽기**:
- 별도 스레드 풀에서 stdout, stderr 각각 drain
- 양쪽 다 읽고 나서 waitFor

### 원인 분석
- 단순 사용 패턴(간단한 ffmpeg 명령) 가정
- 짧은 비디오(1초 위치 1프레임만 추출)이므로 stderr가 작을 거라는 낙관
- 실제로는 ffmpeg의 stderr는 비디오 길이/코덱과 무관하게 input 분석 정보(스트림, metadata)만으로도 수 KB ~ 수십 KB 발생

### 영향 범위
- 큰 비디오 파일(긴 길이 또는 복잡한 코덱) 업로드 시 30초 timeout
- 사용자에겐 "썸네일 생성 실패"로 보이지만 실제로는 deadlock
- 동시 업로드 다수일 때 thread pool 고갈 가능

## 변경 프로세스

### 옵션 비교
| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A. ExecutorService로 stderr 별도 drain | 명시적 두 스레드 | 정확한 제어 | 코드 복잡도 ↑ |
| B. `redirectErrorStream(true)` + stdout만 읽기 | merge stdout/stderr | 단순 | binary stdout(JPEG) + text stderr가 섞이면 손상 |
| C. `ProcessBuilder.redirectError(File)` | stderr를 임시 파일에 | 단순, 파이프 deadlock 없음 | 디스크 I/O |
| D. `ProcessHandle.start()` async + Java 21+ Virtual Threads | 모던 | Java 25 환경이라 가능 | 복잡 |

### 채택: 옵션 A (ExecutorService) — 옵션 C는 백업
이유:
1. Java 21+의 `Executors.newVirtualThreadPerTaskExecutor()` 사용 가능 (프로젝트가 Java 25)
2. 임시 파일 안 만들어도 되어 깔끔
3. 옵션 B는 binary stdout 문제 — 절대 안 됨

### Step 1: stdout/stderr를 동시에 drain하는 helper
```java
private static byte[] drainStream(InputStream in) throws IOException {
    try (in; var baos = new ByteArrayOutputStream()) {
        in.transferTo(baos);
        return baos.toByteArray();
    }
}
```

### Step 2: generateFromPath 재작성
```java
@Override
public byte[] generateFromPath(String sourcePath, int width, int height) {
    log.debug("Generating video thumbnail: path={}, size={}x{}", sourcePath, width, height);

    ProcessBuilder processBuilder = new ProcessBuilder(
        ffmpegPath,
        "-i", sourcePath,
        "-ss", SEEK_POSITION,
        "-vframes", "1",
        "-vf", buildScaleFilter(width, height),
        "-q:v", "2",
        "-f", "image2pipe",
        "-vcodec", "mjpeg",
        "-"
    );
    processBuilder.redirectErrorStream(false);

    Process process = null;
    try {
        process = processBuilder.start();

        // stdout / stderr를 별도 가상 스레드로 동시 drain (deadlock 방지)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> stdoutFuture = executor.submit(() -> drainStream(process.getInputStream()));
            Future<byte[]> stderrFuture = executor.submit(() -> drainStream(process.getErrorStream()));

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ThumbnailGenerationException(
                    "FFmpeg process timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds"
                );
            }

            byte[] thumbnailData = stdoutFuture.get(5, TimeUnit.SECONDS);
            byte[] errorBytes = stderrFuture.get(5, TimeUnit.SECONDS);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMessage = truncate(new String(errorBytes), 500);
                log.error("FFmpeg failed with exit code {}: {}", exitCode, errorMessage);
                throw new ThumbnailGenerationException(
                    "FFmpeg failed with exit code " + exitCode + ": " + errorMessage
                );
            }

            if (thumbnailData.length == 0) {
                throw new ThumbnailGenerationException("FFmpeg produced empty output");
            }

            log.debug("Video thumbnail generated: {} bytes", thumbnailData.length);
            return thumbnailData;
        }
    } catch (IOException e) {
        throw new ThumbnailGenerationException("Failed to execute FFmpeg: " + e.getMessage(), e);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (process != null) process.destroyForcibly();
        throw new ThumbnailGenerationException("FFmpeg process interrupted", e);
    } catch (ExecutionException | TimeoutException e) {
        if (process != null) process.destroyForcibly();
        throw new ThumbnailGenerationException("FFmpeg stream drain failed", e);
    }
}

private static String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() > max ? s.substring(0, max) + "..." : s;
}
```

### Step 3: `readErrorStream` 메서드 삭제
이전에 stderr를 별도 호출하던 헬퍼는 이제 drain 흐름에 통합됨.

### Step 4: import 추가
```java
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
```

### Step 5: isFFmpegAvailable() 동일 패턴 적용 (선택)
짧은 명령(`ffmpeg -version`)이라 stderr 작아서 안전하지만 일관성을 위해 동일 패턴 권장:
```java
public boolean isFFmpegAvailable() {
    try {
        ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
        pb.redirectErrorStream(true);  // -version은 작은 출력이라 merge OK
        Process process = pb.start();
        process.getInputStream().transferTo(OutputStream.nullOutputStream());
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) { process.destroyForcibly(); return false; }
        return process.exitValue() == 0;
    } catch (IOException | InterruptedException e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        log.warn("FFmpeg not available at path '{}': {}", ffmpegPath, e.getMessage());
        return false;
    }
}
```

## Before / After

### Before — generateFromPath 핵심 부분
```java
processBuilder.redirectErrorStream(false);

try {
    Process process = processBuilder.start();

    byte[] thumbnailData;
    try (InputStream stdout = process.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = stdout.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        thumbnailData = baos.toByteArray();
    }

    boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    if (!finished) {
        process.destroyForcibly();
        throw new ThumbnailGenerationException(...);
    }

    int exitCode = process.exitValue();
    if (exitCode != 0) {
        String errorMessage = readErrorStream(process);  // 이미 종료된 후 stderr 읽기
        ...
    }
    ...
}
```

### After — generateFromPath 핵심 부분
```java
processBuilder.redirectErrorStream(false);

Process process = null;
try {
    process = processBuilder.start();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        // stdout / stderr 동시 drain — pipe deadlock 방지
        Future<byte[]> stdoutFuture = executor.submit(
            () -> drainStream(process.getInputStream()));
        Future<byte[]> stderrFuture = executor.submit(
            () -> drainStream(process.getErrorStream()));

        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new ThumbnailGenerationException(
                "FFmpeg process timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
        }

        byte[] thumbnailData = stdoutFuture.get(5, TimeUnit.SECONDS);
        byte[] errorBytes = stderrFuture.get(5, TimeUnit.SECONDS);

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorMessage = truncate(new String(errorBytes), 500);
            log.error("FFmpeg failed with exit code {}: {}", exitCode, errorMessage);
            throw new ThumbnailGenerationException(
                "FFmpeg failed with exit code " + exitCode + ": " + errorMessage);
        }

        if (thumbnailData.length == 0) {
            throw new ThumbnailGenerationException("FFmpeg produced empty output");
        }

        log.debug("Video thumbnail generated: {} bytes", thumbnailData.length);
        return thumbnailData;
    }
}
catch (IOException e) {
    throw new ThumbnailGenerationException("Failed to execute FFmpeg: " + e.getMessage(), e);
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    if (process != null) process.destroyForcibly();
    throw new ThumbnailGenerationException("FFmpeg process interrupted", e);
}
catch (ExecutionException | TimeoutException e) {
    if (process != null) process.destroyForcibly();
    throw new ThumbnailGenerationException("FFmpeg stream drain failed", e);
}
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 큰 비디오로 수동 테스트
```bash
# 큰 mp4 (10분+) 업로드 시 30초 안에 썸네일 생성되는지 확인
curl -F file=@long_video.mp4 http://localhost:8080/api/streamix/files
# response에 thumbnailGenerated: true 확인
```

### 3. ffmpeg 없는 환경에서 isFFmpegAvailable() 동작 확인
```bash
./gradlew :streamix-spring-boot-starter:test
# 기존 테스트 회귀 없음
```

### 4. (선택) 스트레스 테스트
```bash
# 동시 10개 업로드
for i in {1..10}; do
    curl -F file=@video.mp4 http://localhost:8080/api/streamix/files &
done
wait
# 모두 성공 + thread dump에서 ffmpeg 관련 deadlock 없음 확인
```

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/thumbnail/FFmpegThumbnailAdapter.java`

## 참고
- [Java Process Pitfalls — pipe deadlock](https://www.javaworld.com/article/2071275)
- Java 21 Virtual Threads: `Executors.newVirtualThreadPerTaskExecutor()`
- `ProcessBuilder.redirectError(Redirect.INHERIT)` 또는 `Redirect.DISCARD`로 stderr 폐기 가능 (단, 에러 진단 어려움)
