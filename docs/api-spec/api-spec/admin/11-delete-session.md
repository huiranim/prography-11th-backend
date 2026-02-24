# DELETE /api/v1/admin/sessions/{id}

## 일정 삭제 (취소)

일정을 Soft-delete 처리합니다. 실제 삭제가 아닌 상태를 CANCELLED로 변경합니다.

---

## Request

**Method**: `DELETE`
**Path**: `/api/v1/admin/sessions/{id}`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 일정 ID |

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
    "title": "정기 모임",
    "date": "2026-03-01",
    "time": "14:00:00",
    "location": "강남",
    "status": "CANCELLED",
    "attendanceSummary": { "present": 0, "absent": 0, "late": 0, "excused": 0, "total": 0 },
    "qrActive": false,
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
| SESSION_ALREADY_CANCELLED | 400 | 이미 취소된 일정 |
