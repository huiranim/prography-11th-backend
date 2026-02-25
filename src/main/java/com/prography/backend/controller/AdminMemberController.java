package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberDetailResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.ok(memberService.createMember(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<MemberDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) MemberStatus status) {
        return ApiResponse.ok(memberService.getMembersDashboard(
                page, size, searchType, searchValue, generation, partName, teamName, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberDetailResponse> getMemberDetail(@PathVariable Long id) {
        return ApiResponse.ok(memberService.getMemberDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberDetailResponse> updateMember(
            @PathVariable Long id, @RequestBody UpdateMemberRequest request) {
        return ApiResponse.ok(memberService.updateMember(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DeleteMemberResponse> deleteMember(@PathVariable Long id) {
        return ApiResponse.ok(memberService.deleteMember(id));
    }
}
