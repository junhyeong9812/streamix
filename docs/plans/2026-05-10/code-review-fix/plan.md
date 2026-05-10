# 계획서: 2026-05-10 전체 코드 리뷰 24개 이슈 일괄 수정

## 작업 메타
- **작업명**: code-review-fix
- **시작일**: 2026-05-10
- **유형**: 버그 수정 (P0, P1) + 리팩토링 (P2) 혼합
- **규모**: 대 (24개 이슈, 약 30개 파일 변경, 3개 신규 파일)
- **이슈 인덱스**: `docs/refactor-log/2026-05-10-code-review/README.md`
- **이슈별 상세 문서**: 같은 폴더의 24개 별개 문서

## 목표
v2.0.0 시점 전체 코드 리뷰에서 발견된 24개 이슈를 일괄 수정하여 v2.0.1 (또는 v2.1.0)으로 릴리스 가능한 상태로 만든다.

| 등급 | 개수 | 의미 |
|------|------|------|
| P0 | 5 | 빌드/CI/배포가 깨지거나 라이브러리가 동작하지 않음 — 즉시 |
| P1 | 7 | 보안 취약점, deadlock 등 안정성 위협 |
| P2 | 12 | 설계/품질/일관성 |

## 변경 대상 파일 (전체)

### 생성 (5개)
| 파일 | 이유 | 관련 이슈 |
|------|------|----------|
| `streamix-core/src/main/java/.../core/domain/util/ByteSizeFormatter.java` | formatSize 6곳 통합 | P2-13 |
| `streamix-core/src/main/java/.../core/domain/exception/RangeNotSatisfiableException.java` | RFC 7233 416 응답 | P1-11 |
| `streamix-core/src/test/java/.../core/domain/util/ByteSizeFormatterTest.java` | 신규 유틸 테스트 | P2-13 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/dashboard/StreamixDashboardController.java` | 디렉토리 이동 (git mv) | P0-01 |
| `LICENSE` | 본문 작성 (기존 빈 파일) | P0-04 |

### 삭제 (2개)
| 파일 | 이유 | 관련 이슈 |
|------|------|----------|
| `streamix-spring-boot-starter/src/main/resources/application.yml` | 사용자 설정 오염 | P1-08 |
| `streamix-spring-boot-starter/src/main/java/.../starter/autoconfigure/StreamixJpaConfiguration.java` | 데드 코드 | P0-03, P2-14 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/StreamixDashboardController.java` | dashboard/ 로 이동 | P0-01 |

### 수정 (Java — 19개)

#### Core 도메인
| 파일 | 변경 | 이슈 |
|------|------|------|
| `core/domain/model/FileType.java` | `isPreviewable()` 삭제, 미사용 import | P1-10 |
| `core/domain/model/FileMetadata.java` | `isPreviewable()` 정확 구현 + `getFormattedSize()` 위임 | P1-10, P2-13 |
| `core/domain/exception/StreamixException.java` | sealed permit에 RangeNotSatisfiableException 추가 | P1-11 |
| `core/domain/exception/FileSizeExceededException.java` | formatSize 위임 | P2-13 |
| `core/domain/service/FileTypeDetector.java` | static utility로 변환 + safeContentType + isValidContentType | P2-16, P1-12 |

#### Core 어플리케이션
| 파일 | 변경 | 이슈 |
|------|------|------|
| `core/application/port/in/DeleteFileUseCase.java` | `deleteIdempotent` 추가 | P2-23 |
| `core/application/service/FileUploadService.java` | formatSize 위임 + Exception 좁히기 + safeContentType + FileTypeDetector static 호출 | P2-13, P2-22, P1-12, P2-16 |
| `core/application/service/FileStreamService.java` | parseRange RFC 7233 준수 | P1-11 |
| `core/application/service/FileDeleteService.java` | 멱등 메서드 + Exception 좁히기 | P2-23, P2-22 |

#### Core 어댑터
| 파일 | 변경 | 이슈 |
|------|------|------|
| `core/adapter/out/storage/LocalFileStorageAdapter.java` | 모든 메서드에 path 검증 + 주석 코드 정리 | P1-06 |

