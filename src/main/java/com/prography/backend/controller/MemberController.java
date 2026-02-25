package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.MemberResponse;
import com.prography.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getMember(@PathVariable Long id) {
        return ApiResponse.ok(memberService.getMember(id));
    }
}
