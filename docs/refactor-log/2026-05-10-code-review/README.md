# 2026-05-10 전체 코드 리뷰 — 이슈 인덱스

> v2.0.0 시점 전체 코드 리뷰(70+ 파일, 약 13,000 줄)에서 발견된 이슈와 개선 작업 인덱스입니다.
> 각 이슈는 **별개의 문서**로 분리되어 있으며, 분석 → 변경 프로세스 → before/after → 검증 방법 순으로 디테일하게 기록합니다.

## 작업 메타
- 리뷰 대상 버전: v2.0.0 (commit `c8f6306` 시점)
- 리뷰 일자: 2026-05-10
- 리뷰자: junhyeong9812 + AI 페어 리뷰
- 영향 모듈: streamix-core, streamix-spring-boot-starter, docs, .github
- 총 이슈 수: **24** (P0 5 / P1 7 / P2 12)

## 우선순위 정의
| 등급 | 의미 | 처리 마감 |
|------|------|----------|
| **P0** | 빌드/CI/배포가 깨지거나 라이브러리가 동작하지 않음 | 즉시 |
| **P1** | 보안 취약점, deadlock 등 안정성에 직접적 위협 | 같은 릴리스 |
| **P2** | 설계/품질/일관성 — 동작에는 영향 없지만 유지보수 부담 | 같은 릴리스 |

## P0 — 빌드/런타임 깨짐
| ID | 제목 | 영향 | 문서 |
|----|------|------|------|
| P0-01 | `StreamixDashboardController` 디렉토리/패키지 불일치 | 빌드 또는 런타임 ClassNotFoundException | [p0-01-dashboard-controller-package.md](p0-01-dashboard-controller-package.md) |
| P0-02 | `deleteByStartedAtBefore` `@Modifying` 누락 | 세션 cleanup 호출 시 InvalidJpaQueryException | [p0-02-modifying-annotation.md](p0-02-modifying-annotation.md) |
| P0-03 | `META-INF/spring/...AutoConfiguration.imports` 부재 + `StreamixJpaConfiguration` 데드 코드 | `@AutoConfiguration` 클래스 자동 로드 안 됨 | [p0-03-autoconfiguration-imports.md](p0-03-autoconfiguration-imports.md) |
| P0-04 | `LICENSE` 파일이 빈 파일 | Maven Central publish 거부 위험 | [p0-04-license.md](p0-04-license.md) |
| P0-05 | CI/Publish 워크플로 브랜치 불일치 (`main` vs 실제 `master`) | CI가 한 번도 안 돌아감 | [p0-05-ci-workflow.md](p0-05-ci-workflow.md) |

## P1 — 보안/안정성
| ID | 제목 | 영향 | 문서 |
|----|------|------|------|
| P1-06 | `LocalFileStorageAdapter` 로 path traversal 검증 누락 | 임의 파일 읽기/삭제 가능 | [p1-06-path-traversal.md](p1-06-path-traversal.md) |
| P1-07 | `dashboard.js showToast` innerHTML XSS | 업로드 파일명 통한 XSS | [p1-07-xss-toast.md](p1-07-xss-toast.md) |
| P1-08 | starter 측 `application.yml`이 사용자 설정 오염 | DDL/DataSource/FFmpeg 경로 강제 적용 | [p1-08-application-yml.md](p1-08-application-yml.md) |
| P1-09 | `FFmpegThumbnailAdapter` stderr drain 누락 | 큰 비디오에서 ffmpeg 프로세스 deadlock | [p1-09-ffmpeg-stderr.md](p1-09-ffmpeg-stderr.md) |
| P1-10 | `FileType.isPreviewable()` PDF 검사 항상 true | 모든 DOCUMENT가 미리보기 분류됨 | [p1-10-previewable-bug.md](p1-10-previewable-bug.md) |
| P1-11 | `FileStreamService.parseRange` RFC 7233 미준수 + NumberFormatException | 잘못된 Range 헤더로 500 반환 | [p1-11-range-rfc7233.md](p1-11-range-rfc7233.md) |
| P1-12 | `MediaType.parseMediaType` 사용자 입력 직접 사용 | 잘못된 contentType이 500 유발 | [p1-12-mediatype-safe.md](p1-12-mediatype-safe.md) |

## P2 — 설계/품질/일관성
| ID | 제목 | 영향 | 문서 |
|----|------|------|------|
| P2-13 | `formatSize` 6곳 중복 | DRY 위반, 일관성 깨짐 | [p2-13-byte-formatter.md](p2-13-byte-formatter.md) |
| P2-14 | `StreamixJpaConfiguration` vs `StreamixRepositoryConfiguration` 중복 | 데드 코드 + 혼란 | [p2-14-jpa-config-merge.md](p2-14-jpa-config-merge.md) |
| P2-15 | `streamixPropertiesAlias` `@Primary` hack | Bean 중복 등록의 임시방편 | [p2-15-properties-alias.md](p2-15-properties-alias.md) |
| P2-16 | `FileTypeDetector` 무상태인데 인스턴스 클래스 | 매번 new 생성 낭비 | [p2-16-filetype-detector-static.md](p2-16-filetype-detector-static.md) |
| P2-17 | `Storage.getResolvedBasePath` 절대경로 판별 부정확 | `data:image` 등 오판 + Windows 호환 | [p2-17-resolved-base-path.md](p2-17-resolved-base-path.md) |
| P2-18 | `EnableStreamix` 의존 순서 어노테이션 부재 | DataSource 늦은 로딩 시 실패 가능 | [p2-18-autoconfigure-after.md](p2-18-autoconfigure-after.md) |
| P2-19 | sidebar `currentPage` 모델 누락 | 대시보드 메뉴 활성화 표시 안 됨 | [p2-19-sidebar-current-page.md](p2-19-sidebar-current-page.md) |
| P2-20 | 템플릿 file size MB 고정 표시 | 1KB도 0.0 MB, 10GB도 10240 MB | [p2-20-template-filesize.md](p2-20-template-filesize.md) |
| P2-21 | `dashboard.js` maxSize 500MB 하드코딩 | 서버 100MB와 불일치 | [p2-21-dashboard-maxsize.md](p2-21-dashboard-maxsize.md) |
| P2-22 | `FileUploadService.generateThumbnail` 광범위 catch | 의도치 않은 예외도 흡수 | [p2-22-upload-exception-narrow.md](p2-22-upload-exception-narrow.md) |
| P2-23 | `FileDeleteService` 멱등성 부재 | 두 번째 DELETE 호출 시 404 | [p2-23-delete-idempotency.md](p2-23-delete-idempotency.md) |
| P2-24 | Bootstrap CDN 의존 | 인터넷 차단 환경 동작 불가 | [p2-24-cdn-webjars.md](p2-24-cdn-webjars.md) |

## 진행 상태 트래킹

| 단계 | 상태 |
|------|------|
| 1. 이슈 문서 작성 (24개) | 진행 중 |
| 2. plan/context/checklist 작성 | 대기 |
| 3. P0 구현 + 빌드 검증 | 대기 |
| 4. P1 구현 + 빌드/테스트 검증 | 대기 |
| 5. P2 구현 + 빌드/테스트 검증 | 대기 |
| 6. learned.md + project-overview.md 업데이트 | 대기 |

## 관련 문서
- 작업 plan: `docs/plans/2026-05-10/code-review-fix/plan.md`
- 작업 context: `docs/plans/2026-05-10/code-review-fix/context.md`
- 작업 checklist: `docs/plans/2026-05-10/code-review-fix/checklist.md`
- 학습 기록: `docs/plans/2026-05-10/code-review-fix/learned.md` (작업 완료 후)
- 프로젝트 overview: `docs/project-overview.md`
- 이전 변경 이력: `docs/refactor-log/README.md`
