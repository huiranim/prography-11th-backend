package com.prography.backend.repository;

import com.prography.backend.domain.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
    List<DepositHistory> findByCohortMemberIdOrderByCreatedAtAsc(Long cohortMemberId);
}
