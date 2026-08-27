package com.example.demo.cart.service;

import com.example.demo.cart.dto.AddToCartRequest;
import com.example.demo.cart.dto.AddToCartResponse;
import com.example.demo.cart.dto.CartResponse;
import com.example.demo.cart.dto.UpdateCartItemQuantityRequest;
import com.example.demo.cart.dto.UpdateCartItemResponse;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.cart.service.impl.CartServiceImpl;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        user = User.builder().id(userId).username("testuser").email("user@example.com").build();
        cart = Cart.builder().id(UUID.randomUUID()).user(user).items(new ArrayList<>()).build();

        product = Product.builder().id(UUID.randomUUID()).name("Polo Shirt").build();
        variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .sku("POLO-BLK-L")
                .color("Black")
                .size("L")
                .price(BigDecimal.valueOf(250000))
                .quantity(10)
                .build();

        cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .productVariant(variant)
                .quantity(2)
                .build();
    }

    @Test
    @DisplayName("Should add new item to cart when not already present")
    void testAddToCart_NewItem() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        AddToCartRequest request = AddToCartRequest.builder()
                .productVariantId(variant.getId())
                .quantity(3)
                .build();

        AddToCartResponse response = cartService.addToCart(user.getId(), request);

        assertNotNull(response);
        assertEquals(variant.getId(), response.getProductVariantId());
        assertEquals(3, response.getQuantity());
    }

    @Test
    @DisplayName("Should reject adding quantity exceeding available stock")
    void testAddToCart_ExceedStock() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));

        AddToCartRequest request = AddToCartRequest.builder()
                .productVariantId(variant.getId())
                .quantity(15) // stock is 10
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(user.getId(), request)
        );
    }

    @Test
    @DisplayName("Should update cart item quantity")
    void testUpdateCartItemQuantity() {
        when(cartItemRepository.findByIdAndCartUserId(cartItem.getId(), user.getId())).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        UpdateCartItemQuantityRequest request = UpdateCartItemQuantityRequest.builder().quantity(4).build();
        UpdateCartItemResponse response = cartService.updateItemQuantity(user.getId(), cartItem.getId(), request);

        assertNotNull(response);
        assertEquals(4, response.getQuantity());
        assertEquals(BigDecimal.valueOf(250000), response.getUnitPrice());
        assertEquals(BigDecimal.valueOf(1000000), response.getTotalPrice());
    }

    @Test
    @DisplayName("Should calculate total cart items, quantity, and subtotal correctly")
    void testGetCart() {
        cart.getItems().add(cartItem);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user.getId());

        assertNotNull(response);
        assertEquals(1, response.getTotalItems());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(BigDecimal.valueOf(500000), response.getSubtotalAmount());
    }
}
