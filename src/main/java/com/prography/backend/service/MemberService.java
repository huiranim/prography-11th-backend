package com.prography.backend.service;

import com.prography.backend.domain.*;
import com.prography.backend.dto.request.*;
import com.prography.backend.dto.response.*;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int currentCohortGeneration;

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberDetailResponse createMember(CreateMemberRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new AppException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        Cohort cohort = cohortRepository.findById(request.cohortId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        Part part = null;
        if (request.partId() != null) {
            part = partRepository.findById(request.partId())
                    .orElseThrow(() -> new AppException(ErrorCode.PART_NOT_FOUND));
        }
        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        }

        Member member = memberRepository.save(Member.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.MEMBER)
                .build());

        CohortMember cohortMember = cohortMemberRepository.save(CohortMember.builder()
                .member(member).cohort(cohort).part(part).team(team)
                .deposit(100_000).excuseCount(0).build());

        depositHistoryRepository.save(DepositHistory.builder()
                .cohortMember(cohortMember).type(DepositType.INITIAL)
                .amount(100_000).balanceAfter(100_000).description("초기 보증금").build());

        return toMemberDetailResponse(member, cohortMember);
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberDashboardResponse> getMembersDashboard(
            int page, int size, String searchType, String searchValue,
            Integer generation, String partName, String teamName, MemberStatus status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Member> memberPage;

        if (status != null && searchType != null && searchValue != null) {
            memberPage = switch (searchType) {
                case "name" -> memberRepository.findByStatusAndNameContaining(status, searchValue, pageable);
                case "loginId" -> memberRepository.findByStatusAndLoginIdContaining(status, searchValue, pageable);
                case "phone" -> memberRepository.findByStatusAndPhoneContaining(status, searchValue, pageable);
                default -> memberRepository.findByStatus(status, pageable);
            };
        } else if (status != null) {
            memberPage = memberRepository.findByStatus(status, pageable);
        } else if (searchType != null && searchValue != null) {
            memberPage = switch (searchType) {
                case "name" -> memberRepository.findByNameContaining(searchValue, pageable);
                case "loginId" -> memberRepository.findByLoginIdContaining(searchValue, pageable);
                case "phone" -> memberRepository.findByPhoneContaining(searchValue, pageable);
                default -> memberRepository.findAll(pageable);
            };
        } else {
            memberPage = memberRepository.findAll(pageable);
        }

        // 메모리 레벨 필터 (generation, partName, teamName)
        List<MemberDashboardResponse> filtered = memberPage.getContent().stream()
                .map(m -> {
                    List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(m.getId());
                    CohortMember latestCm = cms.isEmpty() ? null : cms.get(0);
                    return toMemberDashboardResponse(m, latestCm);
                })
                .filter(r -> generation == null || Objects.equals(r.generation(), generation))
                .filter(r -> partName == null || partName.equals(r.partName()))
                .filter(r -> teamName == null || teamName.equals(r.teamName()))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filtered.size() / size);
        List<MemberDashboardResponse> paged = filtered.stream()
                .skip((long) page * size).limit(size).collect(Collectors.toList());

        return new PageResponse<>(paged, page, size, filtered.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse getMemberDetail(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(id);
        CohortMember latestCm = cms.isEmpty() ? null : cms.get(0);
        return toMemberDetailResponse(member, latestCm);
    }

    @Transactional
    public MemberDetailResponse updateMember(Long id, UpdateMemberRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (request.name() != null) member.setName(request.name());
        if (request.phone() != null) member.setPhone(request.phone());

        CohortMember cohortMember = null;
        if (request.cohortId() != null) {
            Cohort cohort = cohortRepository.findById(request.cohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
            Part part = null;
            if (request.partId() != null) {
                part = partRepository.findById(request.partId())
                        .orElseThrow(() -> new AppException(ErrorCode.PART_NOT_FOUND));
            }
            Team team = null;
            if (request.teamId() != null) {
                team = teamRepository.findById(request.teamId())
                        .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
            }
            Optional<CohortMember> existing = cohortMemberRepository.findByMemberAndCohortId(member, request.cohortId());
            if (existing.isPresent()) {
                cohortMember = existing.get();
                cohortMember.setPart(part);
                cohortMember.setTeam(team);
            } else {
                cohortMember = cohortMemberRepository.save(CohortMember.builder()
                        .member(member).cohort(cohort).part(part).team(team)
                        .deposit(100_000).excuseCount(0).build());
            }
        } else {
            List<CohortMember> cms = cohortMemberRepository.findByMemberIdOrderByGenerationDesc(id);
            cohortMember = cms.isEmpty() ? null : cms.get(0);
        }
        return toMemberDetailResponse(member, cohortMember);
    }

    @Transactional
    public DeleteMemberResponse deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }
        member.setStatus(MemberStatus.WITHDRAWN);
        return DeleteMemberResponse.from(member);
    }

    private MemberDetailResponse toMemberDetailResponse(Member m, CohortMember cm) {
        return new MemberDetailResponse(
                m.getId(), m.getLoginId(), m.getName(), m.getPhone(), m.getStatus(), m.getRole(),
                cm != null ? cm.getCohort().getGeneration() : null,
                cm != null && cm.getPart() != null ? cm.getPart().getName() : null,
                cm != null && cm.getTeam() != null ? cm.getTeam().getName() : null,
                m.getCreatedAt(), m.getUpdatedAt());
    }

    private MemberDashboardResponse toMemberDashboardResponse(Member m, CohortMember cm) {
        return new MemberDashboardResponse(
                m.getId(), m.getLoginId(), m.getName(), m.getPhone(), m.getStatus(), m.getRole(),
                cm != null ? cm.getCohort().getGeneration() : null,
                cm != null && cm.getPart() != null ? cm.getPart().getName() : null,
                cm != null && cm.getTeam() != null ? cm.getTeam().getName() : null,
                cm != null ? cm.getDeposit() : null,
                m.getCreatedAt(), m.getUpdatedAt());
    }
}
