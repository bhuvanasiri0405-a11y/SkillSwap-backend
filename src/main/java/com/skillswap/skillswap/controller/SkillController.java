package com.skillswap.skillswap.controller;

import com.skillswap.skillswap.dto.SkillRequest;
import com.skillswap.skillswap.dto.SkillResponse;
import com.skillswap.skillswap.entity.Skill;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.SkillRepository;
import com.skillswap.skillswap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    // Helper: get the currently logged-in User entity from the JWT-authenticated email
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Add a new skill (teach or learn) for the logged-in user
    @PostMapping
    public ResponseEntity<?> addSkill(@RequestBody SkillRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setCategory(request.getCategory());
        skill.setType(request.getType());
        skill.setUser(user);

        skillRepository.save(skill);
        return ResponseEntity.ok(SkillResponse.fromEntity(skill));
    }

    // Get all skills belonging to the logged-in user
    @GetMapping("/my-skills")
    public ResponseEntity<?> getMySkills(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Skill> skills = skillRepository.findByUser(user);
        List<SkillResponse> response = skills.stream().map(SkillResponse::fromEntity).toList();
        return ResponseEntity.ok(response);
    }

    // Browse all skills from all users (for finding someone to swap with)
    @GetMapping
    public ResponseEntity<?> getAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillResponse> response = skills.stream().map(SkillResponse::fromEntity).toList();
        return ResponseEntity.ok(response);
    }

    // Delete a skill (only if it belongs to the logged-in user)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        if (!skill.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You can only delete your own skills");
        }

        skillRepository.delete(skill);
        return ResponseEntity.ok("Skill deleted");
    }
}
