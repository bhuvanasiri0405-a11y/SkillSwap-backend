package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.Session;
import com.skillswap.skillswap.entity.User;
import java.util.List;
import java.util.Optional;
import com.skillswap.skillswap.entity.SwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findBySwapRequestId(Long swapRequestId);
    List<Session> findBySwapRequestSenderOrSwapRequestReceiverOrderBySessionDateAscSessionTimeAsc(User sender, User receiver);
    long countByStatus(Session.Status status);
}
