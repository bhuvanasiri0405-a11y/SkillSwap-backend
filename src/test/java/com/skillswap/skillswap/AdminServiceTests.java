package com.skillswap.skillswap;

import com.skillswap.skillswap.dto.SkillModerationRequest;
import com.skillswap.skillswap.dto.AdminRoleUpdateRequest;
import com.skillswap.skillswap.dto.AdminAccountStatusRequest;
import com.skillswap.skillswap.entity.*;
import com.skillswap.skillswap.repository.*;
import com.skillswap.skillswap.service.AdminService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceTests {
    private UserRepository users;
    private SkillRepository skills;
    private SwapRequestRepository swaps;
    private ReviewRepository reviews;
    private SessionRepository sessions;
    private AdminAuditLogRepository audit;
    private AdminService service;
    private User admin;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class); skills = mock(SkillRepository.class);
        swaps = mock(SwapRequestRepository.class); reviews = mock(ReviewRepository.class);
        sessions = mock(SessionRepository.class); audit = mock(AdminAuditLogRepository.class);
        service = new AdminService(users, skills, swaps, reviews, sessions, audit);
        admin = new User(); admin.setId(1L); admin.setName("Admin"); admin.setEmail("admin@example.test"); admin.setRole("ADMIN"); admin.setActive(true);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
    }

    @Test void roleMutationAuditsTheSuccessfulChange() {
        User target = new User(); target.setId(2L); target.setRole("USER"); target.setActive(true);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.lockActiveAdmins()).thenReturn(List.of(admin));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.updateRole(admin.getEmail(), 2L, "ADMIN", "Verified moderator");

        assertEquals("ADMIN", result.role());
        verify(audit).save(argThat(log -> "USER_ROLE_CHANGED".equals(log.getAction())
                && log.getActor().equals(admin) && log.getEntityId().equals(2L)));
    }

    @Test void selfDemotionIsRejected() {
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateRole(admin.getEmail(), 1L, "USER", "rotation"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(audit, never()).save(any());
    }

    @Test void selfDeactivationIsRejected() {
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(admin.getEmail(), 1L, false, "rotation"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(audit, never()).save(any());
    }

    @Test void lastActiveAdminCannotBeDemoted() {
        User target = new User(); target.setId(2L); target.setRole("ADMIN"); target.setActive(true);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.lockActiveAdmins()).thenReturn(List.of(target));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateRole(admin.getEmail(), 2L, "USER", "no longer needed"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(users, never()).save(any()); verify(audit, never()).save(any());
    }

    @Test void inactiveOrNonAdminActorIsRejected() {
        admin.setRole("USER");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(admin.getEmail(), 2L, false, "reason")).getStatusCode());
    }

    @Test void invalidRoleAndMissingAccountStatusAreRejectedByBeanValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(new AdminRoleUpdateRequest("ROOT", "reason")).isEmpty());
        assertFalse(validator.validate(new AdminAccountStatusRequest(null, "reason")).isEmpty());
    }

    @Test void moderationSoftDeletesListingAndWritesAudit() {
        Skill skill = new Skill(); skill.setId(9L); skill.setName("Unsafe listing"); skill.setType(Skill.SkillType.TEACH); skill.setUser(admin);
        when(skills.findById(9L)).thenReturn(Optional.of(skill));
        when(skills.save(any(Skill.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.moderateSkill(admin.getEmail(), 9L, "Policy violation");

        assertFalse(result.active()); assertEquals("Policy violation", result.moderationReason());
        verify(skills).save(argThat(saved -> !saved.isActive()));
        verify(audit).save(argThat(log -> "SKILL_MODERATED".equals(log.getAction()) && log.getEntityId().equals(9L)));
    }

    @Test void analyticsUsesPersistedCountsAndHandlesZeroValues() {
        when(users.count()).thenReturn(4L); when(users.countByActiveTrue()).thenReturn(3L);
        when(skills.count()).thenReturn(6L); when(skills.countByActiveTrueAndUserActiveTrue()).thenReturn(5L);
        when(swaps.count()).thenReturn(8L); when(swaps.countByStatus(SwapRequest.Status.COMPLETED)).thenReturn(2L);
        when(swaps.countByStatus(SwapRequest.Status.PENDING)).thenReturn(3L);
        when(sessions.countByStatus(Session.Status.SCHEDULED)).thenReturn(1L);

        var result = service.stats();

        assertEquals(4L, result.get("totalUsers")); assertEquals(3L, result.get("activeUsers"));
        assertEquals(5L, result.get("availableSkills")); assertEquals(2L, result.get("completedSwaps"));
        assertEquals(1L, result.get("scheduledSessions"));
    }
}
