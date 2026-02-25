package com.prography.backend.dto.response;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import java.time.Instant;

public record DeleteMemberResponse(Long id, String loginId, String name, MemberStatus status, Instant updatedAt) {
    public static DeleteMemberResponse from(Member m) {
        return new DeleteMemberResponse(m.getId(), m.getLoginId(), m.getName(), m.getStatus(), m.getUpdatedAt());
    }
}
