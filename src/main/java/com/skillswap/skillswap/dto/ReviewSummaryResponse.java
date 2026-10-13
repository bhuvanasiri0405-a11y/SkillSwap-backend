package com.skillswap.skillswap.dto;

public record ReviewSummaryResponse(
        Long userId,
        double averageRating,
        long totalReviewCount
) {}
