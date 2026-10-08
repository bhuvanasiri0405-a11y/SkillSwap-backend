package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Long reviewerId;
    private String reviewerName;

    private Long revieweeId;
    private String revieweeName;

    private Long swapRequestId;

    private Integer rating;
    private String comment;

    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review review) {

        return new ReviewResponse(
                review.getId(),

                review.getReviewer().getId(),
                review.getReviewer().getName(),

                review.getReviewee().getId(),
                review.getReviewee().getName(),

                review.getSwapRequest().getId(),

                review.getRating(),
                review.getComment(),

                review.getCreatedAt()
        );
    }
}
