# GET /api/v1/admin/attendances/sessions/{sessionId}

## 일정별 출결 목록

특정 일정에 등록된 전체 출결 기록을 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/attendances/sessions/{sessionId}`

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
  "data": {
    "sessionId": 1,
    "sessionTitle": "정기 모임",
    "attendances": [
      {
        "id": 1,
        "sessionId": 1,
        "memberId": 1,
        "status": "PRESENT",
        "lateMinutes": null,
        "penaltyAmount": 0,
        "reason": null,
        "checkedInAt": "2026-03-01T05:00:00Z",
        "createdAt": "2026-03-01T05:00:00Z",
        "updatedAt": "2026-03-01T05:00:00Z"
      },
      {
        "id": 2,
        "sessionId": 1,
        "memberId": 100,
        "status": "LATE",
        "lateMinutes": 10,
        "penaltyAmount": 5000,
        "reason": null,
        "checkedInAt": "2026-03-01T05:10:00Z",
        "createdAt": "2026-03-01T05:10:00Z",
        "updatedAt": "2026-03-01T05:10:00Z"
      }
    ]
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| sessionId | Long | 일정 ID |
| sessionTitle | String | 일정 제목 |
| attendances | List | 출결 기록 배열 (AttendanceResponse) |

### AttendanceResponse Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 출결 ID |
| sessionId | Long | 일정 ID |
| memberId | Long | 회원 ID |
| status | AttendanceStatus | 출결 상태 (PRESENT, ABSENT, LATE, EXCUSED) |
| lateMinutes | Int? | 지각 시간(분) |
| penaltyAmount | Int | 패널티 금액 |
| reason | String? | 사유 |
| checkedInAt | Instant? | 체크인 시각 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| SESSION_NOT_FOUND | 404 | 해당 ID의 일정이 존재하지 않음 |
