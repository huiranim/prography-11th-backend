package com.prography.backend.service;

import com.prography.backend.domain.AttendanceStatus;

public class PenaltyCalculator {
    public static int calculate(AttendanceStatus status, Integer lateMinutes) {
        return switch (status) {
            case PRESENT, EXCUSED -> 0;
            case ABSENT -> 10_000;
            case LATE -> Math.min((lateMinutes != null ? lateMinutes : 0) * 500, 10_000);
        };
    }
}
