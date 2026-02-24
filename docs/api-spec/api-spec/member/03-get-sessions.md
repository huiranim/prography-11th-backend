# GET /api/v1/sessions

## 일정 목록 조회 (회원용)

현재 기수(11기)의 일정 목록을 조회합니다. CANCELLED 상태의 일정은 제외됩니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/sessions`

파라미터 없음

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
      "title": "정기 모임",
      "date": "2026-03-01",
      "time": "14:00:00",
      "location": "강남",
      "status": "SCHEDULED",
      "createdAt": "2026-02-14T00:00:00Z",
      "updatedAt": "2026-02-14T00:00:00Z"
    }
  ],
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 일정 ID |
| title | String | 제목 |
| date | LocalDate | 날짜 (yyyy-MM-dd) |
| time | LocalTime | 시간 (HH:mm:ss) |
| location | String | 장소 |
| status | SessionStatus | 상태 (SCHEDULED, IN_PROGRESS, COMPLETED) |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## 비즈니스 규칙

1. `current-cohort.generation=11` 설정에서 현재 기수 ID 결정
2. CANCELLED 상태의 일정은 결과에서 제외
