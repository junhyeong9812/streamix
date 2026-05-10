# [P0-02] StreamingSessionRepository.deleteByStartedAtBefore — @Modifying 누락

## 분류
| 항목 | 값 |
|------|-----|
| 우선순위 | **P0 — 런타임 예외** |
| 카테고리 | 데이터베이스 / Spring Data JPA |
| 발견 위치 | `streamix-spring-boot-starter` |
| 영향 파일 | `StreamingSessionRepository.java` |
| 호출자 | `StreamingMonitoringService.cleanupOldSessions(int)` |

## 문제 분석

### 현재 동작
```java
// StreamingSessionRepository.java:179-181
@Query("DELETE FROM StreamingSessionEntity s WHERE s.startedAt < :before")
int deleteByStartedAtBefore(@Param("before") LocalDateTime before);
```

### 기대 동작
Spring Data JPA에서 **DELETE 또는 UPDATE JPQL 쿼리**를 작성할 때는 `@Modifying`이 필수. 누락하면:

```
org.springframework.dao.InvalidDataAccessApiUsageException:
  Not supported for DML operations [...]
```
또는
```
java.lang.IllegalStateException: 
  Validation failed for query for method StreamingSessionRepository.deleteByStartedAtBefore!
```

또한 변경 쿼리는 트랜잭션 안에서 실행되어야 하므로 `@Transactional`도 필요. (현재 호출자 `StreamingMonitoringService.cleanupOldSessions`는 `@Transactional`이 붙어 있어 OK이나, repository 메서드 자체에 `@Modifying(clearAutomatically = true)`을 두면 더 안전)

### 원인 분석
- 메서드 이름 기반 자동 쿼리 생성(method name derivation)은 SELECT만 추론. DELETE는 `deleteByXxx` 명명 규칙이 있으나 `@Query`로 명시한 경우 `@Modifying`이 필수
- v1 → v2 작업 중 `@Query`만 추가하고 `@Modifying`을 빠뜨린 것으로 추정

### 영향 범위
- **현재**: `StreamingMonitoringService.cleanupOldSessions(int)`가 호출되지 않으면 잠복 (코드 검색 결과 호출자 없음 — 사용자가 수동 호출하거나 `@Scheduled` 설정 시 폭발)
- **호출 시**: Spring 컨테이너 시작 시점 또는 첫 호출 시 즉시 예외
- **데이터 정합성**: 정리 작업이 동작 안 해서 세션 테이블이 무한정 증가

## 변경 프로세스

### Step 1: 어노테이션 추가
```java
import org.springframework.data.jpa.repository.Modifying;

@Modifying(clearAutomatically = true)
@Query("DELETE FROM StreamingSessionEntity s WHERE s.startedAt < :before")
int deleteByStartedAtBefore(@Param("before") LocalDateTime before);
```

### Step 2: clearAutomatically=true의 의미
- 영속성 컨텍스트(EntityManager 1차 캐시)를 자동으로 clear
- DELETE 후 같은 트랜잭션에서 같은 엔티티를 조회하면 stale data를 보지 않게 함
- cleanup 같은 batch 작업에선 거의 항상 true가 안전

### Step 3: 호출자 검증
`StreamingMonitoringService.cleanupOldSessions`:
```java
@Transactional
public int cleanupOldSessions(int retentionDays) {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    log.info("Cleaning up sessions older than {}", cutoff);
    int deleted = sessionRepository.deleteByStartedAtBefore(cutoff);
    log.info("Deleted {} old streaming sessions", deleted);
    return deleted;
}
```
- `@Transactional` 있음 → 추가 변경 불필요

### Step 4 (선택): 자동 cleanup 스케줄러
이번 작업 범위 밖이지만, 후속으로 `@Scheduled(cron = "0 0 3 * * *")` + `cleanupOldSessions(30)` 같은 정기 작업 추가 검토.

## Before / After

### Before
```java
// StreamingSessionRepository.java:179-181
/**
 * 오래된 세션을 삭제합니다 (정리용).
 *
 * @param before 기준 시각 (이 시각 이전의 세션 삭제)
 * @return 삭제된 세션 수
 */
@Query("DELETE FROM StreamingSessionEntity s WHERE s.startedAt < :before")
int deleteByStartedAtBefore(@Param("before") LocalDateTime before);
```

### After
```java
// StreamingSessionRepository.java
import org.springframework.data.jpa.repository.Modifying;

/**
 * 오래된 세션을 삭제합니다 (정리용).
 *
 * <p>{@code @Modifying(clearAutomatically = true)}으로 영속성 컨텍스트를
 * 자동 clear하여, 같은 트랜잭션에서 stale 엔티티를 조회하지 않도록 합니다.</p>
 *
 * @param before 기준 시각 (이 시각 이전의 세션 삭제)
 * @return 삭제된 세션 수
 */
@Modifying(clearAutomatically = true)
@Query("DELETE FROM StreamingSessionEntity s WHERE s.startedAt < :before")
int deleteByStartedAtBefore(@Param("before") LocalDateTime before);
```

## 검증 방법

### 1. 컴파일
```bash
./gradlew :streamix-spring-boot-starter:compileJava
```

### 2. 단위 테스트 추가 (선택)
`StreamingSessionRepositoryTest`가 없으므로 테스트 추가는 별도 작업. 이번 범위 밖.

### 3. 수동 검증 (선택)
H2 in-memory + ApplicationContextRunner로 확인:
```java
@Test
void cleanupOldSessions_deletesEntitiesBeforeCutoff() {
    // given - 오래된 세션 3개 + 최근 세션 2개
    // when
    int deleted = service.cleanupOldSessions(7);
    // then
    assertThat(deleted).isEqualTo(3);
    assertThat(repository.count()).isEqualTo(2);
}
```

### 4. 기존 테스트 회귀
```bash
./gradlew :streamix-spring-boot-starter:test
```
이 변경은 기존 동작에 영향 없음 (어노테이션 추가만).

## 관련 파일
- `streamix-spring-boot-starter/src/main/java/io/github/junhyeong9812/streamix/starter/adapter/out/persistence/StreamingSessionRepository.java`

## 참고
- Spring Data JPA Reference §3.7.1 Modifying Queries
- `@Modifying`의 `flushAutomatically` 옵션은 DELETE 전에 pending 변경사항을 DB로 flush함 — 이 케이스에선 cleanup 직전에 진행 중인 다른 변경이 있을 가능성 낮아 false(기본) 유지
