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
