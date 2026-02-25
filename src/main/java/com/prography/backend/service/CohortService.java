package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<CohortResponse> getCohorts() {
        return cohortRepository.findAll().stream()
                .map(CohortResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CohortDetailResponse getCohortDetail(Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        List<CohortDetailResponse.PartInfo> parts = partRepository.findByCohortId(cohortId).stream()
                .map(p -> new CohortDetailResponse.PartInfo(p.getId(), p.getName())).toList();
        List<CohortDetailResponse.TeamInfo> teams = teamRepository.findByCohortId(cohortId).stream()
                .map(t -> new CohortDetailResponse.TeamInfo(t.getId(), t.getName())).toList();
        return new CohortDetailResponse(cohort.getId(), cohort.getGeneration(), cohort.getName(),
                parts, teams, cohort.getCreatedAt());
    }
}
