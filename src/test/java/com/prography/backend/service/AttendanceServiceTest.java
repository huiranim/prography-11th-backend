package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.CheckInRequest;
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
        attendanceService = new AttendanceService(qrCodeRepository, sessionRepository, memberRepository,
                attendanceRepository, cohortMemberRepository, depositHistoryRepository, cohortRepository, 11);
    }

    // 패널티 계산 테스트
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
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_INVALID);
    }

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

    // 출결 수정 - 보증금 조정 테스트
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

    @Test
    void updateAttendance_excusedExceedsLimit_throwsException() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).build();
        Member member = Member.builder().id(1L).build();
        Session session = Session.builder().id(1L).build();
        Attendance attendance = Attendance.builder().id(1L).member(member).session(session)
                .status(AttendanceStatus.ABSENT).penaltyAmount(10_000).build();
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
