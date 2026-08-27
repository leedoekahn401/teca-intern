package com.example.demo.cart.service.impl;

import com.example.demo.cart.dto.AddToCartRequest;
import com.example.demo.cart.dto.AddToCartResponse;
import com.example.demo.cart.dto.CartItemResponse;
import com.example.demo.cart.dto.CartResponse;
import com.example.demo.cart.dto.UpdateCartItemQuantityRequest;
import com.example.demo.cart.dto.UpdateCartItemResponse;
import com.example.demo.cart.dto.UpdateCartItemVariantRequest;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.cart.service.CartService;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    public AddToCartResponse addToCart(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with ID: " + request.getProductVariantId()));

        int availableStock = variant.getQuantity() != null ? variant.getQuantity() : 0;
        if (availableStock < request.getQuantity()) {
            throw new IllegalArgumentException(String.format("Requested quantity (%d) exceeds available stock (%d) for variant %s",
                    request.getQuantity(), availableStock, variant.getSku()));
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId());

        CartItem savedItem;
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (newQuantity > availableStock) {
                throw new IllegalArgumentException(String.format("Cannot add %d items. Total quantity in cart (%d) exceeds available stock (%d)",
                        request.getQuantity(), newQuantity, availableStock));
            }
            existingItem.setQuantity(newQuantity);
            savedItem = cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();
            savedItem = cartItemRepository.save(newItem);
            cart.getItems().add(savedItem);
        }

        int totalCartItems = cart.getItems().size();

        return AddToCartResponse.builder()
                .cartItemId(savedItem.getId())
                .productVariantId(variant.getId())
                .quantity(savedItem.getQuantity())
                .totalCartItems(totalCartItems)
                .build();
    }

    @Override
    public UpdateCartItemResponse updateItemQuantity(UUID userId, UUID cartItemId, UpdateCartItemQuantityRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        ProductVariant variant = item.getProductVariant();
        int availableStock = variant.getQuantity() != null ? variant.getQuantity() : 0;
        if (request.getQuantity() > availableStock) {
            throw new IllegalArgumentException(String.format("Requested quantity (%d) exceeds available stock (%d) for variant %s",
                    request.getQuantity(), availableStock, variant.getSku()));
        }

        item.setQuantity(request.getQuantity());
        CartItem savedItem = cartItemRepository.save(item);

        BigDecimal unitPrice = variant.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(savedItem.getQuantity()));

        return UpdateCartItemResponse.builder()
                .cartItemId(savedItem.getId())
                .quantity(savedItem.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
    }

    @Override
    public CartItemResponse updateItemVariant(UUID userId, UUID cartItemId, UpdateCartItemVariantRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        ProductVariant newVariant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with ID: " + request.getProductVariantId()));

        int targetQuantity = request.getQuantity() != null ? request.getQuantity() : item.getQuantity();
        int availableStock = newVariant.getQuantity() != null ? newVariant.getQuantity() : 0;
        if (targetQuantity > availableStock) {
            throw new IllegalArgumentException(String.format("Requested quantity (%d) exceeds available stock (%d) for variant %s",
                    targetQuantity, availableStock, newVariant.getSku()));
        }

        Cart cart = item.getCart();
        Optional<CartItem> duplicateOpt = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), newVariant.getId());

        if (duplicateOpt.isPresent() && !duplicateOpt.get().getId().equals(item.getId())) {
            CartItem duplicate = duplicateOpt.get();
            int mergedQuantity = duplicate.getQuantity() + targetQuantity;
            if (mergedQuantity > availableStock) {
                throw new IllegalArgumentException(String.format("Merged quantity (%d) exceeds available stock (%d)",
                        mergedQuantity, availableStock));
            }
            duplicate.setQuantity(mergedQuantity);
            cartItemRepository.delete(item);
            cart.getItems().remove(item);
            CartItem saved = cartItemRepository.save(duplicate);
            return mapToCartItemResponse(saved);
        } else {
            item.setProductVariant(newVariant);
            item.setQuantity(targetQuantity);
            CartItem saved = cartItemRepository.save(item);
            return mapToCartItemResponse(saved);
        }
    }

    @Override
    public void removeItem(UUID userId, UUID cartItemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
    }

    @Override
    public void removeItems(UUID userId, List<UUID> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }
        List<CartItem> items = cartItemRepository.findByCartUserIdAndIdIn(userId, cartItemIds);
        if (!items.isEmpty()) {
            Cart cart = items.get(0).getCart();
            cart.getItems().removeAll(items);
            cartItemRepository.deleteAll(items);
        }
    }

    @Override
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        int totalItems = itemResponses.size();
        int totalQuantity = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .subtotalAmount(subtotal)
                .items(itemResponses)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant.getProduct();

        String productName = product != null ? product.getName() : "";
        UUID productId = product != null ? product.getId() : null;
        BigDecimal unitPrice = variant.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        String thumbnailUrl = resolveVariantImageUrl(variant, product != null ? product.getImages() : List.of());
        int stockAvailable = variant.getQuantity() != null ? variant.getQuantity() : 0;

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productVariantId(variant.getId())
                .productId(productId)
                .productName(productName)
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .thumbnailUrl(thumbnailUrl)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .totalPrice(totalPrice)
                .stockAvailable(stockAvailable)
                .build();
    }

    private String resolveVariantImageUrl(ProductVariant variant, List<ProductImage> productImages) {
        if (variant.getVariantImages() != null && !variant.getVariantImages().isEmpty()) {
            return variant.getVariantImages().get(0).getImageUrl();
        }
        if (productImages != null && variant.getColor() != null) {
            for (ProductImage img : productImages) {
                if (variant.getColor().equalsIgnoreCase(img.getColor())) {
                    return img.getImageUrl();
                }
            }
            return productImages.stream()
                    .filter(ProductImage::isPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElseGet(() -> productImages.isEmpty() ? null : productImages.get(0).getImageUrl());
        }
        return null;
    }
}
