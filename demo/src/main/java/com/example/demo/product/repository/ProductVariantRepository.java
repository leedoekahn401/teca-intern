package com.example.demo.product.repository;

import com.example.demo.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);
    Optional<ProductVariant> findByProductIdAndColorIgnoreCaseAndSizeIgnoreCase(UUID productId, String color, String size);
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
}
