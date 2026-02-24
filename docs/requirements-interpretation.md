# 요구사항 세부 해석

> 명세에 명시되지 않았거나 여러 해석이 가능한 항목들을 검토하고, 구현 방향을 결정한 기록입니다.

---

## 1. Admin의 CohortMember 소속 기수

### 배경

시드 데이터 명세에는 다음과 같이 기술되어 있습니다.

> 관리자 초기 보증금 100,000원

그런데 보증금(`deposit`)은 `Member` 엔티티가 아니라 `CohortMember` 엔티티에 있습니다.
`CohortMember`를 생성하려면 반드시 `Cohort`(기수)와 연결해야 합니다.
**"admin을 몇 기에 배정할 것인가?"** 는 명세에 명시되어 있지 않습니다.

### 검토

- admin이 `CohortMember` 없이 존재하면 보증금 이력 조회 API(`GET /admin/cohort-members/{cohortMemberId}/deposits`)를 호출할 `cohortMemberId` 자체가 존재하지 않게 됩니다.
- 명세에서 admin도 보증금 100,000원을 가진다고 명시했으므로, 반드시 CohortMember 레코드가 있어야 합니다.
- admin은 현재 운영 중인 기수를 관리하는 주체이므로, 현재 기수에 배정하는 것이 자연스럽습니다.

### 결정

**admin을 11기(현재 운영 기수, id=2)에 배정하여 CohortMember를 생성합니다.**

- part, team은 null (명세에서 선택 필드)
- deposit = 100,000원
- DepositHistory(type=INITIAL, amount=100,000, balanceAfter=100,000) 기록

---

## 2. 일정 삭제(CANCELLED) 시 활성 QR 코드 처리

### 배경

`DELETE /admin/sessions/{id}` 명세에는 다음만 명시되어 있습니다.

> 일정을 Soft-delete 처리합니다. 실제 삭제가 아닌 상태를 CANCELLED로 변경합니다.

QR 코드에 대한 후처리는 명시되어 있지 않습니다.
그런데 일정이 CANCELLED가 되었을 때 해당 일정의 활성 QR(`expiresAt > now()`)이 여전히 살아있다면 문제가 발생할 수 있습니다.

### 검토

QR 출석 체크 검증 순서를 보면:

```
1. QR hashValue 유효성 → QR_INVALID
2. QR 만료 여부     → QR_EXPIRED
3. 일정 IN_PROGRESS → SESSION_NOT_IN_PROGRESS  ← 여기서 막힘
```

일정이 CANCELLED 상태라면 3번 검증에서 `SESSION_NOT_IN_PROGRESS` 에러로 차단됩니다.
따라서 QR이 살아있더라도 실제로 출석 체크에 사용될 수는 없습니다.

그러나 이후 시나리오를 고려하면 명시적 만료 처리가 필요합니다:

- 관리자가 실수로 일정을 CANCELLED로 만들었다가 다시 SCHEDULED 혹은 IN_PROGRESS로 복구할 수 있습니다 (수정 API에서 status 변경 가능).
- 이 경우 원래 QR의 유효기간이 아직 남아있다면 의도치 않게 사용 가능한 QR이 남게 됩니다.
- 또한 QR 생성 API(`POST /admin/sessions/{sessionId}/qrcodes`) 호출 시, 기존 활성 QR이 남아있으면 `QR_ALREADY_ACTIVE` 에러가 발생하여 새 QR을 만들 수 없게 됩니다.

### 결정

**일정 삭제(CANCELLED 처리) 시, 해당 일정의 활성 QR(`expiresAt > now()`)을 즉시 만료 처리합니다.**

```
활성 QR 목록 조회 → 각 QR의 expiresAt = Instant.now() 로 설정
```

이렇게 하면:
- 취소된 일정의 QR이 절대 사용되지 않음
- 일정 복구 후 QR을 새로 만들 때 `QR_ALREADY_ACTIVE` 충돌 없음

