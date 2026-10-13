package com.skillswap.skillswap.dto;

public record AdminManagedSkillResponse(Long id, String name, String category, String type,
                                        Long userId, String userName, boolean active,
                                        String moderationReason, java.time.LocalDateTime moderatedAt) {}
