# AI 사용사례

마감 이틀 전, 사이드 프로젝트를 찾아보다가 프로그라피 모집 공고를 발견했습니다. 활동 방식과 프로젝트 성격이 마음에 들었지만, 남은 시간 대비 과제 분량이 만만치 않았습니다.

주어진 기간 안에 완성도 있는 결과물을 제출하기 위해, AI를 단순한 코드 생성 도구가 아닌 **개발 파트너**로 전면 활용하기로 결정했습니다. 이 문서는 그 선택의 배경과 구체적인 활용 방법을 기록합니다.

---

## 사용 도구

| 도구 | 용도 |
|------|------|
| Claude Code (CLI) | 코드 생성, 리뷰, 문서 작성, 질의응답 |
| 모델 | claude-sonnet-4-6 |

---

## Claude Code 활용 절차

### Step 1. 컨텍스트 주입 — 과제를 AI에게 학습시키기

과제 명세, 에러 코드 목록, 비즈니스 규칙, 도메인 모델을 `CLAUDE.md`에 구조화하여 정리했습니다.
단순히 "출결 관리 API 만들어줘"가 아니라, AI가 명세를 정확히 이해한 상태에서 구현할 수 있도록 선행 작업으로 컨텍스트를 주입했습니다.
명세에 명시되지 않은 모호한 케이스 7건은 직접 판단하여 별도 기록했습니다.

```
CLAUDE.md 포함 내용:
- 전체 API 25개 목록 + 경로
- Enum 정의, 에러 코드 22개
- 핵심 비즈니스 규칙 (패널티 정책, QR 검증 순서 7단계, 공결 한도 등)
- 도메인 모델 9개 엔티티 정의
```

> 산출 파일: `CLAUDE.md`, `docs/requirements-interpretation.md`

### Step 2. 설계 — brainstorming → write-plan 스킬

Claude Code의 `superpowers:brainstorming` 스킬로 구현 전략을 먼저 탐색하고,
`superpowers:writing-plans` 스킬로 전체 구현을 12개 Task로 분해한 계획서를 작성했습니다.

```
Task 1.  프로젝트 기반 설정 (pom.xml, application.yml)
Task 2.  공통 응답 포맷 + 예외 처리
Task 3.  도메인 엔티티 + Enum
Task 4.  Repository 인터페이스
Task 5.  시드 데이터 (DataInitializer)
Task 6.  DTO (요청/응답)
Task 7~11. 서비스 + 컨트롤러 (Auth / Member / Cohort / Session+QR / Attendance)
Task 12. 최종 검증
```

> 산출 파일: `docs/plans/2026-02-24-implementation-plan.md`, `docs/plans/2026-02-24-system-design.md`

### Step 3. 구현 — execute-plan + TDD

`superpowers:executing-plans` 스킬 기반으로 Task 단위 순차 실행했습니다.
Auth 서비스는 TDD 방식을 적용했습니다. 테스트를 먼저 작성해 FAIL을 확인한 뒤 구현하고 PASS를 검증하는 순서를 거쳤습니다.

> 산출 파일: `src/main/java/com/prography/backend/` 전체 소스,
> TDD 선작성 테스트: `src/test/java/com/prography/backend/service/AuthServiceTest.java`

### Step 4. 테스트 커버리지 분석

1차 테스트 작성 완료(32개) 후, AI에게 "현재 테스트 클래스 5개로 모든 엔드포인트 검증이 가능한가?"를 질문했습니다.
AI가 누락 케이스를 분석했고, 이를 검토한 뒤 추가 테스트 13개를 요청해 최종 46개로 완성했습니다.

> 산출 파일:
> `src/test/java/com/prography/backend/service/AuthServiceTest.java` (4개)
> `src/test/java/com/prography/backend/service/MemberServiceTest.java` (11개)
> `src/test/java/com/prography/backend/service/CohortServiceTest.java` (3개)
> `src/test/java/com/prography/backend/service/SessionServiceTest.java` (5개)
> `src/test/java/com/prography/backend/service/AttendanceServiceTest.java` (18개)
> `src/test/java/com/prography/backend/service/QrCodeServiceTest.java` (5개, 커버리지 분석 후 신규 추가)

---

## AI가 수행한 작업

### 1. 프로젝트 초기 설정
`pom.xml`, `application.yml`, 메인 클래스 등 보일러플레이트 설정을 생성했습니다.
Spring Boot 3.x 의존성 조합(JPA, H2, Security Crypto, SpringDoc)을 올바르게 구성했습니다.

### 2. 도메인 모델 설계 및 구현
9개 엔티티(`Member`, `Cohort`, `Part`, `Team`, `CohortMember`, `Session`, `QrCode`, `Attendance`, `DepositHistory`)와 5개 Enum을 설계하고 JPA 어노테이션과 함께 구현했습니다.

### 3. 공통 인프라 구현
- 공통 응답 포맷 `ApiResponse<T>` (record 기반)
- 22개 에러 코드 `ErrorCode` Enum
- `GlobalExceptionHandler` (`@RestControllerAdvice`)

