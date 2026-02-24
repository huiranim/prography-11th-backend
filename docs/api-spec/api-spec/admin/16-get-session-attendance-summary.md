# GET /api/v1/admin/attendances/sessions/{sessionId}/summary

## 일정별 회원 출결 요약

해당 기수(11기) 전체 회원의 출결 통계를 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/attendances/sessions/{sessionId}/summary`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | Long | 일정 ID |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": [
    {
      "memberId": 1,
      "memberName": "관리자",
      "present": 5,
      "absent": 1,
      "late": 2,
      "excused": 0,
      "totalPenalty": 15000,
      "deposit": 85000
    },
    {
      "memberId": 100,
      "memberName": "홍길동",
      "present": 3,
      "absent": 0,
      "late": 1,
      "excused": 1,
      "totalPenalty": 5000,
      "deposit": 95000
    }
  ],
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 회원 ID |
| memberName | String | 회원 이름 |
| present | Int | 출석 횟수 |
| absent | Int | 결석 횟수 |
| late | Int | 지각 횟수 |
| excused | Int | 공결 횟수 |
| totalPenalty | Int | 총 패널티 합계 |
| deposit | Int | 현재 보증금 잔액 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| SESSION_NOT_FOUND | 404 | 해당 ID의 일정이 존재하지 않음 |

---

## 비즈니스 규칙

1. 현재 기수(11기)의 전체 CohortMember 목록 조회
2. 각 회원의 전체 Attendance 레코드에서 상태별 count 집계
3. totalPenalty = 전체 출결의 penaltyAmount 합계
4. deposit = CohortMember.deposit (현재 잔액)
