package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    @Query("select a from AdminAuditLog a where (:action is null or lower(a.action) = lower(:action)) and (:entityType is null or lower(a.entityType) = lower(:entityType)) and (:actorId is null or a.actor.id = :actorId)")
    Page<AdminAuditLog> search(@Param("action") String action, @Param("entityType") String entityType,
                               @Param("actorId") Long actorId, Pageable pageable);
}
