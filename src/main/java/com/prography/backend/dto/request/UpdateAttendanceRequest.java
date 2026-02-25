package com.prography.backend.dto.request;

import com.prography.backend.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(@NotNull AttendanceStatus status, Integer lateMinutes, String reason) {}
