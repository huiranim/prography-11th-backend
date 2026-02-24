# POST /api/v1/admin/attendances

## 출결 등록

관리자가 수동으로 출결을 등록합니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/admin/attendances`
**Content-Type**: `application/json`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sessionId | Long | O | 일정 ID |
| memberId | Long | O | 회원 ID |
| status | AttendanceStatus | O | 출결 상태 (PRESENT, ABSENT, LATE, EXCUSED) |
| lateMinutes | Int | X | 지각 시간(분). min 0. LATE 시 필요 |
| reason | String | X | 사유 |

```json
{
  "sessionId": 1,
  "memberId": 1,
  "status": "ABSENT",
  "lateMinutes": null,
  "reason": "무단 결석"
}
```

---

## Response

**Status**: `201 Created`

### Response Body

```json
{
  "success": true,
  "data": {
    "id": 1,
    "sessionId": 1,
    "memberId": 1,
    "status": "ABSENT",
    "lateMinutes": null,
    "penaltyAmount": 10000,
    "reason": "무단 결석",
    "checkedInAt": null,
    "createdAt": "2026-02-14T00:00:00Z",
    "updatedAt": "2026-02-14T00:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 출결 ID |
| sessionId | Long | 일정 ID |
| memberId | Long | 회원 ID |
| status | AttendanceStatus | 출결 상태 |
| lateMinutes | Int? | 지각 시간(분) |
| penaltyAmount | Int | 패널티 금액 |
| reason | String? | 사유 |
| checkedInAt | Instant? | 체크인 시각 (수동 등록 시 null) |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| SESSION_NOT_FOUND | 404 | 일정이 존재하지 않음 |
| MEMBER_NOT_FOUND | 404 | 회원이 존재하지 않음 |
| ATTENDANCE_ALREADY_CHECKED | 409 | 해당 일정에 이미 출결 기록 존재 |
| COHORT_MEMBER_NOT_FOUND | 404 | 현재 기수의 기수회원 정보가 없음 |
| EXCUSE_LIMIT_EXCEEDED | 400 | EXCUSED 등록 시 공결 횟수 초과 (최대 3회) |
| DEPOSIT_INSUFFICIENT | 400 | 보증금 잔액 부족 |

---

## 비즈니스 규칙

1. 일정/회원 존재 검증
2. (sessionId, memberId) 중복 출결 확인
3. CohortMember 존재 확인
4. EXCUSED 등록 시: excuseCount < 3 검증 → 통과 시 excuseCount++
5. 패널티 계산:
   - PRESENT → 0원
   - ABSENT → 10,000원
   - LATE → min(lateMinutes × 500, 10,000)원
   - EXCUSED → 0원
6. 패널티 > 0이면 보증금 차감 + DepositHistory(PENALTY) 기록
