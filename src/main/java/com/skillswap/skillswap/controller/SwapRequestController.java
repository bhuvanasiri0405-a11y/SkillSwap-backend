package com.skillswap.skillswap.controller;

import com.skillswap.skillswap.dto.SwapResponse;
import com.skillswap.skillswap.entity.Skill;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.SkillRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/swaps")
public class SwapRequestController {

    private final SwapRequestRepository swapRequestRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public SwapRequestController(
            SwapRequestRepository swapRequestRepository,
            UserRepository userRepository,
            SkillRepository skillRepository
    ) {
        this.swapRequestRepository = swapRequestRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    // Send a new swap request
    @PostMapping
    public ResponseEntity<?> createSwapRequest(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam Long offeredSkillId,
            @RequestParam Long requestedSkillId,
            Authentication authentication
    ) {

        User sender = userRepository.findById(senderId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);

        Skill offeredSkill = skillRepository.findById(offeredSkillId).orElse(null);
        Skill requestedSkill = skillRepository.findById(requestedSkillId).orElse(null);

        User authenticatedUser = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (authenticatedUser == null || !authenticatedUser.getId().equals(senderId)) {
            return ResponseEntity.status(403).body("Only the authenticated user can create a swap request as the sender");
        }

        if (sender == null) {
            return ResponseEntity.badRequest().body("Sender not found");
        }

        if (receiver == null) {
            return ResponseEntity.badRequest().body("Receiver not found");
        }

        if (offeredSkill == null) {
            return ResponseEntity.badRequest().body("Offered skill not found");
        }

        if (requestedSkill == null) {
            return ResponseEntity.badRequest().body("Requested skill not found");
        }

        if (!offeredSkill.isActive() || !requestedSkill.isActive()) {
            return ResponseEntity.badRequest().body("Swap requests can only use active skill listings");
        }

        if (sender.getId().equals(receiver.getId())) {
            return ResponseEntity.badRequest().body("You cannot create a swap request with yourself");
        }
        if (!offeredSkill.getUser().getId().equals(sender.getId())
                || !requestedSkill.getUser().getId().equals(receiver.getId())) {
            return ResponseEntity.badRequest().body("Swap skills must belong to the matching participants");
        }

        SwapRequest swapRequest = new SwapRequest();

        swapRequest.setSender(sender);
        swapRequest.setReceiver(receiver);
        swapRequest.setOfferedSkill(offeredSkill);
        swapRequest.setRequestedSkill(requestedSkill);
        swapRequest.setStatus(SwapRequest.Status.PENDING);

        SwapRequest savedRequest = swapRequestRepository.save(swapRequest);

        return ResponseEntity.ok(SwapResponse.fromEntity(savedRequest));
    }

    // Get requests sent by a user
    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<SwapRequest>> getSentRequests(
            @PathVariable Long userId,
            Authentication authentication
    ) {

        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                swapRequestRepository.findBySender(user)
        );
    }

    // Get requests received by a user
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<SwapRequest>> getReceivedRequests(
            @PathVariable Long userId,
            Authentication authentication
    ) {

        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                swapRequestRepository.findByReceiver(user)
        );
    }
    // Get requests sent by the currently logged-in user
@GetMapping("/sent")
public ResponseEntity<List<SwapResponse>> getMySentRequests(
        Authentication authentication
) {
    String email = authentication.getName();

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
        return ResponseEntity.notFound().build();
    }

   return ResponseEntity.ok(
        swapRequestRepository.findBySender(user)
                .stream()
                .map(SwapResponse::fromEntity)
                .toList()
);
}


// Get requests received by the currently logged-in user
@GetMapping("/received")
public ResponseEntity<List<SwapResponse>> getMyReceivedRequests(
        Authentication authentication
) {
    String email = authentication.getName();

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
        return ResponseEntity.notFound().build();
    }

   return ResponseEntity.ok(
        swapRequestRepository.findByReceiver(user)
                .stream()
                .map(SwapResponse::fromEntity)
                .toList()
);
}
        // Accept a swap request
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptSwapRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null || !swapRequest.getReceiver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Only the swap recipient can accept this request");
        }

        if (swapRequest.getStatus() != SwapRequest.Status.PENDING) {
            return ResponseEntity.badRequest()
                    .body("Only pending requests can be accepted");
        }

        swapRequest.setStatus(SwapRequest.Status.ACCEPTED);

        return ResponseEntity.ok(
        SwapResponse.fromEntity(
                swapRequestRepository.save(swapRequest)
        )
);
    }


    // Reject a swap request
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectSwapRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null || !swapRequest.getReceiver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Only the swap recipient can reject this request");
        }

        if (swapRequest.getStatus() != SwapRequest.Status.PENDING) {
            return ResponseEntity.badRequest()
                    .body("Only pending requests can be rejected");
        }

       swapRequest.setStatus(SwapRequest.Status.REJECTED);

return ResponseEntity.ok(
        SwapResponse.fromEntity(
                swapRequestRepository.save(swapRequest)
        )
);
    }


    // Mark an accepted swap as completed
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeSwapRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null || (!swapRequest.getSender().getId().equals(currentUser.getId())
                && !swapRequest.getReceiver().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(403).body("Only swap participants can complete this request");
        }

        if (swapRequest.getStatus() != SwapRequest.Status.ACCEPTED) {
            return ResponseEntity.badRequest()
                    .body("Only accepted swaps can be completed");
        }

       swapRequest.setStatus(SwapRequest.Status.COMPLETED);

return ResponseEntity.ok(
        SwapResponse.fromEntity(
                swapRequestRepository.save(swapRequest)
        )
);
    }
}
