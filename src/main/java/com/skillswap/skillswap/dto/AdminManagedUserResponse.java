package com.skillswap.skillswap.dto;

public record AdminManagedUserResponse(Long id, String name, String email, String role, boolean active) {}
