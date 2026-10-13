package com.skillswap.skillswap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "admin_audit_logs")
@Data
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String entityType;

    private Long entityId;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
