# CLAUDE.md — 프로그라피 11기 BE 과제: 출결 관리 시스템

## 프로젝트 개요

프로그라피 세션 출결 관리를 위한 Spring Boot REST API 서버.

제출 기한: **2026.02.26 (목) 24:00**

---

## 기술 스택 (제약사항)

- **JDK**: 17 이상
- **Framework**: Spring Boot
- **Database**: H2 (인메모리) — 변경 불가
- **Password**: BCrypt 해싱 (cost factor 12)
- **인증/인가**: 구현하지 않음 — 모든 API 공개 접근 가능

---

## Base URL

```
http://localhost:8080/api/v1
```

---

## 응답 형식

### 성공
```json
{ "success": true, "data": { ... }, "error": null }
```

### 실패
```json
{ "success": false, "data": null, "error": { "code": "ERROR_CODE", "message": "메시지" } }
```

### 페이징 (data 내부)
```json
{ "content": [...], "page": 0, "size": 10, "totalElements": 50, "totalPages": 5 }
```

---

## Enum 정의

| Enum | 값 |
|------|-----|
| MemberStatus | ACTIVE, INACTIVE, WITHDRAWN |
| MemberRole | MEMBER, ADMIN |
| SessionStatus | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED |
| AttendanceStatus | PRESENT, ABSENT, LATE, EXCUSED |
| DepositType | INITIAL, PENALTY, REFUND |

---

## 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| INVALID_INPUT | 400 | 입력값 오류 |
| INTERNAL_ERROR | 500 | 서버 내부 오류 |
| LOGIN_FAILED | 401 | 로그인 아이디/비밀번호 불일치 |
| MEMBER_WITHDRAWN | 403 | 탈퇴한 회원 |
| MEMBER_NOT_FOUND | 404 | 회원 없음 |
| DUPLICATE_LOGIN_ID | 409 | 중복 loginId |
| MEMBER_ALREADY_WITHDRAWN | 400 | 이미 탈퇴 처리됨 |
| COHORT_NOT_FOUND | 404 | 기수 없음 |
| PART_NOT_FOUND | 404 | 파트 없음 |
| TEAM_NOT_FOUND | 404 | 팀 없음 |
| COHORT_MEMBER_NOT_FOUND | 404 | 기수 회원 없음 |
| SESSION_NOT_FOUND | 404 | 일정 없음 |
| SESSION_ALREADY_CANCELLED | 400 | 이미 취소된 일정 |
| SESSION_NOT_IN_PROGRESS | 400 | 진행 중이 아닌 일정 |
| QR_NOT_FOUND | 404 | QR 없음 |
| QR_INVALID | 400 | 유효하지 않은 QR |
| QR_EXPIRED | 400 | 만료된 QR |
| QR_ALREADY_ACTIVE | 409 | 활성 QR 이미 존재 |
| ATTENDANCE_NOT_FOUND | 404 | 출결 없음 |
| ATTENDANCE_ALREADY_CHECKED | 409 | 중복 출결 |
| EXCUSE_LIMIT_EXCEEDED | 400 | 공결 3회 초과 |
| DEPOSIT_INSUFFICIENT | 400 | 보증금 잔액 부족 |

---

## API 목록

