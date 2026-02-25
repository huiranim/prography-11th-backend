package com.prography.backend.service;

import com.prography.backend.domain.Cohort;
import com.prography.backend.domain.Part;
import com.prography.backend.domain.Team;
import com.prography.backend.dto.response.CohortDetailResponse;
import com.prography.backend.dto.response.CohortResponse;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.CohortRepository;
import com.prography.backend.repository.PartRepository;
import com.prography.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

    @InjectMocks CohortService cohortService;
    @Mock CohortRepository cohortRepository;
    @Mock PartRepository partRepository;
    @Mock TeamRepository teamRepository;

    @Test
    void getCohorts_returnsList() {
        Cohort c1 = Cohort.builder().id(1L).generation(10).name("10기").build();
        Cohort c2 = Cohort.builder().id(2L).generation(11).name("11기").build();
        when(cohortRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CohortResponse> result = cohortService.getCohorts();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).generation()).isEqualTo(10);
    }

    @Test
    void getCohortDetail_notFound_throwsException() {
        when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cohortService.getCohortDetail(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.COHORT_NOT_FOUND.getMessage());
    }

    @Test
    void getCohortDetail_success() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).name("11기").build();
        Part part = Part.builder().id(1L).cohort(cohort).name("SERVER").build();
        Team team = Team.builder().id(1L).cohort(cohort).name("Team A").build();

        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort));
        when(partRepository.findByCohortId(2L)).thenReturn(List.of(part));
        when(teamRepository.findByCohortId(2L)).thenReturn(List.of(team));

        CohortDetailResponse result = cohortService.getCohortDetail(2L);
        assertThat(result.generation()).isEqualTo(11);
        assertThat(result.parts()).hasSize(1);
        assertThat(result.teams()).hasSize(1);
    }
}
