package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.CreateMemberRequest;
import com.prography.backend.dto.request.UpdateMemberRequest;
import com.prography.backend.dto.response.DeleteMemberResponse;
import com.prography.backend.dto.response.MemberDetailResponse;
import com.prography.backend.dto.response.MemberResponse;
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
        memberService = new MemberService(memberRepository, cohortRepository, partRepository,
                teamRepository, cohortMemberRepository, depositHistoryRepository, passwordEncoder, 11);
    }

    @Test
    void getMember_success() {
        Member member = Member.builder().id(1L).loginId("user1").name("홍길동")
                .phone("010-1234-5678").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse result = memberService.getMember(1L);
        assertThat(result.loginId()).isEqualTo("user1");
    }

    @Test
    void getMember_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.getMember(99L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    void createMember_duplicateLoginId_throwsException() {
        when(memberRepository.existsByLoginId("dup")).thenReturn(true);
        CreateMemberRequest request = new CreateMemberRequest("dup", "pw", "이름", "010-0000-0000", 2L, null, null);
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.DUPLICATE_LOGIN_ID.getMessage());
    }

    @Test
    void createMember_cohortNotFound_throwsException() {
        when(memberRepository.existsByLoginId("user")).thenReturn(false);
        when(cohortRepository.findById(99L)).thenReturn(Optional.empty());
        CreateMemberRequest request = new CreateMemberRequest("user", "pw", "이름", "010-0000-0000", 99L, null, null);
        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.COHORT_NOT_FOUND.getMessage());
    }

    @Test
    void createMember_success() {
        Cohort cohort = Cohort.builder().id(2L).generation(11).name("11기").build();
        Member savedMember = Member.builder().id(1L).loginId("newuser").name("이름")
                .phone("010-0000-0000").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        CohortMember savedCm = CohortMember.builder().id(1L).member(savedMember).cohort(cohort)
                .deposit(100_000).excuseCount(0).build();

        when(memberRepository.existsByLoginId("newuser")).thenReturn(false);
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort));
        when(memberRepository.save(any())).thenReturn(savedMember);
        when(cohortMemberRepository.save(any())).thenReturn(savedCm);
        when(depositHistoryRepository.save(any())).thenReturn(null);

        CreateMemberRequest request = new CreateMemberRequest("newuser", "pw", "이름", "010-0000-0000", 2L, null, null);
        MemberDetailResponse result = memberService.createMember(request);
        assertThat(result.loginId()).isEqualTo("newuser");
        assertThat(result.generation()).isEqualTo(11);
    }

    @Test
    void deleteMember_alreadyWithdrawn_throwsException() {
        Member member = Member.builder().id(1L).status(MemberStatus.WITHDRAWN).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> memberService.deleteMember(1L))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
    }

    @Test
    void deleteMember_success() {
        Member member = Member.builder().id(1L).loginId("user").name("이름")
                .status(MemberStatus.ACTIVE).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        DeleteMemberResponse result = memberService.deleteMember(1L);
        assertThat(result.status()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    void updateMember_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> memberService.updateMember(99L, new UpdateMemberRequest("새이름", null, null, null, null)))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    void updateMember_nameOnly_success() {
        Member member = Member.builder().id(1L).loginId("user").name("구이름")
                .phone("010-0000-0000").status(MemberStatus.ACTIVE).role(MemberRole.MEMBER).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cohortMemberRepository.findByMemberIdOrderByGenerationDesc(1L)).thenReturn(List.of());

        MemberDetailResponse result = memberService.updateMember(1L, new UpdateMemberRequest("새이름", null, null, null, null));
        assertThat(result.name()).isEqualTo("새이름");
    }
}