### 필수 (16개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 1 | POST | `/api/v1/auth/login` | 로그인 |
| 2 | GET | `/api/v1/members/{id}` | 회원 조회 |
| 3 | POST | `/api/v1/admin/members` | 회원 등록 |
| 4 | GET | `/api/v1/admin/members` | 회원 대시보드 (페이징+필터) |
| 5 | GET | `/api/v1/admin/members/{id}` | 회원 상세 |
| 6 | PUT | `/api/v1/admin/members/{id}` | 회원 수정 |
| 7 | DELETE | `/api/v1/admin/members/{id}` | 회원 탈퇴 (soft-delete) |
| 8 | GET | `/api/v1/admin/cohorts` | 기수 목록 |
| 9 | GET | `/api/v1/admin/cohorts/{cohortId}` | 기수 상세 (파트+팀 포함) |
| 10 | GET | `/api/v1/sessions` | 일정 목록 (회원, CANCELLED 제외) |
| 11 | GET | `/api/v1/admin/sessions` | 일정 목록 (관리자, 전체+필터) |
| 12 | POST | `/api/v1/admin/sessions` | 일정 생성 (QR 자동 생성) |
| 13 | PUT | `/api/v1/admin/sessions/{id}` | 일정 수정 |
| 14 | DELETE | `/api/v1/admin/sessions/{id}` | 일정 삭제 (CANCELLED) |
| 15 | POST | `/api/v1/admin/sessions/{sessionId}/qrcodes` | QR 생성 |
| 16 | PUT | `/api/v1/admin/qrcodes/{qrCodeId}` | QR 갱신 |

### 가산점 (9개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 17 | POST | `/api/v1/attendances` | QR 출석 체크 |
| 18 | GET | `/api/v1/attendances` | 내 출결 기록 (memberId 쿼리) |
| 19 | GET | `/api/v1/members/{memberId}/attendance-summary` | 출결 요약 |
| 20 | POST | `/api/v1/admin/attendances` | 출결 등록 (관리자) |
| 21 | PUT | `/api/v1/admin/attendances/{id}` | 출결 수정 (보증금 자동 조정) |
| 22 | GET | `/api/v1/admin/attendances/sessions/{sessionId}/summary` | 일정별 출결 요약 |
| 23 | GET | `/api/v1/admin/attendances/members/{memberId}` | 회원 출결 상세 |
| 24 | GET | `/api/v1/admin/attendances/sessions/{sessionId}` | 일정별 출결 목록 |
| 25 | GET | `/api/v1/admin/cohort-members/{cohortMemberId}/deposits` | 보증금 이력 |

---

## 시드 데이터 (서버 시작 시 자동 로드)

| 항목 | 내용 |
|------|------|
| 기수 | 10기 (id=1), 11기 (id=2, 현재 운영 기수) |
| 파트 | 기수별 SERVER, WEB, iOS, ANDROID, DESIGN (총 10개) |
| 팀 | 11기 Team A, Team B, Team C (총 3개, 11기만 존재) |
| 관리자 | loginId=`admin`, password=`admin1234` (BCrypt), role=ADMIN |
| 보증금 | 관리자 초기 보증금 100,000원 (DepositHistory INITIAL 기록) |

**현재 운영 기수**: 11기 고정 (`application.properties` 또는 설정값으로 관리)

---

## 핵심 비즈니스 규칙

### 회원
- `loginId` 시스템 전체 중복 불가
- 등록 시: `status=ACTIVE`, `role=MEMBER`, CohortMember 생성 + 보증금 100,000원 + DepositHistory(INITIAL)
- 탈퇴: soft-delete → `status=WITHDRAWN`
- `partId`, `teamId`는 회원 등록/수정 시 optional

### 일정
- 일정 생성 시 QR 코드 자동 생성 (UUID hashValue, 유효기간 24시간)
- 삭제: soft-delete → `status=CANCELLED`
- CANCELLED 일정은 수정 불가 (`SESSION_ALREADY_CANCELLED`)
- 회원용 목록: CANCELLED 제외

### QR 코드
- UUID 기반 hashValue, 유효기간 24시간
- 일정당 활성(미만료) QR 코드는 1개만 허용
- 갱신: 기존 QR의 `expiresAt`을 현재 시각으로 설정 → 새 QR 생성

### QR 출석 체크 검증 순서 (순서 중요)
1. QR hashValue 조회 → 없으면 `QR_INVALID`
2. QR 만료 여부 → `QR_EXPIRED`
3. 일정 상태 `IN_PROGRESS` 확인 → `SESSION_NOT_IN_PROGRESS`
4. 회원 존재 여부 → `MEMBER_NOT_FOUND`
5. 회원 탈퇴 여부 → `MEMBER_WITHDRAWN`
6. 중복 출결 여부 → `ATTENDANCE_ALREADY_CHECKED`
7. CohortMember 존재 여부 → `COHORT_MEMBER_NOT_FOUND`

