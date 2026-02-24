# 출결 관리 시스템 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 프로그라피 11기 출결 관리 REST API 서버 (25개 API) 구현

**Architecture:** Spring Boot 3 + Spring Data JPA + H2 인메모리 DB. 레이어드 아키텍처 (controller → service → repository). 인증/인가 없음, 모든 API 공개.

**Tech Stack:** Java 21, Spring Boot 3.x, Maven, H2, Spring Data JPA, JPQL, SpringDoc OpenAPI 2.x, JUnit 5, Mockito, BCrypt (Spring Security Crypto)

---

## Task 1: Maven 프로젝트 초기화

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/prography/backend/BackendApplication.java`

**Step 1: pom.xml 생성**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/>
    </parent>

    <groupId>com.prography</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>backend</name>
    <description>Prography 11th Backend Assignment</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.5</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: application.yml 생성**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:prography;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect

app:
  current-cohort:
    generation: 11

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

**Step 3: BackendApplication.java 생성**

```java
package com.prography.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

**Step 4: 빌드 및 실행 확인**

```bash
cd /Users/n-hryu/Dev/workspace/homework/prography/11th-backend
mvn spring-boot:run
```

Expected: 서버 포트 8080에서 정상 시작

**Step 5: Commit**

```bash
git add pom.xml src/
git commit -m "chore: Spring Boot Maven 프로젝트 초기화"
```

---

## Task 2: 공통 인프라 — ApiResponse, ErrorCode, AppException

**Files:**
- Create: `src/main/java/com/prography/backend/common/ApiResponse.java`
- Create: `src/main/java/com/prography/backend/exception/ErrorCode.java`
- Create: `src/main/java/com/prography/backend/exception/AppException.java`
- Create: `src/main/java/com/prography/backend/exception/GlobalExceptionHandler.java`

**Step 1: ApiResponse.java**

```java
package com.prography.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    public record ErrorDetail(String code, String message) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<?> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }
}
```

**Step 2: ErrorCode.java**

```java
package com.prography.backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 아이디 또는 비밀번호가 올바르지 않습니다"),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 아이디입니다"),
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다"),
    COHORT_NOT_FOUND(HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다"),
    PART_NOT_FOUND(HttpStatus.NOT_FOUND, "파트를 찾을 수 없습니다"),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다"),
    COHORT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "기수 회원 정보를 찾을 수 없습니다"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다"),
    SESSION_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 취소된 일정입니다"),
    SESSION_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중인 일정이 아닙니다"),
    QR_NOT_FOUND(HttpStatus.NOT_FOUND, "QR 코드를 찾을 수 없습니다"),
    QR_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 QR 코드입니다"),
    QR_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 QR 코드입니다"),
    QR_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성화된 QR 코드가 있습니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출결 기록을 찾을 수 없습니다"),
    ATTENDANCE_ALREADY_CHECKED(HttpStatus.CONFLICT, "이미 출결 체크가 완료되었습니다"),
    EXCUSE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "공결 횟수를 초과했습니다 (최대 3회)"),
    DEPOSIT_INSUFFICIENT(HttpStatus.BAD_REQUEST, "보증금 잔액이 부족합니다");

    private final HttpStatus httpStatus;
    private final String message;
}
```

**Step 3: AppException.java**

```java
package com.prography.backend.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

**Step 4: GlobalExceptionHandler.java**

```java
package com.prography.backend.exception;

import com.prography.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
```

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: 공통 응답 래퍼, 에러 코드, 전역 예외 처리 추가"
```

---

## Task 3: 도메인 엔티티 생성

**Files:**
- Create: `src/main/java/com/prography/backend/domain/Member.java`
- Create: `src/main/java/com/prography/backend/domain/Cohort.java`
- Create: `src/main/java/com/prography/backend/domain/Part.java`
- Create: `src/main/java/com/prography/backend/domain/Team.java`
- Create: `src/main/java/com/prography/backend/domain/CohortMember.java`
- Create: `src/main/java/com/prography/backend/domain/Session.java`
- Create: `src/main/java/com/prography/backend/domain/QrCode.java`
- Create: `src/main/java/com/prography/backend/domain/Attendance.java`
- Create: `src/main/java/com/prography/backend/domain/DepositHistory.java`

**Step 1: Enum 타입들**

```java
// src/main/java/com/prography/backend/domain/MemberStatus.java
package com.prography.backend.domain;
public enum MemberStatus { ACTIVE, INACTIVE, WITHDRAWN }

// src/main/java/com/prography/backend/domain/MemberRole.java
package com.prography.backend.domain;
public enum MemberRole { MEMBER, ADMIN }

// src/main/java/com/prography/backend/domain/SessionStatus.java
package com.prography.backend.domain;
public enum SessionStatus { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }

// src/main/java/com/prography/backend/domain/AttendanceStatus.java
package com.prography.backend.domain;
public enum AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }

// src/main/java/com/prography/backend/domain/DepositType.java
package com.prography.backend.domain;
public enum DepositType { INITIAL, PENALTY, REFUND }
```

**Step 2: Member.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "members")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

**Step 3: Cohort.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "cohorts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int generation;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    private Instant createdAt;
}
```

**Step 4: Part.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Column(nullable = false)
    private String name;
}
```

**Step 5: Team.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Column(nullable = false)
    private String name;
}
```

**Step 6: CohortMember.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cohort_members")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    private int deposit;

    @Column(nullable = false)
    private int excuseCount;
}
```

**Step 7: Session.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

**Step 8: QrCode.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "qr_codes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, unique = true)
    private String hashValue;

    @CreationTimestamp
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
```

**Step 9: Attendance.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "attendances")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id")
    private QrCode qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    private Integer lateMinutes;

    @Column(nullable = false)
    private int penaltyAmount;

    private String reason;

    private Instant checkedInAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

**Step 10: DepositHistory.java**

```java
package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "deposit_histories")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_member_id", nullable = false)
    private CohortMember cohortMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositType type;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    private String description;

    @CreationTimestamp
    private Instant createdAt;
}
```

**Step 11: Commit**

```bash
git add src/main/java/com/prography/backend/domain/
git commit -m "feat: 도메인 엔티티 및 Enum 타입 추가"
```

---

## Task 4: Repository 인터페이스 생성

**Files:**
- Create: `src/main/java/com/prography/backend/repository/MemberRepository.java`
- Create: `src/main/java/com/prography/backend/repository/CohortRepository.java`
- Create: `src/main/java/com/prography/backend/repository/PartRepository.java`
- Create: `src/main/java/com/prography/backend/repository/TeamRepository.java`
- Create: `src/main/java/com/prography/backend/repository/CohortMemberRepository.java`
- Create: `src/main/java/com/prography/backend/repository/SessionRepository.java`
- Create: `src/main/java/com/prography/backend/repository/QrCodeRepository.java`
- Create: `src/main/java/com/prography/backend/repository/AttendanceRepository.java`
- Create: `src/main/java/com/prography/backend/repository/DepositHistoryRepository.java`

**Step 1: MemberRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);
    Optional<Member> findByLoginId(String loginId);
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);
    Page<Member> findByNameContaining(String name, Pageable pageable);
    Page<Member> findByLoginIdContaining(String loginId, Pageable pageable);
    Page<Member> findByPhoneContaining(String phone, Pageable pageable);
    Page<Member> findByStatusAndNameContaining(MemberStatus status, String name, Pageable pageable);
    Page<Member> findByStatusAndLoginIdContaining(MemberStatus status, String loginId, Pageable pageable);
    Page<Member> findByStatusAndPhoneContaining(MemberStatus status, String phone, Pageable pageable);
}
```

