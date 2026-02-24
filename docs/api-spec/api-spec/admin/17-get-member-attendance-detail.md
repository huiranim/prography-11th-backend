# GET /api/v1/admin/attendances/members/{memberId}

## 회원 출결 상세

특정 회원의 기수/파트/팀 정보 + 전체 출결 기록 + 보증금/공결 정보를 조회합니다.

---

## Request

**Method**: `GET`
**Path**: `/api/v1/admin/attendances/members/{memberId}`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| memberId | Long | 회원 ID |

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "memberId": 100,
    "memberName": "홍길동",
    "generation": 11,
    "partName": "SERVER",
    "teamName": "Team A",
    "deposit": 90000,
    "excuseCount": 1,
    "attendances": [
      {
        "id": 1,
        "sessionId": 1,
        "memberId": 100,
        "status": "PRESENT",
        "lateMinutes": null,
        "penaltyAmount": 0,
        "reason": null,
        "checkedInAt": "2026-03-01T05:00:00Z",
        "createdAt": "2026-03-01T05:00:00Z",
        "updatedAt": "2026-03-01T05:00:00Z"
      },
      {
        "id": 2,
        "sessionId": 2,
        "memberId": 100,
        "status": "EXCUSED",
        "lateMinutes": null,
        "penaltyAmount": 0,
        "reason": "병가",
        "checkedInAt": null,
        "createdAt": "2026-03-08T05:00:00Z",
        "updatedAt": "2026-03-08T05:00:00Z"
      }
    ]
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 회원 ID |
| memberName | String | 회원 이름 |
| generation | Int? | 기수 번호 |
| partName | String? | 파트명 |
| teamName | String? | 팀명 |
| deposit | Int? | 현재 보증금 잔액 |
| excuseCount | Int? | 공결 사용 횟수 |
| attendances | List | 전체 출결 기록 (AttendanceResponse 배열) |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 해당 ID의 회원이 존재하지 않음 |
