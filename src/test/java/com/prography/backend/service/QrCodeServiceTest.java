package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.response.QrCodeResponse;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.QrCodeRepository;
import com.prography.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QrCodeService 단위 테스트
 *
 * - QrCodeService는 int 필드가 없어 @InjectMocks 사용 가능
 * - 핵심 비즈니스 규칙:
 *   1. 일정당 활성(미만료) QR은 1개만 허용 → 중복 생성 시 QR_ALREADY_ACTIVE
 *   2. QR 갱신: 기존 QR expiresAt을 현재 시각으로 설정(즉시 만료) + 새 QR 생성
 */
@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @InjectMocks QrCodeService qrCodeService;
    @Mock QrCodeRepository qrCodeRepository;
    @Mock SessionRepository sessionRepository;

    /**
     * 존재하지 않는 일정에 QR 생성 시도 → SESSION_NOT_FOUND
     */
    @Test
    void createQrCode_sessionNotFound_throwsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> qrCodeService.createQrCode(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SESSION_NOT_FOUND.getMessage());
    }

    /**
     * 활성 QR이 이미 존재하는 일정에 추가 생성 시도 → QR_ALREADY_ACTIVE
     * - existsBySessionIdAndExpiresAtAfter()가 true 반환 (만료되지 않은 QR 존재)
     * - 요구사항: 일정당 활성 QR 1개 제한
     */
    @Test
    void createQrCode_alreadyActive_throwsException() {
        Session session = Session.builder().id(1L).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> qrCodeService.createQrCode(1L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.QR_ALREADY_ACTIVE.getMessage());
    }

    /**
     * QR 생성 성공
     * - 세션 존재, 활성 QR 없음 → 새 QR(UUID hashValue, 24시간 유효기간) 저장
     * - 반환된 QrCodeResponse의 sessionId와 hashValue가 올바르게 매핑됐는지 확인
     * - 실제 UUID 생성(UUID.randomUUID())은 서비스 내부에서 이뤄지므로 hashValue 값 자체보다
     *   "non-null인지" 또는 Mock 반환값이 올바르게 DTO로 변환됐는지에 초점
     */
    @Test
    void createQrCode_success() {
        Session session = Session.builder().id(1L).build();
        QrCode saved = QrCode.builder().id(1L).session(session)
                .hashValue("test-uuid").createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400)).build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.existsBySessionIdAndExpiresAtAfter(eq(1L), any())).thenReturn(false);
        when(sessionRepository.getReferenceById(1L)).thenReturn(session);
        when(qrCodeRepository.save(any())).thenReturn(saved);

        QrCodeResponse result = qrCodeService.createQrCode(1L);

        assertThat(result.sessionId()).isEqualTo(1L);
        assertThat(result.hashValue()).isEqualTo("test-uuid");
        assertThat(result.expiresAt()).isAfter(Instant.now()); // 24시간 후 만료 확인
    }

    /**
     * 존재하지 않는 QR 갱신 시도 → QR_NOT_FOUND
     */
    @Test
    void renewQrCode_notFound_throwsException() {
        when(qrCodeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> qrCodeService.renewQrCode(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.QR_NOT_FOUND.getMessage());
    }

    /**
     * QR 갱신 성공
     * - 기존 QR의 expiresAt을 현재 시각으로 설정 → 즉시 만료 처리
     * - 새 UUID로 새 QR 생성 → 새 QrCodeResponse 반환
     * - 검증:
     *   1. oldQr.isExpired() == true: expiresAt이 현재 시각으로 변경됐으므로 만료 상태
     *   2. 반환된 response가 새 QR(new-uuid)의 정보인지 확인
     */
    @Test
    void renewQrCode_success() {
        Session session = Session.builder().id(1L).build();
        QrCode oldQr = QrCode.builder().id(1L).session(session)
                .hashValue("old-uuid").expiresAt(Instant.now().plusSeconds(3600)).build();
        QrCode newQr = QrCode.builder().id(2L).session(session)
                .hashValue("new-uuid").createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400)).build();

        when(qrCodeRepository.findById(1L)).thenReturn(Optional.of(oldQr));
        when(qrCodeRepository.save(any())).thenReturn(newQr);

        QrCodeResponse result = qrCodeService.renewQrCode(1L);

        // 기존 QR의 expiresAt이 현재 시각으로 변경됨 → isExpired() = true
        assertThat(oldQr.isExpired()).isTrue();
        // 새 QR 정보가 반환됐는지 확인
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.hashValue()).isEqualTo("new-uuid");
    }
}
