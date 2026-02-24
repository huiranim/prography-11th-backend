# 프로그라피 11기 BE 과제 - 출결 관리 시스템

## 개요

프로그라피 세션 참여 시 출석 관리를 위한 출결관리 Backend를 구현하는 과제입니다.

> 프로그라피와 함께하려면 짧은 시간에 필요한 요구사항을 만들어내는 역량이 필요합니다.
> 프로그라피 활동 진행 중 팀원들과 원활한 협업 및 프로젝트를 진행하기 위함과, 매니저들이 지원자에 따른 적합한 방향성 파악을 위함입니다.

---

## 1. 제출 양식

- **제출 링크** : [프로그라피 11기 지원서](https://docs.google.com/forms/d/1yr3OZokgdz63VCJmUfiJ_iRumQuFtQ3aCiIDyOLr9xw/viewform?edit_requested=true)
- **기한** : **2026.02.26 (목) 24:00 (시간 엄수)**
- Google Form의 과제 제출 양식을 준수하여 GitHub 레포지토리 링크를 업로드해주세요.
- 과제 관련 문의: [Prography Discord - ⁉️│프로그라피-문의](https://discord.com/channels/1038300757734199377/1440312641376747601)

---

## 2. 제약사항

### 개발 환경

- **JDK 17 이상**의 적절한 JDK를 사용해주세요.
- **Spring Boot** Stack이 요구됩니다.
- Database는 **H2**로 강제합니다.

### GitHub

- **반드시 Public**으로 레포지토리 생성 후 과제를 진행해주세요.
- 레포지토리명 : **prography-11th-backend**
- 과제 제출은 commit 시간 및 commit hash를 기준으로 하니 push나 Git Convention은 자유롭게 구성해주세요.
- **단, Google 설문지 기준, 제출 이후 commit은 채점에 포함되지 않습니다.**

### README

- 여러분들의 고민했던 흔적이나 생각 등을 `docs/` package를 만들어 남겨주세요.
- 개발 환경 구성 후 **실행 방법**을 작성해주세요. **(필수)**
- 나머지 내용은 자유롭게 작성하셔도 됩니다. (선택)
    - 다만, 협업을 한다 생각하고 작성해주시면 감사하겠습니다!

### 요구 산출물

- **(필수)** 잘 돌아가는 Source Code
    - 필수 API 16개 구현 완료
    - 가산점 API 9개 구현 완료
- **(필수)** ERD
- **(필수)** System Design Architecture
    - 현재 아키텍처와 동일하지 않아도 괜찮습니다.
    - 이 서비스의 이상적인 아키텍처를 자유롭게 그려주세요!
- AI 사용사례
- 생각을 남긴 docs

---

## 3. 과제

### 핵심 사항

- 테스트 프레임워크, API 문서 도구 등은 자유롭게 선택하세요.
- **인증/인가는 구현하지 않습니다.** 모든 API는 공개 접근 가능합니다.
- 로그인 API는 비밀번호 검증만 수행하며, 토큰을 발급하지 않습니다. 비밀번호는 **BCrypt**로 해싱합니다.
- **AI의 사용을 전면 허용합니다.**

---

## 비즈니스 요구사항

### 배경

Prography는 기수제로 운영되는 IT 동아리입니다. 매 기수마다 정기 모임을 진행하며, 회원들의 출결을 관리해야 합니다. 현재 운영 중인 기수는 **11기**이며, 각 기수에는 파트(SERVER, WEB, iOS, ANDROID, DESIGN)와 팀이 존재합니다.

### 핵심 기능

**1. 회원 관리**

- 관리자가 회원을 등록하고, 기수/파트/팀에 배정합니다.
- 회원 등록 시 보증금 **100,000원**이 자동으로 설정됩니다.
- 회원 탈퇴는 실제 삭제가 아닌 상태 변경(Soft-delete)으로 처리합니다.

**2. 일정 및 QR 코드**

- 관리자가 정기 모임 일정을 생성하면, 출석용 QR 코드가 자동으로 함께 생성됩니다.
- 회원은 모임 현장에서 QR 코드를 스캔하여 출석 체크합니다.

**3. 출석 체크**

- QR 스캔 시 일정 시작 시간 기준으로 **출석(PRESENT)** 또는 **지각(LATE)** 이 판정됩니다.
- 관리자는 수동으로 출결을 등록하거나 기존 출결을 수정할 수 있습니다.

**4. 보증금 및 패널티**

- 결석/지각 시 패널티가 부과되고, 보증금에서 자동 차감됩니다.
- 출결 수정 시 패널티 차이만큼 보증금이 자동으로 조정(추가 차감 또는 환급)됩니다.

---

## API 명세

총 25개의 API를 구현합니다.

> 상세 API 명세는 [`api-spec/`](api-spec) 디렉토리의 명세 문서를 확인해주세요.

### 필수 (16개)

인증, 회원/기수/일정/QR 관리 등 기본 CRUD API입니다.

| # | Method | Path | 설명 | 명세 |
|---|--------|------|------|------|
| 1 | POST | `/api/v1/auth/login` | 로그인 | [01-login.md](api-spec/member/01-login.md) |
| 2 | GET | `/api/v1/members/{id}` | 회원 조회 | [02-get-member.md](api-spec/member/02-get-member.md) |
| 3 | POST | `/api/v1/admin/members` | 회원 등록 | [01-create-member.md](api-spec/admin/01-create-member.md) |
| 4 | GET | `/api/v1/admin/members` | 회원 대시보드 | [02-get-members-dashboard.md](api-spec/admin/02-get-members-dashboard.md) |
| 5 | GET | `/api/v1/admin/members/{id}` | 회원 상세 | [03-get-member-detail.md](api-spec/admin/03-get-member-detail.md) |
| 6 | PUT | `/api/v1/admin/members/{id}` | 회원 수정 | [04-update-member.md](api-spec/admin/04-update-member.md) |
| 7 | DELETE | `/api/v1/admin/members/{id}` | 회원 탈퇴 | [05-delete-member.md](api-spec/admin/05-delete-member.md) |
| 8 | GET | `/api/v1/admin/cohorts` | 기수 목록 | [06-get-cohorts.md](api-spec/admin/06-get-cohorts.md) |
| 9 | GET | `/api/v1/admin/cohorts/{cohortId}` | 기수 상세 | [07-get-cohort-detail.md](api-spec/admin/07-get-cohort-detail.md) |
| 10 | GET | `/api/v1/sessions` | 일정 목록 (회원) | [03-get-sessions.md](api-spec/member/03-get-sessions.md) |
| 11 | GET | `/api/v1/admin/sessions` | 일정 목록 (관리자) | [08-get-sessions.md](api-spec/admin/08-get-sessions.md) |
| 12 | POST | `/api/v1/admin/sessions` | 일정 생성 | [09-create-session.md](api-spec/admin/09-create-session.md) |
| 13 | PUT | `/api/v1/admin/sessions/{id}` | 일정 수정 | [10-update-session.md](api-spec/admin/10-update-session.md) |
| 14 | DELETE | `/api/v1/admin/sessions/{id}` | 일정 삭제 | [11-delete-session.md](api-spec/admin/11-delete-session.md) |
| 15 | POST | `/api/v1/admin/sessions/{sessionId}/qrcodes` | QR 생성 | [12-create-qrcode.md](api-spec/admin/12-create-qrcode.md) |
| 16 | PUT | `/api/v1/admin/qrcodes/{qrCodeId}` | QR 갱신 | [13-renew-qrcode.md](api-spec/admin/13-renew-qrcode.md) |

### 가산점 (9개)

출결 처리, 보증금 자동 조정 등 비즈니스 로직이 포함된 API입니다.

| # | Method | Path | 설명 | 명세 |
|---|--------|------|------|------|
| 17 | POST | `/api/v1/attendances` | QR 출석 체크 | [04-check-in.md](api-spec/member/04-check-in.md) |
| 18 | GET | `/api/v1/attendances` | 내 출결 기록 | [05-get-attendances.md](api-spec/member/05-get-attendances.md) |
| 19 | GET | `/api/v1/members/{memberId}/attendance-summary` | 내 출결 요약 | [06-get-attendance-summary.md](api-spec/member/06-get-attendance-summary.md) |
| 20 | POST | `/api/v1/admin/attendances` | 출결 등록 | [14-register-attendance.md](api-spec/admin/14-register-attendance.md) |
| 21 | PUT | `/api/v1/admin/attendances/{id}` | 출결 수정 | [15-update-attendance.md](api-spec/admin/15-update-attendance.md) |
| 22 | GET | `/api/v1/admin/attendances/sessions/{sessionId}/summary` | 일정별 출결 요약 | [16-get-session-attendance-summary.md](api-spec/admin/16-get-session-attendance-summary.md) |
| 23 | GET | `/api/v1/admin/attendances/members/{memberId}` | 회원 출결 상세 | [17-get-member-attendance-detail.md](api-spec/admin/17-get-member-attendance-detail.md) |
| 24 | GET | `/api/v1/admin/attendances/sessions/{sessionId}` | 일정별 출결 목록 | [18-get-session-attendances.md](api-spec/admin/18-get-session-attendances.md) |
| 25 | GET | `/api/v1/admin/cohort-members/{cohortMemberId}/deposits` | 보증금 이력 | [19-get-deposit-history.md](api-spec/admin/19-get-deposit-history.md) |

---

## 시드 데이터

서버 시작 시 아래 데이터가 자동으로 세팅되어야 합니다.

| 데이터 | 내용 |
|--------|------|
| 기수 | 10기, 11기 |
| 파트 | 기수별 SERVER, WEB, iOS, ANDROID, DESIGN (총 10개) |
| 팀 | 11기 Team A, Team B, Team C (총 3개) |
| 관리자 | loginId: `admin`, password: `admin1234`, role: ADMIN |
| 보증금 | 관리자 초기 보증금 100,000원 |

> 상세한 시드 데이터 구조는 [`seed-data.md`](seed-data.md)를 참조하세요.

---

## 정책 및 제한사항

### 1. 회원

- `loginId`는 시스템 전체에서 중복 불가합니다.
- 회원 탈퇴는 Soft-delete입니다. 상태를 `WITHDRAWN`으로 변경합니다.
- 회원 등록 시 기수(Cohort)에 배정하며, 보증금 100,000원이 자동 설정됩니다.

### 2. 기수

- 현재 운영 기수는 **11기로 고정**합니다.
- 일정 생성, 출결 조회 등에서 기수는 설정값으로 결정되며, API 파라미터로 받지 않습니다.

### 3. 일정

- 일정 생성 시 QR 코드가 자동으로 함께 생성됩니다.
- 일정 삭제는 Soft-delete입니다. 상태를 `CANCELLED`로 변경합니다.
- `CANCELLED` 상태의 일정은 수정할 수 없습니다.
- 회원용 일정 목록에서는 `CANCELLED` 상태가 제외됩니다.

### 4. QR 코드

- UUID 기반 hashValue를 가지며, 유효기간은 생성 시점으로부터 **24시간**입니다.
- 하나의 일정에 활성(미만료) QR 코드는 **1개만** 존재할 수 있습니다.
- QR 갱신 시 기존 QR을 즉시 만료시키고 새 QR을 생성합니다.

### 5. QR 출석 체크

아래 순서대로 검증합니다. 하나라도 실패하면 해당 에러를 반환합니다.

1. QR hashValue 유효성 → `QR_INVALID`
2. QR 만료 여부 → `QR_EXPIRED`
3. 일정 상태가 `IN_PROGRESS`인지 → `SESSION_NOT_IN_PROGRESS`
4. 회원 존재 여부 → `MEMBER_NOT_FOUND`
5. 회원 탈퇴 여부 → `MEMBER_WITHDRAWN`
6. 중복 출결 여부 (동일 일정 + 동일 회원) → `ATTENDANCE_ALREADY_CHECKED`
7. 기수 회원 정보 존재 여부 → `COHORT_MEMBER_NOT_FOUND`

검증 통과 후:

- **지각 판정**: `일정 날짜 + 시간` 기준으로 현재 시각이 이후이면 LATE, 이전이면 PRESENT
- 패널티 계산 후 Attendance 저장
- 패널티 발생 시 보증금 자동 차감

### 6. 패널티

| 출결 상태 | 패널티 |
|-----------|--------|
| PRESENT | 0원 |
| ABSENT | 10,000원 |
| LATE | min(지각분 x 500, 10,000)원 |
| EXCUSED | 0원 |

### 7. 보증금

- 초기 보증금: **100,000원** (회원 등록 시 자동 설정)
- 패널티 발생 시 보증금에서 차감하며, 이력(DepositHistory)을 기록합니다.
- 보증금 잔액보다 큰 패널티는 차감할 수 없습니다 → `DEPOSIT_INSUFFICIENT`
- 모든 보증금 변동(초기, 차감, 환급)은 이력에 기록되어야 합니다.

### 8. 출결 수정 시 보증금 자동 조정

출결 상태를 변경하면 이전 패널티와 새 패널티의 **차이만큼** 보증금이 조정됩니다.

- 패널티 증가 → 차액만큼 추가 차감 (DepositHistory: PENALTY)
- 패널티 감소 → 차액만큼 환급 (DepositHistory: REFUND)
- 패널티 동일 → 보증금 변동 없음

### 9. 공결 (EXCUSED)

- 기수당 최대 **3회**까지 허용됩니다.
- 3회 초과 시 → `EXCUSE_LIMIT_EXCEEDED`
- 다른 상태 → EXCUSED: 공결 횟수 +1
- EXCUSED → 다른 상태: 공결 횟수 -1

---

## 공통 API 명세

### Base URL

```
http://localhost:8080/api/v1
```

### 응답 형식

**성공**
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**실패**
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

**페이징**
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

### Enum 참조

| Enum | 값 |
|------|-----|
| MemberStatus | `ACTIVE`, `INACTIVE`, `WITHDRAWN` |
| MemberRole | `MEMBER`, `ADMIN` |
| SessionStatus | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| AttendanceStatus | `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| DepositType | `INITIAL`, `PENALTY`, `REFUND` |

### 에러 코드

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

---

## 제출 요구사항

### 필수

1. 서버가 정상적으로 실행되어야 합니다.
2. 시드 데이터가 로드된 상태에서 `admin` / `admin1234`로 로그인이 가능해야 합니다.
3. **필수 API 16개**가 명세대로 동작해야 합니다.
4. 서비스 레이어에 대한 단위 테스트를 작성하세요.

### 가산점

1. **가산점 API 9개** 구현 (출결 + 보증금 도메인)
2. 가산점 API에 대한 단위 테스트 작성

---

## 프로젝트 구조

```
├── api-spec/                  # API 명세 (구현 시 참고)
│   ├── common.md              # 공통 사항 (응답 형식, Enum, 에러 코드)
│   ├── member/                # 회원용 API (6개)
│   │   ├── 01-login.md
│   │   ├── 02-get-member.md
│   │   ├── 03-get-sessions.md
│   │   ├── 04-check-in.md
│   │   ├── 05-get-attendances.md
│   │   └── 06-get-attendance-summary.md
│   └── admin/                 # 관리자용 API (19개)
│       ├── 01-create-member.md ~ 19-get-deposit-history.md
├── seed-data.md               # 시드 데이터 명세
└── README.md
```

---

[prography 공식 홈페이지](https://prography.org)