**Step 2: CohortRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByGeneration(int generation);
}
```

**Step 3: PartRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Cohort;
import com.prography.backend.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
    List<Part> findByCohort(Cohort cohort);
    List<Part> findByCohortId(Long cohortId);
}
```

**Step 4: TeamRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCohortId(Long cohortId);
}
```

**Step 5: CohortMemberRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.CohortMember;
import com.prography.backend.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
    Optional<CohortMember> findByMemberAndCohortId(Member member, Long cohortId);
    Optional<CohortMember> findByMemberIdAndCohortId(Long memberId, Long cohortId);
    List<CohortMember> findByCohortId(Long cohortId);

    @Query("SELECT cm FROM CohortMember cm WHERE cm.member.id = :memberId ORDER BY cm.cohort.generation DESC")
    List<CohortMember> findByMemberIdOrderByGenerationDesc(Long memberId);
}
```

**Step 6: SessionRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Session;
import com.prography.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByCohortIdAndStatusNot(Long cohortId, SessionStatus status);

    @Query("""
        SELECT s FROM Session s WHERE s.cohort.id = :cohortId
        AND (:status IS NULL OR s.status = :status)
        AND (:dateFrom IS NULL OR s.date >= :dateFrom)
        AND (:dateTo IS NULL OR s.date <= :dateTo)
        ORDER BY s.date DESC
        """)
    List<Session> findByCohortIdWithFilters(Long cohortId, SessionStatus status,
                                             LocalDate dateFrom, LocalDate dateTo);
}
```

**Step 7: QrCodeRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByHashValue(String hashValue);
    boolean existsBySessionIdAndExpiresAtAfter(Long sessionId, Instant now);
    List<QrCode> findBySessionIdAndExpiresAtAfter(Long sessionId, Instant now);
}
```

**Step 8: AttendanceRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsBySessionIdAndMemberId(Long sessionId, Long memberId);
    Optional<Attendance> findBySessionIdAndMemberId(Long sessionId, Long memberId);
    List<Attendance> findByMemberId(Long memberId);
    List<Attendance> findBySessionId(Long sessionId);
}
```

**Step 9: DepositHistoryRepository.java**

```java
package com.prography.backend.repository;

import com.prography.backend.domain.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
    List<DepositHistory> findByCohortMemberIdOrderByCreatedAtAsc(Long cohortMemberId);
}
```

**Step 10: Commit**

```bash
git add src/main/java/com/prography/backend/repository/
git commit -m "feat: Repository 인터페이스 추가"
```

---

## Task 5: 시드 데이터 초기화

**Files:**
- Create: `src/main/java/com/prography/backend/config/AppConfig.java`
- Create: `src/main/java/com/prography/backend/infrastructure/DataInitializer.java`

**Step 1: AppConfig.java**

```java
package com.prography.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AppConfig {

    @Value("${app.current-cohort.generation}")
    private int currentCohortGeneration;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public int currentCohortGeneration() {
        return currentCohortGeneration;
    }
}
```

**Step 2: DataInitializer.java**

```java
package com.prography.backend.infrastructure;

import com.prography.backend.domain.*;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (cohortRepository.count() > 0) return; // idempotent

        // 기수 생성
        Cohort cohort10 = cohortRepository.save(Cohort.builder()
                .generation(10).name("10기").build());
        Cohort cohort11 = cohortRepository.save(Cohort.builder()
                .generation(11).name("11기").build());

        // 파트 생성 (기수별 5개)
        String[] partNames = {"SERVER", "WEB", "iOS", "ANDROID", "DESIGN"};
        for (String partName : partNames) {
            partRepository.save(Part.builder().cohort(cohort10).name(partName).build());
            partRepository.save(Part.builder().cohort(cohort11).name(partName).build());
        }

        // 팀 생성 (11기만)
        String[] teamNames = {"Team A", "Team B", "Team C"};
        for (String teamName : teamNames) {
            teamRepository.save(Team.builder().cohort(cohort11).name(teamName).build());
        }

        // 관리자 생성
        Member admin = memberRepository.save(Member.builder()
                .loginId("admin")
                .password(passwordEncoder.encode("admin1234"))
                .name("관리자")
                .phone("010-0000-0000")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ADMIN)
                .build());

        // 관리자 CohortMember (11기)
        CohortMember adminCohortMember = cohortMemberRepository.save(CohortMember.builder()
                .member(admin)
                .cohort(cohort11)
                .deposit(100_000)
                .excuseCount(0)
                .build());

        // 보증금 초기 이력
        depositHistoryRepository.save(DepositHistory.builder()
                .cohortMember(adminCohortMember)
                .type(DepositType.INITIAL)
                .amount(100_000)
                .balanceAfter(100_000)
                .description("초기 보증금")
                .build());
    }
}
```

**Step 3: 서버 재시작 후 H2 콘솔 확인**

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:prography
```

**Step 4: Commit**

```bash
git add src/main/java/com/prography/backend/config/ src/main/java/com/prography/backend/infrastructure/
git commit -m "feat: AppConfig 및 시드 데이터 초기화 추가"
```

---

## Task 6: DTO 클래스 생성

**Files:**
- Create: `src/main/java/com/prography/backend/dto/request/` (요청 DTO들)
- Create: `src/main/java/com/prography/backend/dto/response/` (응답 DTO들)

**Step 1: 요청 DTO**

```java
// LoginRequest.java
package com.prography.backend.dto.request;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {}

// CreateMemberRequest.java
package com.prography.backend.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateMemberRequest(
    @NotBlank String loginId, @NotBlank String password,
    @NotBlank String name, @NotBlank String phone,
    @NotNull Long cohortId, Long partId, Long teamId) {}

// UpdateMemberRequest.java
package com.prography.backend.dto.request;
public record UpdateMemberRequest(String name, String phone, Long cohortId, Long partId, Long teamId) {}

// CreateSessionRequest.java
package com.prography.backend.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
public record CreateSessionRequest(
    @NotBlank String title, @NotNull LocalDate date,
    @NotNull LocalTime time, @NotBlank String location) {}

// UpdateSessionRequest.java
package com.prography.backend.dto.request;
import com.prography.backend.domain.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
public record UpdateSessionRequest(String title, LocalDate date, LocalTime time, String location, SessionStatus status) {}

// CheckInRequest.java
package com.prography.backend.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CheckInRequest(@NotBlank String hashValue, @NotNull Long memberId) {}

// RegisterAttendanceRequest.java
package com.prography.backend.dto.request;
import com.prography.backend.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
public record RegisterAttendanceRequest(
    @NotNull Long sessionId, @NotNull Long memberId,
    @NotNull AttendanceStatus status, Integer lateMinutes, String reason) {}

// UpdateAttendanceRequest.java
package com.prography.backend.dto.request;
import com.prography.backend.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
public record UpdateAttendanceRequest(@NotNull AttendanceStatus status, Integer lateMinutes, String reason) {}
```

