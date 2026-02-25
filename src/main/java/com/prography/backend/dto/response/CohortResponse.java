package com.prography.backend.dto.response;

import com.prography.backend.domain.Cohort;
import java.time.Instant;

public record CohortResponse(Long id, int generation, String name, Instant createdAt) {
    public static CohortResponse from(Cohort c) {
        return new CohortResponse(c.getId(), c.getGeneration(), c.getName(), c.getCreatedAt());
    }
}
