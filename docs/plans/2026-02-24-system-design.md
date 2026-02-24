# 출결 관리 시스템 — 설계 문서

**작성일**: 2026-02-24

**제출 기한**: 2026-02-26 24:00

---

## 1. 기술 스택

| 항목 | 결정 |
|------|------|
| 빌드 도구 | Maven |
| JDK | 21 (요구사항 17+) |
| Framework | Spring Boot 3.x |
| Database | H2 (인메모리) |
| ORM | Spring Data JPA + JPQL |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 테스트 | JUnit 5 + Mockito |
| 패스워드 | BCrypt (Spring Security Crypto) |

---

## 2. 아키텍처

### 레이어 구조

```
controller → service → repository
```

인증/인가 없음 — 모든 API 공개 접근.

### 패키지 구조

```
com.prography.backend
├── config/
│   ├── AppConfig.java            # BCryptPasswordEncoder, 현재 기수 설정
│   └── OpenApiConfig.java        # Swagger 설정
├── controller/
│   ├── AuthController.java
│   ├── MemberController.java     # GET /members/{id}, GET /members/{id}/attendance-summary
│   ├── SessionController.java    # GET /sessions
│   ├── AttendanceController.java # POST/GET /attendances
│   ├── AdminMemberController.java
│   ├── AdminCohortController.java
│   ├── AdminSessionController.java
│   ├── AdminQrCodeController.java
│   └── AdminAttendanceController.java
├── service/
│   ├── AuthService.java
│   ├── MemberService.java
│   ├── CohortService.java
│   ├── SessionService.java
│   ├── QrCodeService.java
│   └── AttendanceService.java
├── domain/
│   ├── Member.java
│   ├── Cohort.java
│   ├── Part.java
│   ├── Team.java
│   ├── CohortMember.java
│   ├── Session.java
│   ├── QrCode.java
│   ├── Attendance.java
│   └── DepositHistory.java
├── repository/
│   └── (도메인별 Repository 인터페이스)
├── dto/
│   ├── request/
│   └── response/
├── exception/
│   ├── AppException.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
├── common/
│   └── ApiResponse.java
└── infrastructure/
    └── DataInitializer.java      # ApplicationRunner 기반 시드 데이터
```

---

## 3. 도메인 모델

### ERD 관계

전체 9개 엔티티는 **회원/기수 관리** 그룹과 **출결 관리** 그룹으로 나뉩니다.

#### 회원/기수 관리 그룹

```
Member ─────────────> CohortMember <──────────── Cohort
(계정 정보만 보유)       (연결 + 비즈니스 데이터)      (기수)
                              │
                    ┌─────────┼──────────┐
                    │         │          │
                  Part      Team      deposit
                (파트,      (팀,     excuseCount
               nullable)  nullable)
                              │
                              └──────> DepositHistory[]
                                        (보증금 이력)
```

`Member`는 순수한 계정 정보(loginId, password, name, phone, status, role)만 가집니다.
기수 소속, 파트/팀, 보증금, 공결 횟수는 모두 `CohortMember`가 담습니다.

`CohortMember`는 단순 연결 테이블이 아니라 기수별 비즈니스 상태(`deposit`, `excuseCount`)를 보유하는 핵심 엔티티입니다.
한 회원이 10기와 11기 모두에 소속될 수 있는 구조이며, 기수마다 보증금이 독립적으로 관리됩니다.

`DepositHistory`는 `CohortMember`에 연결됩니다. "특정 기수에서의 보증금 변동 이력"이기 때문입니다.

#### 출결 관리 그룹

```
Cohort ──> Session ──────────> QrCode[]
(기수)      (일정)               (QR 코드)
              │                   (활성: 최대 1개)
              │
              └──> Attendance[] <────── Member
                    (출결 기록)
                        │
                      QrCode (nullable)
                   (QR 체크인 시 참조,
                    수동 등록 시 null)
```

`Session`은 특정 기수의 정기 모임 일정입니다. 하나의 일정에 QR 코드가 여러 개 존재할 수 있지만,
`expiresAt > now()` 인 활성 QR은 항상 최대 1개입니다 (갱신 시 이전 QR 즉시 만료).

