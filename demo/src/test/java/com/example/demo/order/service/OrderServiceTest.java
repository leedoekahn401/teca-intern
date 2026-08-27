package com.example.demo.order.service;

import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.discount.entity.DiscountType;
import com.example.demo.discount.entity.UserVoucher;
import com.example.demo.discount.entity.Voucher;
import com.example.demo.discount.repository.UserVoucherRepository;
import com.example.demo.discount.repository.VoucherRepository;
import com.example.demo.discount.service.VoucherService;
import com.example.demo.order.dto.CancelOrderResponse;
import com.example.demo.order.dto.CheckoutRequest;
import com.example.demo.order.dto.CheckoutResponse;
import com.example.demo.order.dto.OrderDetailResponse;
import com.example.demo.order.dto.OrderPreviewRequest;
import com.example.demo.order.dto.OrderPreviewResponse;
import com.example.demo.order.dto.OrderSummaryResponse;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.order.repository.OrderItemRepository;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.order.service.impl.OrderServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private VoucherService voucherService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;
    private Voucher voucher;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        user = User.builder().id(userId).username("testuser").email("user@example.com").build();
        cart = Cart.builder().id(UUID.randomUUID()).user(user).items(new ArrayList<>()).build();

        product = Product.builder().id(UUID.randomUUID()).name("Polo Shirt Classic").build();
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
        cart.getItems().add(cartItem);

        voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("SUMMER2026")
                .discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(15))
                .minimumSpend(BigDecimal.valueOf(300000))
                .build();
    }

    @Test
    @DisplayName("Should preview order with shipping fee and voucher calculation")
    void testPreviewOrder() {
        when(cartItemRepository.findByCartUserIdAndIdIn(user.getId(), List.of(cartItem.getId())))
                .thenReturn(List.of(cartItem));
        when(voucherService.calculateDiscount(user.getId(), "SUMMER2026", BigDecimal.valueOf(500000)))
                .thenReturn(BigDecimal.valueOf(75000));

        OrderPreviewRequest request = OrderPreviewRequest.builder()
                .cartItemIds(List.of(cartItem.getId()))
                .voucherCode("SUMMER2026")
                .build();

        OrderPreviewResponse response = orderService.previewOrder(user.getId(), request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(500000), response.getSubtotalAmount());
        assertEquals(BigDecimal.valueOf(30000), response.getShippingFee());
        assertEquals(BigDecimal.valueOf(75000), response.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(455000), response.getFinalAmount()); // 500k + 30k - 75k = 455k
        assertEquals(1, response.getItems().size());
    }

    @Test
    @DisplayName("Should checkout order: deduct inventory, snapshot item, record voucher, and clear cart item")
    void testCheckout_Success() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cartItemRepository.findByCartUserIdAndIdIn(user.getId(), List.of(cartItem.getId())))
                .thenReturn(List.of(cartItem));
        when(voucherRepository.findByCodeIgnoreCase("SUMMER2026")).thenReturn(Optional.of(voucher));
        when(voucherService.calculateDiscount(user.getId(), "SUMMER2026", BigDecimal.valueOf(500000)))
                .thenReturn(BigDecimal.valueOf(75000));
        when(userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId())).thenReturn(Optional.empty());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CheckoutRequest request = CheckoutRequest.builder()
                .recipientName("Nguyen Van A")
                .phoneNumber("0987654321")
                .address("123 Nguyen Hue, Q1, HCM")
                .voucherCode("SUMMER2026")
                .cartItemIds(List.of(cartItem.getId()))
                .build();

        CheckoutResponse response = orderService.checkout(user.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(455000), response.getFinalAmount());

        // Verify stock deducted from 10 to 8
        assertEquals(8, variant.getQuantity());
        verify(productVariantRepository).save(variant);

        // Verify snapshot item created
        verify(orderItemRepository).save(any(OrderItem.class));

        // Verify voucher recorded
        verify(userVoucherRepository).save(any(UserVoucher.class));

        // Verify cart items deleted
        verify(cartItemRepository).deleteAll(List.of(cartItem));
    }

    @Test
    @DisplayName("Should cancel PENDING order, restock variant quantity and revert voucher usage")
    void testCancelOrder_Success() {
        UUID orderId = UUID.randomUUID();
        OrderItem orderItem = OrderItem.builder()
                .productVariant(variant)
                .quantity(2)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .voucher(voucher)
                .status(OrderStatus.PENDING)
                .items(List.of(orderItem))
                .build();

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .usage(1)
                .usageLimit(1)
                .build();

        when(orderRepository.findByIdAndUserId(orderId, user.getId())).thenReturn(Optional.of(order));
        when(userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId())).thenReturn(Optional.of(userVoucher));

        int initialStock = variant.getQuantity(); // 10
        CancelOrderResponse response = orderService.cancelOrder(user.getId(), orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(initialStock + 2, variant.getQuantity()); // 12
        assertEquals(0, userVoucher.getUsage());
        verify(orderRepository).save(order);
    }
}
