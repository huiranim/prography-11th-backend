package com.prography.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(@NotBlank String hashValue, @NotNull Long memberId) {}
