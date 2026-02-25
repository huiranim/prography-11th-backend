package com.prography.backend.dto.response;

public record SessionAttendanceSummaryResponse(Long memberId, String memberName, int present, int absent,
    int late, int excused, int totalPenalty, int deposit) {}
