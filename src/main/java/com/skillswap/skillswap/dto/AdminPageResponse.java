package com.skillswap.skillswap.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AdminPageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> AdminPageResponse<T> from(Page<T> result) {
        return new AdminPageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