**Step 2: 응답 DTO**

```java
// MemberResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.*;
import java.time.Instant;
public record MemberResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Instant createdAt, Instant updatedAt) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getLoginId(), m.getName(), m.getPhone(),
            m.getStatus(), m.getRole(), m.getCreatedAt(), m.getUpdatedAt());
    }
}

// MemberDetailResponse.java (with generation/part/team)
package com.prography.backend.dto.response;
import com.prography.backend.domain.*;
import java.time.Instant;
public record MemberDetailResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Integer generation, String partName, String teamName,
    Instant createdAt, Instant updatedAt) {}

// MemberDashboardResponse.java (with deposit)
package com.prography.backend.dto.response;
import com.prography.backend.domain.*;
import java.time.Instant;
public record MemberDashboardResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Integer generation, String partName, String teamName,
    Integer deposit, Instant createdAt, Instant updatedAt) {}

// DeleteMemberResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import java.time.Instant;
public record DeleteMemberResponse(Long id, String loginId, String name, MemberStatus status, Instant updatedAt) {
    public static DeleteMemberResponse from(Member m) {
        return new DeleteMemberResponse(m.getId(), m.getLoginId(), m.getName(), m.getStatus(), m.getUpdatedAt());
    }
}

// CohortResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.Cohort;
import java.time.Instant;
public record CohortResponse(Long id, int generation, String name, Instant createdAt) {
    public static CohortResponse from(Cohort c) {
        return new CohortResponse(c.getId(), c.getGeneration(), c.getName(), c.getCreatedAt());
    }
}

// CohortDetailResponse.java
package com.prography.backend.dto.response;
import java.time.Instant;
import java.util.List;
public record CohortDetailResponse(Long id, int generation, String name,
    List<PartInfo> parts, List<TeamInfo> teams, Instant createdAt) {
    public record PartInfo(Long id, String name) {}
    public record TeamInfo(Long id, String name) {}
}

// SessionResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
public record SessionResponse(Long id, Long cohortId, String title, LocalDate date, LocalTime time,
    String location, SessionStatus status, AttendanceSummary attendanceSummary,
    boolean qrActive, Instant createdAt, Instant updatedAt) {
    public record AttendanceSummary(int present, int absent, int late, int excused, int total) {}
}

// MemberSessionResponse.java (회원용 - cohortId, qrActive, attendanceSummary 없음)
package com.prography.backend.dto.response;
import com.prography.backend.domain.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
public record MemberSessionResponse(Long id, String title, LocalDate date, LocalTime time,
    String location, SessionStatus status, Instant createdAt, Instant updatedAt) {}

// QrCodeResponse.java
package com.prography.backend.dto.response;
import java.time.Instant;
public record QrCodeResponse(Long id, Long sessionId, String hashValue, Instant createdAt, Instant expiresAt) {}

// AttendanceResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.AttendanceStatus;
import java.time.Instant;
public record AttendanceResponse(Long id, Long sessionId, Long memberId, AttendanceStatus status,
    Integer lateMinutes, int penaltyAmount, String reason, Instant checkedInAt,
    Instant createdAt, Instant updatedAt) {}

// MyAttendanceResponse.java (sessionTitle 포함)
package com.prography.backend.dto.response;
import com.prography.backend.domain.AttendanceStatus;
import java.time.Instant;
public record MyAttendanceResponse(Long id, Long sessionId, String sessionTitle, AttendanceStatus status,
    Integer lateMinutes, int penaltyAmount, String reason, Instant checkedInAt, Instant createdAt) {}

// AttendanceSummaryResponse.java (회원 출결 요약)
package com.prography.backend.dto.response;
public record AttendanceSummaryResponse(Long memberId, int present, int absent, int late, int excused,
    int totalPenalty, Integer deposit) {}

// DepositHistoryResponse.java
package com.prography.backend.dto.response;
import com.prography.backend.domain.DepositType;
import java.time.Instant;
public record DepositHistoryResponse(Long id, Long cohortMemberId, DepositType type, int amount,
    int balanceAfter, Long attendanceId, String description, Instant createdAt) {}

// PageResponse.java
package com.prography.backend.dto.response;
import java.util.List;
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

// SessionAttendanceSummaryResponse.java
package com.prography.backend.dto.response;
public record SessionAttendanceSummaryResponse(Long memberId, String memberName, int present, int absent,
    int late, int excused, int totalPenalty, int deposit) {}

// MemberAttendanceDetailResponse.java
package com.prography.backend.dto.response;
import java.util.List;
public record MemberAttendanceDetailResponse(Long memberId, String memberName, Integer generation,
    String partName, String teamName, Integer deposit, Integer excuseCount, List<AttendanceResponse> attendances) {}

// SessionAttendancesResponse.java
package com.prography.backend.dto.response;
import java.util.List;
public record SessionAttendancesResponse(Long sessionId, String sessionTitle, List<AttendanceResponse> attendances) {}
```

**Step 3: Commit**

```bash
git add src/main/java/com/prography/backend/dto/
git commit -m "feat: 요청/응답 DTO 클래스 추가"
```

---

## Task 7: 인증 API (POST /api/v1/auth/login)

**Files:**
- Create: `src/main/java/com/prography/backend/service/AuthService.java`
- Create: `src/main/java/com/prography/backend/controller/AuthController.java`
- Create: `src/test/java/com/prography/backend/service/AuthServiceTest.java`

**Step 1: 테스트 작성 (TDD)**

```java
package com.prography.backend.service;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberRole;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock MemberRepository memberRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;

    @Test
    void login_success() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).role(MemberRole.ADMIN).build();
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("admin1234", "hashed")).thenReturn(true);

        Member result = authService.login(new LoginRequest("admin", "admin1234"));
        assertThat(result.getLoginId()).isEqualTo("admin");
    }

    @Test
    void login_failsWhenLoginIdNotFound() {
        when(memberRepository.findByLoginId("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pw")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    void login_failsWhenPasswordMismatch() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).build();
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    void login_failsWhenMemberWithdrawn() {
        Member member = Member.builder().loginId("user").password("hashed")
                .status(MemberStatus.WITHDRAWN).build();
        when(memberRepository.findByLoginId("user")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "pw")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_WITHDRAWN.getMessage());
    }
}
```

**Step 2: 테스트 실행 → FAIL 확인**

```bash
mvn test -Dtest=AuthServiceTest -pl .
```

**Step 3: AuthService.java 구현**

```java
package com.prography.backend.service;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Member login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AppException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.MEMBER_WITHDRAWN);
        }

        return member;
    }
}
```

**Step 4: AuthController.java 구현**

```java
package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.dto.response.MemberResponse;
import com.prography.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(MemberResponse.from(authService.login(request)));
    }
}
```

**Step 5: 테스트 재실행 → PASS 확인**

```bash
mvn test -Dtest=AuthServiceTest -pl .
```

**Step 6: Commit**

```bash
git add src/
git commit -m "feat: TDD로 로그인 API 구현"
```

---

## Task 8: 회원 관련 API (6개)

**Files:**
- Create: `src/main/java/com/prography/backend/service/MemberService.java`
- Create: `src/main/java/com/prography/backend/controller/MemberController.java`
- Create: `src/main/java/com/prography/backend/controller/AdminMemberController.java`
- Create: `src/test/java/com/prography/backend/service/MemberServiceTest.java`

**Step 1: MemberService.java 구현**

```java
package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int currentCohortGeneration;

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberDetailResponse createMember(CreateMemberRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new AppException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        Cohort cohort = cohortRepository.findById(request.cohortId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        Part part = null;
        if (request.partId() != null) {
            part = partRepository.findById(request.partId())
                    .orElseThrow(() -> new AppException(ErrorCode.PART_NOT_FOUND));
        }
        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        }

        Member member = memberRepository.save(Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.MEMBER)
                .build());

        CohortMember cohortMember = cohortMemberRepository.save(CohortMember.builder()
                .member(member).cohort(cohort).part(part).team(team)
                .deposit(100_000).excuseCount(0).build());

        depositHistoryRepository.save(DepositHistory.builder()
                .cohortMember(cohortMember).type(DepositType.INITIAL)
                .amount(100_000).balanceAfter(100_000).description("초기 보증금").build());

        return toMemberDetailResponse(member, cohortMember);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberDashboardResponse> getMembersDashboard(
            int page, int size, String searchType, String searchValue,
            Integer generation, String partName, String teamName, MemberStatus status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Member> memberPage;

        if (status != null && searchType != null && searchValue != null) {
            memberPage = switch (searchType) {
                case "name" -> memberRepository.findByStatusAndNameContaining(status, searchValue, pageable);
                case "loginId" -> memberRepository.findByStatusAndLoginIdContaining(status, searchValue, pageable);
                case "phone" -> memberRepository.findByStatusAndPhoneContaining(status, searchValue, pageable);
                default -> memberRepository.findByStatus(status, pageable);
            };
        } else if (status != null) {
            memberPage = memberRepository.findByStatus(status, pageable);
        } else if (searchType != null && searchValue != null) {
            memberPage = switch (searchType) {
                case "name" -> memberRepository.findByNameContaining(searchValue, pageable);
                case "loginId" -> memberRepository.findByLoginIdContaining(searchValue, pageable);
                case "phone" -> memberRepository.findByPhoneContaining(searchValue, pageable);
                default -> memberRepository.findAll(pageable);
            };
        } else {
            memberPage = memberRepository.findAll(pageable);
        }

        // 메모리 레벨 필터 (generation, partName, teamName)
        List<MemberDashboardResponse> filtered = memberPage.getContent().stream()
                .map(m -> {
                    List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(m.getId());
                    CohortMember latestCm = cms.isEmpty() ? null : cms.get(0);
                    return toMemberDashboardResponse(m, latestCm);
                })
                .filter(r -> generation == null || Objects.equals(r.generation(), generation))
                .filter(r -> partName == null || partName.equals(r.partName()))
                .filter(r -> teamName == null || teamName.equals(r.teamName()))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filtered.size() / size);
        List<MemberDashboardResponse> paged = filtered.stream()
                .skip((long) page * size).limit(size).collect(Collectors.toList());

        return new PageResponse<>(paged, page, size, filtered.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse getMemberDetail(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(id);
        CohortMember latestCm = cms.isEmpty() ? null : cms.get(0);
        return toMemberDetailResponse(member, latestCm);
    }

    @Transactional
    public MemberDetailResponse updateMember(Long id, UpdateMemberRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (request.name() != null) member.setName(request.name());
        if (request.phone() != null) member.setPhone(request.phone());

        CohortMember cohortMember = null;
        if (request.cohortId() != null) {
            Cohort cohort = cohortRepository.findById(request.cohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
            Part part = null;
            if (request.partId() != null) {
                part = partRepository.findById(request.partId())
                        .orElseThrow(() -> new AppException(ErrorCode.PART_NOT_FOUND));
            }
            Team team = null;
            if (request.teamId() != null) {
                team = teamRepository.findById(request.teamId())
                        .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
            }
            Optional<CohortMember> existing = cohortMemberRepository.findByMemberAndCohortId(member, request.cohortId());
            if (existing.isPresent()) {
                cohortMember = existing.get();
                cohortMember.setPart(part);
                cohortMember.setTeam(team);
            } else {
                cohortMember = cohortMemberRepository.save(CohortMember.builder()
                        .member(member).cohort(cohort).part(part).team(team)
                        .deposit(100_000).excuseCount(0).build());
            }
        } else {
            List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(id);
            cohortMember = cms.isEmpty() ? null : cms.get(0);
        }
        return toMemberDetailResponse(member, cohortMember);
    }

    @Transactional
    public DeleteMemberResponse deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }
        member.setStatus(MemberStatus.WITHDRAWN);
        return DeleteMemberResponse.from(member);
    }

    private MemberDetailResponse toMemberDetailResponse(Member m, CohortMember cm) {
        return new MemberDetailResponse(
                m.getId(), m.getLoginId(), m.getName(), m.getPhone(), m.getStatus(), m.getRole(),
                cm != null ? cm.getCohort().getGeneration() : null,
                cm != null && cm.getPart() != null ? cm.getPart().getName() : null,
                cm != null && cm.getTeam() != null ? cm.getTeam().getName() : null,
                m.getCreatedAt(), m.getUpdatedAt());
    }

    private MemberDashboardResponse toMemberDashboardResponse(Member m, CohortMember cm) {
        return new MemberDashboardResponse(
                m.getId(), m.getLoginId(), m.getName(), m.getPhone(), m.getStatus(), m.getRole(),
                cm != null ? cm.getCohort().getGeneration() : null,
                cm != null && cm.getPart() != null ? cm.getPart().getName() : null,
                cm != null && cm.getTeam() != null ? cm.getTeam().getName() : null,
                cm != null ? cm.getDeposit() : null,
                m.getCreatedAt(), m.getUpdatedAt());
    }
}
```

**Step 2: MemberController.java**

```java
package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.MemberResponse;
import com.prography.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getMember(@PathVariable Long id) {
        return ApiResponse.ok(memberService.getMember(id));
    }
}
```

