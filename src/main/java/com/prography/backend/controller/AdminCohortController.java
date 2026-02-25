package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.*;
import com.prography.backend.service.CohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cohorts")
@RequiredArgsConstructor
public class AdminCohortController {

    private final CohortService cohortService;

    @GetMapping
    public ApiResponse<List<CohortResponse>> getCohorts() {
        return ApiResponse.ok(cohortService.getCohorts());
    }

    @GetMapping("/{cohortId}")
    public ApiResponse<CohortDetailResponse> getCohortDetail(@PathVariable Long cohortId) {
        return ApiResponse.ok(cohortService.getCohortDetail(cohortId));
    }
}
