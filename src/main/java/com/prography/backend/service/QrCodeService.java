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
        oldQr.setExpiresAt(Instant.now());
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