**Step 3: AdminMemberController.java**

```java
package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberDetailResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.ok(memberService.createMember(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MemberDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) MemberStatus status) {
        return ApiResponse.ok(memberService.getMembersDashboard(
                page, size, searchType, searchValue, generation, partName, teamName, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberDetailResponse> getMemberDetail(@PathVariable Long id) {
        return ApiResponse.ok(memberService.getMemberDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberDetailResponse> updateMember(
            @PathVariable Long id, @RequestBody UpdateMemberRequest request) {
        return ApiResponse.ok(memberService.updateMember(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DeleteMemberResponse> deleteMember(@PathVariable Long id) {
        return ApiResponse.ok(memberService.deleteMember(id));
    }
}
```

**Step 4: MemberServiceTest.java 작성**

```java
// createMember_duplicateLoginId_throwsException 등 핵심 케이스 테스트
// 위 AuthServiceTest 패턴과 동일하게 @ExtendWith(MockitoExtension.class) 사용
```

**Step 5: 테스트 실행**

```bash
mvn test -Dtest=MemberServiceTest -pl .
```

**Step 6: Commit**

```bash
git add src/
git commit -m "feat: 회원 CRUD API 구현"
```

---

## Task 9: 기수 API (2개)

**Files:**
- Create: `src/main/java/com/prography/backend/service/CohortService.java`
- Create: `src/main/java/com/prography/backend/controller/AdminCohortController.java`

**Step 1: CohortService.java**

```java
package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<CohortResponse> getCohorts() {
        return cohortRepository.findAll().stream()
                .map(CohortResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CohortDetailResponse getCohortDetail(Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        List<CohortDetailResponse.PartInfo> parts = partRepository.findByCohortId(cohortId).stream()
                .map(p -> new CohortDetailResponse.PartInfo(p.getId(), p.getName())).toList();
        List<CohortDetailResponse.TeamInfo> teams = teamRepository.findByCohortId(cohortId).stream()
                .map(t -> new CohortDetailResponse.TeamInfo(t.getId(), t.getName())).toList();
        return new CohortDetailResponse(cohort.getId(), cohort.getGeneration(), cohort.getName(),
                parts, teams, cohort.getCreatedAt());
    }
}
```

**Step 2: AdminCohortController.java**

```java
package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.CohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cohorts")
@RequiredArgsConstructor
public class AdminCohortController {

    private final CohortService cohortService;

    @GetMapping
    public ApiResponse<List<CohortResponse>> getCohorts() {
        return ApiResponse.ok(cohortService.getCohorts());
    }

    @GetMapping("/{cohortId}")
    public ApiResponse<CohortDetailResponse> getCohortDetail(@PathVariable Long cohortId) {
        return ApiResponse.ok(cohortService.getCohortDetail(cohortId));
    }
}
```

**Step 3: Commit**

```bash
git add src/
git commit -m "feat: 기수 조회 API 구현"
```

---

## Task 10: 일정 + QR API (7개)

**Files:**
- Create: `src/main/java/com/prography/backend/service/SessionService.java`
- Create: `src/main/java/com/prography/backend/service/QrCodeService.java`
- Create: `src/main/java/com/prography/backend/controller/SessionController.java`
- Create: `src/main/java/com/prography/backend/controller/AdminSessionController.java`
- Create: `src/main/java/com/prography/backend/controller/AdminQrCodeController.java`
- Create: `src/test/java/com/prography/backend/service/SessionServiceTest.java`

**Step 1: SessionService.java**

```java
package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final CohortRepository cohortRepository;
    private final QrCodeRepository qrCodeRepository;
    private final AttendanceRepository attendanceRepository;
    private final int currentCohortGeneration;

    private Cohort getCurrentCohort() {
        return cohortRepository.findByGeneration(currentCohortGeneration)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<MemberSessionResponse> getMemberSessions() {
        Cohort cohort = getCurrentCohort();
        return sessionRepository.findByCohortIdAndStatusNot(cohort.getId(), SessionStatus.CANCELLED)
                .stream().map(this::toMemberSessionResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getAdminSessions(SessionStatus status,
                                                    java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        Cohort cohort = getCurrentCohort();
        return sessionRepository.findByCohortIdWithFilters(cohort.getId(), status, dateFrom, dateTo)
                .stream().map(this::toSessionResponse).toList();
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        Cohort cohort = getCurrentCohort();
        Session session = sessionRepository.save(Session.builder()
                .cohort(cohort).title(request.title()).date(request.date())
                .time(request.time()).location(request.location())
                .status(SessionStatus.SCHEDULED).build());

        // QR 자동 생성
        qrCodeRepository.save(QrCode.builder()
                .session(session).hashValue(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(86400)).build());

        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse updateSession(Long id, UpdateSessionRequest request) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new AppException(ErrorCode.SESSION_ALREADY_CANCELLED);
        }
        if (request.title() != null) session.setTitle(request.title());
        if (request.date() != null) session.setDate(request.date());
        if (request.time() != null) session.setTime(request.time());
        if (request.location() != null) session.setLocation(request.location());
        if (request.status() != null) session.setStatus(request.status());
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new AppException(ErrorCode.SESSION_ALREADY_CANCELLED);
        }
        session.setStatus(SessionStatus.CANCELLED);
        // 활성 QR 만료 처리
        qrCodeRepository.findBySessionIdAndExpiresAtAfter(session.getId(), Instant.now())
                .forEach(qr -> qr.setExpiresAt(Instant.now()));
        return toSessionResponse(session);
    }

    private SessionResponse toSessionResponse(Session s) {
        List<Attendance> attendances = attendanceRepository.findBySessionId(s.getId());
        long present = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long late = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long excused = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
        boolean qrActive = qrCodeRepository.existsBySessionIdAndExpiresAtAfter(s.getId(), Instant.now());

        return new SessionResponse(s.getId(), s.getCohort().getId(), s.getTitle(), s.getDate(), s.getTime(),
                s.getLocation(), s.getStatus(),
                new SessionResponse.AttendanceSummary((int)present, (int)absent, (int)late, (int)excused, attendances.size()),
                qrActive, s.getCreatedAt(), s.getUpdatedAt());
    }

    private MemberSessionResponse toMemberSessionResponse(Session s) {
        return new MemberSessionResponse(s.getId(), s.getTitle(), s.getDate(), s.getTime(),
                s.getLocation(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
```

**Step 2: QrCodeService.java**

