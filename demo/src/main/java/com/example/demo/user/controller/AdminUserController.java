package com.example.demo.user.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.service.UserService;
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
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {

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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable UUID id) {
        UserProfileResponse response = userService.getUserProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * Get user profile by username (Admin only).
     *
     * @param username username to look up
     * @return UserProfileResponse
     */
    @GetMapping("/by-username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(@RequestParam String username) {
        UserProfileResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    /**
     * Get user profile by email (Admin only).
     *
     * @param email email to look up
     * @return UserProfileResponse
     */
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByEmail(@RequestParam String email) {
        UserProfileResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }
}
