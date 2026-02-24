# GET /api/v1/admin/members/{id}

## 회원 상세 조회

회원의 상세 정보를 기수/파트/팀 정보와 함께 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/members/{id}`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 회원 ID |

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
    "name": "홍길동",
    "phone": "010-1234-5678",
    "status": "ACTIVE",
    "role": "MEMBER",
    "generation": 11,
    "partName": "SERVER",
    "teamName": "Team A",
    "createdAt": "2026-02-14T00:00:00Z",
    "updatedAt": "2026-02-14T00:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 회원 ID |
| loginId | String | 로그인 아이디 |
| name | String | 이름 |
| phone | String | 전화번호 |
| status | MemberStatus | 회원 상태 |
| role | MemberRole | 역할 |
| generation | Int? | 기수 번호 (CohortMember 없으면 null) |
| partName | String? | 파트명 |
| teamName | String? | 팀명 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |
