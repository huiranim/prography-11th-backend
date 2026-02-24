# POST /api/v1/auth/login

## 로그인

loginId와 password로 회원 인증을 수행합니다. 토큰을 발급하지 않으며, 비밀번호 검증 결과만 반환합니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/auth/login`
**Content-Type**: `application/json`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | O | 로그인 아이디 |
| password | String | O | 비밀번호 |

```json
{
  "loginId": "admin",
  "password": "admin1234"
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
    "loginId": "admin",
    "name": "관리자",
    "phone": "010-0000-0000",
    "status": "ACTIVE",
    "role": "ADMIN",
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
| status | MemberStatus | 회원 상태 (ACTIVE, INACTIVE, WITHDRAWN) |
| role | MemberRole | 역할 (MEMBER, ADMIN) |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| LOGIN_FAILED | 401 | loginId가 존재하지 않거나 비밀번호 불일치 |
| MEMBER_WITHDRAWN | 403 | 탈퇴한 회원으로 로그인 시도 |

---

## 비즈니스 규칙

1. loginId로 회원 조회 → 없으면 `LOGIN_FAILED`
2. BCrypt 비밀번호 검증 → 불일치 시 `LOGIN_FAILED`
3. 회원 상태가 WITHDRAWN이면 `MEMBER_WITHDRAWN`
4. 토큰 발급 없음 — 회원 정보만 반환
