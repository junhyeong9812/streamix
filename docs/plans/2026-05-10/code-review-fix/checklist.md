# 체크리스트: code-review-fix

## Phase A — 신규 유틸리티
- [ ] A1. `ByteSizeFormatter.java` 작성
- [ ] A2. `RangeNotSatisfiableException.java` 작성
- [ ] A3. `StreamixException.java` sealed permit 갱신
- [ ] A4. `ByteSizeFormatterTest.java` 작성
- [ ] **검증**: `./gradlew :streamix-core:compileJava` + `./gradlew :streamix-core:test --tests ByteSizeFormatterTest`

## Phase B — P0 (빌드 깨짐 위험)
- [ ] B1. `LICENSE` 본문 작성 (P0-04)
- [ ] B2. `StreamixDashboardController.java` 디렉토리 이동 (P0-01)
  - [ ] dashboard/ 디렉토리 생성
  - [ ] git mv 또는 mv + git add
- [ ] B3. `StreamingSessionRepository.deleteByStartedAtBefore`에 `@Modifying(clearAutomatically = true)` (P0-02)
- [ ] B4. 6개 `*Configuration` `@AutoConfiguration` → `@Configuration(proxyBeanMethods = false)` (P0-03)
  - [ ] StreamixAutoConfiguration
  - [ ] StreamixDashboardConfiguration
  - [ ] StreamixMonitoringConfiguration
  - [ ] StreamixRepositoryConfiguration (이미 @Configuration)
  - [ ] StreamixThumbnailConfiguration
  - [ ] StreamixWebConfiguration
- [ ] B5. `StreamixJpaConfiguration.java` 삭제 (P0-03, P2-14)
- [ ] B6. `EnableStreamix.java` javadoc 갱신 (P0-03)
- [ ] B7. `.github/workflows/ci.yml` 수정 (P0-05)
- [ ] B8. `.github/workflows/publish.yml` 수정 (P0-05)
- [ ] B9. `build.gradle` version 동적화 (P0-05)
- [ ] **검증**: `./gradlew compileJava`
- [ ] **검증**: `./gradlew test`

## Phase C — P1 (보안/안정성)
- [ ] C1. `FileType.isPreviewable()` 삭제 + Arrays import 제거 (P1-10)
- [ ] C2. `FileMetadata.isPreviewable()` 정확 구현 (P1-10)
- [ ] C3. `FileTypeDetector` — `isValidContentType`, `safeContentType` 추가 (P1-12)
- [ ] C4. `LocalFileStorageAdapter` — 모든 메서드 path 검증 (P1-06)
  - [ ] resolveAndValidatePath 확장 (절대경로 처리)
  - [ ] load, loadPartial, delete, exists, getSize 모두 호출
  - [ ] 주석 코드 제거
- [ ] C5. `FileStreamService.parseRange` 재작성 (P1-11)
  - [ ] RangeNotSatisfiableException 사용
  - [ ] NumberFormatException → IllegalArgumentException 변환
  - [ ] multi-range fallback
  - [ ] 빈 range 헤더 처리
- [ ] C6. `GlobalExceptionHandler.handleRangeNotSatisfiable` 추가 (P1-11)
- [ ] C7. `StreamixApiController.safeMediaType` 추가 (P1-12)
- [ ] C8. `FFmpegThumbnailAdapter.generateFromPath` — stdout/stderr 동시 drain (P1-09)
  - [ ] drainStream helper
  - [ ] truncate helper
  - [ ] readErrorStream 메서드 삭제
  - [ ] isFFmpegAvailable 정리 (선택)
- [ ] C9. `dashboard.js showToast` XSS 방지 (P1-07)
- [ ] C10. `application.yml` 삭제 (P1-08)
- [ ] C11. `FileUploadService` — safeContentType + Exception 좁히기 (P1-12, P2-22)
- [ ] C12. `FileDeleteService.deleteFileQuietly` — Exception 좁히기 (P2-22)
- [ ] **검증**: 신규 테스트 추가 후 실행
  - [ ] FileStreamServiceTest — invalid/416/multi-range 추가
  - [ ] FileMetadataTest — isPreviewable 신규 테스트
  - [ ] StreamixExceptionTest — RangeNotSatisfiableException 추가
  - [ ] LocalFileStorageAdapterTest — path traversal load/delete/getSize
  - [ ] FileTypeDetectorTest — safeContentType 추가
  - [ ] FileUploadServiceTest — Exception 변경 (RuntimeException → ThumbnailGenerationException)
  - [ ] FileDeleteServiceTest — Exception 변경 (RuntimeException → StorageException)
- [ ] **검증**: `./gradlew test`

