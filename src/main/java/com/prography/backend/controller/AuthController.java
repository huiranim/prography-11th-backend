package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.dto.response.MemberResponse;
import com.prography.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(MemberResponse.from(authService.login(request)));
    }
}
