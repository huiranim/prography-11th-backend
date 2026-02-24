# GET /api/v1/admin/cohort-members/{cohortMemberId}/deposits

## 보증금 이력 조회

특정 기수 회원의 보증금 변동 이력을 시간순으로 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/cohort-members/{cohortMemberId}/deposits`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| cohortMemberId | Long | 기수 회원 ID (CohortMember.id) |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "cohortMemberId": 1,
      "type": "INITIAL",
      "amount": 100000,
      "balanceAfter": 100000,
      "attendanceId": null,
      "description": "초기 보증금",
      "createdAt": "2026-02-14T00:00:00Z"
    },
    {
      "id": 2,
      "cohortMemberId": 1,
      "type": "PENALTY",
      "amount": -10000,
      "balanceAfter": 90000,
      "attendanceId": 1,
      "description": "출결 등록 - ABSENT 패널티 10000원",
      "createdAt": "2026-03-01T05:00:00Z"
    },
    {
      "id": 3,
      "cohortMemberId": 1,
      "type": "REFUND",
      "amount": 10000,
      "balanceAfter": 100000,
      "attendanceId": 1,
      "description": "출결 수정 - 환급 10000원",
      "createdAt": "2026-03-01T06:00:00Z"
    }
  ],
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 이력 ID |
| cohortMemberId | Long | 기수 회원 ID |
| type | DepositType | 이력 유형 (INITIAL, PENALTY, REFUND) |
| amount | Int | 금액 (양수: 입금/환급, 음수: 차감) |
| balanceAfter | Int | 변경 후 잔액 |
| attendanceId | Long? | 연관 출결 ID (INITIAL이면 null) |
| description | String? | 설명 |
| createdAt | Instant | 생성일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| COHORT_MEMBER_NOT_FOUND | 404 | 해당 ID의 기수 회원이 존재하지 않음 |

---

## DepositType 설명

| 타입 | 설명 | amount 부호 |
|------|------|-------------|
| INITIAL | 초기 보증금 설정 | 양수 (+100,000) |
| PENALTY | 출결 패널티 차감 | 음수 (-금액) |
| REFUND | 출결 수정 시 환급 | 양수 (+금액) |
