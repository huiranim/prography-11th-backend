package com.prography.backend.repository;

import com.prography.backend.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsBySessionIdAndMemberId(Long sessionId, Long memberId);
    Optional<Attendance> findBySessionIdAndMemberId(Long sessionId, Long memberId);
    List<Attendance> findByMemberId(Long memberId);
    List<Attendance> findBySessionId(Long sessionId);
}