```java
package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.response.QrCodeResponse;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public QrCodeResponse createQrCode(Long sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (qrCodeRepository.existsBySessionIdAndExpiresAtAfter(sessionId, Instant.now())) {
            throw new AppException(ErrorCode.QR_ALREADY_ACTIVE);
        }
        QrCode qr = qrCodeRepository.save(QrCode.builder()
                .session(sessionRepository.getReferenceById(sessionId))
                .hashValue(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(86400)).build());
        return toResponse(qr);
    }

    @Transactional
    public QrCodeResponse renewQrCode(Long qrCodeId) {
        QrCode oldQr = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new AppException(ErrorCode.QR_NOT_FOUND));
        oldQr.setExpiresAt(Instant.now()); // 즉시 만료
        QrCode newQr = qrCodeRepository.save(QrCode.builder()
                .session(oldQr.getSession())
                .hashValue(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(86400)).build());
        return toResponse(newQr);
    }

    private QrCodeResponse toResponse(QrCode qr) {
        return new QrCodeResponse(qr.getId(), qr.getSession().getId(),
                qr.getHashValue(), qr.getCreatedAt(), qr.getExpiresAt());
    }
}
```

**Step 3: Controllers**

```java
// SessionController.java
@RestController @RequestMapping("/api/v1/sessions") @RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    @GetMapping
    public ApiResponse<List<MemberSessionResponse>> getSessions() {
        return ApiResponse.ok(sessionService.getMemberSessions());
    }
}

// AdminSessionController.java
@RestController @RequestMapping("/api/v1/admin/sessions") @RequiredArgsConstructor
public class AdminSessionController {
    private final SessionService sessionService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getSessions(
            @RequestParam(required=false) SessionStatus status,
            @RequestParam(required=false) LocalDate dateFrom,
            @RequestParam(required=false) LocalDate dateTo) {
        return ApiResponse.ok(sessionService.getAdminSessions(status, dateFrom, dateTo));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest req) {
        return ApiResponse.ok(sessionService.createSession(req));
    }
    @PutMapping("/{id}")
    public ApiResponse<SessionResponse> updateSession(@PathVariable Long id, @RequestBody UpdateSessionRequest req) {
        return ApiResponse.ok(sessionService.updateSession(id, req));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<SessionResponse> deleteSession(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.deleteSession(id));
    }
    @PostMapping("/{sessionId}/qrcodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrCodeResponse> createQrCode(@PathVariable Long sessionId) {
        return ApiResponse.ok(qrCodeService.createQrCode(sessionId));
    }
}

// AdminQrCodeController.java
@RestController @RequestMapping("/api/v1/admin/qrcodes") @RequiredArgsConstructor
public class AdminQrCodeController {
    private final QrCodeService qrCodeService;
    @PutMapping("/{qrCodeId}")
    public ApiResponse<QrCodeResponse> renewQrCode(@PathVariable Long qrCodeId) {
        return ApiResponse.ok(qrCodeService.renewQrCode(qrCodeId));
    }
}
```

**Step 4: SessionServiceTest 작성 및 실행**

```bash
mvn test -Dtest=SessionServiceTest -pl .
```

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: 일정 및 QR 코드 API 구현"
```

---

## Task 11: 출결 API (가산점 9개)

**Files:**
- Create: `src/main/java/com/prography/backend/service/AttendanceService.java`
- Create: `src/main/java/com/prography/backend/controller/AttendanceController.java`
- Create: `src/main/java/com/prography/backend/controller/AdminAttendanceController.java`
- Create: `src/test/java/com/prography/backend/service/AttendanceServiceTest.java`

**Step 1: PenaltyCalculator 유틸**

```java
package com.prography.backend.service;

import com.prography.backend.domain.AttendanceStatus;

