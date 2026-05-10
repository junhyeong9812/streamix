# [P0-04] LICENSE 파일이 빈 파일

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P0 — Maven Central publish 거부 위험** |
| 카테고리 | 라이센스 / 배포 |
| 발견 위치 | 프로젝트 루트 |
| 영향 파일 | `LICENSE` |

## 문제 분석

### 현재 동작
```bash
$ ls -la LICENSE
-rw-rw-r-- 1 jun jun 0  5월 10 18:52 LICENSE
```
- 파일 크기 0바이트
- README는 "MIT License - LICENSE 파일 참조"라고 명시 (line 462)
- `streamix-core/build.gradle`, `streamix-spring-boot-starter/build.gradle`의 POM 메타데이터에도 MIT License 명시

### 기대 동작
- LICENSE 파일에 실제 MIT License 본문 포함
- GitHub의 라이센스 자동 감지가 정상 작동
- Maven Central publish 정책 준수

### 원인 분석
- `git init` 또는 GitHub 템플릿에서 빈 파일이 생성된 후 본문 작성을 빠뜨림
- 빌드/CI 모두 LICENSE 내용 검증 안 함

### 영향 범위
1. **법적 모호성**: 라이센스가 명시되지 않은 코드는 사용자가 법적으로 사용할 권리 없음 (US 저작권법 기본). README 명시는 보조 증거일 뿐
2. **Maven Central 배포**: Sonatype OSSRH 정책상 LICENSE 파일이 비어있으면 publish 거부될 수 있음 (POM의 license 정보가 1차 검증이지만 일부 정책에서 LICENSE 파일 본문 확인)
3. **GitHub UX**: 저장소 페이지에 라이센스 표시가 깨짐
4. **사용자 신뢰도**: 오픈소스 프로젝트로서 첫인상 손상

## 변경 프로세스

### Step 1: MIT License 표준 본문 작성
연도 + 저작권자 명시. SPDX 표준 형식 사용:
- 연도: 2025-2026 (1.0.0 릴리스 2025-12-14 ~ 현재)
- 저작권자: Junhyeong (junhyeong9812)

### Step 2: 파일 작성
표준 MIT License 본문(opensource.org/licenses/MIT)을 그대로 사용. 불필요한 변형 금지.

### Step 3: build.gradle의 license url 검증
```groovy
licenses {
    license {
        name = 'MIT License'
        url = 'https://opensource.org/licenses/MIT'
    }
}
```
→ 변경 불필요 (이미 정확)

### Step 4: README 검증
README line 462: `MIT License - [LICENSE](LICENSE) 파일 참조`
→ LICENSE 파일이 채워지면 자동으로 동작. 변경 불필요

## Before / After

### Before
```
$ wc -c LICENSE
0 LICENSE
```

### After
```
MIT License

Copyright (c) 2025-2026 Junhyeong (junhyeong9812)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 검증 방법

### 1. 파일 크기 확인
```bash
wc -c LICENSE
# 기대: 1000+ bytes
```

### 2. SPDX 검증 (선택)
```bash
# spdx-license-checker가 있으면
spdx-license-checker LICENSE
```

### 3. GitHub 라이센스 자동 감지
파일 push 후 GitHub 저장소 페이지 우측에 "MIT License" 표시 확인.

### 4. Maven Central publish 시뮬레이션
```bash
./gradlew :streamix-core:publishToMavenLocal
ls ~/.m2/repository/io/github/junhyeong9812/streamix-core/2.0.0/
# .pom 파일에 license 정보 포함 확인
```

## 관련 파일
- `LICENSE` (빈 파일 → 본문 채움)

## 참고
- [opensource.org/licenses/MIT](https://opensource.org/licenses/MIT) — 표준 본문
- [SPDX MIT License](https://spdx.org/licenses/MIT.html)
- Sonatype Central Publishing Requirements: license 정보가 POM에 있어야 하며 LICENSE 파일 본문은 모범 사례
- GitHub Licensing detection: github.com/licensee/licensee
