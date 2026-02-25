package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cohort_members")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    private int deposit;

    @Column(nullable = false)
    private int excuseCount;
}
