package com.example.demo.user.dto;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private boolean isEmailVerified;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserProfileResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .isEmailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
