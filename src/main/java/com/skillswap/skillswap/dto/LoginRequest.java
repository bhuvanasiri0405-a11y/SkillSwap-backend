package com.skillswap.skillswap.dto;

import lombok.Data;
import java.util.Locale;

@Data
public class LoginRequest {
    private String email;
    private String password;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
