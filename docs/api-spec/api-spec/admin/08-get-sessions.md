# GET /api/v1/admin/sessions

## 일정 목록 조회 (관리자용)

현재 기수(11기)의 일정 목록을 출결 요약 정보와 함께 조회합니다. CANCELLED 포함 모든 상태의 일정이 반환됩니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/sessions`

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| dateFrom | LocalDate | X | 시작 날짜 필터 (yyyy-MM-dd) |
| dateTo | LocalDate | X | 종료 날짜 필터 (yyyy-MM-dd) |
| status | SessionStatus | X | 상태 필터 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) |

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
      "cohortId": 2,
      "title": "정기 모임",
      "date": "2026-03-01",
      "time": "14:00:00",
      "location": "강남",
      "status": "SCHEDULED",
      "attendanceSummary": {
        "present": 10,
        "absent": 2,
        "late": 3,
        "excused": 1,
        "total": 16
      },
      "qrActive": true,
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
| cohortId | Long | 기수 ID |
| title | String | 제목 |
| date | LocalDate | 날짜 |
| time | LocalTime | 시간 |
| location | String | 장소 |
| status | SessionStatus | 상태 |
| attendanceSummary | Object | 출결 요약 |
| attendanceSummary.present | Int | 출석 수 |
| attendanceSummary.absent | Int | 결석 수 |
| attendanceSummary.late | Int | 지각 수 |
| attendanceSummary.excused | Int | 공결 수 |
| attendanceSummary.total | Int | 전체 출결 수 |
| qrActive | Boolean | 활성 QR 코드 존재 여부 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |
