package com.example.demo.user.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Endpoints for administrator user management (Admin role required)")
public class AdminUserController {

    private final UserService userService;

    /**
     * Get paginated and filtered list of users (Admin only).
     *
     * @param page    Page index (zero-based, default: 0)
     * @param size    Page size (default: 10)
     * @param sortBy  Field to sort by (default: createdAt)
     * @param sortDir Direction to sort by (asc / desc, default: desc)
     * @param keyword Optional search keyword (matches username or email)
     * @param role    Optional role filter (ADMIN, CUSTOMER, STAFF)
     * @param status  Optional status filter (ACTIVE, INACTIVE, BANNED)
     * @return Paginated user response
     */
    @Operation(summary = "Get paginated list of users", description = "Retrieves paginated and filtered users (search by keyword, role, status, sorting).")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getAllUsers(
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Keyword to filter by username or email") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by user role") @RequestParam(required = false) UserRole role,
            @Parameter(description = "Filter by user status") @RequestParam(required = false) UserStatus status) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<UserProfileResponse> response = userService.getAllUsers(keyword, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    /**
     * Get specific user profile by user UUID (Admin only).
     *
     * @param id UUID of the user
     * @return UserProfileResponse
     */
    @Operation(summary = "Get user profile by UUID (Admin)", description = "Retrieves a user's full profile using their UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @Parameter(description = "UUID of the user") @PathVariable UUID id) {
        UserProfileResponse response = userService.getUserProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * Get user profile by username (Admin only).
     *
     * @param username username to look up
     * @return UserProfileResponse
     */
    @Operation(summary = "Get user profile by username (Admin)", description = "Retrieves a user's profile using their username.")
    @GetMapping("/by-username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(
            @Parameter(description = "Username to search for") @RequestParam String username) {
        UserProfileResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * Get user profile by email (Admin only).
     *
     * @param email email to look up
     * @return UserProfileResponse
     */
    @Operation(summary = "Get user profile by email (Admin)", description = "Retrieves a user's profile using their email address.")
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByEmail(
            @Parameter(description = "Email address to search for") @RequestParam String email) {
        UserProfileResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }
}
