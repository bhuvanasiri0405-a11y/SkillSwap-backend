package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.Skill;
import lombok.Data;

@Data
public class SkillRequest {
    private String name;
    private String category;
    private Skill.SkillType type; // TEACH or LEARN
}
