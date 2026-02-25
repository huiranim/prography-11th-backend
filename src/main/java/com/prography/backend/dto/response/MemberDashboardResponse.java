package com.prography.backend.dto.response;

import com.prography.backend.domain.MemberRole;
import com.prography.backend.domain.MemberStatus;
import java.time.Instant;

public record MemberDashboardResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Integer generation, String partName, String teamName,
    Integer deposit, Instant createdAt, Instant updatedAt) {}
