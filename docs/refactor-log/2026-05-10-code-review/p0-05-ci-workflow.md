# [P0-05] CI/Publish 워크플로 브랜치 불일치 + 버전 하드코딩

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P0 — CI 미동작 + publish 실패** |
| 카테고리 | CI/CD / GitHub Actions |
| 발견 위치 | `.github/workflows/` |
| 영향 파일 | `.github/workflows/ci.yml`, `.github/workflows/publish.yml`, `build.gradle` |

## 문제 분석

### 현재 동작

#### 문제 1: ci.yml의 트리거 브랜치
```yaml
# .github/workflows/ci.yml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
```

#### 문제 2: publish.yml의 트리거 브랜치
```yaml
# .github/workflows/publish.yml
on:
  push:
    branches:
      - main
    paths:
      - 'streamix-core/**'
      ...
```

#### 실제 git 상태
```bash
$ git branch -a
* master
  remotes/origin/HEAD -> origin/master
  remotes/origin/master
```

→ **CI/Publish 모두 트리거되지 않음**. 이전 commit 8개(`5af10df` ~ `c8f6306`) 어느 것도 CI 검증을 거치지 않았을 가능성.

#### 문제 3: 버전 하드코딩 + 자동 publish
```groovy
// build.gradle
version = '2.0.0'
```

publish.yml은 main(=master) push 시 자동 실행 → 같은 버전 publish 시도 → Sonatype은 같은 좌표 재배포 거부.

#### 문제 4: 필요 없는 별도 test 단계
```yaml
- name: Build
  run: ./gradlew build
- name: Test
  run: ./gradlew test
```
`./gradlew build`는 이미 `test` task 포함 → 중복 실행으로 시간 낭비.

#### 문제 5: SIGNING_KEY_ID 미사용
```yaml
env:
  SIGNING_KEY_ID: ${{ secrets.GPG_KEY_ID }}
```
`build.gradle` 어디에서도 `SIGNING_KEY_ID`를 읽지 않음 → 의미 없는 환경변수.

### 기대 동작
1. 모든 master push와 PR이 CI 검증을 거침
2. publish는 git tag(예: `v2.0.1`) 또는 GitHub Release 생성 시에만 실행
3. 버전은 git tag 또는 환경변수로 결정 (not hardcoded)
4. 빌드와 테스트 중복 제거

### 원인 분석
- 프로젝트 초기 GitHub 기본 브랜치 명명 변화 (master → main 전환기) 영향
- `git init` 후 명시적으로 main으로 변경 안 함, workflow 작성 시점엔 이미 master였으나 템플릿 그대로 사용
- 자동 publish는 처음 v1.0.0 publish 시 한 번 동작했고, 이후 수동 publish (Sonatype 콘솔)로 운영하다가 워크플로 정리 누락 가능성

## 변경 프로세스

### Step 1: ci.yml 브랜치 정정 + 중복 제거
```yaml
# Before
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

# After
on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
```

```yaml
# Before
- name: Build
  run: ./gradlew build
- name: Test
  run: ./gradlew test

# After
- name: Build (test 포함)
  run: ./gradlew build
```

### Step 2: publish.yml 트리거 변경 + SIGNING_KEY_ID 제거
**옵션 비교**:
| 옵션 | 트리거 | 장점 | 단점 |
|------|--------|------|------|
| A. master push 유지 | 위험 | 자동화 강함 | 매번 publish 시도 → 실패 누적 |
| B. tag(`v*`) push만 | `push: tags: ['v*']` | 명시적 릴리스 | tag 잊으면 publish 안 됨 |
| C. GitHub Release만 | `release: types: [created]` | UI에서 확인 | tag도 같이 생김 |
| D. workflow_dispatch만 | 수동 | 가장 안전 | CI/CD 효과 떨어짐 |

채택: **B + C + D 조합** (가장 유연):
```yaml
on:
  push:
    tags:
      - 'v*'
  release:
    types: [ created ]
  workflow_dispatch:
```

### Step 3: 버전 동적화
```groovy
// build.gradle
allprojects {
    group = 'io.github.junhyeong9812'
    version = System.getenv('RELEASE_VERSION') ?: '2.0.0-SNAPSHOT'
}
```

publish.yml에서 tag로부터 version 추출:
```yaml
- name: Extract version from tag
  id: version
  run: echo "RELEASE_VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_ENV

- name: Publish
  env:
    RELEASE_VERSION: ${{ env.RELEASE_VERSION }}
  run: ./gradlew :streamix-core:publishToMavenCentralPortal :streamix-spring-boot-starter:publishToMavenCentralPortal
```

