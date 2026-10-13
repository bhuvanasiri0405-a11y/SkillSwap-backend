package com.skillswap.skillswap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillModerationRequest(@NotBlank @Size(max = 500) String reason) {}
