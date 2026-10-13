package com.skillswap.skillswap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountStatusRequest(
        @NotNull Boolean active,
        @NotBlank @Size(max = 500) String reason
) {}
