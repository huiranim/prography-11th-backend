package com.prography.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "cohorts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int generation;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    private Instant createdAt;
}
