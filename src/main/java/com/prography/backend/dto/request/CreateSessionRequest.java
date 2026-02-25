package com.prography.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateSessionRequest(
    @NotBlank String title, @NotNull LocalDate date,
    @NotNull LocalTime time, @NotBlank String location) {}
