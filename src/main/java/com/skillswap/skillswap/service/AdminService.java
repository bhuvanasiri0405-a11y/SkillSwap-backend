package com.skillswap.skillswap.service;

import com.skillswap.skillswap.dto.*;
import com.skillswap.skillswap.entity.*;
import com.skillswap.skillswap.repository.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository users;
    private final SkillRepository skills;
    private final SwapRequestRepository swaps;
    private final ReviewRepository reviews;
    private final SessionRepository sessions;
    private final AdminAuditLogRepository auditLogs;

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminManagedUserResponse> users(int page, int size, String search, String role) {
        Pageable pageable = pageable(page, size);
        String normalizedRole = optional(role);
        if (normalizedRole != null) {
            normalizedRole = normalizedRole.toUpperCase();
            if (!normalizedRole.equals("USER") && !normalizedRole.equals("ADMIN")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role filter must be USER or ADMIN");
            }
        }
        Page<AdminManagedUserResponse> result = users.searchAdminUsers(optional(search), normalizedRole, pageable)
                .map(this::userResponse);
        return AdminPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse userDetails(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<SwapRequest> userSwaps = new ArrayList<>(swaps.findBySender(user));
        userSwaps.addAll(swaps.findByReceiver(user));
        return new AdminUserDetailResponse(userResponse(user),
                skills.findByUser(user).stream().map(this::skillResponse).toList(),
                userSwaps.stream().map(SwapResponse::fromEntity).toList(),
                reviews.findByReviewee(user).stream().map(ReviewResponse::fromEntity).toList());
    }

    @Transactional
    public AdminManagedUserResponse updateRole(String actorEmail, Long targetId, String newRole, String reason) {
        User actor = requireActiveAdmin(actorEmail);
        User target = users.findById(targetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
        }
        if (target.getId().equals(actor.getId()) && !"ADMIN".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot remove your own administrator role");
        }
        List<User> activeAdmins = users.lockActiveAdmins();
        if (target.isActive() && "ADMIN".equals(target.getRole()) && "USER".equals(newRole)
                && activeAdmins.size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last active administrator");
        }
        if (!target.getRole().equals(newRole)) {
            String previous = target.getRole();
            target.setRole(newRole);
            users.save(target);
            record(actor, "USER_ROLE_CHANGED", "USER", target.getId(), "role " + previous + " -> " + newRole + "; reason: " + reason);
        }
        return userResponse(target);
    }

    @Transactional
    public AdminManagedUserResponse updateStatus(String actorEmail, Long targetId, boolean active, String reason) {
        User actor = requireActiveAdmin(actorEmail);
        User target = users.findById(targetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (target.getId().equals(actor.getId()) && !active) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own administrator account");
        }
        List<User> activeAdmins = users.lockActiveAdmins();
        if (!active && target.isActive() && "ADMIN".equals(target.getRole()) && activeAdmins.size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate the last active administrator");
        }
        if (target.isActive() != active) {
            target.setActive(active);
            users.save(target);
            record(actor, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", target.getId(), reason);
        }
        return userResponse(target);
    }

    @Transactional
    public AdminManagedSkillResponse moderateSkill(String actorEmail, Long skillId, String reason) {
        User actor = requireActiveAdmin(actorEmail);
        Skill skill = skills.findById(skillId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
        if (!skill.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill is already moderated");
        skill.setActive(false);
        skill.setModerationReason(reason);
        skill.setModeratedAt(LocalDateTime.now());
        skill.setModeratedBy(actor);
        skills.save(skill);
        record(actor, "SKILL_MODERATED", "SKILL", skill.getId(), reason);
        return skillResponse(skill);
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminManagedSkillResponse> skills(int page, int size, String search, String category, String type) {
        Skill.SkillType parsedType = null;
        if (optional(type) != null) {
            try { parsedType = Skill.SkillType.valueOf(type.trim().toUpperCase()); }
            catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill type must be TEACH or LEARN");
            }
        }
        Page<AdminManagedSkillResponse> result = skills.searchAdminSkills(optional(search), optional(category), parsedType,
                pageable(page, size)).map(this::skillResponse);
        return AdminPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminAuditLogResponse> auditLogs(int page, int size, String action, String entityType, Long actorId) {
        return AdminPageResponse.from(auditLogs.search(optional(action), optional(entityType), actorId, pageable(page, size))
                .map(AdminAuditLogResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        return Map.of(
                "totalUsers", users.count(),
                "activeUsers", users.countByActiveTrue(),
                "totalSkills", skills.count(),
                "availableSkills", skills.countByActiveTrueAndUserActiveTrue(),
                "totalSwapRequests", swaps.count(),
                "completedSwaps", swaps.countByStatus(SwapRequest.Status.COMPLETED),
                "pendingRequests", swaps.countByStatus(SwapRequest.Status.PENDING),
                "scheduledSessions", sessions.countByStatus(Session.Status.SCHEDULED)
        );
    }

    private User requireActiveAdmin(String email) {
        User actor = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required"));
        if (!actor.isActive() || !"ADMIN".equals(actor.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
        return actor;
    }

    private void record(User actor, String action, String entityType, Long entityId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActor(actor); log.setAction(action); log.setEntityType(entityType);
        log.setEntityId(entityId); log.setDetails(details); log.setCreatedAt(LocalDateTime.now());
        auditLogs.save(log);
    }

    private AdminManagedUserResponse userResponse(User user) {
        return new AdminManagedUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }

    private AdminManagedSkillResponse skillResponse(Skill skill) {
        return new AdminManagedSkillResponse(skill.getId(), skill.getName(), skill.getCategory(),
                skill.getType() == null ? null : skill.getType().name(), skill.getUser().getId(),
                skill.getUser().getName(), skill.isActive(), skill.getModerationReason(), skill.getModeratedAt());
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    private String optional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
