package com.example.demo.product.service;

import com.example.demo.common.dto.PageResponse;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.product.dto.ProductDetailResponse;
import com.example.demo.product.dto.ProductSummaryResponse;
import com.example.demo.product.dto.ProductVariantResponse;
import com.example.demo.product.dto.VariantCheckResponse;
import com.example.demo.product.entity.Category;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.entity.Tag;
import com.example.demo.product.repository.CategoryRepository;
import com.example.demo.product.repository.ProductImageRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private Category category;
    private Tag tag;

    @BeforeEach
    void setUp() {
        UUID productId = UUID.randomUUID();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Men")
                .description("Men fashion")
                .build();

        tag = Tag.builder()
                .id(UUID.randomUUID())
                .code("NEW_ARRIVAL")
                .name("New Arrival")
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Polo Shirt Classic")
                .description("High quality cotton")
                .categories(new HashSet<>(Set.of(category)))
                .tags(new HashSet<>(Set.of(tag)))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        variant1 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .sku("POLO-BLK-M")
                .color("Black")
                .size("M")
                .price(BigDecimal.valueOf(250000))
                .quantity(20)
                .build();

        variant2 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .sku("POLO-BLK-L")
                .color("Black")
                .size("L")
                .price(BigDecimal.valueOf(270000))
                .quantity(30)
                .build();

        testProduct.getVariants().add(variant1);
        testProduct.getVariants().add(variant2);

        ProductImage image1 = ProductImage.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .imageUrl("https://example.com/polo-black.jpg")
                .color("Black")
                .isPrimary(true)
                .indexOrder(1)
                .build();

        testProduct.getImages().add(image1);
    }

    @Test
    @DisplayName("Should retrieve filtered products with calculated price range and total stock")
    void testGetProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductSummaryResponse> result = productService.getProducts(
                "Polo", BigDecimal.valueOf(200000), BigDecimal.valueOf(300000), null, null, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        ProductSummaryResponse summary = result.getContent().get(0);
        assertEquals("Polo Shirt Classic", summary.getName());
        assertEquals(BigDecimal.valueOf(250000), summary.getMinPrice());
        assertEquals(BigDecimal.valueOf(270000), summary.getMaxPrice());
        assertEquals(50, summary.getTotalStock());
        assertEquals("https://example.com/polo-black.jpg", summary.getPrimaryImageUrl());
    }

    @Test
    @DisplayName("Should retrieve full product detail with variants, colors, and sizes")
    void testGetProductById() {
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));

        ProductDetailResponse response = productService.getProductById(testProduct.getId());

        assertNotNull(response);
        assertEquals("Polo Shirt Classic", response.getName());
        assertEquals(1, response.getCategories().size());
        assertEquals(1, response.getTags().size());
        assertEquals(1, response.getAvailableColors().size());
        assertEquals("Black", response.getAvailableColors().get(0));
        assertEquals(2, response.getAvailableSizes().size());
        assertEquals(2, response.getVariants().size());
    }

    @Test
    @DisplayName("Should check variant availability by color and size")
    void testCheckVariant_Success() {
        when(productRepository.existsById(testProduct.getId())).thenReturn(true);
        when(productVariantRepository.findByProductIdAndColorIgnoreCaseAndSizeIgnoreCase(testProduct.getId(), "Black", "M"))
                .thenReturn(Optional.of(variant1));

        VariantCheckResponse response = productService.checkVariant(testProduct.getId(), "Black", "M");

        assertNotNull(response);
        assertEquals("POLO-BLK-M", response.getSku());
        assertEquals(BigDecimal.valueOf(250000), response.getPrice());
        assertEquals(20, response.getStockQuantity());
        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when checking non-existent variant")
    void testCheckVariant_NotFound() {
        when(productRepository.existsById(testProduct.getId())).thenReturn(true);
        when(productVariantRepository.findByProductIdAndColorIgnoreCaseAndSizeIgnoreCase(testProduct.getId(), "Red", "XL"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productService.checkVariant(testProduct.getId(), "Red", "XL")
        );
    }
}
