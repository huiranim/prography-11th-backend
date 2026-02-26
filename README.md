# 프로그라피 11기 BE 과제 — 출결 관리 시스템

프로그라피 세션 출결을 관리하는 Spring Boot REST API 서버입니다.

---

## 문서 읽는 순서

| 순서 | 문서 | 내용 |
|------|------|------|
| 1 | README.md (현재) | 실행 방법, API 목록, 프로젝트 구조 |
| 2 | [docs/ai-usage.md](docs/ai-usage.md) | AI 활용 방법과 협업 절차 |
| 3 | [docs/requirements-interpretation.md](docs/requirements-interpretation.md) | 명세 해석 및 판단 기록 |
| 4 | [docs/ERD.md](docs/ERD.md) | 데이터 모델 |
| 5 | [docs/system-design.md](docs/system-design.md) | 이상적 아키텍처 설계 |

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.3 |
| Database | H2 (인메모리) |
| ORM | Spring Data JPA / Hibernate |
| Password | BCrypt (cost factor 12) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Test | JUnit 5 + Mockito |
| Build | Maven |

---

## 실행 방법

### 사전 요구사항

- JDK 17 이상
- Maven 3.6 이상

### 빌드 및 실행

```bash
# 프로젝트 루트에서
mvn spring-boot:run
```

또는 JAR로 빌드 후 실행:

```bash
mvn package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

서버가 정상 실행되면 아래 메시지가 출력됩니다.

```
Started BackendApplication in X.XXX seconds
```

> 서버 시작 시 시드 데이터(기수, 파트, 팀, 관리자 계정)가 자동으로 로드됩니다.

---

## 접속 정보

| 항목 | URL |
|------|-----|
| API Base URL | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| H2 콘솔 | `http://localhost:8080/h2-console` |

### H2 콘솔 접속 설정

| 항목 | 값 |
|------|-----|
| JDBC URL | `jdbc:h2:mem:prography` |
| User Name | `sa` |
| Password | (비워두기) |

---

## 시드 데이터

서버 시작 시 아래 데이터가 자동으로 로드됩니다.

| 항목 | 내용 |
|------|------|
| 기수 | 10기 (id=1), 11기 (id=2, 현재 운영 기수) |
| 파트 | 기수별 SERVER / WEB / iOS / ANDROID / DESIGN (총 10개) |
| 팀 | 11기 Team A / Team B / Team C (총 3개) |
| 관리자 | loginId: `admin` / password: `admin1234` |
| 보증금 | 관리자 초기 보증금 100,000원 |

### 로그인 확인

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId": "admin", "password": "admin1234"}'
```

---

## API 목록

전체 API 명세는 서버 실행 후 **Swagger UI**(`http://localhost:8080/swagger-ui.html`)에서 확인할 수 있습니다.

### 필수 API (16개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 1 | POST | `/api/v1/auth/login` | 로그인 |
| 2 | GET | `/api/v1/members/{id}` | 회원 조회 |
| 3 | POST | `/api/v1/admin/members` | 회원 등록 |
| 4 | GET | `/api/v1/admin/members` | 회원 대시보드 (페이징+필터) |
| 5 | GET | `/api/v1/admin/members/{id}` | 회원 상세 |
| 6 | PUT | `/api/v1/admin/members/{id}` | 회원 수정 |
| 7 | DELETE | `/api/v1/admin/members/{id}` | 회원 탈퇴 (soft-delete) |
| 8 | GET | `/api/v1/admin/cohorts` | 기수 목록 |
| 9 | GET | `/api/v1/admin/cohorts/{cohortId}` | 기수 상세 |
| 10 | GET | `/api/v1/sessions` | 일정 목록 (회원용) |
| 11 | GET | `/api/v1/admin/sessions` | 일정 목록 (관리자) |
| 12 | POST | `/api/v1/admin/sessions` | 일정 생성 (QR 자동 생성) |
| 13 | PUT | `/api/v1/admin/sessions/{id}` | 일정 수정 |
| 14 | DELETE | `/api/v1/admin/sessions/{id}` | 일정 삭제 (soft-delete) |
| 15 | POST | `/api/v1/admin/sessions/{sessionId}/qrcodes` | QR 생성 |
| 16 | PUT | `/api/v1/admin/qrcodes/{qrCodeId}` | QR 갱신 |

### 가산점 API (9개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 17 | POST | `/api/v1/attendances` | QR 출석 체크 |
| 18 | GET | `/api/v1/attendances` | 내 출결 기록 |
| 19 | GET | `/api/v1/members/{memberId}/attendance-summary` | 출결 요약 |
| 20 | POST | `/api/v1/admin/attendances` | 출결 등록 (관리자) |
| 21 | PUT | `/api/v1/admin/attendances/{id}` | 출결 수정 |
| 22 | GET | `/api/v1/admin/attendances/sessions/{sessionId}/summary` | 일정별 출결 요약 |
| 23 | GET | `/api/v1/admin/attendances/members/{memberId}` | 회원 출결 상세 |
| 24 | GET | `/api/v1/admin/attendances/sessions/{sessionId}` | 일정별 출결 목록 |
| 25 | GET | `/api/v1/admin/cohort-members/{cohortMemberId}/deposits` | 보증금 이력 |

---

## 테스트 실행

```bash
mvn test
```

서비스 레이어 단위 테스트 46개가 실행됩니다.

```
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 응답 형식

모든 API는 아래 공통 형식으로 응답합니다.

**성공**
```json
{ "success": true, "data": { ... }, "error": null }
```

**실패**
```json
{ "success": false, "data": null, "error": { "code": "ERROR_CODE", "message": "메시지" } }
```

---

## 프로젝트 구조

```
src/main/java/com/prography/backend/
├── config/           # AppConfig (BCryptPasswordEncoder, 현재 기수 설정)
├── controller/       # REST 컨트롤러
├── domain/           # JPA 엔티티 + Enum
├── dto/
│   ├── request/      # 요청 DTO
│   └── response/     # 응답 DTO
├── exception/        # 에러 코드, 공통 예외 처리
├── infrastructure/   # DataInitializer (시드 데이터)
├── repository/       # Spring Data JPA 인터페이스
└── service/          # 비즈니스 로직

docs/
├── ERD.md                       # 엔티티 관계 다이어그램
├── system-design.md             # 이상적 아키텍처 설계
├── requirements-interpretation.md  # 명세 해석 및 결정 기록
└── api-spec/                    # 상세 API 명세
```

