# POST /api/v1/admin/members

## 회원 등록

신규 회원을 등록하고, 기수에 배정하며, 보증금을 초기화합니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/admin/members`
**Content-Type**: `application/json`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | O | 로그인 아이디 |
| password | String | O | 비밀번호 |
| name | String | O | 이름 |
| phone | String | O | 전화번호 |
| cohortId | Long | O | 기수 ID |
| partId | Long | X | 파트 ID |
| teamId | Long | X | 팀 ID |

```json
{
  "loginId": "user1",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "cohortId": 2,
  "partId": 6,
  "teamId": 1
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
| status | MemberStatus | 회원 상태 (ACTIVE) |
| role | MemberRole | 역할 (MEMBER) |
| generation | Int? | 기수 번호 |
| partName | String? | 파트명 |
| teamName | String? | 팀명 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| DUPLICATE_LOGIN_ID | 409 | 이미 사용 중인 loginId |
| COHORT_NOT_FOUND | 404 | cohortId에 해당하는 기수 없음 |
| PART_NOT_FOUND | 404 | partId에 해당하는 파트 없음 |
| TEAM_NOT_FOUND | 404 | teamId에 해당하는 팀 없음 |

---

## 비즈니스 규칙

1. loginId 중복 검사
2. cohortId, partId, teamId 존재 검증
3. 비밀번호 BCrypt 해싱 (cost factor 12)
4. Member 생성 (status=ACTIVE, role=MEMBER)
5. CohortMember 생성 (deposit=100,000원)
6. DepositHistory 생성 (type=INITIAL, amount=100,000원)
