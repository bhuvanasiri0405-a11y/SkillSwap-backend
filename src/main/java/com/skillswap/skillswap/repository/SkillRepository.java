package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.Skill;
import com.skillswap.skillswap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByUser(User user);
    List<Skill> findByType(Skill.SkillType type);
}
