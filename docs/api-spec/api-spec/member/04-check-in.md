# POST /api/v1/attendances

## QR 출석 체크

QR 코드의 hashValue와 memberId를 전송하여 출석 체크를 수행합니다.

---

## Request

**Method**: `POST`
**Path**: `/api/v1/attendances`
**Content-Type**: `application/json`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| hashValue | String | O | QR 코드 해시값 |
| memberId | Long | O | 회원 ID |

```json
{
  "hashValue": "550e8400-e29b-41d4-a716-446655440000",
  "memberId": 1
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
    "id": 1,
    "sessionId": 1,
    "memberId": 1,
    "status": "PRESENT",
    "lateMinutes": null,
    "penaltyAmount": 0,
    "reason": null,
    "checkedInAt": "2026-03-01T05:00:00Z",
    "createdAt": "2026-03-01T05:00:00Z",
    "updatedAt": "2026-03-01T05:00:00Z"
  },
  "error": null
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 출결 ID |
| sessionId | Long | 일정 ID |
| memberId | Long | 회원 ID |
| status | AttendanceStatus | 출결 상태 (PRESENT 또는 LATE) |
| lateMinutes | Int? | 지각 시간(분). 정시 출석 시 null |
| penaltyAmount | Int | 패널티 금액 |
| reason | String? | 사유 (QR 체크인 시 null) |
| checkedInAt | Instant? | 체크인 시각 |
| createdAt | Instant | 생성일시 |
| updatedAt | Instant | 수정일시 |

---

## Error

| 에러 코드 | HTTP Status | 조건 |
|-----------|-------------|------|
| QR_INVALID | 400 | hashValue에 해당하는 QR 코드가 없음 |
| QR_EXPIRED | 400 | QR 코드가 만료됨 |
| SESSION_NOT_IN_PROGRESS | 400 | 일정이 IN_PROGRESS 상태가 아님 |
| MEMBER_NOT_FOUND | 404 | 회원이 존재하지 않음 |
| MEMBER_WITHDRAWN | 403 | 탈퇴한 회원 |
| ATTENDANCE_ALREADY_CHECKED | 409 | 해당 일정에 이미 출결 기록 존재 |
| COHORT_MEMBER_NOT_FOUND | 404 | 현재 기수의 기수회원 정보가 없음 |
| DEPOSIT_INSUFFICIENT | 400 | 보증금 잔액 부족 (패널티 차감 불가) |

---

## 비즈니스 규칙

검증 순서:

1. QR hashValue로 QrCode 조회 → 없으면 `QR_INVALID`
2. QR 만료 검증 (expiresAt < 현재 시각) → 만료면 `QR_EXPIRED`
3. QrCode의 sessionId로 Session 조회 → 상태가 IN_PROGRESS 아니면 `SESSION_NOT_IN_PROGRESS`
4. memberId로 회원 조회 → 없으면 `MEMBER_NOT_FOUND`
5. 회원 상태 WITHDRAWN 확인 → `MEMBER_WITHDRAWN`
6. (sessionId, memberId) 중복 출결 확인 → 이미 존재하면 `ATTENDANCE_ALREADY_CHECKED`
7. 현재 기수의 CohortMember 존재 확인 → 없으면 `COHORT_MEMBER_NOT_FOUND`
8. 지각 시간 계산:
   - `session.date + session.time` (Asia/Seoul 타임존) vs 현재 시각
   - 현재 > 일정시각 → **LATE** (지각분 = 차이(분))
   - 현재 <= 일정시각 → **PRESENT**
9. 패널티 계산:
   - PRESENT → 0원
   - LATE → min(지각분 × 500, 10,000)원
10. Attendance 저장 (qrCodeId, checkedInAt 포함)
11. 패널티 > 0이면:
    - CohortMember.deposit에서 차감
    - DepositHistory(type=PENALTY) 기록
    - 잔액 부족 시 `DEPOSIT_INSUFFICIENT`