## Phase D — P2 (품질)
- [ ] D1. `ByteSizeFormatter` 6곳 위임 (P2-13)
  - [ ] FileMetadata.getFormattedSize
  - [ ] FileSizeExceededException.formatSize 삭제 + 메시지 변경
  - [ ] FileUploadService.formatSize 삭제 + 호출 변경
  - [ ] StreamixAutoConfiguration.formatSize 삭제 + 호출 변경
  - [ ] GlobalExceptionHandler.formatSize 삭제 + 호출 변경
  - [ ] StreamingMonitoringService DashboardStats/FileStreamingStats — formatBytes 삭제
- [ ] D2. `FileTypeDetector` static utility 변환 (P2-16)
  - [ ] final class + private constructor (AssertionError)
  - [ ] 모든 메서드 static
  - [ ] detectAllowingAll 삭제
- [ ] D3. `FileUploadService` — FileTypeDetector 인스턴스 → static 호출 (P2-16)
- [ ] D4. `StreamixProperties.Storage.getResolvedBasePath` Path API (P2-17)
- [ ] D5. 6개 Configuration `@AutoConfigureAfter` 추가 (P2-18)
  - [ ] StreamixRepositoryConfiguration: DataSourceAutoConfiguration, HibernateJpaAutoConfiguration
  - [ ] StreamixAutoConfiguration: StreamixRepositoryConfiguration, StreamixThumbnailConfiguration
  - [ ] StreamixMonitoringConfiguration: StreamixRepositoryConfiguration
  - [ ] StreamixDashboardConfiguration: StreamixMonitoringConfiguration
  - [ ] StreamixWebConfiguration: StreamixAutoConfiguration
- [ ] D6. `StreamixRepositoryConfiguration` (P2-14, P2-15)
  - [ ] streamingMonitoringService Bean 제거
  - [ ] streamixPropertiesAlias → streamixPropertiesNamedBean (이름 + javadoc)
- [ ] D7. `StreamixDashboardController.currentPage` 모델 (P2-19)
  - [ ] 상수 정의 (ATTR_CURRENT_PAGE, PAGE_DASHBOARD, PAGE_FILES, PAGE_SESSIONS)
  - [ ] 4개 핸들러에 model.addAttribute 추가
- [ ] D8. 4개 템플릿 — file.formattedSize 사용 (P2-20)
  - [ ] dashboard.html
  - [ ] files.html
  - [ ] file-detail.html
  - [ ] sessions.html (session.bytesSentFormatted)
- [ ] D9. `StreamingSessionEntity.getBytesSentFormatted()` 추가 (P2-20)
- [ ] D10. `dashboard.js` + layout.html + files.html — maxSize 동적 (P2-21)
  - [ ] layout.html에 data-max-file-size 추가
  - [ ] dashboard.js handleFileUpload — body dataset 읽기
  - [ ] files.html 안내문 동적 표시
  - [ ] formatFileSize JS 함수 정밀도 통일
- [ ] D11. `DeleteFileUseCase.deleteIdempotent` 시그니처 추가 (P2-23)
- [ ] D12. `FileDeleteService.deleteIdempotent` 구현 (P2-23)
- [ ] D13. `StreamixDashboardController.deleteFile` — deleteIdempotent 사용 (P2-23)
- [ ] D14. `streamix-spring-boot-starter/build.gradle` — WebJars 추가 (P2-24)
- [ ] D15. `layout.html` — webjars 경로 (P2-24)

## 최종 검증
- [ ] **전체 빌드**: `./gradlew clean build`
- [ ] **모든 테스트**: `./gradlew test`
- [ ] **JAR 검증**: `unzip -l streamix-spring-boot-starter/build/libs/*.jar | grep -E "application.yml|webjars|StreamixJpaConfig"` (application.yml 미포함, webjars 포함, JpaConfig 미포함)
- [ ] **사용자 시나리오**: 단순 Spring Boot 앱에서 `@EnableStreamix` 후 정상 동작
- [ ] **CHANGELOG/learned.md 작성**

## Breaking Changes 기록
- [ ] `FileType.isPreviewable()` 삭제
- [ ] `FileTypeDetector` 인스턴스 → static
- [ ] `LocalFileStorageAdapter` — 절대경로 외부 거부 강화
- [ ] `application.yml` 사용자 설정 강제 제거 (사용자가 의존했다면 자체 설정 필요)
- [ ] `StreamixJpaConfiguration` 삭제 (사용자가 import했다면 깨짐 — 가능성 매우 낮음)
- [ ] `formatSize` GB 정밀도 2 → 1 (사용자에게 보이는 출력 변화)
- [ ] CI workflow main → master (사용자 fork 영향)

## 진행 상황 기록
| Phase | 시작 | 완료 | 메모 |
|-------|------|------|------|
| A | (대기) | | |
| B | (대기) | | |
| C | (대기) | | |
| D | (대기) | | |
| 검증 | (대기) | | |

## 수정 기록 (Phase별)
*(작업 진행하면서 추가)*

| Phase | 파일 | 변경 요약 |
|-------|------|----------|
| | | |
