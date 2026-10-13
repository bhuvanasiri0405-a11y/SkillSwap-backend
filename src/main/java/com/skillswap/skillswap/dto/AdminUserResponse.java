
package com.skillswap.skillswap.dto;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String role
) {}