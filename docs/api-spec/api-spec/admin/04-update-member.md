# PUT /api/v1/admin/members/{id}

## 회원 수정

회원 정보를 수정합니다. 모든 필드는 optional이며, 전달된 필드만 수정됩니다.

---

## Request

**Method**: `PUT`
**Path**: `/api/v1/admin/members/{id}`
**Content-Type**: `application/json`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 회원 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | String | X | 변경할 이름 |
| phone | String | X | 변경할 전화번호 |
| cohortId | Long | X | 변경할 기수 ID |
| partId | Long | X | 변경할 파트 ID |
| teamId | Long | X | 변경할 팀 ID |

```json
{
  "name": "새이름",
  "phone": "010-9999-9999",
  "cohortId": 2,
  "partId": 7,
  "teamId": 2
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
    "id": 100,
    "loginId": "user1",
    "name": "새이름",
    "phone": "010-9999-9999",
    "status": "ACTIVE",
    "role": "MEMBER",
    "generation": 11,
    "partName": "WEB",
    "teamName": "Team B",
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
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |
| COHORT_NOT_FOUND | 404 | cohortId에 해당하는 기수 없음 |
| PART_NOT_FOUND | 404 | partId에 해당하는 파트 없음 |
| TEAM_NOT_FOUND | 404 | teamId에 해당하는 팀 없음 |

---

## 비즈니스 규칙

1. name, phone은 전달 시 직접 수정
2. cohortId 전달 시:
   - 해당 기수의 CohortMember가 이미 존재하면 partId/teamId 업데이트
   - 존재하지 않으면 새 CohortMember 생성
3. cohortId/partId/teamId 존재 검증
