package com.prography.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record CohortDetailResponse(Long id, int generation, String name,
    List<PartInfo> parts, List<TeamInfo> teams, Instant createdAt) {
    public record PartInfo(Long id, String name) {}
    public record TeamInfo(Long id, String name) {}
}
