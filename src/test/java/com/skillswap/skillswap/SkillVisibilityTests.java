package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.SkillController;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.SkillRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import static org.mockito.Mockito.*;

class SkillVisibilityTests {
    @Test void publicBrowseUsesOnlyActiveListingsAndActiveOwners() {
        SkillRepository skills = mock(SkillRepository.class);
        UserRepository users = mock(UserRepository.class);
        SkillController controller = new SkillController(skills, users);
        when(skills.findByActiveTrueAndUserActiveTrue()).thenReturn(List.of());

        controller.getAllSkills();

        verify(skills).findByActiveTrueAndUserActiveTrue();
        verify(skills, never()).findAll();
    }
}
