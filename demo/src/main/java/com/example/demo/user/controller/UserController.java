package com.example.demo.user.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user profile retrieval and management")
public class UserController {

    private final UserService userService;

    /**
     * Get profile information of the currently authenticated user.
     * Accessible by any authenticated user (CUSTOMER, STAFF, ADMIN).
     *
     * @param currentUser Details of the authenticated user
     * @return UserProfileResponse
     */
    @Operation(summary = "Get current user profile", description = "Retrieves the profile information of the currently logged-in user.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        UserProfileResponse response = userService.getUserProfileById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    /**
     * Get user information by user ID.
     * Accessible if the caller is requesting their own ID or has ADMIN role.
     *
     * @param id          UUID of the user to fetch
     * @param currentUser Details of the authenticated user
     * @return UserProfileResponse
     */
    @Operation(summary = "Get user profile by ID", description = "Retrieves profile information by user UUID (self or ADMIN).")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @Parameter(description = "UUID of the user") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        boolean isSelf = currentUser.getId().equals(id);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this user's profile");
        }

        UserProfileResponse response = userService.getUserProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }
}
