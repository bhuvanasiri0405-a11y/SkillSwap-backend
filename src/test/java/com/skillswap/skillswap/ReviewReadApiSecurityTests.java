package com.skillswap.skillswap;

import com.skillswap.skillswap.entity.User;
import com.skillswap.skillswap.repository.ReviewRepository;
import com.skillswap.skillswap.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewReadApiSecurityTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository users;
    @MockitoBean private ReviewRepository reviews;

    @BeforeEach
    void setUp() {
        User profile = new User();
        profile.setId(99L);
        profile.setName("Public Profile");
        profile.setEmail("profile@example.test");
        when(users.findById(99L)).thenReturn(Optional.of(profile));
        when(reviews.findReceivedByRevieweeAndSwapStatus(eq(profile), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    @Test
    void anonymousReaderIsRejected() throws Exception {
        mockMvc.perform(get("/api/reviews/users/99"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403));
    }

    @Test
    void authenticatedUserCanReadPublicReviewsForAnotherUser() throws Exception {
        mockMvc.perform(get("/api/reviews/users/99").with(user("reader").roles("USER")))
                .andExpect(status().isOk());
    }
}
