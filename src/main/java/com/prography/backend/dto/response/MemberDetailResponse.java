package com.prography.backend.dto.response;

import com.prography.backend.domain.MemberRole;
import com.prography.backend.domain.MemberStatus;
import java.time.Instant;

public record MemberDetailResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Integer generation, String partName, String teamName,
    Instant createdAt, Instant updatedAt) {}
