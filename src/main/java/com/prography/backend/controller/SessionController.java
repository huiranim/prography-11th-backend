package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.MemberSessionResponse;
import com.prography.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResponse<List<MemberSessionResponse>> getSessions() {
        return ApiResponse.ok(sessionService.getMemberSessions());
    }
}
