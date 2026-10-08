package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReviewee(User reviewee);

    List<Review> findByReviewer(User reviewer);

    boolean existsByReviewerAndSwapRequest(
            User reviewer,
            SwapRequest swapRequest
    );
}