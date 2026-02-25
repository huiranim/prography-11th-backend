package com.prography.backend.dto.response;

import com.prography.backend.domain.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record MemberSessionResponse(Long id, String title, LocalDate date, LocalTime time,
    String location, SessionStatus status, Instant createdAt, Instant updatedAt) {}
