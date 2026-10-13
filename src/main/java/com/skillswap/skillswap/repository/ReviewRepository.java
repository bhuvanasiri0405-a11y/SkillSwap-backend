package com.skillswap.skillswap.repository;

import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReviewee(User reviewee);

    List<Review> findByReviewer(User reviewer);

    boolean existsByReviewerAndSwapRequest(
            User reviewer,
            SwapRequest swapRequest
    );

    long countByReviewee(User reviewee);

    @Query("select r from Review r where r.reviewee = :reviewee and r.swapRequest.status = :status")
    Page<Review> findReceivedByRevieweeAndSwapStatus(
            @Param("reviewee") User reviewee,
            @Param("status") SwapRequest.Status status,
            Pageable pageable
    );

    @Query("select count(r) from Review r where r.reviewee = :reviewee and r.swapRequest.status = :status")
    long countReceivedByRevieweeAndSwapStatus(
            @Param("reviewee") User reviewee,
            @Param("status") SwapRequest.Status status
    );

    @Query("select avg(r.rating) from Review r where r.reviewee = :reviewee and r.swapRequest.status = :status")
    Double averageRatingByRevieweeAndSwapStatus(
            @Param("reviewee") User reviewee,
            @Param("status") SwapRequest.Status status
    );
}
