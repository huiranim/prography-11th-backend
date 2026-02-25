package com.prography.backend.repository;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);
    Optional<Member> findByLoginId(String loginId);
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);
    Page<Member> findByNameContaining(String name, Pageable pageable);
    Page<Member> findByLoginIdContaining(String loginId, Pageable pageable);
    Page<Member> findByPhoneContaining(String phone, Pageable pageable);
    Page<Member> findByStatusAndNameContaining(MemberStatus status, String name, Pageable pageable);
    Page<Member> findByStatusAndLoginIdContaining(MemberStatus status, String loginId, Pageable pageable);
    Page<Member> findByStatusAndPhoneContaining(MemberStatus status, String phone, Pageable pageable);
}
