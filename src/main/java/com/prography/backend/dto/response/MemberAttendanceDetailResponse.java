package com.prography.backend.dto.response;

import java.util.List;

public record MemberAttendanceDetailResponse(Long memberId, String memberName, Integer generation,
    String partName, String teamName, Integer deposit, Integer excuseCount, List<AttendanceResponse> attendances) {}
