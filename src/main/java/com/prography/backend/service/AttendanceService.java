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
    private static final int MAX_EXCUSE_COUNT = 3;

    private final QrCodeRepository qrCodeRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final CohortRepository cohortRepository;
    private final int currentCohortGeneration;

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

        // 지각 판정 (Asia/Seoul)
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
