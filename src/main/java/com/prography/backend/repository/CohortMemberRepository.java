package com.prography.backend.repository;

import com.prography.backend.domain.CohortMember;
import com.prography.backend.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
    Optional<CohortMember> findByMemberAndCohortId(Member member, Long cohortId);
    Optional<CohortMember> findByMemberIdAndCohortId(Long memberId, Long cohortId);
    List<CohortMember> findByCohortId(Long cohortId);

    @Query("SELECT cm FROM CohortMember cm WHERE cm.member.id = :memberId ORDER BY cm.cohort.generation DESC")
    List<CohortMember> findByMemberIdOrderByGenerationDesc(Long memberId);
}
