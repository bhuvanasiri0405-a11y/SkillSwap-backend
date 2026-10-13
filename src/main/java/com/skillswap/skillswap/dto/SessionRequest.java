package com.skillswap.skillswap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Future;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionRequest(
        @NotNull @Future LocalDate sessionDate,
        @NotNull LocalTime sessionTime,
        @Pattern(regexp = "^https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}(\\?.*)?$", message = "Must be a valid HTTPS Google Meet URL")
        String googleMeetUrl
) {}
