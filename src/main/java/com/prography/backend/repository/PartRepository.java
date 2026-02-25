package com.prography.backend.repository;

import com.prography.backend.domain.Cohort;
import com.prography.backend.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
    List<Part> findByCohort(Cohort cohort);
    List<Part> findByCohortId(Long cohortId);
}
