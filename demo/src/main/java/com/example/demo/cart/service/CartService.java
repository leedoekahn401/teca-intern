package com.example.demo.cart.service;

import com.example.demo.cart.dto.AddToCartRequest;
import com.example.demo.cart.dto.AddToCartResponse;
import com.example.demo.cart.dto.CartItemResponse;
import com.example.demo.cart.dto.CartResponse;
import com.example.demo.cart.dto.UpdateCartItemQuantityRequest;
import com.example.demo.cart.dto.UpdateCartItemResponse;
import com.example.demo.cart.dto.UpdateCartItemVariantRequest;

import java.util.List;
import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);

    AddToCartResponse addToCart(UUID userId, AddToCartRequest request);

    UpdateCartItemResponse updateItemQuantity(UUID userId, UUID cartItemId, UpdateCartItemQuantityRequest request);

    CartItemResponse updateItemVariant(UUID userId, UUID cartItemId, UpdateCartItemVariantRequest request);

    void removeItem(UUID userId, UUID cartItemId);

    void removeItems(UUID userId, List<UUID> cartItemIds);

    void clearCart(UUID userId);
}
