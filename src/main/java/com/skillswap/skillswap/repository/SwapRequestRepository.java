package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    List<SwapRequest> findBySender(User sender);

    List<SwapRequest> findByReceiver(User receiver);

    List<SwapRequest> findByReceiverAndStatus(
            User receiver,
            SwapRequest.Status status
    );

    List<SwapRequest> findBySenderAndStatus(
            User sender,
            SwapRequest.Status status
    );
}
