package com.prography.backend.dto.response;

import com.prography.backend.domain.DepositType;
import java.time.Instant;

public record DepositHistoryResponse(Long id, Long cohortMemberId, DepositType type, int amount,
    int balanceAfter, Long attendanceId, String description, Instant createdAt) {}
