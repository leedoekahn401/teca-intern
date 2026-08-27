package com.example.demo.product.service.impl;

import com.example.demo.common.dto.PageResponse;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.product.dto.CategoryResponse;
import com.example.demo.product.dto.ProductDetailResponse;
import com.example.demo.product.dto.ProductImageResponse;
import com.example.demo.product.dto.ProductSummaryResponse;
import com.example.demo.product.dto.ProductVariantResponse;
import com.example.demo.product.dto.TagResponse;
import com.example.demo.product.dto.VariantCheckResponse;
import com.example.demo.product.entity.Category;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.entity.Tag;
import com.example.demo.product.repository.CategoryRepository;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.repository.ProductSpecification;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public PageResponse<ProductSummaryResponse> getProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            UUID categoryId,
            UUID tagId,
            Pageable pageable
    ) {
        Specification<Product> spec = ProductSpecification.filterProducts(keyword, minPrice, maxPrice, categoryId, tagId);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        Page<ProductSummaryResponse> responsePage = productPage.map(this::mapToSummaryResponse);
        return PageResponse.fromPage(responsePage);
    }

    @Override
    public ProductDetailResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getIndexOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .name(img.getName())
                        .color(img.getColor())
                        .isPrimary(img.isPrimary())
                        .indexOrder(img.getIndexOrder())
                        .build())
                .collect(Collectors.toList());

        List<String> availableColors = product.getVariants().stream()
                .map(ProductVariant::getColor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> availableSizes = product.getVariants().stream()
                .map(ProductVariant::getSize)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .map(variant -> mapToVariantResponse(variant, product.getImages()))
                .collect(Collectors.toList());

        List<CategoryResponse> categoryResponses = product.getCategories().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());

        List<TagResponse> tagResponses = product.getTags().stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categories(categoryResponses)
                .tags(tagResponses)
                .images(imageResponses)
                .availableColors(availableColors)
                .availableSizes(availableSizes)
                .variants(variantResponses)
                .build();
    }

    @Override
    public List<ProductVariantResponse> getProductVariants(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        return product.getVariants().stream()
                .map(variant -> mapToVariantResponse(variant, product.getImages()))
                .collect(Collectors.toList());
    }

    @Override
    public VariantCheckResponse checkVariant(UUID productId, String color, String size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with ID: " + productId);
        }

        ProductVariant variant = productVariantRepository
                .findByProductIdAndColorIgnoreCaseAndSizeIgnoreCase(productId, color.trim(), size.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Variant not found for product %s with color '%s' and size '%s'", productId, color, size)));

        Product product = variant.getProduct();
        String imageUrl = resolveVariantImageUrl(variant, product != null ? product.getImages() : List.of());
        int stock = variant.getQuantity() != null ? variant.getQuantity() : 0;

        return VariantCheckResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(stock)
                .isAvailable(stock > 0)
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    private ProductSummaryResponse mapToSummaryResponse(Product product) {
        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));

        List<BigDecimal> prices = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal minPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        int totalStock = product.getVariants().stream()
                .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                .sum();

        List<CategoryResponse> categoryResponses = product.getCategories().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());

        List<TagResponse> tagResponses = product.getTags().stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .primaryImageUrl(primaryImageUrl)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalStock(totalStock)
                .categories(categoryResponses)
                .tags(tagResponses)
                .build();
    }

    private ProductVariantResponse mapToVariantResponse(ProductVariant variant, List<ProductImage> productImages) {
        String imageUrl = resolveVariantImageUrl(variant, productImages);
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .quantity(variant.getQuantity())
                .imageUrl(imageUrl)
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

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }

    private TagResponse mapToTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .code(tag.getCode())
                .name(tag.getName())
                .build();
    }
}
