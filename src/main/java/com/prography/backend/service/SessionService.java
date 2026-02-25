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
import java.time.LocalDate;
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
    public List<SessionResponse> getAdminSessions(SessionStatus status, LocalDate dateFrom, LocalDate dateTo) {
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
                new SessionResponse.AttendanceSummary((int) present, (int) absent, (int) late, (int) excused, attendances.size()),
                qrActive, s.getCreatedAt(), s.getUpdatedAt());
    }

    private MemberSessionResponse toMemberSessionResponse(Session s) {
        return new MemberSessionResponse(s.getId(), s.getTitle(), s.getDate(), s.getTime(),
                s.getLocation(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
