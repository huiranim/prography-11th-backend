package com.prography.backend.dto.response;

import java.util.List;

public record SessionAttendancesResponse(Long sessionId, String sessionTitle, List<AttendanceResponse> attendances) {}
