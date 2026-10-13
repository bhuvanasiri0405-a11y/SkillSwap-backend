package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.SwapRequest;

public record ReviewStatusResponse(
        Long swapRequestId,
        SwapRequest.Status swapStatus,
        boolean reviewed,
        boolean canReview
) {}
