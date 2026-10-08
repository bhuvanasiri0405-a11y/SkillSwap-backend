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
@CrossOrigin(origins = "http://localhost:5173")
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
            @RequestParam Long requestedSkillId
    ) {

        User sender = userRepository.findById(senderId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);

        Skill offeredSkill = skillRepository.findById(offeredSkillId).orElse(null);
        Skill requestedSkill = skillRepository.findById(requestedSkillId).orElse(null);

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
            @PathVariable Long userId
    ) {

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
            @PathVariable Long userId
    ) {

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
            @PathVariable Long id
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
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
            @PathVariable Long id
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
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
            @PathVariable Long id
    ) {

        SwapRequest swapRequest =
                swapRequestRepository.findById(id).orElse(null);

        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
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
