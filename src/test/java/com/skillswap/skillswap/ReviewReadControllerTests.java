package com.skillswap.skillswap;

import com.skillswap.skillswap.controller.ReviewController;
import com.skillswap.skillswap.dto.PublicReviewResponse;
import com.skillswap.skillswap.dto.ReviewPageResponse;
import com.skillswap.skillswap.dto.ReviewSummaryResponse;
import com.skillswap.skillswap.entity.Review;
import com.skillswap.skillswap.entity.SwapRequest;
import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.SwapRequestRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewReadControllerTests {
    private ReviewRepository reviews;
    private UserRepository users;
    private ReviewController controller;
    private User reviewee;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        users = mock(UserRepository.class);
        controller = new ReviewController(reviews, users, mock(SwapRequestRepository.class));
        reviewee = user(7L, "reviewee@example.test", "Reviewee Name");
    }

    @Test
    void returnsPublicReviewFieldsAndRequestedPage() {
        User reviewer = user(8L, "private@example.test", "Reviewer Display");
        Review review = review(reviewer, reviewee, 5, "Excellent swap");
        Pageable requested = PageRequest.of(1, 1);
        when(users.findById(7L)).thenReturn(Optional.of(reviewee));
        when(reviews.findReceivedByRevieweeAndSwapStatus(
                eq(reviewee), eq(SwapRequest.Status.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review), requested, 3));

        var response = controller.getReceivedReviews(7L, 1, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReviewPageResponse body = (ReviewPageResponse) response.getBody();
        assertEquals(1, body.page());
        assertEquals(1, body.size());
        assertEquals(3, body.totalElements());
        assertEquals(3, body.totalPages());
        PublicReviewResponse item = body.content().get(0);
        assertEquals("Reviewer Display", item.reviewerDisplayName());
        assertEquals(5, item.rating());
        assertEquals("Excellent swap", item.comment());
        assertEquals(review.getCreatedAt(), item.reviewDate());
        assertFalse(item.toString().contains("private@example.test"));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviews).findReceivedByRevieweeAndSwapStatus(
                eq(reviewee), eq(SwapRequest.Status.COMPLETED), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(1, pageableCaptor.getValue().getPageSize());
        assertFalse(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void emptyReviewListReturnsEmptyPage() {
        when(users.findById(7L)).thenReturn(Optional.of(reviewee));
        when(reviews.findReceivedByRevieweeAndSwapStatus(
                eq(reviewee), eq(SwapRequest.Status.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        var response = controller.getReceivedReviews(7L, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReviewPageResponse body = (ReviewPageResponse) response.getBody();
        assertEquals(List.of(), body.content());
        assertEquals(0, body.totalElements());
        assertEquals(0, body.totalPages());
    }

    @Test
    void unknownUserReturnsNotFoundForListAndSummary() {
        when(users.findById(404L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.getReceivedReviews(404L, 0, 10).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.getReviewSummary(404L).getStatusCode());
        verify(reviews, never()).findReceivedByRevieweeAndSwapStatus(any(), any(), any());
        verify(reviews, never()).countReceivedByRevieweeAndSwapStatus(any(), any());
    }

    @Test
    void invalidPaginationIsRejected() {
        when(users.findById(7L)).thenReturn(Optional.of(reviewee));

        assertEquals(HttpStatus.BAD_REQUEST, controller.getReceivedReviews(7L, -1, 10).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.getReceivedReviews(7L, 0, 101).getStatusCode());
        verify(reviews, never()).findReceivedByRevieweeAndSwapStatus(any(), any(), any());
    }

    @Test
    void summaryUsesCompletedReviewsAndReturnsZeroWhenEmpty() {
        when(users.findById(7L)).thenReturn(Optional.of(reviewee));
        when(reviews.countReceivedByRevieweeAndSwapStatus(reviewee, SwapRequest.Status.COMPLETED)).thenReturn(0L);
        when(reviews.averageRatingByRevieweeAndSwapStatus(reviewee, SwapRequest.Status.COMPLETED)).thenReturn(null);

        var response = controller.getReviewSummary(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new ReviewSummaryResponse(7L, 0.0, 0L), response.getBody());
    }

    @Test
    void summaryReturnsCompletedReviewAverageAndCount() {
        when(users.findById(7L)).thenReturn(Optional.of(reviewee));
        when(reviews.countReceivedByRevieweeAndSwapStatus(reviewee, SwapRequest.Status.COMPLETED)).thenReturn(2L);
        when(reviews.averageRatingByRevieweeAndSwapStatus(reviewee, SwapRequest.Status.COMPLETED)).thenReturn(4.5);

        var response = controller.getReviewSummary(7L);

        assertEquals(new ReviewSummaryResponse(7L, 4.5, 2L), response.getBody());
    }

    private Review review(User reviewer, User reviewee, int rating, String comment) {
        SwapRequest swap = new SwapRequest();
        swap.setId(12L);
        swap.setSender(reviewer);
        swap.setReceiver(reviewee);
        swap.setStatus(SwapRequest.Status.COMPLETED);

        Review review = new Review();
        review.setId(17L);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setSwapRequest(swap);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.of(2026, 1, 10, 12, 30));
        return review;
    }

    private User user(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        return user;
    }
}