#### Starter — autoconfigure
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/autoconfigure/StreamixAutoConfiguration.java` | `@AutoConfiguration` → `@Configuration` + AutoConfigureAfter + ByteSizeFormatter 위임 | P0-03, P2-13, P2-18 |
| `starter/autoconfigure/StreamixDashboardConfiguration.java` | 같은 변경 | P0-03, P2-18 |
| `starter/autoconfigure/StreamixMonitoringConfiguration.java` | 같은 변경 | P0-03, P2-18 |
| `starter/autoconfigure/StreamixRepositoryConfiguration.java` | 같은 변경 + StreamingMonitoringService Bean 제거 + alias 메서드 명/javadoc 개선 | P0-03, P2-14, P2-15, P2-18 |
| `starter/autoconfigure/StreamixThumbnailConfiguration.java` | 같은 변경 | P0-03 |
| `starter/autoconfigure/StreamixWebConfiguration.java` | 같은 변경 + AutoConfigureAfter | P0-03, P2-18 |

#### Starter — properties
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/properties/StreamixProperties.java` | `Storage.getResolvedBasePath()` Path API | P2-17 |

#### Starter — adapter (web/dashboard)
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/adapter/in/dashboard/StreamixDashboardController.java` | currentPage 모델 + deleteIdempotent 사용 | P2-19, P2-23 |
| `starter/adapter/in/web/StreamixApiController.java` | safeMediaType | P1-12 |
| `starter/adapter/in/web/GlobalExceptionHandler.java` | RangeNotSatisfiableException 핸들러 + ByteSizeFormatter 위임 | P1-11, P2-13 |

#### Starter — adapter (persistence/thumbnail)
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/adapter/out/persistence/StreamingSessionRepository.java` | `@Modifying` 추가 | P0-02 |
| `starter/adapter/out/persistence/StreamingSessionEntity.java` | `getBytesSentFormatted` 추가 | P2-20 |
| `starter/adapter/out/thumbnail/FFmpegThumbnailAdapter.java` | stdout/stderr 동시 drain | P1-09 |

