package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.SessionController;
import com.skillswap.skillswap.dto.SessionRequest;
import com.skillswap.skillswap.entity.Session;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.SessionRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionControllerTests {
    private SessionRepository sessions;
    private SwapRequestRepository swaps;
    private UserRepository users;
    private SessionController controller;
    private User alice, bob, outsider;
    private SwapRequest swap;
    private Authentication auth;

    @BeforeEach void setup() {
        sessions = mock(SessionRepository.class); swaps = mock(SwapRequestRepository.class); users = mock(UserRepository.class);
        controller = new SessionController(sessions, swaps, users);
        alice = user(1L, "alice@test.com"); bob = user(2L, "bob@test.com"); outsider = user(3L, "other@test.com");
        swap = new SwapRequest(); swap.setId(10L); swap.setSender(alice); swap.setReceiver(bob); swap.setStatus(SwapRequest.Status.ACCEPTED);
        auth = mock(Authentication.class); when(auth.getName()).thenReturn(alice.getEmail()); when(users.findByEmail(alice.getEmail())).thenReturn(Optional.of(alice));
    }

    private User user(Long id, String email) { User u = new User(); u.setId(id); u.setName(email); u.setEmail(email); return u; }
    private Session session(Session.Status status) { Session s = new Session(); s.setId(5L); s.setSwapRequest(swap); s.setSessionDate(LocalDate.now().plusDays(1)); s.setSessionTime(LocalTime.NOON); s.setStatus(status); return s; }
    private SessionRequest request(String url) { return new SessionRequest(LocalDate.now().plusDays(1), LocalTime.NOON, url); }

    @Test void acceptedParticipantCanSchedule() {
        when(swaps.findById(10L)).thenReturn(Optional.of(swap)); when(sessions.findBySwapRequestId(10L)).thenReturn(Optional.empty());
        when(sessions.save(any())).thenAnswer(i -> { Session s = i.getArgument(0); s.setId(5L); return s; });
        var response = controller.create(10L, request("https://meet.google.com/abc-defg-hij"), auth);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test void pendingSwapCannotBeScheduled() {
        swap.setStatus(SwapRequest.Status.PENDING);
        when(swaps.findById(10L)).thenReturn(Optional.of(swap));
        var response = controller.create(10L, request(null), auth);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(sessions, never()).save(any());
    }

    @Test void nonParticipantCannotSchedule() {
        when(users.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
        when(auth.getName()).thenReturn(outsider.getEmail());
        when(swaps.findById(10L)).thenReturn(Optional.of(swap));
        var response = controller.create(10L, request(null), auth);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(sessions, never()).save(any());
    }

    @Test void nonParticipantCannotReadOrUpdate() {
        when(users.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider)); when(auth.getName()).thenReturn(outsider.getEmail());
        Session s = session(Session.Status.SCHEDULED); when(sessions.findById(5L)).thenReturn(Optional.of(s));
        assertEquals(HttpStatus.FORBIDDEN, controller.get(5L, auth).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.update(5L, request(null), auth).getStatusCode());
    }

    @Test void terminalSessionCannotTransitionAgain() {
        Session s = session(Session.Status.CANCELLED); when(sessions.findById(5L)).thenReturn(Optional.of(s));
        assertEquals(HttpStatus.BAD_REQUEST, controller.complete(5L, auth).getStatusCode());
    }

    @Test void invalidMeetUrlDoesNotMatchAllowedFormat() {
        var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(request("http://meet.google.com/abc-defg-hij")).isEmpty());
        assertFalse(validator.validate(request("https://evil.example/abc-defg-hij")).isEmpty());
        assertFalse(validator.validate(new SessionRequest(LocalDate.now(), LocalTime.NOON, null)).isEmpty());
        assertTrue(validator.validate(request("https://meet.google.com/abc-defg-hij")).isEmpty());
    }
}
