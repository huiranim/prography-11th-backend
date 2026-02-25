package com.prography.backend.dto.response;

import com.prography.backend.domain.AttendanceStatus;
import java.time.Instant;

public record MyAttendanceResponse(Long id, Long sessionId, String sessionTitle, AttendanceStatus status,
    Integer lateMinutes, int penaltyAmount, String reason, Instant checkedInAt, Instant createdAt) {}