### Step 4: 환경변수 정리
```yaml
# Before
env:
  MAVEN_CENTRAL_TOKEN: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
  SIGNING_KEY_ID: ${{ secrets.GPG_KEY_ID }}
  SIGNING_PASSWORD: ${{ secrets.GPG_PASSPHRASE }}
  GPG_SIGNING_KEY: ${{ secrets.GPG_PRIVATE_KEY }}

# After (SIGNING_KEY_ID 제거 — 사용 안 함)
env:
  MAVEN_CENTRAL_TOKEN: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
  SIGNING_PASSWORD: ${{ secrets.GPG_PASSPHRASE }}
  GPG_SIGNING_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
```

## Before / After

### Before — `.github/workflows/ci.yml`
```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Build
        run: ./gradlew build
      - name: Test
        run: ./gradlew test
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/test-results/test/*.xml'
```

### After — `.github/workflows/ci.yml`
```yaml
name: CI

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Build (test 포함)
        run: ./gradlew build
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/test-results/test/*.xml'
```

### Before — `.github/workflows/publish.yml`
```yaml
name: Publish to Maven Central

on:
  push:
    branches:
      - main
    paths:
      - 'streamix-core/**'
      - 'streamix-spring-boot-starter/**'
      - 'build.gradle'
      - 'settings.gradle'
  release:
    types: [ created ]
  workflow_dispatch:

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      # ...
      - name: Publish to Maven Central
        run: |
          ./gradlew :streamix-core:publishToMavenCentralPortal
          ./gradlew :streamix-spring-boot-starter:publishToMavenCentralPortal
        env:
          MAVEN_CENTRAL_TOKEN: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
          SIGNING_KEY_ID: ${{ secrets.GPG_KEY_ID }}
          SIGNING_PASSWORD: ${{ secrets.GPG_PASSPHRASE }}
          GPG_SIGNING_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
```

### After — `.github/workflows/publish.yml`
```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'
  release:
    types: [ created ]
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version (e.g. 2.0.1)'
        required: true

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Import GPG key
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | gpg --batch --import
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Resolve release version
        run: |
          if [ "${{ github.event_name }}" = "push" ]; then
            echo "RELEASE_VERSION=${GITHUB_REF#refs/tags/v}" >> "$GITHUB_ENV"
          elif [ "${{ github.event_name }}" = "release" ]; then
            echo "RELEASE_VERSION=${GITHUB_REF#refs/tags/v}" >> "$GITHUB_ENV"
          else
            echo "RELEASE_VERSION=${{ github.event.inputs.version }}" >> "$GITHUB_ENV"
          fi

      - name: Build
        run: ./gradlew build -PreleaseVersion=$RELEASE_VERSION

      - name: Publish to Maven Central
        run: |
          ./gradlew :streamix-core:publishToMavenCentralPortal -PreleaseVersion=$RELEASE_VERSION
          ./gradlew :streamix-spring-boot-starter:publishToMavenCentralPortal -PreleaseVersion=$RELEASE_VERSION
        env:
          MAVEN_CENTRAL_TOKEN: ${{ secrets.MAVEN_CENTRAL_TOKEN }}
          SIGNING_PASSWORD: ${{ secrets.GPG_PASSPHRASE }}
          GPG_SIGNING_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
```

### Before — `build.gradle`
```groovy
allprojects {
    group = 'io.github.junhyeong9812'
    version = '2.0.0'
    ...
}
```

### After — `build.gradle`
```groovy
allprojects {
    group = 'io.github.junhyeong9812'
    // CI/CD에서 -PreleaseVersion=x.y.z 또는 RELEASE_VERSION env로 주입
    // 로컬 개발 시 기본값
    version = providers.gradleProperty('releaseVersion')
        .orElse(providers.environmentVariable('RELEASE_VERSION'))
        .getOrElse('2.0.1-SNAPSHOT')
    ...
}
```

## 검증 방법

### 1. workflow YAML 문법 검증 (로컬)
```bash
# yamllint 가 있으면
yamllint .github/workflows/*.yml

# act (GitHub Actions 로컬 러너)가 있으면
act -W .github/workflows/ci.yml --list
```

### 2. master 브랜치 push 시뮬레이션
실제 master에 push 후 GitHub Actions 탭에서 워크플로 실행 확인.

### 3. tag publish 시뮬레이션
```bash
git tag v2.0.1-test
git push origin v2.0.1-test
# Actions에서 publish 워크플로 실행되는지 확인
# 검증 후 tag 삭제
git push --delete origin v2.0.1-test
git tag -d v2.0.1-test
```

### 4. 로컬 빌드 검증 (버전 주입)
```bash
./gradlew :streamix-core:build -PreleaseVersion=2.0.1
ls streamix-core/build/libs/
# streamix-core-2.0.1.jar 생성 확인
```

## 관련 파일
- `.github/workflows/ci.yml`
- `.github/workflows/publish.yml`
- `build.gradle`

## 참고
- GitHub Actions Trigger Events: `push`, `pull_request`, `release`, `workflow_dispatch`
- Gradle Provider API for property resolution: `providers.gradleProperty()`
- Sonatype Central Publishing — same coordinate(group:artifact:version) 재배포 정책: 기본적으로 거부, snapshot 저장소만 허용
