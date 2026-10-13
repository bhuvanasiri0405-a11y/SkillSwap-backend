package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.AdminAuditLog;
import java.time.LocalDateTime;

public record AdminAuditLogResponse(Long id, Long actorId, String actorName, String action,
                                    String entityType, Long entityId, String details, LocalDateTime createdAt) {
    public static AdminAuditLogResponse fromEntity(AdminAuditLog log) {
        return new AdminAuditLogResponse(log.getId(), log.getActor().getId(), log.getActor().getName(),
                log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }
}
