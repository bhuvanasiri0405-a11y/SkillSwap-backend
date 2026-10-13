package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.ReviewController;
import com.skillswap.skillswap.controller.SwapRequestController;
import com.skillswap.skillswap.dto.ReviewRequest;
import com.skillswap.skillswap.dto.SwapResponse;
import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.Skill;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.SkillRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SwapWorkflowTests {
    private SwapRequestRepository swaps;
    private UserRepository users;
    private SkillRepository skills;
    private ReviewRepository reviews;
    private SwapRequestController swapController;
    private ReviewController reviewController;
    private User sender, receiver, outsider;
    private SwapRequest swap;
    private Authentication authentication;

    @BeforeEach void setUp() {
        swaps = mock(SwapRequestRepository.class);
        users = mock(UserRepository.class);
        skills = mock(SkillRepository.class);
        reviews = mock(ReviewRepository.class);
        swapController = new SwapRequestController(swaps, users, skills);
        reviewController = new ReviewController(reviews, users, swaps);
        sender = user(1L, "sender@example.test");
        receiver = user(2L, "receiver@example.test");
        outsider = user(3L, "outsider@example.test");
        Skill offered = new Skill(); offered.setId(4L); offered.setName("Java");
        Skill requested = new Skill(); requested.setId(5L); requested.setName("Design");
        swap = new SwapRequest(); swap.setId(7L); swap.setSender(sender); swap.setReceiver(receiver);
        swap.setOfferedSkill(offered); swap.setRequestedSkill(requested); swap.setCreatedAt(LocalDateTime.now());
        swap.setStatus(SwapRequest.Status.ACCEPTED);
        authentication = mock(Authentication.class);
    }

    private User user(Long id, String email) {
        User user = new User(); user.setId(id); user.setName(email); user.setEmail(email); return user;
    }

    @Test void completingSwapPersistsAndReturnsCompletedState() {
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(swaps.findById(7L)).thenReturn(Optional.of(swap));
        when(swaps.save(any(SwapRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = swapController.completeSwapRequest(7L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SwapRequest.Status.COMPLETED, ((SwapResponse) response.getBody()).getStatus());
        verify(swaps).save(argThat(saved -> saved.getStatus() == SwapRequest.Status.COMPLETED));
    }

    @Test void unrelatedUserCannotCompleteSwap() {
        when(authentication.getName()).thenReturn(outsider.getEmail());
        when(users.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
        when(swaps.findById(7L)).thenReturn(Optional.of(swap));

        var response = swapController.completeSwapRequest(7L, authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(swaps, never()).save(any());
    }

    @Test void onlySwapParticipantsCanReviewCompletedSwap() {
        swap.setStatus(SwapRequest.Status.COMPLETED);
        when(authentication.getName()).thenReturn(outsider.getEmail());
        when(users.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
        when(swaps.findById(7L)).thenReturn(Optional.of(swap));
        ReviewRequest request = reviewRequest(2L);

        var response = reviewController.createReview(request, authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(reviews, never()).save(any());
    }

    @Test void reviewMustBeForOtherSwapParticipant() {
        swap.setStatus(SwapRequest.Status.COMPLETED);
        when(authentication.getName()).thenReturn(sender.getEmail());
        when(users.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(swaps.findById(7L)).thenReturn(Optional.of(swap));
        when(users.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        var response = reviewController.createReview(reviewRequest(outsider.getId()), authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(reviews, never()).save(any());
    }

    private ReviewRequest reviewRequest(Long revieweeId) {
        ReviewRequest request = new ReviewRequest(); request.setSwapRequestId(7L);
        request.setRevieweeId(revieweeId); request.setRating(5); return request;
    }
}
