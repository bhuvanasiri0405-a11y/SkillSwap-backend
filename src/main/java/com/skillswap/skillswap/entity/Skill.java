package com.skillswap.skillswap.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "skills")
@Data
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    private SkillType type;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean active = true;

    @Column(length = 500)
    private String moderationReason;

    private LocalDateTime moderatedAt;

    @ManyToOne
    @JoinColumn(name = "moderated_by_id")
    private User moderatedBy;

    public enum SkillType {
        TEACH, LEARN
    }
}

