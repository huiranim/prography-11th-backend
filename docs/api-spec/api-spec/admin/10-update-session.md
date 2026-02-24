# PUT /api/v1/admin/sessions/{id}

## 일정 수정

일정 정보를 수정합니다. 모든 필드는 optional이며, 전달된 필드만 수정됩니다.

---

## Request

**Method**: `PUT`
**Path**: `/api/v1/admin/sessions/{id}`
**Content-Type**: `application/json`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 일정 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | X | 제목 |
| date | LocalDate | X | 날짜 (yyyy-MM-dd) |
| time | LocalTime | X | 시간 (HH:mm) |
| location | String | X | 장소 |
| status | SessionStatus | X | 상태 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) |

```json
{
  "title": "정기 모임 (변경)",
  "status": "IN_PROGRESS"
}
```

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "id": 1,
    "cohortId": 2,
    "title": "정기 모임 (변경)",
    "date": "2026-03-01",
    "time": "14:00:00",
    "location": "강남",
    "status": "IN_PROGRESS",
    "attendanceSummary": { "present": 0, "absent": 0, "late": 0, "excused": 0, "total": 0 },
    "qrActive": true,
    "createdAt": "2026-02-14T00:00:00Z",
    "updatedAt": "2026-02-14T01:00:00Z"
  },
  "error": null
}
```

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| SESSION_NOT_FOUND | 404 | 해당 ID의 일정이 존재하지 않음 |
| SESSION_ALREADY_CANCELLED | 400 | 이미 취소된 일정은 수정 불가 |
