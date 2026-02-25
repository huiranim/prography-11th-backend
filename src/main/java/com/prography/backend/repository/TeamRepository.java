package com.prography.backend.repository;

import com.prography.backend.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCohortId(Long cohortId);
}
