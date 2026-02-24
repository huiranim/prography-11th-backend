# GET /api/v1/admin/cohorts/{cohortId}

## 기수 상세 조회

기수 정보와 소속 파트/팀 목록을 함께 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/cohorts/{cohortId}`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| cohortId | Long | 기수 ID |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "id": 2,
    "generation": 11,
    "name": "11기",
    "parts": [
      { "id": 6, "name": "SERVER" },
      { "id": 7, "name": "WEB" },
      { "id": 8, "name": "iOS" },
      { "id": 9, "name": "ANDROID" },
      { "id": 10, "name": "DESIGN" }
    ],
    "teams": [
      { "id": 1, "name": "Team A" },
      { "id": 2, "name": "Team B" },
      { "id": 3, "name": "Team C" }
    ],
    "createdAt": "2026-02-14T00:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 기수 ID |
| generation | Int | 기수 번호 |
| name | String | 기수명 |
| parts | List | 소속 파트 목록 (id, name) |
| teams | List | 소속 팀 목록 (id, name) |
| createdAt | Instant | 생성일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| COHORT_NOT_FOUND | 404 | 해당 ID의 기수가 존재하지 않음 |