#### Starter — service
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/service/StreamingMonitoringService.java` | DashboardStats/FileStreamingStats — ByteSizeFormatter 위임 | P2-13 |

#### Starter — annotation
| 파일 | 변경 | 이슈 |
|------|------|------|
| `starter/annotation/EnableStreamix.java` | javadoc 갱신 (autoconfiguration imports 언급 제거) | P0-03 |

### 수정 (리소스 — 5개)
| 파일 | 변경 | 이슈 |
|------|------|------|
| `static/streamix/js/dashboard.js` | XSS 방지 (textContent) + maxSize 동적 처리 + formatFileSize 통일 | P1-07, P2-21 |
| `templates/streamix/layout.html` | data-max-file-size 추가 | P2-21 |
| `templates/streamix/dashboard.html` | file.formattedSize 사용 | P2-20 |
| `templates/streamix/files.html` | file.formattedSize + maxFileSize 안내 | P2-20, P2-21 |
| `templates/streamix/file-detail.html` | file.formattedSize | P2-20 |
| `templates/streamix/sessions.html` | session.bytesSentFormatted | P2-20 |

### 수정 (build/CI — 4개)
| 파일 | 변경 | 이슈 |
|------|------|------|
| `build.gradle` | version 동적화 (-PreleaseVersion) | P0-05 |
| `streamix-spring-boot-starter/build.gradle` | WebJars 의존성 추가 | P2-24 |
| `.github/workflows/ci.yml` | branches: master + 중복 단계 제거 | P0-05 |
| `.github/workflows/publish.yml` | tag 트리거 + version 추출 + SIGNING_KEY_ID 제거 | P0-05 |

### 테스트 수정/추가 (8개)
| 파일 | 변경 | 이슈 |
|------|------|------|
| `core/test/.../core/domain/model/FileMetadataTest.java` | GB 정밀도 갱신 + isPreviewable 신규 테스트 | P2-13, P1-10 |
| `core/test/.../core/domain/service/FileTypeDetectorTest.java` | static 호출 + safeContentType 테스트 | P2-16, P1-12 |
| `core/test/.../core/domain/exception/StreamixExceptionTest.java` | RangeNotSatisfiableException 추가 | P1-11 |
| `core/test/.../core/application/service/FileUploadServiceTest.java` | RuntimeException → ThumbnailGenerationException | P2-22 |
| `core/test/.../core/application/service/FileDeleteServiceTest.java` | RuntimeException → StorageException + deleteIdempotent 추가 | P2-22, P2-23 |
| `core/test/.../core/application/service/FileStreamServiceTest.java` | invalid range / 416 / multi-range 테스트 | P1-11 |
| `core/test/.../core/adapter/out/storage/LocalFileStorageAdapterTest.java` | path traversal load/delete/getSize 테스트 + 기존 messages 갱신 | P1-06 |
| `streamix-spring-boot-starter/src/test/.../starter/properties/StoragePropertiesTest.java` (신규) | absolute/relative path 테스트 | P2-17 |

## 절대 변경하지 않는 영역 (금지 영역)
| 영역 | 이유 |
|------|------|
| `streamix-core/src/main/java/.../core/domain/model/UploadResult.java` | 변경 사유 없음 |
| `streamix-core/src/main/java/.../core/domain/model/StreamableFile.java` | 변경 사유 없음 |
| `streamix-core/src/main/java/.../core/application/port/out/*.java` (FileMetadataPort, FileStoragePort, ThumbnailGeneratorPort) | 외부 SPI — breaking change 회피 |
| `streamix-core/src/main/java/.../core/adapter/out/metadata/InMemoryMetadataAdapter.java` | 변경 사유 없음 |
| `streamix-core/src/main/java/.../core/adapter/out/thumbnail/ImageThumbnailAdapter.java` | 변경 사유 없음 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/in/web/dto/*.java` | 변경 사유 없음 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/FileMetadataEntity.java` | 변경 사유 없음 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/FileMetadataJpaRepository.java` | 변경 사유 없음 |
| `streamix-spring-boot-starter/src/main/java/.../starter/adapter/out/persistence/JpaFileMetadataAdapter.java` | 변경 사유 없음 |
| `streamix-spring-boot-starter/src/main/resources/static/streamix/css/dashboard.css` | 디자인 영역 — 별도 작업 |
| `README.md` | 별도 PR로 (changelog v2.0.1) |
| `docs/concepts/`, `docs/implement/`, `docs/refactor-log/README.md` | 학습 자료 — 본 작업과 무관 |

## 구현 순서 (의존성 + 안전성 고려)

### Phase A — 신규 유틸리티 (의존성 기반)
1. ✅ `ByteSizeFormatter.java` 신규 (P2-13 — 다른 5곳이 의존)
2. ✅ `RangeNotSatisfiableException.java` 신규 (P1-11)
3. ✅ `StreamixException.java` sealed permit 갱신 (P1-11)

### Phase B — P0 (빌드 깨짐 위험)
4. ✅ `LICENSE` 본문 작성 (P0-04)
5. ✅ `StreamixDashboardController.java` git mv (P0-01)
6. ✅ `StreamingSessionRepository.deleteByStartedAtBefore`에 `@Modifying` (P0-02)
7. ✅ 6개 `*Configuration.java` `@AutoConfiguration` → `@Configuration` (P0-03)
8. ✅ `StreamixJpaConfiguration.java` 삭제 (P0-03, P2-14)
9. ✅ `EnableStreamix.java` javadoc 갱신 (P0-03)
10. ✅ `.github/workflows/ci.yml` (P0-05)
11. ✅ `.github/workflows/publish.yml` (P0-05)
12. ✅ `build.gradle` version 동적화 (P0-05)

→ **빌드 검증**: `./gradlew compileJava` + `./gradlew test`

### Phase C — P1 (보안/안정성)
13. ✅ `FileType.isPreviewable()` 삭제 + import 정리 (P1-10)
14. ✅ `FileMetadata.isPreviewable()` 정확 구현 (P1-10)
15. ✅ `FileTypeDetector` — `safeContentType`, `isValidContentType` 추가 (P1-12)
16. ✅ `LocalFileStorageAdapter` — 모든 메서드 path 검증 (P1-06)
17. ✅ `FileStreamService.parseRange` 재작성 (P1-11)
18. ✅ `GlobalExceptionHandler` — RangeNotSatisfiableException 핸들러 (P1-11)
19. ✅ `StreamixApiController` — safeMediaType (P1-12)
20. ✅ `FFmpegThumbnailAdapter` — stdout/stderr 동시 drain (P1-09)
21. ✅ `dashboard.js` — showToast XSS 방지 (P1-07)
22. ✅ `application.yml` 삭제 (P1-08)
23. ✅ `FileUploadService` — safeContentType 적용 + Exception 좁히기 (P1-12, P2-22)

→ **테스트 검증**: 신규 테스트 + 기존 테스트 실행

### Phase D — P2 (품질)
24. ✅ `ByteSizeFormatter` 6곳 위임 변경 (P2-13)
25. ✅ `FileTypeDetector` static utility 변환 (P2-16)
26. ✅ `FileUploadService` — FileTypeDetector static 호출 변경 (P2-16)
27. ✅ `Storage.getResolvedBasePath` Path API (P2-17)
28. ✅ 6개 Configuration `@AutoConfigureAfter` 추가 (P2-18)
29. ✅ `StreamixRepositoryConfiguration` — StreamingMonitoringService Bean 제거 + alias 메서드 명 개선 (P2-14, P2-15)
30. ✅ `StreamixDashboardController` — currentPage 모델 (P2-19)
31. ✅ 4개 템플릿 — file.formattedSize / session.bytesSentFormatted (P2-20)
32. ✅ `StreamingSessionEntity.getBytesSentFormatted()` 추가 (P2-20)
33. ✅ `dashboard.js` + layout.html + files.html — maxSize 동적 (P2-21)
34. ✅ `FileDeleteService` — deleteIdempotent (P2-23)
35. ✅ `DeleteFileUseCase` — deleteIdempotent 시그니처 (P2-23)
36. ✅ `StreamixDashboardController` — deleteIdempotent 사용 (P2-23)
37. ✅ `streamix-spring-boot-starter/build.gradle` — WebJars 추가 (P2-24)
38. ✅ `layout.html` — webjars 경로 (P2-24)

→ **최종 검증**: `./gradlew build` 전체

## 트레이드오프 / 리스크

### 리스크 1: Breaking Changes
- `FileType.isPreviewable()` 삭제 (P1-10)
- `FileTypeDetector` 인스턴스 → static (P2-16)
- `LocalFileStorageAdapter`의 절대경로 외부 거부 (P1-06)

→ CHANGELOG에 명시. v2.0.1 → v2.1.0 권장 (semver minor bump)

### 리스크 2: 전체 빌드 시간
- 38단계 변경 → 컴파일/테스트 반복 다수
- Phase 단위로 검증 → 실패 시 빠른 isolation

### 리스크 3: 테스트 실패 cascade
- `formatSize` 정밀도 변경(2자리→1자리) → FileMetadataTest 갱신
- Exception narrowing → FileUploadServiceTest, FileDeleteServiceTest 갱신
- path traversal 강제 → LocalFileStorageAdapterTest 갱신

→ 각 Phase 후 테스트 실행으로 cascade 확인

### 리스크 4: WebJars 신규 의존성
- 사용자 빌드에서 의존성 충돌 가능성 (사용자가 다른 버전 Bootstrap WebJars 사용 시)
- → Spring Boot의 BOM/dependencyManagement로 자동 해결 기대

## 외부 큐레이션 (B1.5)
**생략 사유**: 본 작업은 기존 코드 리뷰 결과를 기반으로 한 수정 작업이며, 새로운 라이브러리/패턴 도입이 아니다. Spring Boot 4.0/Java 25 영역의 신기능 사용은 P1-09(가상 스레드) 한 곳뿐이며 이는 표준 Java API. 외부 큐레이션 없이 진행한다.

## 승인 게이트
이 plan을 사용자가 검토 후 승인하면 Phase A부터 순차 구현 시작.

## 변경 이력
| 날짜 | 변경 |
|------|------|
| 2026-05-10 | 초기 작성 (24개 이슈 통합 plan) |
