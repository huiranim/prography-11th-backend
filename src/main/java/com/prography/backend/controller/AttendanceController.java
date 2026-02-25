package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.request.CheckInRequest;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok(attendanceService.checkIn(request));
    }

    @GetMapping("/attendances")
    public ApiResponse<List<MyAttendanceResponse>> getMyAttendances(@RequestParam Long memberId) {
        return ApiResponse.ok(attendanceService.getMyAttendances(memberId));
    }

    @GetMapping("/members/{memberId}/attendance-summary")
    public ApiResponse<AttendanceSummaryResponse> getAttendanceSummary(@PathVariable Long memberId) {
        return ApiResponse.ok(attendanceService.getAttendanceSummary(memberId));
    }
}
