# Back-end 과제 안내

### 개요

<aside>
💡 프로그라피와 함께하려면 짧은 시간에 필요한 요구사항을 만들어내는 역량이 필요합니다. 

프로그라피 활동 진행 중 팀원들과 원활한 협업 및 프로젝트를 진행하기 위함과, 매니저들이 지원자에 따른 적합한 방향성 파악을 위함입니다.

</aside>

### 1. 제출양식 - Google Form

- 제출 링크 : [프로그라피 11기 지원서](https://docs.google.com/forms/d/1yr3OZokgdz63VCJmUfiJ_iRumQuFtQ3aCiIDyOLr9xw/viewform?edit_requested=true)
- 기한 : **2026.02.26 (목) 24:00 (꼭! 시간 엄수해 주세요.)**
- Google Form의 과제 제출 양식을 준수하여 Github 레포지토리 링크를 업로드 해 주세요.
- 과제 관련 문의가 있을 시 [Prography Discord - ⁉️│프로그라피-문의](https://discord.com/channels/1038300757734199377/1440312641376747601) 채널로 문의해주세요.

### 2. 제약사항

- **개발 환경**
    - JDK 17 이상의 적절한 JDK 를 사용해주세요.
    - Spring Boot Stack이 요구됩니다.
    - Database는 H2로 강제합니다.
- **GitHub**
    - **반드시 Public**으로 레포지토리 생성 후 과제를 진행해 주세요.
    - 레포지토리명 : **prography-11th-backend**
    - 과제 제출은 commit 시간 및 commit hash를 기준으로 하니 push나 Git Convention은 자유롭게 구성해주세요. **단, Google 설문지 기준, 제출 이후 commit은 채점에 포함되지 않습니다.**
- **README**
    - 여러분들의 고민했던 흔적이나 생각 등을 docs/ package를 만들어 남겨주세요.
    - 개발 환경 구성 후  **실행 방법**을 작성해 주세요. **(필수)**
    - 나머지 내용은 자유롭게 작성하셔도 됩니다. (선택)
        - 다만, 협업을 한다 생각하고 작성해주시면 감사하겠습니다!
- 요구 산출물
    - **(필수)** 잘 돌아가는 Source Code
        - 필수 API 16개 구현 완료
        - 가산점 API 9개 구현 완료
    - **(필수)** ERD
    - **(필수)** System Design Architecture
        - 현재 아키텍처와 동일하지 않아도 괜찮습니다.
        - 이 서비스의 이상적인 아키텍처를 자유롭게 그려주세요!
    - AI 사용사례
    - 생각을 남긴 docs

### 3. 과제

## **개요**

본 과제는 프로그라피 세션에 참여할 때의 출석 관리를 위한 출결관리 Backend입니다.

- 테스트 프레임워크, API 문서 도구 등은 자유롭게 선택하세요.
- 인증/인가는 구현하지 않습니다. 모든 API는 공개 접근 가능합니다.
- 로그인 API는 비밀번호 검증만 수행하며, 토큰을 발급하지 않습니다. 비밀번호는 BCrypt로 해싱합니다.
- **AI 의 사용을 전면 허용합니다.**

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

## API 명세

총 25개의 API를 구현합니다.

- 상세 API 명세의 경우, 아래 docs/api-spec repository의 명세 문서 확인 부탁드립니다.

### 필수 (16개)

인증, 회원/기수/일정/QR 관리 등 기본 CRUD API입니다.

- 상세 명세 문서는 위 요구사항 확인하여 작성 부탁드립니다.

| # | Method | Path | 설명 |
| --- | --- | --- | --- |
| 1 | POST | /auth/login | 로그인 |
| 2 | GET | /members/{id} | 회원 조회 |
| 3 | POST | /admin/members | 회원 등록 |
| 4 | GET | /admin/members | 회원 대시보드 |
| 5 | GET | /admin/members/{id} | 회원 상세 |
| 6 | PUT | /admin/members/{id} | 회원 수정 |
| 7 | DELETE | /admin/members/{id} | 회원 탈퇴 |
| 8 | GET | /admin/cohorts | 기수 목록 |
| 9 | GET | /admin/cohorts/{id} | 기수 상세 |
| 10 | GET | /sessions | 일정 목록 (회원) |
| 11 | GET | /admin/sessions | 일정 목록 (관리자) |
| 12 | POST | /admin/sessions | 일정 생성 |
| 13 | PUT | /admin/sessions/{id} | 일정 수정 |
| 14 | DELETE | /admin/sessions/{id} | 일정 삭제 |
| 15 | POST | /admin/sessions/{id}/qrcodes | QR 생성 |
| 16 | PUT | /admin/qrcodes/{id} | QR 갱신 |

### 가산점 (9개)

출결 처리, 보증금 자동 조정 등 비즈니스 로직이 포함된 API입니다.

- 상세 명세 문서는 위 요구사항 확인하여 작성 부탁드립니다.

| # | Method | Path | 설명 |
| --- | --- | --- | --- |
| 17 | POST | /attendances | QR 출석 체크 |
| 18 | GET | /attendances | 내 출결 기록 |
| 19 | GET | /members/{id}/attendance-summary | 내 출결 요약 |
| 20 | POST | /admin/attendances | 출결 등록 |
| 21 | PUT | /admin/attendances/{id} | 출결 수정 |
| 22 | GET | /admin/attendances/sessions/{id}/summary | 일정별 출결 요약 |
| 23 | GET | /admin/attendances/members/{id} | 회원 출결 상세 |
| 24 | GET | /admin/attendances/sessions/{id} | 일정별 출결 목록 |
| 25 | GET | /admin/cohort-members/{id}/deposits | 보증금 이력 |

---

## 시드 데이터

서버 시작 시 아래 데이터가 자동으로 세팅되어야 합니다.

| 데이터 | 내용 |
| --- | --- |
| 기수 | 10기, 11기 |
| 파트 | 기수별 SERVER, WEB, iOS, ANDROID, DESIGN (총 10개) |
| 팀 | 11기 Team A, Team B, Team C (총 3개) |
| 관리자 | loginId: `admin`, password: `admin1234`, role: ADMIN |
| 보증금 | 관리자 초기 보증금 100,000원 |

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
| --- | --- |
| PRESENT | 0원 |
| ABSENT | 10,000원 |
| LATE | min(지각분 × 500, 10,000)원 |
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

## 제출 요구사항

### 필수

1. 서버가 정상적으로 실행되어야 합니다.
2. 시드 데이터가 로드된 상태에서 `admin` / `admin1234`로 로그인이 가능해야 합니다.
3. **필수 API 16개**가 명세대로 동작해야 합니다.
4. 서비스 레이어에 대한 단위 테스트를 작성하세요.

### 가산점

1. **가산점 API 9개** 구현 (출결 + 보증금 도메인)
2. 가산점 API에 대한 단위 테스트 작성