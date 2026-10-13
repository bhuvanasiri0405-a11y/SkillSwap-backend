package com.skillswap.skillswap.dto;

import java.util.List;

public record ReviewPageResponse(
        List<PublicReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
