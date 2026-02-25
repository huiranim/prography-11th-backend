package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.CheckInRequest;
import com.prography.backend.dto.request.RegisterAttendanceRequest;
import com.prography.backend.dto.request.UpdateAttendanceRequest;
import com.prography.backend.dto.response.AttendanceResponse;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AttendanceService 단위 테스트
 *
 * - AttendanceService 생성자에 int currentCohortGeneration이 포함되어 있어
 *   @InjectMocks 사용 불가 (primitive 타입은 Mock 생성 불가)
 * - @BeforeEach setUp()에서 new AttendanceService(...)로 직접 생성하여 해결
 *
 * 테스트 구성 (총 13개):
 * 1. 패널티 계산 6개 — PenaltyCalculator를 직접 호출해 금액 계산 공식 검증
 * 2. QR 체크인 검증 순서 4개 — 7단계 검증 중 앞부분 4개 시나리오
 * 3. 출결 수정(보증금 조정) 2개 — 패널티 증감에 따른 차감/환급 로직
 * 4. 공결 한도 초과 1개 — EXCUSED 3회 초과 시 예외 발생
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    AttendanceService attendanceService;
    @Mock QrCodeRepository qrCodeRepository;
    @Mock SessionRepository sessionRepository;
    @Mock MemberRepository memberRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock CohortMemberRepository cohortMemberRepository;
    @Mock DepositHistoryRepository depositHistoryRepository;
    @Mock CohortRepository cohortRepository;

    @BeforeEach
    void setUp() {
        // int 타입 currentCohortGeneration은 Mock 불가 → 리터럴 값(11) 직접 전달
        attendanceService = new AttendanceService(qrCodeRepository, sessionRepository, memberRepository,
                attendanceRepository, cohortMemberRepository, depositHistoryRepository, cohortRepository, 11);
    }

    // ─── 패널티 계산 테스트 ────────────────────────────────────────────────────
    // PenaltyCalculator는 순수 계산 로직(static 메서드)이므로
    // Mock 없이 입력값 → 기대 출력값만 검증 (가장 단순한 단위 테스트 형태)

    /**
     * 정상 출석(PRESENT) 패널티는 0원
     * - lateMinutes가 null이어도 계산 오류 없이 0 반환
     */
    @Test
    void penalty_present_isZero() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.PRESENT, null)).isEqualTo(0);
    }

    /**
     * 결석(ABSENT) 패널티는 고정 10,000원
     */
    @Test
    void penalty_absent_is10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.ABSENT, null)).isEqualTo(10_000);
    }

    /**
     * 지각 19분 → 19 × 500 = 9,500원 (상한 미달)
     * - 공식: min(지각분 × 500, 10,000)
     */
    @Test
    void penalty_late19min_is9500() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 19)).isEqualTo(9_500);
    }

    /**
     * 지각 20분 → 20 × 500 = 10,000원 (상한 정확히 도달)
     * - 경계값(Boundary Value) 테스트
     */
    @Test
    void penalty_late20min_is10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 20)).isEqualTo(10_000);
    }

    /**
     * 지각 25분 → 25 × 500 = 12,500 → min(12,500, 10,000) = 10,000원 (상한 적용)
     * - 상한선(cap) 동작을 확인하는 경계값 테스트
     */
    @Test
    void penalty_late25min_cappedAt10000() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.LATE, 25)).isEqualTo(10_000);
    }

    /**
     * 공결(EXCUSED) 패널티는 0원
     * - 공결은 보증금 차감 없음 (단, 횟수 제한은 별도 검증)
     */
    @Test
    void penalty_excused_isZero() {
        assertThat(PenaltyCalculator.calculate(AttendanceStatus.EXCUSED, null)).isEqualTo(0);
    }

    // ─── QR 체크인 검증 순서 테스트 ───────────────────────────────────────────
    // checkIn()의 7단계 검증 중 앞 4개를 테스트.
    // 각 테스트에서 이전 단계의 Mock은 성공 조건으로, 현재 단계만 실패 조건으로 설정해
    // "정확히 이 순서에서 이 예외가 발생하는지"를 검증.

    /**
     * 1단계: QR hashValue 존재 여부 → 없으면 QR_INVALID
     * - qrCodeRepository.findByHashValue()가 Optional.empty() 반환
     * - 이후 단계(만료 확인, 세션 상태 확인 등)는 도달하지 않음
     */
    @Test
    void checkIn_QR_INVALID_whenHashNotFound() {
        when(qrCodeRepository.findByHashValue("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest("invalid", 1L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_INVALID);
    }

    /**
     * 2단계: QR 만료 여부 → 만료됐으면 QR_EXPIRED
     * - expiresAt을 현재 시각보다 1초 이전으로 설정하여 만료 상태 시뮬레이션
     * - 1단계(QR 존재)는 성공 → Mock이 만료된 QrCode 객체를 반환
     * - QrCode.isExpired()는 Instant.now().isAfter(expiresAt) 로직
     */
    @Test
    void checkIn_QR_EXPIRED_whenExpired() {
        QrCode expiredQr = QrCode.builder()
                .hashValue("hash").expiresAt(Instant.now().minusSeconds(1)).build();
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(expiredQr));
        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest("hash", 1L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_EXPIRED);
    }

    /**
     * 3단계: 일정 상태 IN_PROGRESS 확인 → SCHEDULED이면 SESSION_NOT_IN_PROGRESS
     * - 1단계(QR 존재), 2단계(미만료) 성공 상태에서 세션 status가 SCHEDULED인 경우
     * - QR에 연결된 session 객체를 SCHEDULED 상태로 설정
     */
    @Test
    void checkIn_SESSION_NOT_IN_PROGRESS() {
        Session session = Session.builder().id(1L).status(SessionStatus.SCHEDULED)
                .date(LocalDate.now()).time(LocalTime.now()).build();
        QrCode qr = QrCode.builder().hashValue("hash").session(session)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qr));
        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest("hash", 1L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_IN_PROGRESS);
    }

    /**
     * 6단계: 중복 출결 여부 → 이미 출결 기록 있으면 ATTENDANCE_ALREADY_CHECKED
     * - 1단계(QR 존재), 2단계(미만료), 3단계(IN_PROGRESS), 4단계(회원 존재),
     *   5단계(탈퇴 아님) 모두 성공 → 6단계에서 existsBySessionIdAndMemberId()가 true 반환
     * - 참고: MEMBER_NOT_FOUND(4단계), MEMBER_WITHDRAWN(5단계) 검증은 생략
     *   (구조가 3단계와 동일하며, 핵심 비즈니스 로직은 6단계부터)
     */
    @Test
    void checkIn_ATTENDANCE_ALREADY_CHECKED() {
        Session session = Session.builder().id(1L).status(SessionStatus.IN_PROGRESS)
                .date(LocalDate.now()).time(LocalTime.now()).build();
        QrCode qr = QrCode.builder().hashValue("hash").session(session)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        Member member = Member.builder().id(1L).status(MemberStatus.ACTIVE).build();
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qr));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest("hash", 1L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
    }

    // ─── QR 체크인 성공 케이스 ────────────────────────────────────────────────

    /**
     * QR 체크인 성공 — PRESENT (세션 시작 전 도착)
     * - session.date = 내일 → 현재 시각이 세션 시작 전 → PRESENT 판정
     * - penalty = 0 → 보증금 변동 없음, depositHistoryRepository.save() 미호출
     * - 지각 판정 기준: ZonedDateTime.now(Asia/Seoul).isAfter(sessionDateTime) → false → PRESENT
     */
    @Test
    void checkIn_present_success() {
        // 내일 오후 2시 세션 → 현재 시각 기준 아직 시작 전 → PRESENT
        Session session = Session.builder().id(1L).status(SessionStatus.IN_PROGRESS)
                .date(LocalDate.now().plusDays(1)).time(LocalTime.of(14, 0)).build();
        QrCode qr = QrCode.builder().hashValue("hash").session(session)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        Member member = Member.builder().id(1L).status(MemberStatus.ACTIVE).build();
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();
        Attendance saved = Attendance.builder().id(1L).session(session).member(member)
                .status(AttendanceStatus.PRESENT).lateMinutes(null).penaltyAmount(0).build();

        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qr));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));
        when(attendanceRepository.save(any())).thenReturn(saved);

        AttendanceResponse result = attendanceService.checkIn(new CheckInRequest("hash", 1L));

        assertThat(result.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.penaltyAmount()).isEqualTo(0);
        assertThat(cm.getDeposit()).isEqualTo(100_000); // penalty=0 → 보증금 변동 없음
        // penalty=0 → if (penalty > 0) 분기 미진입 → DepositHistory 저장 없음
        verify(depositHistoryRepository, never()).save(any());
    }

    /**
     * QR 체크인 성공 — LATE (세션 시작 후 도착)
     * - session.date = 2020-01-01 00:00 → 현재 시각이 훨씬 이후 → LATE 판정
     * - lateMinutes = 수천 분 → penalty = min(수천 × 500, 10,000) = 10,000원 (상한)
     * - 보증금 100,000 → 90,000으로 차감, depositHistoryRepository.save() 호출
     */
    @Test
    void checkIn_late_success() {
        // 2020-01-01 00:00 세션 → 현재 시각이 확실히 이후 → LATE
        Session session = Session.builder().id(1L).status(SessionStatus.IN_PROGRESS)
                .date(LocalDate.of(2020, 1, 1)).time(LocalTime.of(0, 0)).build();
        QrCode qr = QrCode.builder().hashValue("hash").session(session)
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        Member member = Member.builder().id(1L).status(MemberStatus.ACTIVE).build();
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();
        // 서비스가 save()에 어떤 객체를 넘기든 이 mock 객체를 반환
        Attendance saved = Attendance.builder().id(1L).session(session).member(member)
                .status(AttendanceStatus.LATE).lateMinutes(2000).penaltyAmount(10_000).build();

        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qr));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));
        when(attendanceRepository.save(any())).thenReturn(saved);
        when(depositHistoryRepository.save(any())).thenReturn(null);

        AttendanceResponse result = attendanceService.checkIn(new CheckInRequest("hash", 1L));

        assertThat(result.status()).isEqualTo(AttendanceStatus.LATE);
        assertThat(result.penaltyAmount()).isEqualTo(10_000);
        // 서비스가 cm.setDeposit(100_000 - 10_000) 호출 → 실제 Java 객체 필드 변경됨
        assertThat(cm.getDeposit()).isEqualTo(90_000);
    }

    // ─── 출결 수정 - 보증금 자동 조정 테스트 ─────────────────────────────────
    // diff = newPenalty - oldPenalty
    //   diff > 0 → 추가 차감 + DepositHistory(PENALTY)
    //   diff < 0 → 환급 + DepositHistory(REFUND)
    //   diff = 0 → 변동 없음

    /**
     * 패널티 증가 시 보증금 추가 차감
     * - PRESENT(0원) → ABSENT(10,000원): diff = 10,000 > 0 → 차감
     * - cm.deposit: 100,000 → 90,000 으로 변경됐는지 검증
     * - Mock이 반환한 CohortMember 객체(진짜 Java 객체)의 deposit 필드가 실제로 바뀐 것을 확인
     *   (@Transactional 더티 체킹으로 실제 DB UPDATE도 발생하지만 단위 테스트에선 필드값만 확인)
     */
    @Test
    void updateAttendance_penaltyIncreases_deductsDeposit() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        Attendance attendance = Attendance.builder().id(1L).member(member).session(session)
                .status(AttendanceStatus.PRESENT).penaltyAmount(0).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));
        when(depositHistoryRepository.save(any())).thenReturn(null);

        AttendanceResponse result = attendanceService.updateAttendance(1L,
                new UpdateAttendanceRequest(AttendanceStatus.ABSENT, null, null));
        assertThat(result.penaltyAmount()).isEqualTo(10_000);
        assertThat(cm.getDeposit()).isEqualTo(90_000);
    }

    /**
     * 패널티 감소 시 보증금 환급
     * - ABSENT(10,000원) → PRESENT(0원): diff = -10,000 < 0 → 환급
     * - cm.deposit: 90,000 → 100,000 으로 복원됐는지 검증
     */
    @Test
    void updateAttendance_penaltyDecreases_refundsDeposit() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        Attendance attendance = Attendance.builder().id(1L).member(member).session(session)
                .status(AttendanceStatus.ABSENT).penaltyAmount(10_000).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(90_000).excuseCount(0).build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));
        when(depositHistoryRepository.save(any())).thenReturn(null);

        AttendanceResponse result = attendanceService.updateAttendance(1L,
                new UpdateAttendanceRequest(AttendanceStatus.PRESENT, null, null));
        assertThat(result.penaltyAmount()).isEqualTo(0);
        assertThat(cm.getDeposit()).isEqualTo(100_000);
    }

    /**
     * 패널티 동일 (diff=0) — 보증금 변동 없음, DepositHistory 저장 없음
     * - ABSENT(10,000) → ABSENT(10,000): diff = newPenalty - oldPenalty = 0
     * - 서비스 내부: if (diff > 0) / else if (diff < 0) 모두 미진입
     * - depositHistoryRepository.save()가 단 한 번도 호출되지 않는지 verify로 검증
     */
    @Test
    void updateAttendance_noDiff_noDepositChange() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        Attendance attendance = Attendance.builder().id(1L).member(member).session(session)
                .status(AttendanceStatus.ABSENT).penaltyAmount(10_000).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(90_000).excuseCount(0).build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));

        AttendanceResponse result = attendanceService.updateAttendance(1L,
                new UpdateAttendanceRequest(AttendanceStatus.ABSENT, null, null));

        assertThat(result.penaltyAmount()).isEqualTo(10_000); // 패널티 동일
        assertThat(cm.getDeposit()).isEqualTo(90_000);        // 보증금 변동 없음
        // diff=0 이므로 보증금 이력 저장이 발생하지 않아야 함
        verify(depositHistoryRepository, never()).save(any());
    }

    // ─── 관리자 출결 등록 테스트 ──────────────────────────────────────────────

    /**
     * 관리자 출결 등록 성공 — ABSENT, 패널티 차감
     * - QR 없이 직접 상태를 지정하는 관리자용 registerAttendance()
     * - ABSENT 등록 → 패널티 10,000원 차감, DepositHistory(PENALTY) 저장
     * - checkIn()과 달리 QR 조회 단계가 없고, 상태/lateMinutes를 요청에서 직접 받음
     */
    @Test
    void registerAttendance_success() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();
        Attendance saved = Attendance.builder().id(1L).session(session).member(member)
                .status(AttendanceStatus.ABSENT).lateMinutes(null).penaltyAmount(10_000).build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));
        when(attendanceRepository.save(any())).thenReturn(saved);
        when(depositHistoryRepository.save(any())).thenReturn(null);

        AttendanceResponse result = attendanceService.registerAttendance(
                new RegisterAttendanceRequest(1L, 1L, AttendanceStatus.ABSENT, null, null));

        assertThat(result.status()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.penaltyAmount()).isEqualTo(10_000);
        assertThat(cm.getDeposit()).isEqualTo(90_000); // 패널티 차감 확인
    }

    /**
     * 관리자 출결 등록 — 중복 출결 시 예외 발생
     * - 동일 세션+회원 조합이 이미 존재하면 ATTENDANCE_ALREADY_CHECKED
     * - QR 체크인(checkIn)과 동일한 중복 검증 로직
     */
    @Test
    void registerAttendance_alreadyChecked_throwsException() {
        Session session = Session.builder().id(1L).build();
        Member member = Member.builder().id(1L).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> attendanceService.registerAttendance(
                new RegisterAttendanceRequest(1L, 1L, AttendanceStatus.PRESENT, null, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
    }

    /**
     * 공결(EXCUSED) 한도 초과 시 예외 발생
     * - excuseCount가 이미 3인 상태에서 EXCUSED로 변경 시도
     * - 요구사항: 기수당 EXCUSED 최대 3회 (초과 시 EXCUSE_LIMIT_EXCEEDED)
     * - 서비스가 cm.excuseCount >= 3 검증 후 예외를 던지는지 확인
     */
    @Test
    void updateAttendance_excusedExceedsLimit_throwsException() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        Attendance attendance = Attendance.builder().id(1L).member(member).session(session)
                .status(AttendanceStatus.ABSENT).penaltyAmount(10_000).build();
        // excuseCount=3으로 설정하여 한도 초과 상태 시뮬레이션
        CohortMember cm = CohortMember.builder().id(1L).member(member).cohort(cohort)
                .deposit(90_000).excuseCount(3).build();

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(Optional.of(cm));

        assertThatThrownBy(() -> attendanceService.updateAttendance(1L,
                new UpdateAttendanceRequest(AttendanceStatus.EXCUSED, null, null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
    }
}
