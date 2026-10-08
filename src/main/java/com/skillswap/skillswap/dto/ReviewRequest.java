package com.skillswap.skillswap.dto;

import lombok.Data;

@Data
public class ReviewRequest {

    private Long swapRequestId;

    private Long revieweeId;

    private Integer rating;

    private String comment;
}
