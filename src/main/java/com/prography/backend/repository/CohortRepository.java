package com.prography.backend.repository;

import com.prography.backend.domain.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByGeneration(int generation);
}
