package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.Review;
import java.time.LocalDateTime;

public record PublicReviewResponse(
        String reviewerDisplayName,
        Integer rating,
        String comment,
        LocalDateTime reviewDate
) {
    public static PublicReviewResponse fromEntity(Review review) {
        return new PublicReviewResponse(
                review.getReviewer().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
