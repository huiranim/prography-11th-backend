# POST /api/v1/admin/sessions

## 일정 생성

새 일정을 생성합니다. QR 코드가 자동으로 함께 생성됩니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/admin/sessions`
**Content-Type**: `application/json`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 제목 |
| date | LocalDate | O | 날짜 (yyyy-MM-dd) |
| time | LocalTime | O | 시간 (HH:mm) |
| location | String | O | 장소 |

```json
{
  "title": "정기 모임",
  "date": "2026-03-01",
  "time": "14:00",
  "location": "강남"
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
    "cohortId": 2,
    "title": "정기 모임",
    "date": "2026-03-01",
    "time": "14:00:00",
    "location": "강남",
    "status": "SCHEDULED",
    "attendanceSummary": {
      "present": 0,
      "absent": 0,
      "late": 0,
      "excused": 0,
      "total": 0
    },
    "qrActive": true,
    "createdAt": "2026-02-14T00:00:00Z",
    "updatedAt": "2026-02-14T00:00:00Z"
  },
  "error": null
}
```

---

## 비즈니스 규칙

1. cohortId는 `current-cohort.generation=11` 설정에서 자동 결정
2. 초기 상태: SCHEDULED
3. UUID 기반 QR 코드 자동 생성 (유효기간 24시간)
