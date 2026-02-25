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

/**
 * CohortService 단위 테스트
 *
 * - CohortService는 int 타입 필드가 없어 @InjectMocks 사용 가능
 * - @InjectMocks: @Mock 객체들을 CohortService 생성자에 자동 주입
 */
@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

    @InjectMocks CohortService cohortService;   // @Mock 객체들이 자동 주입된 진짜 서비스
    @Mock CohortRepository cohortRepository;
    @Mock PartRepository partRepository;
    @Mock TeamRepository teamRepository;

    /**
     * 기수 목록 조회 성공
     * - findAll()이 2개의 Cohort를 반환하면 2개의 CohortResponse DTO로 변환되는지 검증
     * - 첫 번째 항목의 generation 값이 올바르게 매핑됐는지 확인
     */
    @Test
    void getCohorts_returnsList() {
        Cohort c1 = Cohort.builder().id(1L).generation(10).name("10기").build();
        Cohort c2 = Cohort.builder().id(2L).generation(11).name("11기").build();
        when(cohortRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CohortResponse> result = cohortService.getCohorts();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).generation()).isEqualTo(10);
    }

    /**
     * 존재하지 않는 기수 상세 조회 시 예외 발생
     * - findById()가 Optional.empty() 반환 → COHORT_NOT_FOUND 예외를 던지는지 검증
     */
    @Test
    void getCohortDetail_notFound_throwsException() {
        when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cohortService.getCohortDetail(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.COHORT_NOT_FOUND.getMessage());
    }

    /**
     * 기수 상세 조회 성공 (파트 + 팀 포함)
     * - 기수, 파트 목록, 팀 목록을 각각 Mock에서 반환
     * - 서비스가 세 가지 조회 결과를 하나의 CohortDetailResponse로 조합하는지 검증
     */
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
        assertThat(result.parts()).hasSize(1);   // 파트 1개 포함 확인
        assertThat(result.teams()).hasSize(1);   // 팀 1개 포함 확인
    }
}
