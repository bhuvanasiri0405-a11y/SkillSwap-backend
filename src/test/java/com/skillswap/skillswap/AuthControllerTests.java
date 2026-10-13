package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.AuthController;
import com.skillswap.skillswap.config.SecurityConfig;
import com.skillswap.skillswap.dto.AuthResponse;
import com.skillswap.skillswap.dto.LoginRequest;
import com.skillswap.skillswap.dto.RegisterRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.UserRepository;
import com.skillswap.skillswap.security.JwtUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.skillswap.skillswap.security.JwtAuthFilter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTests {
    private UserRepository users;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthController controller;
    private Validator validator;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        controller = new AuthController(users, passwordEncoder, jwtUtil);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test void validRegistrationNormalizesEmailAndStoresOnlyEncodedPassword() {
        RegisterRequest request = registration("  Person@Example.com ", "StrongPass1!");
        assertTrue(validator.validate(request).isEmpty());
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("person@example.com")).thenReturn("test-token");

        var response = controller.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthResponse body = (AuthResponse) response.getBody();
        assertEquals("person@example.com", body.getEmail());
        assertEquals("test-token", body.getToken());
        verify(passwordEncoder).encode("StrongPass1!");
        verify(users).save(argThat(user -> "bcrypt-hash".equals(user.getPassword())
                && !"StrongPass1!".equals(user.getPassword())));
    }

    @Test void configuredPasswordEncoderRemainsBcrypt() {
        PasswordEncoder configured = new SecurityConfig(mock(JwtAuthFilter.class)).passwordEncoder();
        assertInstanceOf(BCryptPasswordEncoder.class, configured);
        String encoded = configured.encode("StrongPass1!");
        assertTrue(configured.matches("StrongPass1!", encoded));
    }

    @Test void malformedEmailIsRejected() {
        assertTrue(validator.validate(registration("not-an-email", "StrongPass1!")).stream()
                .anyMatch(error -> "email".equals(error.getPropertyPath().toString())));
    }

    @Test void shortPasswordIsRejected() {
        assertTrue(validator.validate(registration("person@example.com", "Ab1!")).stream()
                .anyMatch(error -> "password".equals(error.getPropertyPath().toString())));
    }

    @Test void missingPasswordIsRejected() {
        assertTrue(validator.validate(registration("person@example.com", null)).stream()
                .anyMatch(error -> "password".equals(error.getPropertyPath().toString())));
    }

    @Test void duplicateEmailReturnsConflict() {
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        var response = controller.register(registration(" Person@Example.com ", "StrongPass1!"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test void incorrectLoginCredentialsUseGenericResponseAndNormalizedEmailLookup() {
        User user = new User(); user.setEmail("person@example.com"); user.setPassword("bcrypt-hash");
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "bcrypt-hash")).thenReturn(false);
        LoginRequest request = new LoginRequest();
        request.setEmail(" Person@Example.com "); request.setPassword("WrongPass1!");

        var response = controller.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody());
        verify(jwtUtil, never()).generateToken(any());
    }

    private RegisterRequest registration(String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Example User"); request.setEmail(email); request.setPassword(password);
        return request;
    }
}
