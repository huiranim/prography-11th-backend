package com.prography.backend.infrastructure;

import com.prography.backend.domain.*;
import com.prography.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (cohortRepository.count() > 0) return; // idempotent

        // 기수 생성
        Cohort cohort10 = cohortRepository.save(Cohort.builder()
                .generation(10).name("10기").build());
        Cohort cohort11 = cohortRepository.save(Cohort.builder()
                .generation(11).name("11기").build());

        // 파트 생성 (기수별 5개)
        String[] partNames = {"SERVER", "WEB", "iOS", "ANDROID", "DESIGN"};
        for (String partName : partNames) {
            partRepository.save(Part.builder().cohort(cohort10).name(partName).build());
            partRepository.save(Part.builder().cohort(cohort11).name(partName).build());
        }

        // 팀 생성 (11기만)
        String[] teamNames = {"Team A", "Team B", "Team C"};
        for (String teamName : teamNames) {
            teamRepository.save(Team.builder().cohort(cohort11).name(teamName).build());
        }

        // 관리자 생성
        Member admin = memberRepository.save(Member.builder()
                .loginId("admin")
                .password(passwordEncoder.encode("admin1234"))
                .name("관리자")
                .phone("010-0000-0000")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ADMIN)
                .build());

        // 관리자 CohortMember (11기)
        CohortMember adminCohortMember = cohortMemberRepository.save(CohortMember.builder()
                .member(admin)
                .cohort(cohort11)
                .deposit(100_000)
                .excuseCount(0)
                .build());

        // 보증금 초기 이력
        depositHistoryRepository.save(DepositHistory.builder()
                .cohortMember(adminCohortMember)
                .type(DepositType.INITIAL)
                .amount(100_000)
                .balanceAfter(100_000)
                .description("초기 보증금")
                .build());
    }
}
