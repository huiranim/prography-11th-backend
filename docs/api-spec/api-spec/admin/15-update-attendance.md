# PUT /api/v1/admin/attendances/{id}

## 출결 수정

기존 출결의 상태를 변경하고, 패널티 차이에 따라 보증금을 자동 조정합니다.

---

## Request

**Method**: `PUT`
**Path**: `/api/v1/admin/attendances/{id}`
**Content-Type**: `application/json`

### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 출결 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| status | AttendanceStatus | O | 변경할 출결 상태 |
| lateMinutes | Int | X | 지각 시간(분). min 0 |
| reason | String | X | 사유 (전달 시 덮어쓰기) |

```json
{
  "status": "EXCUSED",
  "lateMinutes": null,
  "reason": "병가"
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
    "sessionId": 1,
    "memberId": 1,
    "status": "EXCUSED",
    "lateMinutes": null,
    "penaltyAmount": 0,
    "reason": "병가",
    "checkedInAt": null,
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
| ATTENDANCE_NOT_FOUND | 404 | 해당 ID의 출결 기록이 없음 |
| COHORT_MEMBER_NOT_FOUND | 404 | 현재 기수의 기수회원 정보가 없음 |
| EXCUSE_LIMIT_EXCEEDED | 400 | EXCUSED 전환 시 공결 횟수 초과 (최대 3회) |
| DEPOSIT_INSUFFICIENT | 400 | 추가 패널티 차감 시 보증금 잔액 부족 |

---

## 비즈니스 규칙

### 패널티 차이 계산
```
이전 패널티: oldPenalty
새 패널티: newPenalty = calculatePenalty(newStatus, newLateMinutes)
차이: diff = newPenalty - oldPenalty
```

### EXCUSED 상태 전환
| 전환 | 동작 |
|------|------|
| 다른 상태 → EXCUSED | excuseCount < 3 검증 → excuseCount++ |
| EXCUSED → 다른 상태 | excuseCount-- (최소 0) |
| EXCUSED → EXCUSED | 변동 없음 |

### 보증금 자동 조정
| 조건 | 동작 |
|------|------|
| diff > 0 (패널티 증가) | CohortMember.deposit 추가 차감 + DepositHistory(PENALTY) |
| diff < 0 (패널티 감소) | CohortMember.deposit 환급 + DepositHistory(REFUND) |
| diff = 0 | 보증금 변동 없음 |

### 예시
- ABSENT(10,000원) → EXCUSED(0원): diff = -10,000 → 10,000원 환급
- PRESENT(0원) → LATE 5분(2,500원): diff = +2,500 → 2,500원 추가 차감
- LATE 10분(5,000원) → LATE 20분(10,000원): diff = +5,000 → 5,000원 추가 차감
