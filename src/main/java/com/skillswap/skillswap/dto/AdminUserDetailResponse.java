package com.skillswap.skillswap.dto;

import java.util.List;

public record AdminUserDetailResponse(AdminManagedUserResponse user, List<AdminManagedSkillResponse> skills,
                                      List<SwapResponse> swaps, List<ReviewResponse> reviewsReceived) {}
