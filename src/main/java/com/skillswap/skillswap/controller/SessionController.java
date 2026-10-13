package com.skillswap.skillswap.controller;

import com.skillswap.skillswap.dto.SessionRequest;
import com.skillswap.skillswap.dto.SessionResponse;
import com.skillswap.skillswap.entity.Session;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.SessionRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionRepository sessions;
    private final SwapRequestRepository swaps;
    private final UserRepository users;

    public SessionController(SessionRepository sessions, SwapRequestRepository swaps, UserRepository users) {
        this.sessions = sessions; this.swaps = swaps; this.users = users;
    }

    private User current(Authentication auth) {
        return users.findByEmail(auth.getName()).orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
    private boolean participant(Session s, User u) {
        return s.getSwapRequest().getSender().getId().equals(u.getId()) || s.getSwapRequest().getReceiver().getId().equals(u.getId());
    }
    private ResponseEntity<?> unavailable() { return ResponseEntity.notFound().build(); }

    @PostMapping("/swaps/{swapId}")
    public ResponseEntity<?> create(@PathVariable Long swapId, @Valid @RequestBody SessionRequest request, Authentication auth) {
        User user = current(auth);
        SwapRequest swap = swaps.findById(swapId).orElse(null);
        if (swap == null) return unavailable();
        if (!swap.getSender().getId().equals(user.getId()) && !swap.getReceiver().getId().equals(user.getId())) return ResponseEntity.status(403).build();
        if (swap.getStatus() != SwapRequest.Status.ACCEPTED) return ResponseEntity.badRequest().body("Session requires an accepted swap");
        if (sessions.findBySwapRequestId(swapId).isPresent()) return ResponseEntity.badRequest().body("A session already exists for this swap; update it instead");
        Session s = new Session(); s.setSwapRequest(swap); apply(s, request);
        return ResponseEntity.status(201).body(SessionResponse.fromEntity(sessions.save(s)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SessionRequest request, Authentication auth) {
        Session s = sessions.findById(id).orElse(null); if (s == null) return unavailable();
        if (!participant(s, current(auth))) return ResponseEntity.status(403).build();
        if (s.getStatus() != Session.Status.SCHEDULED) return ResponseEntity.badRequest().body("Only scheduled sessions can be updated");
        apply(s, request); return ResponseEntity.ok(SessionResponse.fromEntity(sessions.save(s)));
    }

    @GetMapping
    public List<SessionResponse> list(Authentication auth) {
        User user = current(auth);
        return sessions.findBySwapRequestSenderOrSwapRequestReceiverOrderBySessionDateAscSessionTimeAsc(user, user)
                .stream().filter(s -> participant(s, user)).map(SessionResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication auth) {
        Session s = sessions.findById(id).orElse(null); if (s == null) return unavailable();
        if (!participant(s, current(auth))) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(SessionResponse.fromEntity(s));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, Authentication auth) { return transition(id, auth, Session.Status.CANCELLED); }
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id, Authentication auth) { return transition(id, auth, Session.Status.COMPLETED); }

    private ResponseEntity<?> transition(Long id, Authentication auth, Session.Status target) {
        Session s = sessions.findById(id).orElse(null); if (s == null) return unavailable();
        if (!participant(s, current(auth))) return ResponseEntity.status(403).build();
        if (s.getStatus() != Session.Status.SCHEDULED) return ResponseEntity.badRequest().body("Only scheduled sessions can be cancelled or completed");
        s.setStatus(target); return ResponseEntity.ok(SessionResponse.fromEntity(sessions.save(s)));
    }

    private void apply(Session s, SessionRequest request) {
        s.setSessionDate(request.sessionDate()); s.setSessionTime(request.sessionTime()); s.setGoogleMeetUrl(request.googleMeetUrl());
    }
}
