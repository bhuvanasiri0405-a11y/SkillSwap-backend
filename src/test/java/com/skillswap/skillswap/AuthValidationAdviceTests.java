package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.AuthValidationAdvice;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthValidationAdviceTests {
    private final AuthValidationAdvice advice = new AuthValidationAdvice();

    @Test void singlePasswordFailureUsesPasswordMessageAtTopLevel() {
        String passwordMessage = "Password must include uppercase and lowercase letters, a digit, and a special character";
        var response = advice.handleInvalidRequest(exception(
                new FieldError("registerRequest", "password", "invalid", false, null, null, passwordMessage)));

        assertEquals(passwordMessage, response.getBody().get("message"));
        assertEquals(Map.of("password", passwordMessage), response.getBody().get("errors"));
    }

    @Test void singleEmailFailureUsesClearEmailMessageAtTopLevel() {
        String emailMessage = "Please enter a valid email address.";
        var response = advice.handleInvalidRequest(exception(
                new FieldError("registerRequest", "email", "invalid", false, null, null, emailMessage)));

        assertEquals(emailMessage, response.getBody().get("message"));
        assertEquals(Map.of("email", emailMessage), response.getBody().get("errors"));
    }

    @Test void multipleFailuresKeepEachFieldMessageAndCombineTheSummary() {
        String emailMessage = "Please enter a valid email address.";
        String passwordMessage = "Password must be at least 8 characters";
        var response = advice.handleInvalidRequest(exception(
                new FieldError("registerRequest", "email", "invalid", false, null, null, emailMessage),
                new FieldError("registerRequest", "password", "short", false, null, null, passwordMessage)));

        assertEquals(Map.of("email", emailMessage, "password", passwordMessage), response.getBody().get("errors"));
        String summary = (String) response.getBody().get("message");
        assertTrue(summary.contains("email: " + emailMessage));
        assertTrue(summary.contains("password: " + passwordMessage));
    }

    private MethodArgumentNotValidException exception(FieldError... errors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errors));
        return new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
    }
}
