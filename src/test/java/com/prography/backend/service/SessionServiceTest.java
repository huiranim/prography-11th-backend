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

/**
 * SessionService 단위 테스트
 *
 * - SessionService도 int currentCohortGeneration을 가지므로 @BeforeEach에서 직접 생성
 * - 11기를 현재 기수로 고정하여 테스트
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    SessionService sessionService;
    @Mock SessionRepository sessionRepository;
    @Mock CohortRepository cohortRepository;
    @Mock QrCodeRepository qrCodeRepository;
    @Mock AttendanceRepository attendanceRepository;

    // 여러 테스트에서 공유하는 11기 기수 객체
    Cohort cohort11 = Cohort.builder().id(2L).generation(11).name("11기").build();

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, cohortRepository,
                qrCodeRepository, attendanceRepository, 11);
    }

    /**
     * 일정 생성 성공
     * - 현재 기수(11기)를 조회하고, Session을 저장하며, QR 코드를 자동 생성하는 흐름 검증
     * - toSessionResponse() 내부에서 attendanceRepository와 qrCodeRepository도 호출되므로
     *   각각의 Mock 반환값도 설정 필요
     * - 생성된 일정의 title과 status(SCHEDULED)가 올바른지 확인
     */
    @Test
    void createSession_success() {
        when(cohortRepository.findByGeneration(11)).thenReturn(Optional.of(cohort11));
        Session savedSession = Session.builder().id(1L).cohort(cohort11)
                .title("1회 세션").date(LocalDate.of(2026, 3, 1))
                .time(LocalTime.of(14, 0)).location("강남").status(SessionStatus.SCHEDULED).build();
        when(sessionRepository.save(any())).thenReturn(savedSession);
        when(qrCodeRepository.save(any())).thenReturn(null);
        // toSessionResponse()에서 출결 집계와 QR 활성 여부를 조회하므로 반환값 설정
        when(attendanceRepository.findBySessionId(anyLong())).thenReturn(List.of());
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(false);

        SessionResponse result = sessionService.createSession(
                new CreateSessionRequest("1회 세션", LocalDate.of(2026, 3, 1), LocalTime.of(14, 0), "강남"));
        assertThat(result.title()).isEqualTo("1회 세션");
        assertThat(result.status()).isEqualTo(SessionStatus.SCHEDULED);
    }

    /**
     * CANCELLED 상태 일정 수정 시도 시 예외 발생
     * - 이미 취소된 일정은 수정 불가 → SESSION_ALREADY_CANCELLED 예외 검증
     * - 요구사항: CANCELLED 일정은 어떠한 수정도 거부
     */
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

    /**
     * 존재하지 않는 일정 삭제 시도 시 예외 발생
     */
    @Test
    void deleteSession_notFound_throwsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sessionService.deleteSession(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SESSION_NOT_FOUND.getMessage());
    }

    /**
     * 일정 수정 성공 — 제목만 변경 (Partial Update)
     * - title만 요청에 포함, 나머지 필드는 null
     * - 서비스 내부에서 if (request.title() != null) session.setTitle(...) 로 처리하므로
     *   title은 바뀌고 status는 원래 SCHEDULED 그대로여야 함
     * - null 필드를 건너뛰는 Partial Update 로직이 올바른지 검증
     */
    @Test
    void updateSession_success_partialTitle() {
        Session session = Session.builder().id(1L).cohort(cohort11)
                .title("원래 제목").date(LocalDate.of(2026, 3, 1))
                .time(LocalTime.of(14, 0)).location("강남").status(SessionStatus.SCHEDULED).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(attendanceRepository.findBySessionId(anyLong())).thenReturn(List.of());
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(false);

        SessionResponse result = sessionService.updateSession(1L,
                new UpdateSessionRequest("새 제목", null, null, null, null));

        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.status()).isEqualTo(SessionStatus.SCHEDULED);  // null이었으므로 변경 없음
        assertThat(result.location()).isEqualTo("강남");                  // null이었으므로 변경 없음
    }

    /**
     * 일정 삭제 성공 (Soft Delete)
     * - 실제 DB 삭제가 아닌 status를 CANCELLED로 변경하는 방식 검증
     * - Mock이 반환한 session 객체(진짜 Java 객체)의 status가 CANCELLED로 바뀌는 것을 확인
     * - 활성 QR이 없는 경우(빈 리스트 반환)도 정상 처리되는지 확인
     */
    @Test
    void deleteSession_success_setsStatusCancelled() {
        Session session = Session.builder().id(1L).cohort(cohort11)
                .title("세션").date(LocalDate.now()).time(LocalTime.now())
                .location("강남").status(SessionStatus.SCHEDULED).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        // 활성 QR 없음 → 만료 처리 루프 스킵
        when(qrCodeRepository.findBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(List.of());
        when(attendanceRepository.findBySessionId(anyLong())).thenReturn(List.of());
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(anyLong(), any())).thenReturn(false);

        SessionResponse result = sessionService.deleteSession(1L);
        // DB 저장 없이 Java 객체의 status가 변경됐는지 결과 DTO로 검증
        assertThat(result.status()).isEqualTo(SessionStatus.CANCELLED);
    }
}
