package com.example.demo.user.service;

import com.example.demo.common.dto.PageResponse;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves user profile details by user ID.
     *
     * @param userId UUID of the user
     * @return UserProfileResponse
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * Retrieves user profile details by username.
     *
     * @param username username of the user
     * @return UserProfileResponse
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * Retrieves user profile details by email.
     *
     * @param email email of the user
     * @return UserProfileResponse
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * Retrieves a paginated list of users with optional filtering (keyword search, role, status).
     *
     * @param keyword  optional search term matching username or email
     * @param role     optional filter by UserRole (ADMIN, CUSTOMER, STAFF)
     * @param status   optional filter by UserStatus (ACTIVE, INACTIVE, BANNED)
     * @param pageable pagination and sorting parameters
     * @return PageResponse containing UserProfileResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> getAllUsers(String keyword,
                                                         UserRole role,
                                                         UserStatus status,
                                                         Pageable pageable) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate usernamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchPattern);
                Predicate emailPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern);
                predicates.add(criteriaBuilder.or(usernamePredicate, emailPredicate));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> usersPage = userRepository.findAll(spec, pageable);
        Page<UserProfileResponse> dtoPage = usersPage.map(UserProfileResponse::fromEntity);

        return PageResponse.fromPage(dtoPage);
    }
}
