# GET /api/v1/admin/members

## 회원 대시보드 조회

회원 목록을 페이징, 필터링, 검색 조건으로 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/members`

### Query Parameters

| 파라미터 | 타입 | 기본값 | 필수 | 설명 |
|----------|------|--------|------|------|
| page | Int | 0 | X | 페이지 번호 (0-based) |
| size | Int | 10 | X | 페이지 크기 |
| searchType | String | - | X | 검색 유형: `name`, `loginId`, `phone` |
| searchValue | String | - | X | 검색어 |
| generation | Int | - | X | 기수 필터 |
| partName | String | - | X | 파트명 필터 |
| teamName | String | - | X | 팀명 필터 |
| status | MemberStatus | - | X | 상태 필터 (ACTIVE, INACTIVE, WITHDRAWN) |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "loginId": "admin",
        "name": "관리자",
        "phone": "010-0000-0000",
        "status": "ACTIVE",
        "role": "ADMIN",
        "generation": 11,
        "partName": "SERVER",
        "teamName": "Team A",
        "deposit": 100000,
        "createdAt": "2026-02-14T00:00:00Z",
        "updatedAt": "2026-02-14T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null
}
```

### Content Item Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 회원 ID |
| loginId | String | 로그인 아이디 |
| name | String | 이름 |
| phone | String | 전화번호 |
| status | MemberStatus | 회원 상태 |
| role | MemberRole | 역할 |
| generation | Int? | 기수 번호 |
| partName | String? | 파트명 |
| teamName | String? | 팀명 |
| deposit | Int? | 보증금 잔액 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## 비즈니스 규칙

1. DB 레벨 필터: status, searchType+searchValue
2. 메모리 레벨 필터: generation, partName, teamName (CohortMember 기반 후처리)
3. 후처리 필터 적용 시 totalElements/totalPages는 필터 결과 기준으로 재계산
