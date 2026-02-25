package com.prography.backend.repository;

import com.prography.backend.domain.Session;
import com.prography.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByCohortIdAndStatusNot(Long cohortId, SessionStatus status);

    @Query("""
        SELECT s FROM Session s WHERE s.cohort.id = :cohortId
        AND (:status IS NULL OR s.status = :status)
        AND (:dateFrom IS NULL OR s.date >= :dateFrom)
        AND (:dateTo IS NULL OR s.date <= :dateTo)
        ORDER BY s.date DESC
        """)
    List<Session> findByCohortIdWithFilters(Long cohortId, SessionStatus status,
                                             LocalDate dateFrom, LocalDate dateTo);
}
