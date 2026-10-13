package com.skillswap.skillswap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.skillswap.skillswap.service.AdminService;
import java.util.List;
import com.skillswap.skillswap.dto.AdminAuditLogResponse;
import com.skillswap.skillswap.dto.AdminPageResponse;
import static org.mockito.Mockito.when;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminService adminService;

    @Test void unauthenticatedRequestCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?page=-1"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403));
    }

    @Test
    void ordinaryUserIsDeniedAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?page=-1").with(user("ordinary").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassesRoleGate() throws Exception {
        when(adminService.auditLogs(-1, 20, null, null, null))
                .thenReturn(new AdminPageResponse<AdminAuditLogResponse>(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/admin/audit-logs?page=-1").with(user("administrator").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
