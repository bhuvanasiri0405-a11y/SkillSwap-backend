package com.skillswap.skillswap.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    public enum SkillType {
        TEACH, LEARN
    }
}
   