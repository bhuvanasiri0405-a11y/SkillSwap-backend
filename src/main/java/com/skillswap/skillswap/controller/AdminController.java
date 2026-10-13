
package com.skillswap.skillswap.controller;


import com.skillswap.skillswap.dto.AdminSkillResponse;
import com.skillswap.skillswap.dto.AdminUserResponse;
import com.skillswap.skillswap.dto.AdminManagedUserResponse;
import com.skillswap.skillswap.dto.AdminManagedSkillResponse;
import com.skillswap.skillswap.dto.AdminUserDetailResponse;
import com.skillswap.skillswap.dto.AdminPageResponse;
import com.skillswap.skillswap.dto.AdminAuditLogResponse;
import com.skillswap.skillswap.dto.AdminRoleUpdateRequest;
import com.skillswap.skillswap.dto.AdminAccountStatusRequest;
import com.skillswap.skillswap.dto.SkillModerationRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.UserRepository;
import com.skillswap.skillswap.repository.SkillRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
private final SkillRepository skillRepository;
private final SwapRequestRepository swapRequestRepository;
private final com.skillswap.skillswap.service.AdminService adminService;
    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .toList();
    }

    @GetMapping("/users/page")
    public AdminPageResponse<AdminManagedUserResponse> searchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        return adminService.users(page, size, search, role);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetailResponse getUser(@PathVariable Long id) {
        return adminService.userDetails(id);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<AdminManagedUserResponse> updateRole(@PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(adminService.updateRole(authentication.getName(), id, request.role(), request.reason()));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminManagedUserResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody AdminAccountStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(adminService.updateStatus(authentication.getName(), id, request.active(), request.reason()));
    }
@GetMapping("/stats")
public java.util.Map<String, Long> getStats() {
    return adminService.stats();
}



@GetMapping("/skills")
public List<AdminSkillResponse> getAllSkills() {
    return skillRepository.findAll().stream()
            .map(skill -> new AdminSkillResponse(
                    skill.getId(),
                    skill.getName(),
                    skill.getCategory(),
                    skill.getType().name(),
                    skill.getUser().getId(),
                    skill.getUser().getName()
            ))
            .toList();
}

    @GetMapping("/skills/page")
    public AdminPageResponse<AdminManagedSkillResponse> searchSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return adminService.skills(page, size, search, category, type);
    }

    @PatchMapping("/skills/{id}/moderation")
    public ResponseEntity<AdminManagedSkillResponse> moderateSkill(@PathVariable Long id,
            @Valid @RequestBody SkillModerationRequest request, Authentication authentication) {
        return ResponseEntity.ok(adminService.moderateSkill(authentication.getName(), id, request.reason()));
    }

    @GetMapping("/audit-logs")
    public AdminPageResponse<AdminAuditLogResponse> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorId) {
        return adminService.auditLogs(page, size, action, entityType, actorId);
    }


}
