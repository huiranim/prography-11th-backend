# GET /api/v1/attendances

## 내 출결 기록 조회

특정 회원의 전체 출결 기록을 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/attendances`

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| memberId | Long | O | 회원 ID |

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
      "sessionId": 1,
      "sessionTitle": "정기 모임",
      "status": "PRESENT",
      "lateMinutes": null,
      "penaltyAmount": 0,
      "reason": null,
      "checkedInAt": "2026-03-01T05:00:00Z",
      "createdAt": "2026-03-01T05:00:00Z"
    }
  ],
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 출결 ID |
| sessionId | Long | 일정 ID |
| sessionTitle | String | 일정 제목 |
| status | AttendanceStatus | 출결 상태 (PRESENT, ABSENT, LATE, EXCUSED) |
| lateMinutes | Int? | 지각 시간(분) |
| penaltyAmount | Int | 패널티 금액 |
| reason | String? | 사유 |
| checkedInAt | Instant? | 체크인 시각 |
| createdAt | Instant | 생성일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |
