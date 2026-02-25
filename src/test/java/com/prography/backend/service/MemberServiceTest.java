package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.CreateMemberRequest;
import com.prography.backend.dto.request.UpdateMemberRequest;
import com.prography.backend.dto.response.DeleteMemberResponse;
import com.prography.backend.dto.response.MemberDashboardResponse;
import com.prography.backend.dto.response.MemberDetailResponse;
import com.prography.backend.dto.response.MemberResponse;
import com.prography.backend.dto.response.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemberService 단위 테스트
 *
 * - MemberService 생성자에 int currentCohortGeneration이 포함되어 있어
 *   @InjectMocks 사용 불가 (primitive 타입은 Mock 생성 불가)
 * - @BeforeEach setUp()에서 new MemberService(...)로 직접 생성하여 해결
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    MemberService memberService;
    @Mock MemberRepository memberRepository;
    @Mock CohortRepository cohortRepository;
    @Mock PartRepository partRepository;
    @Mock TeamRepository teamRepository;
    @Mock CohortMemberRepository cohortMemberRepository;
    @Mock DepositHistoryRepository depositHistoryRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // int 타입 currentCohortGeneration은 Mock 불가 → 리터럴 값(11) 직접 전달
        memberService = new MemberService(memberRepository, cohortRepository, partRepository,
                teamRepository, cohortMemberRepository, depositHistoryRepository, passwordEncoder, 11);
    }

    /**
     * 회원 단건 조회 성공
     * - findById()가 Member를 반환하면 MemberResponse DTO로 변환해 반환하는지 검증
     * - DTO 변환 로직(loginId, name 등 필드 매핑)이 올바른지 확인
     */
    @Test
    void getMember_success() {
        Member member = Member.builder().id(1L).loginId("user1").name("홍길동")
                .phone("010-1234-5678").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse result = memberService.getMember(1L);
        assertThat(result.loginId()).isEqualTo("user1");
    }

    /**
     * 존재하지 않는 회원 조회 시 예외 발생
     * - findById()가 Optional.empty() 반환 → MEMBER_NOT_FOUND 예외를 던지는지 검증
     */
    @Test
    void getMember_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.getMember(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    /**
     * 중복 loginId로 회원 등록 시도 시 예외 발생
     * - existsByLoginId()가 true 반환 → 뒤의 로직(기수 조회, save 등)은 실행되지 않아야 함
     * - 서비스가 DUPLICATE_LOGIN_ID를 가장 먼저 검증하는지 확인
     */
    @Test
    void createMember_duplicateLoginId_throwsException() {
        when(memberRepository.existsByLoginId("dup")).thenReturn(true);
        CreateMemberRequest request = new CreateMemberRequest("dup", "pw", "이름", "010-0000-0000", 2L, null, null);
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.DUPLICATE_LOGIN_ID.getMessage());
    }

    /**
     * 존재하지 않는 기수로 회원 등록 시도 시 예외 발생
     * - loginId 중복 검사는 통과하지만, 기수 조회에서 실패하는 케이스
     */
    @Test
    void createMember_cohortNotFound_throwsException() {
        when(memberRepository.existsByLoginId("user")).thenReturn(false);
        when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
        CreateMemberRequest request = new CreateMemberRequest("user", "pw", "이름", "010-0000-0000", 99L, null, null);
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.COHORT_NOT_FOUND.getMessage());
    }

    /**
     * 회원 등록 성공
     * - save()가 반환할 객체를 Mock으로 미리 지정 (thenReturn)
     * - 서비스가 Member save → CohortMember save → DepositHistory save 순서로 호출하고
     *   최종적으로 MemberDetailResponse(generation 포함)를 올바르게 반환하는지 검증
     */
    @Test
    void createMember_success() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).name("11기").build();
        // save()가 호출될 때 반환할 객체를 수동으로 정의
        Member savedMember = Member.builder().id(1L).loginId("newuser").name("이름")
                .phone("010-0000-0000").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        CohortMember savedCm = CohortMember.builder().id(1L).member(savedMember).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();

        when(memberRepository.existsByLoginId("newuser")).thenReturn(false);
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort));
        when(memberRepository.save(any())).thenReturn(savedMember);       // any(): 어떤 객체로 호출해도 반환
        when(cohortMemberRepository.save(any())).thenReturn(savedCm);
        when(depositHistoryRepository.save(any())).thenReturn(null);      // 반환값 미사용이므로 null 지정

        CreateMemberRequest request = new CreateMemberRequest("newuser", "pw", "이름", "010-0000-0000", 2L, null, null);
        MemberDetailResponse result = memberService.createMember(request);
        assertThat(result.loginId()).isEqualTo("newuser");
        assertThat(result.generation()).isEqualTo(11); // CohortMember를 통해 기수 정보가 포함되는지 확인
    }

    /**
     * 이미 탈퇴한 회원을 다시 탈퇴 시도 시 예외 발생
     * - status가 이미 WITHDRAWN인 경우 MEMBER_ALREADY_WITHDRAWN을 던지는지 검증
     */
    @Test
    void deleteMember_alreadyWithdrawn_throwsException() {
        Member member = Member.builder().id(1L).status(MemberStatus.WITHDRAWN).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> memberService.deleteMember(1L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
    }

    /**
     * 회원 탈퇴 성공 (Soft Delete)
     * - 서비스 내부에서 member.setStatus(WITHDRAWN) 호출됨
     * - @Transactional + JPA 더티 체킹으로 실제 UPDATE 쿼리가 실행되지만,
     *   단위 테스트에서는 Java 객체의 필드값이 변경됐는지만 확인
     * - Mock이 반환한 member 객체(진짜 Java 객체)의 status 필드가 실제로 바뀐 것을 검증
     */
    @Test
    void deleteMember_success() {
        Member member = Member.builder().id(1L).loginId("user").name("이름")
                .status(MemberStatus.ACTIVE).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        DeleteMemberResponse result = memberService.deleteMember(1L);
        // DB 저장 여부가 아닌, 반환 DTO에 WITHDRAWN 상태가 반영됐는지 검증
        assertThat(result.status()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    /**
     * 존재하지 않는 회원 수정 시도 시 예외 발생
     */
    @Test
    void updateMember_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.updateMember(99L, new UpdateMemberRequest("새이름", null, null, null, null)))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    /**
     * 이름만 수정 성공
     * - Mock이 반환한 member 객체(진짜 Java 객체)에 setName()이 호출되면 실제 필드값이 바뀜
     * - 서비스가 member.setName("새이름")을 호출했는지 결과 DTO를 통해 간접 검증
     * - cohortId=null이므로 CohortMember 변경 없이 기존 CohortMember 조회만 수행
     */
    @Test
    void updateMember_nameOnly_success() {
        Member member = Member.builder().id(1L).loginId("user").name("구이름")
                .phone("010-0000-0000").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        // CohortMember 없는 경우 → generation/partName/teamName이 null로 반환됨
        when(cohortMemberRepository.findByMemberIdOrderByGenerationDesc(1L)).thenReturn(List.of());

        MemberDetailResponse result = memberService.updateMember(1L, new UpdateMemberRequest("새이름", null, null, null, null));
        assertThat(result.name()).isEqualTo("새이름");
    }

    /**
     * 존재하지 않는 파트로 회원 등록 시도 시 예외 발생
     * - loginId 중복 검사, 기수 조회는 성공
     * - partId를 지정했지만 해당 파트가 없는 경우 PART_NOT_FOUND 예외
     * - 서비스가 partId != null 조건을 확인한 뒤 partRepository를 조회하는지 검증
     */
    @Test
    void createMember_partNotFound_throwsException() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).name("11기").build();
        when(memberRepository.existsByLoginId("user")).thenReturn(false);
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort));
        when(partRepository.findById(99L)).thenReturn(Optional.empty());

        CreateMemberRequest request = new CreateMemberRequest("user", "pw", "이름", "010-0000-0000", 2L, 99L, null);
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PART_NOT_FOUND.getMessage());
    }

    /**
     * 대시보드 generation 필터 — 메모리 후처리로 11기만 반환
     * - DB에서 회원 2명 반환 (11기, 10기)
     * - generation=11 필터 적용 후 11기 회원 1명만 결과에 포함
     * - 핵심: totalElements가 DB 결과(2)가 아닌 필터 후(1)로 재계산됐는지 검증
     *   서비스 내부: filtered.size()로 재계산 → new PageResponse(..., filtered.size(), ...)
     */
    @Test
    void getMembersDashboard_generationFilter_returnsFiltered() {
        Cohort cohort11 = Cohort.builder().id(2L).generation(11).name("11기").build();
        Cohort cohort10 = Cohort.builder().id(1L).generation(10).name("10기").build();
        Member m1 = Member.builder().id(1L).loginId("user1").name("11기회원").phone("010-1111-1111")
                .status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        Member m2 = Member.builder().id(2L).loginId("user2").name("10기회원").phone("010-2222-2222")
                .status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        CohortMember cm1 = CohortMember.builder().id(1L).member(m1).cohort(cohort11)
                .deposit(100_000).excuseCount(0).build();
        CohortMember cm2 = CohortMember.builder().id(2L).member(m2).cohort(cohort10)
                .deposit(100_000).excuseCount(0).build();

        // DB에서 2명 반환 (status/search 필터 없음 → findAll 호출)
        when(memberRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(m1, m2)));
        // 각 회원의 최신 기수 정보 반환
        when(cohortMemberRepository.findByMemberIdOrderByGenerationDesc(1L)).thenReturn(List.of(cm1));
        when(cohortMemberRepository.findByMemberIdOrderByGenerationDesc(2L)).thenReturn(List.of(cm2));

        PageResponse<MemberDashboardResponse> result =
                memberService.getMembersDashboard(0, 10, null, null, 11, null, null, null);

        // DB 결과 2명 → generation=11 메모리 필터 → 1명만 남아야 함
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).loginId()).isEqualTo("user1");
    }
}
