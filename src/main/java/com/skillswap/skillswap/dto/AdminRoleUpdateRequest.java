package com.skillswap.skillswap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminRoleUpdateRequest(
        @NotBlank @Pattern(regexp = "USER|ADMIN", message = "Role must be USER or ADMIN") String role,
        @NotBlank @Size(max = 500) String reason
) {}
