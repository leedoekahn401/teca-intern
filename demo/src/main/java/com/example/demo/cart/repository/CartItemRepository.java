package com.example.demo.cart.repository;

import com.example.demo.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);
    Optional<CartItem> findByIdAndCartUserId(UUID itemId, UUID userId);
    List<CartItem> findByCartUserIdAndIdIn(UUID userId, List<UUID> itemIds);
    void deleteByCartId(UUID cartId);
    void deleteByCartUserIdAndIdIn(UUID userId, List<UUID> itemIds);
}
