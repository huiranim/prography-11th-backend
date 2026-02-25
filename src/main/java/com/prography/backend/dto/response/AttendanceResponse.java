package com.prography.backend.dto.response;

import com.prography.backend.domain.AttendanceStatus;
import java.time.Instant;

public record AttendanceResponse(Long id, Long sessionId, Long memberId, AttendanceStatus status,
    Integer lateMinutes, int penaltyAmount, String reason, Instant checkedInAt,
    Instant createdAt, Instant updatedAt) {}
