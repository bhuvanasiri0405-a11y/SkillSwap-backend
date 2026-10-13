package com.skillswap.skillswap;

import com.skillswap.skillswap.security.CustomUserDetailsService;
import com.skillswap.skillswap.security.JwtAuthFilter;
import com.skillswap.skillswap.security.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InactiveJwtTests {
    @Test void inactiveAccountCannotAuthenticateWithExistingJwt() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
        UserDetails details = mock(UserDetails.class);
        when(jwt.isTokenValid("opaque-test-token")).thenReturn(true);
        when(jwt.extractEmail("opaque-test-token")).thenReturn("disabled@example.test");
        when(detailsService.loadUserByUsername("disabled@example.test")).thenReturn(details);
        when(details.isEnabled()).thenReturn(false);
        JwtAuthFilter filter = new JwtAuthFilter(jwt, detailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer opaque-test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
