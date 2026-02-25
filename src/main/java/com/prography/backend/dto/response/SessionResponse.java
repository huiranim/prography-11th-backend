package com.prography.backend.dto.response;

import com.prography.backend.domain.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(Long id, Long cohortId, String title, LocalDate date, LocalTime time,
    String location, SessionStatus status, AttendanceSummary attendanceSummary,
    boolean qrActive, Instant createdAt, Instant updatedAt) {
    public record AttendanceSummary(int present, int absent, int late, int excused, int total) {}
}
