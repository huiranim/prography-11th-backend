package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttendanceResponse> register(@Valid @RequestBody RegisterAttendanceRequest request) {
        return ApiResponse.ok(attendanceService.registerAttendance(request));
    }

    @PutMapping("/attendances/{id}")
    public ApiResponse<AttendanceResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.ok(attendanceService.updateAttendance(id, request));
    }

    @GetMapping("/attendances/sessions/{sessionId}/summary")
    public ApiResponse<List<SessionAttendanceSummaryResponse>> sessionSummary(@PathVariable Long sessionId) {
        return ApiResponse.ok(attendanceService.getSessionAttendanceSummary(sessionId));
    }

    @GetMapping("/attendances/members/{memberId}")
    public ApiResponse<MemberAttendanceDetailResponse> memberDetail(@PathVariable Long memberId) {
        return ApiResponse.ok(attendanceService.getMemberAttendanceDetail(memberId));
    }

    @GetMapping("/attendances/sessions/{sessionId}")
    public ApiResponse<SessionAttendancesResponse> sessionAttendances(@PathVariable Long sessionId) {
        return ApiResponse.ok(attendanceService.getSessionAttendances(sessionId));
    }

    @GetMapping("/cohort-members/{cohortMemberId}/deposits")
    public ApiResponse<List<DepositHistoryResponse>> depositHistory(@PathVariable Long cohortMemberId) {
        return ApiResponse.ok(attendanceService.getDepositHistory(cohortMemberId));
    }
}
