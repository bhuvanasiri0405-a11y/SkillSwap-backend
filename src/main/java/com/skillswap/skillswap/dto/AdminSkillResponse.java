
package com.skillswap.skillswap.dto;

public record AdminSkillResponse(
        Long id,
        String name,
        String category,
        String type,
        Long userId,
        String userName
) {}