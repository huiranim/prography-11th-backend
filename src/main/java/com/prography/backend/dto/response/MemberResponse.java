package com.prography.backend.dto.response;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberRole;
import com.prography.backend.domain.MemberStatus;
import java.time.Instant;

public record MemberResponse(
    Long id, String loginId, String name, String phone,
    MemberStatus status, MemberRole role,
    Instant createdAt, Instant updatedAt) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getLoginId(), m.getName(), m.getPhone(),
            m.getStatus(), m.getRole(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
