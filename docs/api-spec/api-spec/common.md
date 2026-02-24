# 공통 API 명세

## Base URL

```
http://localhost:8080/api/v1
```

---

## 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

### 실패 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 페이징 응답

페이징이 필요한 API의 `data`는 아래 형식을 따릅니다.

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| content | List | 데이터 목록 |
| page | Int | 현재 페이지 번호 (0-based) |
| size | Int | 페이지 크기 |
| totalElements | Long | 전체 데이터 수 |
| totalPages | Int | 전체 페이지 수 |

---

## ID 타입

모든 엔티티의 ID는 `Long` (auto-increment) 타입입니다.

---

## Enum

| Enum | 값 |
|------|-----|
| MemberStatus | ACTIVE, INACTIVE, WITHDRAWN |
| MemberRole | MEMBER, ADMIN |
| SessionStatus | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED |
| AttendanceStatus | PRESENT, ABSENT, LATE, EXCUSED |
| DepositType | INITIAL, PENALTY, REFUND |

---

## 에러 코드

| 코드 | HTTP Status | 메시지 |
|------|-------------|--------|
| INVALID_INPUT | 400 | 입력값이 올바르지 않습니다 |
| INTERNAL_ERROR | 500 | 서버 내부 오류가 발생했습니다 |
| LOGIN_FAILED | 401 | 로그인 아이디 또는 비밀번호가 올바르지 않습니다 |
| MEMBER_WITHDRAWN | 403 | 탈퇴한 회원입니다 |
| MEMBER_NOT_FOUND | 404 | 회원을 찾을 수 없습니다 |
| DUPLICATE_LOGIN_ID | 409 | 이미 사용 중인 로그인 아이디입니다 |
| MEMBER_ALREADY_WITHDRAWN | 400 | 이미 탈퇴한 회원입니다 |
| COHORT_NOT_FOUND | 404 | 기수를 찾을 수 없습니다 |
| PART_NOT_FOUND | 404 | 파트를 찾을 수 없습니다 |
| TEAM_NOT_FOUND | 404 | 팀을 찾을 수 없습니다 |
| COHORT_MEMBER_NOT_FOUND | 404 | 기수 회원 정보를 찾을 수 없습니다 |
| SESSION_NOT_FOUND | 404 | 일정을 찾을 수 없습니다 |
| SESSION_ALREADY_CANCELLED | 400 | 이미 취소된 일정입니다 |
| SESSION_NOT_IN_PROGRESS | 400 | 진행 중인 일정이 아닙니다 |
| QR_NOT_FOUND | 404 | QR 코드를 찾을 수 없습니다 |
| QR_INVALID | 400 | 유효하지 않은 QR 코드입니다 |
| QR_EXPIRED | 400 | 만료된 QR 코드입니다 |
| QR_ALREADY_ACTIVE | 409 | 이미 활성화된 QR 코드가 있습니다 |
| ATTENDANCE_NOT_FOUND | 404 | 출결 기록을 찾을 수 없습니다 |
| ATTENDANCE_ALREADY_CHECKED | 409 | 이미 출결 체크가 완료되었습니다 |
| EXCUSE_LIMIT_EXCEEDED | 400 | 공결 횟수를 초과했습니다 (최대 3회) |
| DEPOSIT_INSUFFICIENT | 400 | 보증금 잔액이 부족합니다 |
