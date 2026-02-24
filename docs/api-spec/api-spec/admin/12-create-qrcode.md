# POST /api/v1/admin/sessions/{sessionId}/qrcodes

## QR 코드 생성

해당 일정에 새 QR 코드를 생성합니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/admin/sessions/{sessionId}/qrcodes`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | Long | 일정 ID |

Request Body 없음

---

## Response

**Status**: `201 Created`

### Response Body

```json
{
  "success": true,
  "data": {
    "id": 1,
    "sessionId": 1,
    "hashValue": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-02-14T00:00:00Z",
    "expiresAt": "2026-02-15T00:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | QR 코드 ID |
| sessionId | Long | 일정 ID |
| hashValue | String | QR 해시값 (UUID) |
| createdAt | Instant | 생성일시 |
| expiresAt | Instant | 만료일시 (생성 후 24시간) |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| SESSION_NOT_FOUND | 404 | 해당 ID의 일정이 존재하지 않음 |
| QR_ALREADY_ACTIVE | 409 | 해당 일정에 이미 활성(미만료) QR 코드 존재 |

---

## 비즈니스 규칙

1. 일정 존재 검증
2. 해당 일정에 활성(expiresAt > 현재시각) QR 코드가 있으면 중복 생성 불가
3. UUID 기반 hashValue 생성
4. 유효기간: 생성 시각 + 24시간
