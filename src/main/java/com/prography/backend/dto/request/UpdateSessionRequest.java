package com.prography.backend.dto.request;

import com.prography.backend.domain.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateSessionRequest(String title, LocalDate date, LocalTime time, String location, SessionStatus status) {}
