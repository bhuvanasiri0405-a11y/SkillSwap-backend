package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.ReviewController;
import com.skillswap.skillswap.dto.ReviewRequest;
import com.skillswap.skillswap.dto.ReviewStatusResponse;
import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewStatusControllerTests {
    private ReviewRepository reviews;
    private SwapRequestRepository swaps;
    private UserRepository users;
    private ReviewController controller;
    private User sender;
    private User receiver;
    private User outsider;
    private SwapRequest swap;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        swaps = mock(SwapRequestRepository.class);
        users = mock(UserRepository.class);
        controller = new ReviewController(reviews, users, swaps);

        sender = user(1L, "sender@example.test");
        receiver = user(2L, "receiver@example.test");
        outsider = user(3L, "outsider@example.test");
        swap = new SwapRequest();
        swap.setId(12L);
        swap.setSender(sender);
        swap.setReceiver(receiver);
        swap.setStatus(SwapRequest.Status.COMPLETED);

        authentication = mock(Authentication.class);
        when(swaps.findById(12L)).thenReturn(Optional.of(swap));
    }

    @Test
    void bothParticipantsCanReadTheirOwnUnreviewedStatus() {
        when(authentication.getName()).thenReturn(sender.getEmail(), receiver.getEmail());
        when(users.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(users.findByEmailIgnoreCase(receiver.getEmail())).thenReturn(Optional.of(receiver));
        when(reviews.existsByReviewerAndSwapRequest(sender, swap)).thenReturn(false);
        when(reviews.existsByReviewerAndSwapRequest(receiver, swap)).thenReturn(false);

        var senderResponse = controller.getReviewStatus(12L, authentication);
        var receiverResponse = controller.getReviewStatus(12L, authentication);

        assertEquals(HttpStatus.OK, senderResponse.getStatusCode());
        assertEquals(HttpStatus.OK, receiverResponse.getStatusCode());
        assertEquals(new ReviewStatusResponse(12L, SwapRequest.Status.COMPLETED, false, true), senderResponse.getBody());
        assertEquals(new ReviewStatusResponse(12L, SwapRequest.Status.COMPLETED, false, true), receiverResponse.getBody());
    }

    @Test
    void alreadyReviewedParticipantGetsReviewedStatus() {
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(reviews.existsByReviewerAndSwapRequest(sender, swap)).thenReturn(true);

        var response = controller.getReviewStatus(12L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new ReviewStatusResponse(12L, SwapRequest.Status.COMPLETED, true, false), response.getBody());
    }

    @Test
    void nonparticipantCannotReadReviewStatus() {
        when(authentication.getName()).thenReturn(outsider.getEmail());
        when(users.findByEmailIgnoreCase(outsider.getEmail())).thenReturn(Optional.of(outsider));

        var response = controller.getReviewStatus(12L, authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(reviews, never()).existsByReviewerAndSwapRequest(any(), any());
    }

    @Test
    void missingSwapReturnsNotFound() {
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(swaps.findById(404L)).thenReturn(Optional.empty());

        var response = controller.getReviewStatus(404L, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void duplicateReviewSubmissionRemainsRejected() {
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(users.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(swaps.findById(12L)).thenReturn(Optional.of(swap));
        when(reviews.existsByReviewerAndSwapRequest(sender, swap)).thenReturn(true);

        ReviewRequest request = new ReviewRequest();
        request.setSwapRequestId(12L);
        request.setRevieweeId(receiver.getId());
        request.setRating(5);

        var response = controller.createReview(request, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("already reviewed"));
        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    void unreviewedSwapThatIsNotCompletedCannotBeReviewedYet() {
        swap.setStatus(SwapRequest.Status.ACCEPTED);
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
        when(reviews.existsByReviewerAndSwapRequest(sender, swap)).thenReturn(false);

        var response = controller.getReviewStatus(12L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().canReview());
        assertFalse(response.getBody().reviewed());
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName(email);
        user.setEmail(email);
        return user;
    }
}