public class PenaltyCalculator {
    public static int calculate(AttendanceStatus status, Integer lateMinutes) {
        return switch (status) {
            case PRESENT, EXCUSED -> 0;
            case ABSENT -> 10_000;
            case LATE -> Math.min((lateMinutes != null ? lateMinutes : 0) * 500, 10_000);
        };
    }
}
```

**Step 2: AttendanceServiceTest.java (핵심 케이스)**

```java
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks AttendanceService attendanceService;
    @Mock QrCodeRepository qrCodeRepository;
    @Mock SessionRepository sessionRepository;
    @Mock MemberRepository memberRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock CohortMemberRepository cohortMemberRepository;
    @Mock DepositHistoryRepository depositHistoryRepository;

    @Test
    void penalty_present_isZero() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.PRESENT, null)).isEqualTo(0);
    }

    @Test
    void penalty_absent_is10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.ABSENT, null)).isEqualTo(10_000);
    }

    @Test
    void penalty_late19min_is9500() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 19)).isEqualTo(9_500);
    }

    @Test
    void penalty_late20min_is10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 20)).isEqualTo(10_000);
    }

    @Test
    void penalty_late25min_cappedAt10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 25)).isEqualTo(10_000);
    }

    @Test
    void penalty_excused_isZero() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.EXCUSED, null)).isEqualTo(0);
    }

    // QR 체크인 검증 순서 테스트
    @Test
    void checkIn_QR_INVALID_whenHashNotFound() {
        when(qrCodeRepository.findByHashValue("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest("invalid", 1L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException)e).getErrorCode())
                .isEqualTo(ErrorCode.QR_INVALID);
    }

    // 출결 수정 - 보증금 조정 테스트
    @Test
    void updateAttendance_penalty_increases_deductsDeposit() { /* ... */ }

    @Test
    void updateAttendance_penalty_decreases_refundsDeposit() { /* ... */ }

    @Test
    void updateAttendance_excused_exceedsLimit_throwsException() { /* ... */ }
}
```

**Step 3: AttendanceService.java 구현**

```java
package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int INITIAL_DEPOSIT = 100_000;
    private static final int MAX_EXCUSE_COUNT = 3;

    private final QrCodeRepository qrCodeRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final int currentCohortGeneration;
    private final com.prography.backend.repository.CohortRepository cohortRepository;

    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        // 1. QR hashValue 조회
        QrCode qrCode = qrCodeRepository.findByHashValue(request.hashValue())
                .orElseThrow(() -> new AppException(ErrorCode.QR_INVALID));
        // 2. QR 만료
        if (qrCode.isExpired()) throw new AppException(ErrorCode.QR_EXPIRED);
        // 3. 일정 IN_PROGRESS
        Session session = qrCode.getSession();
        if (session.getStatus() != SessionStatus.IN_PROGRESS)
            throw new AppException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        // 4. 회원 존재
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        // 5. 회원 탈퇴
        if (member.getStatus() == MemberStatus.WITHDRAWN)
            throw new AppException(ErrorCode.MEMBER_WITHDRAWN);
        // 6. 중복 출결
        if (attendanceRepository.existsBySessionIdAndMemberId(session.getId(), member.getId()))
            throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
        // 7. CohortMember 존재
        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        CohortMember cohortMember = cohortMemberRepository
                .findByMemberIdAndCohortId(member.getId(), currentCohort.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        // 지각 판정
        LocalDateTime sessionDateTime = LocalDateTime.of(session.getDate(), session.getTime());
        ZonedDateTime sessionZdt = sessionDateTime.atZone(SEOUL);
        ZonedDateTime nowZdt = ZonedDateTime.now(SEOUL);
        AttendanceStatus status;
        Integer lateMinutes = null;
        if (nowZdt.isAfter(sessionZdt)) {
            status = AttendanceStatus.LATE;
            lateMinutes = (int) ChronoUnit.MINUTES.between(sessionZdt, nowZdt);
        } else {
            status = AttendanceStatus.PRESENT;
        }

        int penalty = PenaltyCalculator.calculate(status, lateMinutes);
        if (penalty > 0) {
            if (cohortMember.getDeposit() < penalty) throw new AppException(ErrorCode.DEPOSIT_INSUFFICIENT);
            cohortMember.setDeposit(cohortMember.getDeposit() - penalty);
        }

        Attendance attendance = attendanceRepository.save(Attendance.builder()
                .session(session).member(member).qrCode(qrCode)
                .status(status).lateMinutes(lateMinutes).penaltyAmount(penalty)
                .checkedInAt(Instant.now()).build());

        if (penalty > 0) {
            depositHistoryRepository.save(DepositHistory.builder()
                    .cohortMember(cohortMember).type(DepositType.PENALTY)
                    .amount(-penalty).balanceAfter(cohortMember.getDeposit())
                    .attendance(attendance)
                    .description("QR 체크인 - " + status + " 패널티 " + penalty + "원").build());
        }

        return toAttendanceResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<MyAttendanceResponse> getMyAttendances(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        return attendanceRepository.findByMemberId(memberId).stream()
                .map(a -> new MyAttendanceResponse(a.getId(), a.getSession().getId(),
                        a.getSession().getTitle(), a.getStatus(), a.getLateMinutes(),
                        a.getPenaltyAmount(), a.getReason(), a.getCheckedInAt(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        List<Attendance> attendances = attendanceRepository.findByMemberId(memberId);
        int present = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int absent = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        int late = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        int excused = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
        int totalPenalty = attendances.stream().mapToInt(Attendance::getPenaltyAmount).sum();

        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration).orElse(null);
        Integer deposit = null;
        if (currentCohort != null) {
            deposit = cohortMemberRepository.findByMemberIdAndCohortId(memberId, currentCohort.getId())
                    .map(CohortMember::getDeposit).orElse(null);
        }
        return new AttendanceSummaryResponse(memberId, present, absent, late, excused, totalPenalty, deposit);
    }

    @Transactional
    public AttendanceResponse registerAttendance(RegisterAttendanceRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (attendanceRepository.existsBySessionIdAndMemberId(session.getId(), member.getId()))
            throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);

        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        CohortMember cohortMember = cohortMemberRepository
                .findByMemberIdAndCohortId(member.getId(), currentCohort.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        if (request.status() == AttendanceStatus.EXCUSED) {
            if (cohortMember.getExcuseCount() >= MAX_EXCUSE_COUNT)
                throw new AppException(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
            cohortMember.setExcuseCount(cohortMember.getExcuseCount() + 1);
        }

        int penalty = PenaltyCalculator.calculate(request.status(), request.lateMinutes());
        if (penalty > 0) {
            if (cohortMember.getDeposit() < penalty) throw new AppException(ErrorCode.DEPOSIT_INSUFFICIENT);
            cohortMember.setDeposit(cohortMember.getDeposit() - penalty);
        }

        Attendance attendance = attendanceRepository.save(Attendance.builder()
                .session(session).member(member).status(request.status())
                .lateMinutes(request.lateMinutes()).penaltyAmount(penalty)
                .reason(request.reason()).build());

        if (penalty > 0) {
            depositHistoryRepository.save(DepositHistory.builder()
                    .cohortMember(cohortMember).type(DepositType.PENALTY)
                    .amount(-penalty).balanceAfter(cohortMember.getDeposit())
                    .attendance(attendance)
                    .description("출결 등록 - " + request.status() + " 패널티 " + penalty + "원").build());
        }

        return toAttendanceResponse(attendance);
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, UpdateAttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_FOUND));

        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        CohortMember cohortMember = cohortMemberRepository
                .findByMemberIdAndCohortId(attendance.getMember().getId(), currentCohort.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        AttendanceStatus oldStatus = attendance.getStatus();
        int oldPenalty = attendance.getPenaltyAmount();
        AttendanceStatus newStatus = request.status();
        Integer newLateMinutes = request.lateMinutes();
        int newPenalty = PenaltyCalculator.calculate(newStatus, newLateMinutes);

        // 공결 상태 전환 처리
        if (oldStatus != AttendanceStatus.EXCUSED && newStatus == AttendanceStatus.EXCUSED) {
            if (cohortMember.getExcuseCount() >= MAX_EXCUSE_COUNT)
                throw new AppException(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
            cohortMember.setExcuseCount(cohortMember.getExcuseCount() + 1);
        } else if (oldStatus == AttendanceStatus.EXCUSED && newStatus != AttendanceStatus.EXCUSED) {
            cohortMember.setExcuseCount(Math.max(0, cohortMember.getExcuseCount() - 1));
        }

        // 보증금 조정
        int diff = newPenalty - oldPenalty;
        if (diff > 0) {
            if (cohortMember.getDeposit() < diff) throw new AppException(ErrorCode.DEPOSIT_INSUFFICIENT);
            cohortMember.setDeposit(cohortMember.getDeposit() - diff);
            depositHistoryRepository.save(DepositHistory.builder()
                    .cohortMember(cohortMember).type(DepositType.PENALTY)
                    .amount(-diff).balanceAfter(cohortMember.getDeposit())
                    .attendance(attendance).description("출결 수정 - 추가 패널티 " + diff + "원").build());
        } else if (diff < 0) {
            int refund = -diff;
            cohortMember.setDeposit(cohortMember.getDeposit() + refund);
            depositHistoryRepository.save(DepositHistory.builder()
                    .cohortMember(cohortMember).type(DepositType.REFUND)
                    .amount(refund).balanceAfter(cohortMember.getDeposit())
                    .attendance(attendance).description("출결 수정 - 환급 " + refund + "원").build());
        }

        attendance.setStatus(newStatus);
        attendance.setLateMinutes(newLateMinutes);
        attendance.setPenaltyAmount(newPenalty);
        if (request.reason() != null) attendance.setReason(request.reason());

        return toAttendanceResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<SessionAttendanceSummaryResponse> getSessionAttendanceSummary(Long sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        return cohortMemberRepository.findByCohortId(currentCohort.getId()).stream()
                .map(cm -> {
                    Member m = cm.getMember();
                    List<Attendance> atts = attendanceRepository.findByMemberId(m.getId());
                    int p = (int) atts.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                    int ab = (int) atts.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
                    int l = (int) atts.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
                    int ex = (int) atts.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
                    int totalPenalty = atts.stream().mapToInt(Attendance::getPenaltyAmount).sum();
                    return new SessionAttendanceSummaryResponse(m.getId(), m.getName(), p, ab, l, ex, totalPenalty, cm.getDeposit());
                }).toList();
    }

    @Transactional(readOnly = true)
    public MemberAttendanceDetailResponse getMemberAttendanceDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        Cohort currentCohort = cohortRepository.findByGeneration(currentCohortGeneration).orElse(null);
        CohortMember cm = currentCohort != null ?
                cohortMemberRepository.findByMemberIdAndCohortId(memberId, currentCohort.getId()).orElse(null) : null;
        List<AttendanceResponse> attendances = attendanceRepository.findByMemberId(memberId).stream()
                .map(this::toAttendanceResponse).toList();
        return new MemberAttendanceDetailResponse(
                member.getId(), member.getName(),
                cm != null ? cm.getCohort().getGeneration() : null,
                cm != null && cm.getPart() != null ? cm.getPart().getName() : null,
                cm != null && cm.getTeam() != null ? cm.getTeam().getName() : null,
                cm != null ? cm.getDeposit() : null,
                cm != null ? cm.getExcuseCount() : null,
                attendances);
    }

    @Transactional(readOnly = true)
    public SessionAttendancesResponse getSessionAttendances(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        List<AttendanceResponse> attendances = attendanceRepository.findBySessionId(sessionId).stream()
                .map(this::toAttendanceResponse).toList();
        return new SessionAttendancesResponse(sessionId, session.getTitle(), attendances);
    }

    @Transactional(readOnly = true)
    public List<DepositHistoryResponse> getDepositHistory(Long cohortMemberId) {
        cohortMemberRepository.findById(cohortMemberId)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_MEMBER_NOT_FOUND));
        return depositHistoryRepository.findByCohortMemberIdOrderByCreatedAtAsc(cohortMemberId).stream()
                .map(dh -> new DepositHistoryResponse(dh.getId(), dh.getCohortMember().getId(),
                        dh.getType(), dh.getAmount(), dh.getBalanceAfter(),
                        dh.getAttendance() != null ? dh.getAttendance().getId() : null,
                        dh.getDescription(), dh.getCreatedAt()))
                .toList();
    }

    private AttendanceResponse toAttendanceResponse(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getSession().getId(), a.getMember().getId(),
                a.getStatus(), a.getLateMinutes(), a.getPenaltyAmount(), a.getReason(),
                a.getCheckedInAt(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
```

**Step 4: Controllers 구현**

```java
// AttendanceController.java
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final MemberService memberService;

    @PostMapping("/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest req) {
        return ApiResponse.ok(attendanceService.checkIn(req));
    }

    @GetMapping("/attendances")
    public ApiResponse<List<MyAttendanceResponse>> getMyAttendances(@RequestParam Long memberId) {
        return ApiResponse.ok(attendanceService.getMyAttendances(memberId));
    }

    @GetMapping("/members/{memberId}/attendance-summary")
    public ApiResponse<AttendanceSummaryResponse> getAttendanceSummary(@PathVariable Long memberId) {
        return ApiResponse.ok(attendanceService.getAttendanceSummary(memberId));
    }
}

// AdminAttendanceController.java
@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminAttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttendanceResponse> register(@Valid @RequestBody RegisterAttendanceRequest req) {
        return ApiResponse.ok(attendanceService.registerAttendance(req));
    }

    @PutMapping("/attendances/{id}")
    public ApiResponse<AttendanceResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateAttendanceRequest req) {
        return ApiResponse.ok(attendanceService.updateAttendance(id, req));
    }

    @GetMapping("/attendances/sessions/{sessionId}/summary")
    public ApiResponse<List<SessionAttendanceSummaryResponse>> sessionSummary(@PathVariable Long sessionId) {
        return ApiResponse.ok(attendanceService.getSessionAttendanceSummary(sessionId));
    }

    @GetMapping("/attendances/members/{memberId}")
    public ApiResponse<MemberAttendanceDetailResponse> memberDetail(@PathVariable Long memberId) {
        return ApiResponse.ok(attendanceService.getMemberAttendanceDetail(memberId));
    }

    @GetMapping("/attendances/sessions/{sessionId}")
    public ApiResponse<SessionAttendancesResponse> sessionAttendances(@PathVariable Long sessionId) {
        return ApiResponse.ok(attendanceService.getSessionAttendances(sessionId));
    }

    @GetMapping("/cohort-members/{cohortMemberId}/deposits")
    public ApiResponse<List<DepositHistoryResponse>> depositHistory(@PathVariable Long cohortMemberId) {
        return ApiResponse.ok(attendanceService.getDepositHistory(cohortMemberId));
    }
}
```

**Step 5: 테스트 실행**

```bash
mvn test -Dtest=AttendanceServiceTest -pl .
```

**Step 6: Commit**

```bash
git add src/
git commit -m "feat: 출결 및 보증금 API 구현 (가산점)"
```

---

## Task 12: 전체 테스트 실행 및 최종 검증

**Step 1: 전체 테스트**

```bash
mvn test
```

Expected: BUILD SUCCESS, 모든 테스트 PASS

**Step 2: 서버 시작 및 Swagger 확인**

```bash
mvn spring-boot:run
```

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/h2-console
```

**Step 3: 로그인 API 검증**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"admin1234"}'
```

Expected: `{"success":true,"data":{"loginId":"admin",...}}`

**Step 4: 최종 Commit**

```bash
git add .
git commit -m "feat: 전체 25개 API 구현 완료 및 단위 테스트 작성"
```

---

## 구현 체크리스트

### 필수 API (16개)
- [ ] POST /api/v1/auth/login
- [ ] GET /api/v1/members/{id}
- [ ] POST /api/v1/admin/members
- [ ] GET /api/v1/admin/members
- [ ] GET /api/v1/admin/members/{id}
- [ ] PUT /api/v1/admin/members/{id}
- [ ] DELETE /api/v1/admin/members/{id}
- [ ] GET /api/v1/admin/cohorts
- [ ] GET /api/v1/admin/cohorts/{cohortId}
- [ ] GET /api/v1/sessions
- [ ] GET /api/v1/admin/sessions
- [ ] POST /api/v1/admin/sessions
- [ ] PUT /api/v1/admin/sessions/{id}
- [ ] DELETE /api/v1/admin/sessions/{id}
- [ ] POST /api/v1/admin/sessions/{sessionId}/qrcodes
- [ ] PUT /api/v1/admin/qrcodes/{qrCodeId}

### 가산점 API (9개)
- [ ] POST /api/v1/attendances
- [ ] GET /api/v1/attendances
- [ ] GET /api/v1/members/{memberId}/attendance-summary
- [ ] POST /api/v1/admin/attendances
- [ ] PUT /api/v1/admin/attendances/{id}
- [ ] GET /api/v1/admin/attendances/sessions/{sessionId}/summary
- [ ] GET /api/v1/admin/attendances/members/{memberId}
- [ ] GET /api/v1/admin/attendances/sessions/{sessionId}
- [ ] GET /api/v1/admin/cohort-members/{cohortMemberId}/deposits

### 제출 산출물
- [ ] ERD (docs/에 추가)
- [ ] System Design Architecture (docs/에 추가)
- [ ] README (실행 방법 포함)
