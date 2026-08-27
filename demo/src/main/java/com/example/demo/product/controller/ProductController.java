package com.example.demo.product.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.product.dto.ProductDetailResponse;
import com.example.demo.product.dto.ProductSummaryResponse;
import com.example.demo.product.dto.ProductVariantResponse;
import com.example.demo.product.dto.VariantCheckResponse;
import com.example.demo.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for browsing products, viewing details, and checking variant stock & pricing")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Browse products", description = "Retrieves a paginated list of products with optional filtering by keyword, price range, category, and tag.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property (e.g. createdAt, name)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Keyword to search in name or description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Minimum variant price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum variant price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by Category UUID") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter by Tag UUID") @RequestParam(required = false) UUID tagId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ProductSummaryResponse> response = productService.getProducts(keyword, minPrice, maxPrice, categoryId, tagId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", response));
    }

    @Operation(summary = "Get product detail", description = "Retrieves comprehensive details of a product including categories, tags, images, available colors/sizes, and all variants.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(
            @Parameter(description = "UUID of the product") @PathVariable UUID id
    ) {
        ProductDetailResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product detail retrieved successfully", response));
    }

    @Operation(summary = "Get product variants", description = "Retrieves all variants belonging to a specific product.")
    @GetMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getProductVariants(
            @Parameter(description = "UUID of the product") @PathVariable UUID id
    ) {
        List<ProductVariantResponse> response = productService.getProductVariants(id);
        return ResponseEntity.ok(ApiResponse.success("Product variants retrieved successfully", response));
    }

    @Operation(summary = "Check variant by color & size", description = "Retrieves variant stock availability, SKU, pricing, and matching image for selected color and size.")
    @GetMapping("/{id}/variants/check")
    public ResponseEntity<ApiResponse<VariantCheckResponse>> checkVariant(
            @Parameter(description = "UUID of the product") @PathVariable UUID id,
            @Parameter(description = "Color of the variant (e.g. Black)") @RequestParam String color,
            @Parameter(description = "Size of the variant (e.g. L)") @RequestParam String size
    ) {
        VariantCheckResponse response = productService.checkVariant(id, color, size);
        return ResponseEntity.ok(ApiResponse.success("Variant fetched successfully", response));
    }
}
