package com.prography.backend.dto.response;

public record AttendanceSummaryResponse(Long memberId, int present, int absent, int late, int excused,
    int totalPenalty, Integer deposit) {}
