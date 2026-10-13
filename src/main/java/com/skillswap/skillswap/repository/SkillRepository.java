package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.Skill;
import com.skillswap.skillswap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByUser(User user);
    List<Skill> findByUserAndActiveTrue(User user);
    List<Skill> findByType(Skill.SkillType type);
    List<Skill> findByActiveTrueAndUserActiveTrue();
    long countByActiveTrueAndUserActiveTrue();

    @Query("select s from Skill s where (:query is null or lower(s.name) like lower(concat('%', :query, '%'))) and (:category is null or lower(s.category) = lower(:category)) and (:type is null or s.type = :type)")
    Page<Skill> searchAdminSkills(@Param("query") String query, @Param("category") String category,
                                  @Param("type") Skill.SkillType type, Pageable pageable);
}