`Attendance`는 "누가(Member) 어느 일정(Session)에 어떻게 출석했는가"를 기록합니다.
QR 체크인이면 `qrCode` 필드에 사용된 QR을 참조하고, 관리자 수동 등록이면 `qrCode`는 null입니다.

#### 전체 관계 요약

| 관계 | 카디널리티 | 설명 |
|------|-----------|------|
| Member : CohortMember | 1 : N | 한 회원이 여러 기수에 소속 가능 |
| Cohort : CohortMember | 1 : N | 한 기수에 여러 회원 소속 |
| CohortMember : Part | N : 1 (nullable) | 파트 미배정 가능 |
| CohortMember : Team | N : 1 (nullable) | 팀 미배정 가능 |
| CohortMember : DepositHistory | 1 : N | 보증금 변동마다 이력 추가 |
| Cohort : Session | 1 : N | 한 기수에 여러 일정 존재 |
| Session : QrCode | 1 : N | 일정당 QR 이력 누적 (활성은 최대 1개) |
| Session : Attendance | 1 : N | 일정당 출결 기록 |
| Member : Attendance | 1 : N | 회원당 출결 기록 |
| QrCode : Attendance | 1 : N (nullable) | QR 체크인 출결에만 연결 |

---

### 엔티티 핵심 설계

| Entity | 필드 | 설계 포인트 |
|--------|------|-------------|
| `Member` | status, role | `@Enumerated(EnumType.STRING)` — 숫자 대신 문자열 저장, Enum 순서 변경에 안전 |
| `CohortMember` | part, team | `@ManyToOne(optional=true)` — 파트/팀 없이 등록 가능 (명세 선택 필드) |
| `CohortMember` | deposit | `int` — 현재 잔액 직접 관리, 변동 시 DepositHistory에 이력 추가 |
| `CohortMember` | excuseCount | `int` — 기수당 최대 3회 제한, 상태 전환 시 증감 |
| `Session` | date, time | `LocalDate` + `LocalTime` 분리 저장 — 지각 판정 시 `Asia/Seoul` 타임존 적용을 위해 분리 (Instant로 합치면 타임존 처리 복잡) |
| `QrCode` | expiresAt | `Instant` (UTC) — `Instant.now().isAfter(expiresAt)` 로 만료 판정 |
| `QrCode` | hashValue | `String` (UUID) — unique 제약, 출석 체크 시 조회 키 |
| `Attendance` | qrCode | `@ManyToOne(nullable)` — QR 체크인 vs 수동 등록 구분 |
| `Attendance` | lateMinutes | `Integer` (nullable) — PRESENT/ABSENT/EXCUSED 시 null, LATE 시 지각 분 |
| `DepositHistory` | amount | `int` — PENALTY: 음수(-10000), INITIAL/REFUND: 양수(+100000, +10000) |
| `DepositHistory` | balanceAfter | `int` — 변경 후 잔액 스냅샷, 이력 추적 용이 |
| `DepositHistory` | attendance | `@ManyToOne(nullable)` — INITIAL은 출결 없음, PENALTY/REFUND는 출결 참조 |

---

## 4. 공통 패턴

### ApiResponse 래퍼

```java
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static ApiResponse<?> fail(ErrorCode code) { ... }
}
```

### 에러 처리

```java
// ErrorCode enum — HTTP status + 메시지 포함
// AppException extends RuntimeException
// @RestControllerAdvice GlobalExceptionHandler
```

### 현재 기수 설정

```yaml
app:
  current-cohort:
    generation: 11
```

---

## 5. 핵심 비즈니스 로직

### 지각 판정 (Asia/Seoul 타임존)

```java
ZoneId SEOUL = ZoneId.of("Asia/Seoul");
LocalDateTime sessionDateTime = LocalDateTime.of(session.getDate(), session.getTime());
long lateMinutes = ChronoUnit.MINUTES.between(
    sessionDateTime.atZone(SEOUL), ZonedDateTime.now(SEOUL)
);
// lateMinutes > 0 → LATE, else PRESENT
```