---

## 3. EXCUSED → EXCUSED 출결 수정 시 처리

### 배경

출결 수정 API 명세에는 공결 횟수 처리가 다음과 같이 기술되어 있습니다.

> - 다른 상태 → EXCUSED: 공결 횟수 +1
> - EXCUSED → 다른 상태: 공결 횟수 -1

**"EXCUSED → EXCUSED"** 케이스는 명세에 없습니다.

### 검토

이 케이스는 출결 상태는 동일하지만 다른 필드(예: `reason`, `lateMinutes`)만 변경하는 경우입니다.

공결 횟수 관점:
- 기존 출결이 이미 EXCUSED이므로 excuseCount에 1이 반영된 상태입니다.
- 다시 EXCUSED로 저장해도 새로운 공결이 발생한 것이 아닙니다.
- +1 → -1 을 하면 결과는 동일하지만, 중간에 3회 한도 검증에 걸릴 위험이 있습니다.

패널티/보증금 관점:
- EXCUSED의 패널티는 0원, 기존 패널티도 0원입니다.
- `diff = 0 - 0 = 0` 이므로 보증금 변동이 없습니다.

### 결정

**EXCUSED → EXCUSED 수정 시, excuseCount 변동 없음. 보증금도 변동 없음.**

구현 조건:
```
oldStatus == EXCUSED && newStatus == EXCUSED → excuseCount 조작 없이 통과
```

---

## 4. 일정 생성 후 QR 생성 API 별도 호출 시 처리

### 배경

정책에는 다음이 명시되어 있습니다.

> 일정 생성 시 QR 코드가 자동으로 함께 생성됩니다.

그런데 API 목록에는 QR 코드를 수동으로 생성하는 API도 별도로 존재합니다.

> `POST /api/v1/admin/sessions/{sessionId}/qrcodes` — QR 생성

일정 생성 직후 이 API를 호출하면 이미 활성 QR이 있는 상태이므로 어떻게 응답해야 할지 명세에 없습니다.

또한 QR 코드 정책에는 다음이 있습니다.

> 하나의 일정에 활성(미만료) QR 코드는 1개만 존재할 수 있습니다.

### 검토

QR 생성 API의 에러 목록에 이미 다음이 포함되어 있습니다.

> `QR_ALREADY_ACTIVE` (409) — 해당 일정에 이미 활성화된 QR 코드 존재

이는 명세가 이 상황을 에러로 처리하도록 의도하고 있음을 명확히 보여줍니다.

QR 생성 API의 주 사용 목적:
1. 일정 생성 시 자동 생성된 QR의 유효기간(24시간)이 만료된 후 새 QR을 수동 생성
2. (QR 갱신 API와의 차이) 갱신은 기존 QR id를 알고 교체하는 것, 생성은 만료 후 새로 만드는 것

### 결정

**활성 QR이 있는 상태에서 QR 생성 API 호출 시 `QR_ALREADY_ACTIVE` (409)를 반환합니다.**

이는 명세에 명시된 에러 코드이므로 별도 해석 없이 명세를 그대로 따릅니다.

---

## 5. 회원 대시보드 필터링과 페이지네이션

### 배경

`GET /admin/members` 명세에는 다음과 같은 필터링 구분이 있습니다.

> - DB 레벨 필터: `status`, `searchType(name/loginId/phone) + searchValue`
> - 메모리 레벨 후처리: `generation`, `partName`, `teamName` (CohortMember 기반 후처리)
> - 후처리 필터 적용 시 totalElements/totalPages는 필터 결과 기준으로 재계산

`generation`, `partName`, `teamName` 정보는 `Member` 테이블이 아닌 `CohortMember` 테이블에 있습니다.
JOIN 쿼리로 DB 레벨에서 처리할 수도 있지만, 명세는 명시적으로 "메모리 레벨 후처리"를 지시하고 있습니다.

### 검토

메모리 후처리 방식의 특성:

1. DB에서 `status` + `searchType` 조건으로 1차 필터링 후 전체 조회
2. 각 `Member`에 대해 `CohortMember` 정보를 조회하여 `generation`, `partName`, `teamName` 확인
3. 조건에 맞지 않는 항목 제거
4. 최종 결과에서 `page`, `size` 기준으로 슬라이싱
5. `totalElements` = 최종 결과 수, `totalPages` = ceil(totalElements / size)

이 방식은 데이터가 많을수록 성능이 저하될 수 있지만, 명세가 명시한 방식이므로 그대로 따릅니다.
실제 운영 시 회원 수가 수백~수천 명 수준이라면 충분히 허용 가능한 방식입니다.

### 결정

**명세대로 메모리 후처리 방식을 사용하고, 후처리 후 totalElements와 totalPages를 재계산합니다.**

구현 흐름:
```
1. DB 조회 (status, searchType 필터) → List<Member> 전체
2. 각 Member의 최신 CohortMember 조회 → MemberDashboardResponse 변환
3. generation / partName / teamName 필터 적용 → 후처리 완료 목록
4. page * size ~ (page+1) * size 슬라이싱
5. totalElements = 후처리 목록 크기, totalPages = ceil(totalElements / size)
```

---

## 6. 출결 등록 시 회원 탈퇴 상태 검증

### 배경

QR 출석 체크 API 검증 순서에는 "회원 탈퇴 여부 → MEMBER_WITHDRAWN" 검증이 포함되어 있습니다.
그런데 관리자 수동 출결 등록 API(`POST /admin/attendances`) 명세의 에러 목록에는 `MEMBER_WITHDRAWN`이 없습니다.

### 검토

관리자 수동 등록은 관리자가 의도적으로 출결을 기록하는 행위입니다.
탈퇴한 회원의 출결을 등록하는 케이스가 실제로 발생할 수 있는지는 명세에 언급이 없습니다.

`MEMBER_WITHDRAWN` 에러가 에러 코드 목록에는 존재하지만, 관리자 수동 등록 명세의 에러 목록에는 포함되어 있지 않습니다.

### 결정

**관리자 수동 출결 등록 시 회원 탈퇴 검증은 수행하지 않습니다.**

명세의 에러 목록에 없으므로, 명세가 의도적으로 이 검증을 제외한 것으로 해석합니다.
관리자는 탈퇴 회원의 과거 출결도 사후 등록할 수 있는 권한이 있다고 판단합니다.

---

## 7. 지각 판정 기준 시각의 타임존

### 배경

명세에는 다음과 같이 기술되어 있습니다.

> 지각 판정: `일정 날짜 + 시간` 기준으로 현재 시각이 이후이면 LATE, 이전이면 PRESENT

타임존에 대한 명시가 없습니다.

### 검토

`Session`의 `date`(LocalDate)와 `time`(LocalTime)은 타임존 정보가 없는 순수 날짜/시간입니다.
일정은 한국(Asia/Seoul, UTC+9)에서 진행되므로, "14:00에 시작하는 모임"은 한국 시각 14:00을 의미합니다.

H2는 JVM 타임존을 따르므로, 서버 JVM 타임존 설정에 따라 결과가 달라질 수 있습니다.

### 결정

**지각 판정은 `Asia/Seoul` 타임존을 명시적으로 지정하여 수행합니다.**

```java
ZoneId SEOUL = ZoneId.of("Asia/Seoul");
ZonedDateTime sessionZdt = LocalDateTime.of(session.getDate(), session.getTime()).atZone(SEOUL);
ZonedDateTime nowZdt = ZonedDateTime.now(SEOUL);
```

JVM 타임존 설정과 무관하게 항상 한국 시각 기준으로 판정합니다.

---

## 변경 이력

| 날짜 | 항목 | 내용 |
|------|------|------|
| 2026-02-24 | 최초 작성 | 7개 항목 해석 및 결정 |
