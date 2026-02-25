package com.prography.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMemberRequest(
    @NotBlank String loginId, @NotBlank String password,
    @NotBlank String name, @NotBlank String phone,
    @NotNull Long cohortId, Long partId, Long teamId) {}