### 패널티 계산

| 상태 | 패널티 |
|------|--------|
| PRESENT | 0원 |
| ABSENT | 10,000원 |
| LATE | min(lateMinutes × 500, 10,000)원 |
| EXCUSED | 0원 |

### QR 출석 체크 검증 순서 (엄격히 준수)

1. QR hashValue 조회 → `QR_INVALID`
2. QR 만료 → `QR_EXPIRED`
3. 일정 `IN_PROGRESS` → `SESSION_NOT_IN_PROGRESS`
4. 회원 존재 → `MEMBER_NOT_FOUND`
5. 회원 탈퇴 → `MEMBER_WITHDRAWN`
6. 중복 출결 → `ATTENDANCE_ALREADY_CHECKED`
7. CohortMember 존재 → `COHORT_MEMBER_NOT_FOUND`

### 출결 수정 시 보증금 조정

```
diff = newPenalty - oldPenalty
diff > 0 → 추가 차감 + DepositHistory(PENALTY, -diff)
diff < 0 → 환급 + DepositHistory(REFUND, +|diff|)
diff = 0 → 변동 없음
```

---

## 6. 명세 모호성 해소

| 이슈 | 결정 |
|------|------|
| admin CohortMember 소속 기수 | 11기 (deposit 100,000원, INITIAL 이력 포함) |
| 일정 삭제 시 QR 처리 | 활성 QR을 즉시 만료 (`expiresAt = now()`) |
| `EXCUSED → EXCUSED` 수정 | excuseCount 변동 없음, diff=0 이므로 보증금 변동 없음 |
| 일정 생성 후 QR 생성 API (#15) 재호출 | `QR_ALREADY_ACTIVE` (정상 에러, 명세 일치) |
| 대시보드 generation/part/team 필터 | 메모리 후처리 + totalElements 재계산 (명세 준수) |

---

## 7. 시드 데이터

- `ApplicationRunner` 구현, 멱등성 보장 (이미 존재 시 스킵)
- 10기 + 11기 + 파트 10개 + 팀 3개 (11기) + admin + CohortMember(11기) + DepositHistory(INITIAL)

---

## 8. 테스트 전략

- 서비스 레이어 단위 테스트 (Repository Mock)
- 핵심 케이스: 패널티 경계값, 보증금 조정, 공결 한도, QR 검증 순서
- 가산점 API 서비스도 단위 테스트 필요

---

## 9. API 구현 목록

### 필수 (16개)
1. POST `/api/v1/auth/login`
2. GET `/api/v1/members/{id}`
3. POST `/api/v1/admin/members`
4. GET `/api/v1/admin/members`
5. GET `/api/v1/admin/members/{id}`
6. PUT `/api/v1/admin/members/{id}`
7. DELETE `/api/v1/admin/members/{id}`
8. GET `/api/v1/admin/cohorts`
9. GET `/api/v1/admin/cohorts/{cohortId}`
10. GET `/api/v1/sessions`
11. GET `/api/v1/admin/sessions`
12. POST `/api/v1/admin/sessions`
13. PUT `/api/v1/admin/sessions/{id}`
14. DELETE `/api/v1/admin/sessions/{id}`
15. POST `/api/v1/admin/sessions/{sessionId}/qrcodes`
16. PUT `/api/v1/admin/qrcodes/{qrCodeId}`

### 가산점 (9개)
17. POST `/api/v1/attendances`
18. GET `/api/v1/attendances`
19. GET `/api/v1/members/{memberId}/attendance-summary`
20. POST `/api/v1/admin/attendances`
21. PUT `/api/v1/admin/attendances/{id}`
22. GET `/api/v1/admin/attendances/sessions/{sessionId}/summary`
23. GET `/api/v1/admin/attendances/members/{memberId}`
24. GET `/api/v1/admin/attendances/sessions/{sessionId}`
25. GET `/api/v1/admin/cohort-members/{cohortMemberId}/deposits`
