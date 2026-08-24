package com.example.demo.user.controller;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.service.AuthService;
import com.example.demo.user.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/users/me without token returns 401 Unauthorized")
    void testGetMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me with valid user token returns user profile")
    void testGetMeWithValidToken() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("Password123!")
                .role(UserRole.CUSTOMER)
                .build();

        AuthResponse authResponse = authService.register(registerRequest, null);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john_doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} by self succeeds, by another customer returns 403 Forbidden")
    void testGetUserByIdAccessControl() throws Exception {
        // Create user 1
        RegisterRequest user1Req = RegisterRequest.builder()
                .username("user1_test")
                .email("user1@example.com")
                .password("Password123!")
                .role(UserRole.CUSTOMER)
                .build();
        AuthResponse user1Auth = authService.register(user1Req, null);
        UUID user1Id = user1Auth.getUser().getId();

        // Create user 2
        RegisterRequest user2Req = RegisterRequest.builder()
                .username("user2_test")
                .email("user2@example.com")
                .password("Password123!")
                .role(UserRole.CUSTOMER)
                .build();
        AuthResponse user2Auth = authService.register(user2Req, null);

        // User 1 accesses their own profile -> 200 OK
        mockMvc.perform(get("/api/v1/users/" + user1Id)
                        .header("Authorization", "Bearer " + user1Auth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user1_test"));

        // User 2 accesses user 1 profile -> 403 Forbidden
        mockMvc.perform(get("/api/v1/users/" + user1Id)
                        .header("Authorization", "Bearer " + user2Auth.getAccessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users with customer token returns 403 Forbidden")
    void testAdminEndpointsForbiddenForCustomer() throws Exception {
        RegisterRequest customerReq = RegisterRequest.builder()
                .username("regular_user")
                .email("regular@example.com")
                .password("Password123!")
                .role(UserRole.CUSTOMER)
                .build();
        AuthResponse customerAuth = authService.register(customerReq, null);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + customerAuth.getAccessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user can list users, filter, and fetch by ID / username / email")
    void testAdminUserOperations() throws Exception {
        // Create admin
        RegisterRequest adminReq = RegisterRequest.builder()
                .username("admin_boss")
                .email("admin_boss@example.com")
                .password("Password123!")
                .role(UserRole.ADMIN)
                .build();
        AuthResponse adminAuth = authService.register(adminReq, null);
        UUID adminId = adminAuth.getUser().getId();

        // 1. Admin lists all users
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());

        // 2. Admin fetches by ID
        mockMvc.perform(get("/api/v1/admin/users/" + adminId)
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin_boss"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // 3. Admin fetches by username
        mockMvc.perform(get("/api/v1/admin/users/by-username")
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .param("username", "admin_boss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin_boss@example.com"));

        // 4. Admin fetches by email
        mockMvc.perform(get("/api/v1/admin/users/by-email")
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .param("email", "admin_boss@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin_boss"));
    }
}
