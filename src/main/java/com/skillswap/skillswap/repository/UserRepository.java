package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    long countByActiveTrue();
    long countByRoleAndActiveTrue(String role);

    @Query("select u from User u where (:query is null or lower(u.name) like lower(concat('%', :query, '%')) or lower(u.email) like lower(concat('%', :query, '%'))) and (:role is null or u.role = :role)")
    Page<User> searchAdminUsers(@Param("query") String query, @Param("role") String role, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = 'ADMIN' and u.active = true")
    List<User> lockActiveAdmins();
}
