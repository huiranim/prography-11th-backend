# GET /api/v1/members/{memberId}/attendance-summary

## 내 출결 요약 조회

특정 회원의 출결 통계와 보증금 잔액을 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/members/{memberId}/attendance-summary`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| memberId | Long | 회원 ID |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "memberId": 1,
    "present": 5,
    "absent": 1,
    "late": 2,
    "excused": 1,
    "totalPenalty": 15000,
    "deposit": 85000
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 회원 ID |
| present | Int | 출석 횟수 |
| absent | Int | 결석 횟수 |
| late | Int | 지각 횟수 |
| excused | Int | 공결 횟수 |
| totalPenalty | Int | 총 패널티 합계 |
| deposit | Int? | 현재 보증금 잔액 (CohortMember 없으면 null) |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |

---

## 비즈니스 규칙

1. 회원의 전체 Attendance 레코드에서 상태별 count 집계
2. totalPenalty는 모든 출결의 penaltyAmount 합계
3. deposit은 현재 기수(11기) CohortMember의 잔액 (CohortMember가 없으면 null)
