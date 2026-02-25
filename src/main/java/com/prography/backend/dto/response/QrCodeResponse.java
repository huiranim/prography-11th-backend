package com.prography.backend.dto.response;

import java.time.Instant;

public record QrCodeResponse(Long id, Long sessionId, String hashValue, Instant createdAt, Instant expiresAt) {}
