package com.skillswap.skillswap.controller;

import com.skillswap.skillswap.dto.ReviewRequest;
import com.skillswap.skillswap.dto.ReviewResponse;
import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SwapRequestRepository swapRequestRepository;

   @PostMapping
public ResponseEntity<?> createReview(
        @RequestBody ReviewRequest request,
        Authentication authentication
) {
         System.out.println("REVIEW ENDPOINT REACHED");
System.out.println("AUTHENTICATED USER: " + authentication.getName());
        String email = authentication.getName();

        User reviewer = userRepository.findByEmail(email).orElse(null);

        if (reviewer == null) {
            return ResponseEntity.status(404).body("Reviewer not found");
        }

        SwapRequest swapRequest =
        swapRequestRepository.findById(request.getSwapRequestId()).orElse(null);
        if (swapRequest == null) {
            return ResponseEntity.badRequest()
                    .body("Swap request not found");
        }

        if (swapRequest.getStatus() != SwapRequest.Status.COMPLETED) {
            return ResponseEntity.badRequest()
                    .body("You can review only completed swaps");
        }

        User reviewee =
        userRepository.findById(request.getRevieweeId()).orElse(null);

        if (reviewee == null) {
            return ResponseEntity.badRequest()
                    .body("Reviewee not found");
        }

        if (reviewer.getId().equals(reviewee.getId())) {
            return ResponseEntity.badRequest()
                    .body("You cannot review yourself");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest()
                    .body("Rating must be between 1 and 5");
        }

        if (reviewRepository.existsByReviewerAndSwapRequest(
                reviewer,
                swapRequest
        )) {
            return ResponseEntity.badRequest()
                    .body("You have already reviewed this swap");
        }

        Review review = new Review();

        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setSwapRequest(swapRequest);
        review.setRating(request.getRating());
review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        return ResponseEntity.ok(
                ReviewResponse.fromEntity(savedReview)
        );
    }
}

