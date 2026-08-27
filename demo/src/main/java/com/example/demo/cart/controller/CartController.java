package com.example.demo.cart.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.cart.dto.AddToCartRequest;
import com.example.demo.cart.dto.AddToCartResponse;
import com.example.demo.cart.dto.BulkDeleteCartItemsRequest;
import com.example.demo.cart.dto.CartItemResponse;
import com.example.demo.cart.dto.CartResponse;
import com.example.demo.cart.dto.UpdateCartItemQuantityRequest;
import com.example.demo.cart.dto.UpdateCartItemResponse;
import com.example.demo.cart.dto.UpdateCartItemVariantRequest;
import com.example.demo.cart.service.CartService;
import com.example.demo.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Endpoints for managing the user's shopping cart")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get user cart", description = "Retrieves the current shopping cart contents, total items, and subtotal for the authenticated user.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        validateUser(currentUser);
        CartResponse response = cartService.getCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", response));
    }

    @Operation(summary = "Add item to cart", description = "Adds a product variant with specified quantity to the authenticated user's cart.")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<AddToCartResponse>> addToCart(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AddToCartRequest request
    ) {
        validateUser(currentUser);
        AddToCartResponse response = cartService.addToCart(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart successfully", response));
    }

    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a specific item in the user's cart.")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<UpdateCartItemResponse>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "UUID of the cart item") @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        validateUser(currentUser);
        UpdateCartItemResponse response = cartService.updateItemQuantity(currentUser.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", response));
    }

    @Operation(summary = "Update cart item variant", description = "Changes the product variant (e.g. size/color) of an item in the cart.")
    @PutMapping("/items/{itemId}/variant")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItemVariant(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "UUID of the cart item") @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateCartItemVariantRequest request
    ) {
        validateUser(currentUser);
        CartItemResponse response = cartService.updateItemVariant(currentUser.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item variant updated successfully", response));
    }

    @Operation(summary = "Remove single item from cart", description = "Removes a specific cart item from the user's cart.")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "UUID of the cart item") @PathVariable("itemId") UUID itemId
    ) {
        validateUser(currentUser);
        cartService.removeItem(currentUser.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item(s) removed from cart successfully", null));
    }

    @Operation(summary = "Remove multiple items from cart", description = "Removes a list of selected cart items from the user's cart.")
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> removeItems(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BulkDeleteCartItemsRequest request
    ) {
        validateUser(currentUser);
        cartService.removeItems(currentUser.getId(), request.getCartItemIds());
        return ResponseEntity.ok(ApiResponse.success("Item(s) removed from cart successfully", null));
    }

    @Operation(summary = "Clear all items from cart", description = "Deletes all items in the authenticated user's cart.")
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        validateUser(currentUser);
        cartService.clearCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Item(s) removed from cart successfully", null));
    }

    private void validateUser(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
    }
}
