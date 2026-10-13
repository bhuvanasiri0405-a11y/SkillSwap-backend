package com.skillswap.skillswap.controller;

import com.skillswap.skillswap.dto.ReviewRequest;
import com.skillswap.skillswap.dto.PublicReviewResponse;
import com.skillswap.skillswap.dto.ReviewPageResponse;
import com.skillswap.skillswap.dto.ReviewResponse;
import com.skillswap.skillswap.dto.ReviewStatusResponse;
import com.skillswap.skillswap.dto.ReviewSummaryResponse;
import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SwapRequestRepository swapRequestRepository;

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getReceivedReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Page must be non-negative and size must be between 1 and 100"));
        }

        User reviewee = userRepository.findById(userId).orElse(null);
        if (reviewee == null) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<PublicReviewResponse> reviews = reviewRepository
                .findReceivedByRevieweeAndSwapStatus(reviewee, SwapRequest.Status.COMPLETED, pageable)
                .map(PublicReviewResponse::fromEntity);

        return ResponseEntity.ok(new ReviewPageResponse(
                reviews.getContent(), reviews.getNumber(), reviews.getSize(),
                reviews.getTotalElements(), reviews.getTotalPages()));
    }

    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<?> getReviewSummary(@PathVariable Long userId) {
        User reviewee = userRepository.findById(userId).orElse(null);
        if (reviewee == null) {
            return ResponseEntity.notFound().build();
        }

        long count = reviewRepository.countReceivedByRevieweeAndSwapStatus(
                reviewee, SwapRequest.Status.COMPLETED);
        Double average = reviewRepository.averageRatingByRevieweeAndSwapStatus(
                reviewee, SwapRequest.Status.COMPLETED);
        return ResponseEntity.ok(new ReviewSummaryResponse(
                reviewee.getId(), average == null ? 0.0 : average, count));
    }

    @GetMapping("/swaps/{swapRequestId}/status")
    public ResponseEntity<ReviewStatusResponse> getReviewStatus(
            @PathVariable Long swapRequestId,
            Authentication authentication
    ) {
        User currentUser = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SwapRequest swapRequest = swapRequestRepository.findById(swapRequestId).orElse(null);
        if (swapRequest == null) {
            return ResponseEntity.notFound().build();
        }

        boolean participant = swapRequest.getSender().getId().equals(currentUser.getId())
                || swapRequest.getReceiver().getId().equals(currentUser.getId());
        if (!participant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean reviewed = reviewRepository.existsByReviewerAndSwapRequest(currentUser, swapRequest);
        boolean canReview = swapRequest.getStatus() == SwapRequest.Status.COMPLETED && !reviewed;
        return ResponseEntity.ok(new ReviewStatusResponse(
                swapRequest.getId(), swapRequest.getStatus(), reviewed, canReview));
    }

   @PostMapping
public ResponseEntity<?> createReview(
        @RequestBody ReviewRequest request,
        Authentication authentication
) {
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

        boolean reviewerIsSender = swapRequest.getSender().getId().equals(reviewer.getId());
        boolean reviewerIsReceiver = swapRequest.getReceiver().getId().equals(reviewer.getId());
        if (!reviewerIsSender && !reviewerIsReceiver) {
            return ResponseEntity.status(403).body("Only swap participants can review this swap");
        }

        User reviewee =
        userRepository.findById(request.getRevieweeId()).orElse(null);

        if (reviewee == null) {
            return ResponseEntity.badRequest()
                    .body("Reviewee not found");
        }

        User expectedReviewee = reviewerIsSender ? swapRequest.getReceiver() : swapRequest.getSender();
        if (!expectedReviewee.getId().equals(reviewee.getId())) {
            return ResponseEntity.badRequest().body("Reviewee must be the other participant in this swap");
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

