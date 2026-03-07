package com.smartsquad.backend.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Player Entity
 * This class represents the 'players' table PostgreSQL database.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Primary skill level from 1 (Beginner) to 5 (Pro).
     */
    @Column(name = "skill_level", nullable = false)
    private int skillLevel;

    /**
     * Secondary skill used for tie-breaking during the balancing algorithm.
     */
    @Column(name = "secondary_skill")
    private int secondarySkill;

    @Column(name = "must_to_be_with")
    private String hasToBeWith;

    @Column(name = "cannot_be_with")
    private String cannotBeWith;
}