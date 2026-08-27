package com.example.demo.order.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.order.dto.CancelOrderResponse;
import com.example.demo.order.dto.CheckoutRequest;
import com.example.demo.order.dto.CheckoutResponse;
import com.example.demo.order.dto.OrderDetailResponse;
import com.example.demo.order.dto.OrderPreviewRequest;
import com.example.demo.order.dto.OrderPreviewResponse;
import com.example.demo.order.dto.OrderSummaryResponse;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.order.service.OrderService;
import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.user.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for order preview, placement, history, tracking, and cancellation")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Preview order calculations", description = "Calculates shipping fee, voucher discount, and total final price for items before final checkout.")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody(required = false) OrderPreviewRequest request
    ) {
        validateUser(currentUser);
        OrderPreviewResponse response = orderService.previewOrder(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order preview calculated successfully", response));
    }

    @Operation(summary = "Confirm and place order (Checkout)", description = "Creates a new order, snapshots purchased products, deducts inventory, decrements voucher usage, and empties checked-out cart items.")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CheckoutRequest request
    ) {
        validateUser(currentUser);
        CheckoutResponse response = orderService.checkout(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @Operation(summary = "Get user order history", description = "Retrieves a paginated list of past orders for the authenticated user with optional status filter.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Optional filter by order status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        validateUser(currentUser);
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<OrderSummaryResponse> response = orderService.getOrders(currentUser.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", response));
    }

    @Operation(summary = "Get order details", description = "Retrieves full details and snapshot items for a specific order.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "UUID of the order") @PathVariable UUID id
    ) {
        validateUser(currentUser);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        OrderDetailResponse response = orderService.getOrderDetail(currentUser.getId(), id, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Order detail retrieved successfully", response));
    }

    @Operation(summary = "Cancel order", description = "Cancels a PENDING order, restocks inventory, and refunds any applied voucher usage.")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CancelOrderResponse>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "UUID of the order") @PathVariable UUID id
    ) {
        validateUser(currentUser);
        CancelOrderResponse response = orderService.cancelOrder(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    private void validateUser(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
    }
}
