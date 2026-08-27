package com.example.demo.product.service;

import com.example.demo.common.dto.PageResponse;
import com.example.demo.product.dto.CategoryResponse;
import com.example.demo.product.dto.ProductDetailResponse;
import com.example.demo.product.dto.ProductSummaryResponse;
import com.example.demo.product.dto.ProductVariantResponse;
import com.example.demo.product.dto.VariantCheckResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    PageResponse<ProductSummaryResponse> getProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            UUID categoryId,
            UUID tagId,
            Pageable pageable
    );

    ProductDetailResponse getProductById(UUID id);

    List<ProductVariantResponse> getProductVariants(UUID productId);

    VariantCheckResponse checkVariant(UUID productId, String color, String size);

    List<CategoryResponse> getAllCategories();
}