검증 통과 후:
- **지각 판정**: `session.date + session.time` (Asia/Seoul) 기준 현재 시각이 이후 → LATE, 이전 → PRESENT
- LATE: `lateMinutes` = 차이(분)
- 패널티 계산 후 Attendance 저장, 보증금 차감

### 패널티 정책

| 상태 | 패널티 |
|------|--------|
| PRESENT | 0원 |
| ABSENT | 10,000원 |
| LATE | min(지각분 × 500, 10,000)원 |
| EXCUSED | 0원 |

### 보증금
- 초기: 100,000원
- 모든 변동(초기/차감/환급)은 DepositHistory 기록 필수
- 잔액 부족 시 차감 불가 → `DEPOSIT_INSUFFICIENT`
- PENALTY: amount 음수 (예: -10000), REFUND/INITIAL: 양수

### 출결 수정 시 보증금 자동 조정
- `diff = newPenalty - oldPenalty`
- diff > 0 → 추가 차감 + DepositHistory(PENALTY)
- diff < 0 → 환급 + DepositHistory(REFUND)
- diff = 0 → 변동 없음

### 공결 (EXCUSED)
- 기수당 최대 3회 (`CohortMember.excuseCount`)
- 다른 상태 → EXCUSED: excuseCount < 3 검증 후 ++
- EXCUSED → 다른 상태: excuseCount-- (최소 0)
- 3회 초과 시 → `EXCUSE_LIMIT_EXCEEDED`

### 회원 대시보드 필터
- DB 레벨: `status`, `searchType(name/loginId/phone)+searchValue`
- 메모리 레벨 후처리: `generation`, `partName`, `teamName` (CohortMember 기반)
- 후처리 후 totalElements/totalPages 재계산 필요

---

## 도메인 모델 (핵심 엔티티)

```
Member (회원)
  - id, loginId, password(BCrypt), name, phone
  - status(MemberStatus), role(MemberRole)
  - createdAt, updatedAt

Cohort (기수)
  - id, generation(int), name

Part (파트)
  - id, cohort, name

Team (팀)
  - id, cohort, name

CohortMember (기수 회원 — Member x Cohort 연결)
  - id, member, cohort, part, team
  - deposit(int), excuseCount(int)

Session (일정)
  - id, cohort, title, date(LocalDate), time(LocalTime), location
  - status(SessionStatus)
  - createdAt, updatedAt

QrCode (QR 코드)
  - id, session, hashValue(UUID String)
  - createdAt, expiresAt

Attendance (출결)
  - id, session, member, qrCode(nullable)
  - status(AttendanceStatus), lateMinutes(nullable), penaltyAmount
  - reason(nullable), checkedInAt(nullable)
  - createdAt, updatedAt

DepositHistory (보증금 이력)
  - id, cohortMember, type(DepositType)
  - amount(int), balanceAfter(int)
  - attendance(nullable), description(nullable)
  - createdAt
```

---

## 개발 가이드

### 레이어 구조 (표준 Spring 패턴)
```
controller → service → repository
```

### 테스트 요구사항 (필수)
- 서비스 레이어 단위 테스트 작성
- 가산점 API 서비스도 단위 테스트 필요

### 제출 산출물 (필수)
- 동작하는 소스 코드 (필수 16개 API)
- ERD
- System Design Architecture (이상적인 아키텍처 자유 설계)

### 주의사항
- 모든 ID는 `Long` (auto-increment)
- Instant 타입으로 날짜/시간 직렬화 (ISO-8601)
- `date`는 `LocalDate` (yyyy-MM-dd), `time`은 `LocalTime` (HH:mm:ss)
- 지각 판정 타임존: **Asia/Seoul**
- 상세 명세: `docs/api-spec/api-spec/` 디렉토리 참조
