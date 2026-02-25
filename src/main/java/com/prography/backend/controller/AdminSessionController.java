package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.domain.SessionStatus;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.QrCodeService;
import com.prography.backend.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final SessionService sessionService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getSessions(
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        return ApiResponse.ok(sessionService.getAdminSessions(status, dateFrom, dateTo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(sessionService.createSession(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SessionResponse> updateSession(
            @PathVariable Long id, @RequestBody UpdateSessionRequest request) {
        return ApiResponse.ok(sessionService.updateSession(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<SessionResponse> deleteSession(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.deleteSession(id));
    }

    @PostMapping("/{sessionId}/qrcodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrCodeResponse> createQrCode(@PathVariable Long sessionId) {
        return ApiResponse.ok(qrCodeService.createQrCode(sessionId));
    }
}
