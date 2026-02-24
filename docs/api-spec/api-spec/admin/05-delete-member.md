# DELETE /api/v1/admin/members/{id}

## 회원 탈퇴

회원을 Soft-delete 처리합니다. 실제 삭제가 아닌 상태를 WITHDRAWN으로 변경합니다.

---

## Request

**Method**: `DELETE`
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
    "status": "WITHDRAWN",
    "updatedAt": "2026-02-14T01:00:00Z"
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
| status | MemberStatus | 회원 상태 (WITHDRAWN) |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |
| MEMBER_ALREADY_WITHDRAWN | 400 | 이미 탈퇴 처리된 회원 |