### 4. 전체 API 구현 (25개)
명세를 기반으로 서비스 레이어와 컨트롤러를 구현했습니다.
비즈니스 규칙이 복잡한 로직(QR 체크인 7단계 검증, 보증금 diff 계산, 메모리 후처리 페이지네이션 등)도 명세를 정확히 반영하여 구현했습니다.

### 5. 단위 테스트 작성 (46개)
TDD 방식으로 서비스 테스트를 작성했습니다. `@Mock` + `@InjectMocks` 패턴, primitive 타입 한계로 인한 `@BeforeEach` 직접 생성 방식 등을 적용했습니다.

### 6. 산출물 문서 작성
ERD(Mermaid erDiagram), System Design Architecture(Mermaid graph), requirements-interpretation.md, README.md를 작성했습니다.

---

## 개발자의 역할 — AI와의 협업 방식

핵심 원칙은 **"무엇을 AI에게 위임할지"를 의도적으로 결정**하는 것이었습니다.

### 정답이 명확한 반복 구현은 AI에게 위임

패턴이 동일하고 정답이 하나인 작업은 AI가 실수 없이 빠르게 처리할 수 있습니다.

- **보일러플레이트 코드**: 9개 Repository 인터페이스, 17개 DTO record 클래스처럼 패턴이 반복되는 코드
- **명세가 명확한 구현**: QR 체크인 7단계 검증 순서처럼 순서와 에러코드가 명시된 로직
- **개념 학습**: `@Transactional` + JPA 더티 체킹 원리, Mockito `@Mock`/`@InjectMocks` 동작 방식 등 즉각적인 설명이 필요한 개념

### 판단이 필요한 의사결정은 직접 수행

명세에 없거나 여러 해석이 가능한 경우, AI에게 구현을 맡기기 전에 직접 결정을 내렸습니다.

**예시 — admin의 기수 배정 문제:**
보증금(`deposit`)은 `Member`가 아닌 `CohortMember`에 있어 admin을 반드시 특정 기수에 배정해야 했지만, 명세에는 이에 대한 언급이 없었습니다. API 의존 관계를 분석해 "admin을 현재 운영 기수(11기)에 배정한다"고 결정한 뒤 AI에게 구현을 요청했습니다.

**예시 — 일정 삭제 시 활성 QR 처리:**
일정이 CANCELLED 될 때 활성 QR을 만료시킬지 명세에 없었습니다. QR 생성 API의 `QR_ALREADY_ACTIVE` 에러 코드 존재를 근거로 삼아 "즉시 만료 처리"가 올바른 동작임을 직접 판단했습니다.

이런 결정 7건을 `docs/requirements-interpretation.md`에 기록했습니다.

### 결과물 검증은 직접, 수정 방향 제시 후 재구현은 AI에게

AI가 생성한 결과물의 정합성은 직접 검토하고, 문제가 있으면 원인과 수정 방향을 제시한 뒤 AI가 재구현하게 했습니다.

**예시 — `@Mock int` primitive 타입 오류:**
테스트 실행 시 9개 전부 ERROR. 원인(`int` primitive는 Mockito가 Mock 불가)을 직접 파악한 뒤, `@BeforeEach`에서 직접 생성하는 방식으로 수정을 지시했습니다.

**예시 — 테스트 커버리지 누락:**
AI가 완성했다고 보고한 테스트를 검토해 QR 체크인 성공 케이스, `diff=0` 케이스, `QrCodeService` 파일 자체 누락 등을 직접 발견하고 추가 요청했습니다.

**예시 — System Design 다이어그램 오류:**
CDN dead-end, AUTH 서비스 DB 연결 누락, Primary-Replica 복제 관계 미표현 등 4가지 문제를 발견하고 수정을 지시했습니다.

---

## AI 활용 시 느낀 점

### 효과적이었던 것

- **반복적인 보일러플레이트 제거**: 9개 Repository 인터페이스, 17개 DTO 같은 패턴이 반복되는 코드는 AI가 훨씬 빠르게 처리했습니다.
- **명세 기반 구현의 정확성**: QR 체크인 7단계 검증 순서처럼 명세가 명확한 경우, 명세를 그대로 코드로 옮기는 작업에서 실수가 적었습니다.
- **즉각적인 질의응답**: `@Transactional` + JPA 더티 체킹의 동작 원리, Mockito의 Mock 동작 방식 등 개념 질문에 즉시 답변을 받을 수 있었습니다.

### 주의가 필요했던 것

- **AI는 누락을 인지하지 못한다**: 테스트 커버리지 누락, 다이어그램 오류 등 AI가 "완성했다"고 해도 직접 검토하지 않으면 발견하기 어려웠습니다.
- **모호한 명세는 직접 판단해야 한다**: AI는 명세에 없는 케이스에서 그럴듯하지만 틀린 답을 줄 수 있어, 최종 결정은 항상 직접 내렸습니다.
- **컨텍스트 한도**: 긴 세션에서 컨텍스트 한도에 도달하면 이전 내용을 잃어버려 MEMORY.md로 세션 간 정보를 유지했습니다.
