# PUT /api/v1/admin/qrcodes/{qrCodeId}

## QR 코드 갱신

기존 QR 코드를 즉시 만료시키고, 동일 일정에 새 QR 코드를 생성합니다.

---

## Request

**Method**: `PUT`
**Path**: `/api/v1/admin/qrcodes/{qrCodeId}`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| qrCodeId | Long | 기존 QR 코드 ID |

Request Body 없음

---

## Response

**Status**: `200 OK`

### Response Body

```json
{
  "success": true,
  "data": {
    "id": 2,
    "sessionId": 1,
    "hashValue": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "createdAt": "2026-02-14T01:00:00Z",
    "expiresAt": "2026-02-15T01:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 새로 생성된 QR 코드 ID |
| sessionId | Long | 일정 ID |
| hashValue | String | 새 QR 해시값 (UUID) |
| createdAt | Instant | 생성일시 |
| expiresAt | Instant | 만료일시 (생성 후 24시간) |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| QR_NOT_FOUND | 404 | 해당 ID의 QR 코드가 존재하지 않음 |

---

## 비즈니스 규칙

1. 기존 QR 코드의 expiresAt을 현재 시각으로 설정 (즉시 만료)
2. 동일 sessionId로 새 QR 코드 생성 (UUID hashValue, 24시간 유효)
3. 새로 생성된 QR 코드 정보 반환
