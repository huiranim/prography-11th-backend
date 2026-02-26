# ERD — 프로그라피 출결 관리 시스템

## 다이어그램

```mermaid
erDiagram
    Member {
        bigint   id          PK
        varchar  loginId     UK "시스템 전체 중복 불가"
        varchar  password       "BCrypt 해싱"
        varchar  name
        varchar  phone
        enum     status         "ACTIVE | INACTIVE | WITHDRAWN"
        enum     role           "MEMBER | ADMIN"
        timestamp createdAt
        timestamp updatedAt
    }

    Cohort {
        bigint  id          PK
        int     generation  UK "기수 번호 (10, 11, ...)"
        varchar name           "ex) 11기"
    }

    Part {
        bigint  id        PK
        bigint  cohort_id FK
        varchar name         "SERVER | WEB | iOS | ANDROID | DESIGN"
    }

    Team {
        bigint  id        PK
        bigint  cohort_id FK
        varchar name         "ex) Team A"
    }

    CohortMember {
        bigint id           PK
        bigint member_id    FK
        bigint cohort_id    FK
        bigint part_id      FK "nullable"
        bigint team_id      FK "nullable"
        int    deposit         "보증금 잔액 (원)"
        int    excuseCount     "공결 사용 횟수 (기수당 최대 3회)"
    }

    Session {
        bigint  id        PK
        bigint  cohort_id FK
        varchar title
        date    date         "yyyy-MM-dd"
        time    time         "HH:mm:ss"
        varchar location
        enum    status       "SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED"
        timestamp createdAt
        timestamp updatedAt
    }

    QrCode {
        bigint    id         PK
        bigint    session_id FK
        varchar   hashValue  UK "UUID 기반"
        timestamp createdAt
        timestamp expiresAt     "생성 후 24시간"
    }

    Attendance {
        bigint    id           PK
        bigint    session_id   FK
        bigint    member_id    FK
        bigint    qrCode_id    FK "nullable (관리자 수동 등록 시 null)"
        enum      status          "PRESENT | ABSENT | LATE | EXCUSED"
        int       lateMinutes     "nullable (LATE일 때만 값 존재)"
        int       penaltyAmount   "패널티 금액 (원)"
        varchar   reason          "nullable (공결 사유 등)"
        timestamp checkedInAt     "nullable (QR 체크인 시각)"
        timestamp createdAt
        timestamp updatedAt
    }

    DepositHistory {
        bigint    id              PK
        bigint    cohortMember_id FK
        bigint    attendance_id   FK "nullable"
        enum      type               "INITIAL | PENALTY | REFUND"
        int       amount             "PENALTY: 음수, INITIAL/REFUND: 양수"
        int       balanceAfter       "변동 후 잔액"
        varchar   description        "nullable"
        timestamp createdAt
    }

    Cohort       ||--o{ Part         : "has"
    Cohort       ||--o{ Team         : "has"
    Cohort       ||--o{ CohortMember : "has"
    Cohort       ||--o{ Session      : "has"
    Member       ||--o{ CohortMember : "belongs to"
    Part         |o--o{ CohortMember : "assigned (optional)"
    Team         |o--o{ CohortMember : "assigned (optional)"
    Session      ||--o{ QrCode       : "has"
    Session      ||--o{ Attendance   : "has"
    Member       ||--o{ Attendance   : "has"
    QrCode       |o--o{ Attendance   : "used by (optional)"
    CohortMember ||--o{ DepositHistory : "has"
    Attendance   |o--o{ DepositHistory : "causes (optional)"
```

---

## 설계 결정 사항

### Member ↔ Cohort 다대다 → CohortMember 중간 테이블

한 회원이 여러 기수에 걸쳐 활동할 수 있으므로 CohortMember 테이블로 분리했습니다.
CohortMember는 단순 연결 테이블이 아니라 `deposit`, `excuseCount` 등 **기수별 독립 데이터**를 보유합니다.

### Part/Team → Cohort 직접 귀속

파트와 팀은 기수마다 새로 구성됩니다. 따라서 `cohort_id`를 FK로 갖고, CohortMember에서 Part/Team을 optional로 참조합니다.

### QrCode.hashValue → UUID

UUID 기반으로 생성하여 추측 불가능하고 시스템 전체에서 유일성이 보장됩니다.

### Attendance.qrCode_id → nullable

QR 체크인(자동)과 관리자 수동 등록 두 경로를 하나의 테이블에서 처리합니다.
QR 경로면 qrCode_id에 값이 있고, 관리자 수동 등록이면 null입니다.

### DepositHistory.amount 부호 규칙

| type    | amount  | 예시        |
|---------|---------|-------------|
| INITIAL | 양수    | +100,000    |
| PENALTY | 음수    | -10,000     |
| REFUND  | 양수    | +10,000     |

`balanceAfter`에 변동 후 실제 잔액을 기록하여 이력만으로도 잔액 추적이 가능합니다.
