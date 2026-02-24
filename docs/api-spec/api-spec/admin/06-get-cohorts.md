# GET /api/v1/admin/cohorts

## 기수 목록 조회

전체 기수 목록을 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/cohorts`

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
      "generation": 10,
      "name": "10기",
      "createdAt": "2026-02-14T00:00:00Z"
    },
    {
      "id": 2,
      "generation": 11,
      "name": "11기",
      "createdAt": "2026-02-14T00:00:00Z"
    }
  ],
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 기수 ID |
| generation | Int | 기수 번호 |
| name | String | 기수명 |
| createdAt | Instant | 생성일시 |
