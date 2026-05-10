# 맥락 노트: code-review-fix

## 프로젝트 개요
- **Streamix**: Spring Boot 4 미디어 스트리밍 라이브러리 (`@EnableStreamix`)
- 멀티 모듈 Gradle: `streamix-core` + `streamix-spring-boot-starter`
- 헥사고날 아키텍처 (Ports & Adapters)
- Java 25, Spring Boot 4.0

## 이전 작업 흐름
1. 사용자가 "코드 리뷰 전체 부탁" → AI가 70+ 파일 전체 검토
2. 24개 이슈 발견 (P0 5 / P1 7 / P2 12)
3. 사용자가 docs에 issue 문서 + 개선 진행 요청
4. 각 이슈를 별개 문서로 작성 완료 (`docs/refactor-log/2026-05-10-code-review/`)
5. 본 plan 작성 → 사용자 승인 대기

## 코드베이스 핵심 컨벤션 (관찰)
- **Java Record 적극 사용**: 도메인 모델/DTO/Properties
- **Compact Constructor 검증**: `Objects.requireNonNull` + `IllegalArgumentException`
- **sealed exception 계층**: switch exhaustive
- **slf4j 로깅**: private static final Logger
- **인터페이스 기반 테스트**: Mockito + AssertJ
- **테스트 네이밍**: 한글 `@DisplayName` + Nested 그룹
- **import 순서**: 일관된 형식 (java → 외부 → 내부)

## 금지 영역 (절대 수정 금지)
| 영역 | 이유 |
|------|------|
| `core/domain/model/UploadResult.java` | 변경 사유 없음, breaking 회피 |
| `core/domain/model/StreamableFile.java` | 변경 사유 없음 |
| `core/application/port/out/FileMetadataPort.java` | 외부 SPI — 사용자가 직접 구현 가능 |
| `core/application/port/out/FileStoragePort.java` | 외부 SPI |
| `core/application/port/out/ThumbnailGeneratorPort.java` | 외부 SPI |
| `core/adapter/out/metadata/InMemoryMetadataAdapter.java` | 안정 |
| `core/adapter/out/thumbnail/ImageThumbnailAdapter.java` | 안정 |
| `starter/adapter/in/web/dto/*.java` | DTO 변경은 API 호환성 영향 |
| `starter/adapter/out/persistence/FileMetadataEntity.java` | DB 스키마 변경 회피 |
| `starter/adapter/out/persistence/FileMetadataJpaRepository.java` | 변경 사유 없음 |
| `starter/adapter/out/persistence/JpaFileMetadataAdapter.java` | 변경 사유 없음 |
| `static/streamix/css/dashboard.css` | 디자인 영역 — 별도 작업 |
| `README.md` | changelog 별도 PR |
| `docs/concepts/`, `docs/implement/`, `docs/refactor-log/README.md` | 학습 자료 |

## 도메인 객체 변경 시 주의
- `FileMetadata`는 record로 모든 필드가 immutable
- 새 필드 추가 시 → JPA Entity, JsonResponse DTO도 같이 갱신
- 본 작업에선 새 필드 추가 없음 (메서드만 추가/변경)

## 빌드/테스트 명령어
```bash
# 컴파일만 (빠른 검증)
./gradlew compileJava

# 모듈별 컴파일
./gradlew :streamix-core:compileJava
./gradlew :streamix-spring-boot-starter:compileJava

# 테스트
./gradlew test
./gradlew :streamix-core:test
./gradlew :streamix-spring-boot-starter:test

# 특정 테스트
./gradlew test --tests FileMetadataTest
./gradlew test --tests "*UploadService*"

# 전체 (test 포함)
./gradlew build

# Java 25 toolchain 자동 다운로드 시간 소요 가능
```

## 외부 의존성 / 환경
- Java 25 (toolchain 자동)
- Gradle 9.2.1 (wrapper)
- 테스트는 H2 in-memory + Mockito + ApplicationContextRunner
- FFmpeg는 시스템 PATH (테스트에서는 mock 사용)

## 사용자 명시 결정 사항
- 이슈 문서 위치: `docs/refactor-log/2026-05-10-code-review/`
- 작업 범위: P0 + P1 + P2 전부 (24개)
- 각 이슈는 별개 문서 + 프로세스 디테일 작성

## Phase별 검증 전략
1. **Phase A 후**: 컴파일 통과 (신규 파일만 추가 — 기존 코드 영향 없음)
2. **Phase B 후**: 컴파일 + 기본 테스트 통과 (P0 — autoconfig 변경, build 영향)
3. **Phase C 후**: 신규 + 기존 테스트 통과 (P1 — 동작 변경 다수)
4. **Phase D 후**: 전체 빌드 + 모든 테스트 통과 (P2 — 리팩토링)

## 알려진 잠재 위험
- `FileMetadataTest.formatsGigabytes`: 정밀도 2 → 1 변경 시 깨짐. 같이 갱신 필요.
- `FileUploadServiceTest`: RuntimeException 사용처를 ThumbnailGenerationException으로 변경 필요
- `FileDeleteServiceTest`: 동일 (StorageException으로)
- `LocalFileStorageAdapterTest`: 절대경로 검증 강화로 일부 케이스 갱신 필요
- `StreamixException sealed permit` 변경 → `StreamixExceptionTest.allExceptionsExtendStreamixException`도 갱신

## 사용자가 제기할 가능성 있는 질문
- "정말 24개 다 한 번에 해도 안전한가?" → Phase 단위 검증으로 isolation
- "왜 데드 코드 삭제부터 안 하나?" → P0가 더 우선
- "WebJars 추가가 진짜 필요한가?" → P2이므로 의견 받을 수 있음

## 의존성 그래프 (작업 순서)
```
ByteSizeFormatter (Phase A 1) ──┐
                                  │
RangeNotSatisfiableException (A2)│
   └─ StreamixException (A3) ───┐│
                                ││
LICENSE (B4)                    ││
StreamixDashboardController     ││
  이동 (B5)                     ││
@Modifying (B6)                 ││
                                ││
6개 Configuration               ││
  @AutoConfiguration→@Config (B7)││
StreamixJpaConfig 삭제 (B8)    ││
EnableStreamix javadoc (B9)     ││
CI/Publish workflow (B10-12)    ││
                                ││
P1-10 isPreviewable (C13-14) ───┘│
P1-12 safeContentType (C15) ─────┤
P1-06 path traversal (C16) ─────┤
P1-11 parseRange (C17-18) ──────┘
P1-12 safeMediaType (C19)
P1-09 ffmpeg (C20)
P1-07 XSS (C21)
P1-08 application.yml (C22)
P2-22 + P1-12 합쳐서 FileUploadService (C23)

P2-13 byte formatter 6곳 위임 (D24)
P2-16 FileTypeDetector static (D25-26)
P2-17 path API (D27)
P2-18 AutoConfigureAfter (D28)
P2-14, P2-15 (D29)
P2-19 currentPage (D30)
P2-20 templates (D31-32)
P2-21 maxSize 동적 (D33)
P2-23 deleteIdempotent (D34-36)
P2-24 WebJars (D37-38)
```
