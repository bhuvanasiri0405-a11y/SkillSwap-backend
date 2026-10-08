package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.Skill;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillResponse {
    private Long id;
    private String name;
    private String category;
    private Skill.SkillType type;
    private Long ownerId;
    private String ownerName;

    public static SkillResponse fromEntity(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getCategory(),
                skill.getType(),
                skill.getUser().getId(),
                skill.getUser().getName()
        );
    }
}