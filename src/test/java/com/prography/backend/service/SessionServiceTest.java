package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.CreateSessionRequest;
import com.prography.backend.dto.request.UpdateSessionRequest;
import com.prography.backend.dto.response.SessionResponse;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    SessionService sessionService;
    @Mock SessionRepository sessionRepository;
    @Mock CohortRepository cohortRepository;
    @Mock QrCodeRepository qrCodeRepository;
    @Mock AttendanceRepository attendanceRepository;

    Cohort cohort11 = Cohort.builder().id(2L).generation(11).name("11기").build();

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, cohortRepository,
                qrCodeRepository, attendanceRepository, 11);
    }

    @Test
    void createSession_success() {
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort11));
        Session savedSession = Session.builder().id(1L).cohort(cohort11)
                .title("1회 세션").date(LocalDate.of(2026, 3, 1))
                .time(LocalTime.of(14, 0)).location("강남").status(SessionStatus.SCHEDULED).build();
        when(sessionRepository.save(any())).thenReturn(savedSession);
        when(qrCodeRepository.save(any())).thenReturn(null);
        when(attendanceRepository.findBySessionId(anyLong())).thenReturn(List.of());
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(false);

        SessionResponse result = sessionService.createSession(
                new CreateSessionRequest("1회 세션", LocalDate.of(2026, 3, 1), LocalTime.of(14, 0), "강남"));
        assertThat(result.title()).isEqualTo("1회 세션");
        assertThat(result.status()).isEqualTo(SessionStatus.SCHEDULED);
    }

    @Test
    void updateSession_cancelled_throwsException() {
        Session session = Session.builder().id(1L).cohort(cohort11)
                .status(SessionStatus.CANCELLED).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.updateSession(1L,
                new UpdateSessionRequest("새 제목", null, null, null, null)))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SESSION_ALREADY_CANCELLED.getMessage());
    }

    @Test
    void deleteSession_notFound_throwsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sessionService.deleteSession(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SESSION_NOT_FOUND.getMessage());
    }

    @Test
    void deleteSession_success_setsStatusCancelled() {
        Session session = Session.builder().id(1L).cohort(cohort11)
                .title("세션").date(LocalDate.now()).time(LocalTime.now())
                .location("강남").status(SessionStatus.SCHEDULED).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.findBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(anyLong())).thenReturn(List.of());
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(false);

        SessionResponse result = sessionService.deleteSession(1L);
        assertThat(result.status()).isEqualTo(SessionStatus.CANCELLED);
    }
}
