package com.example.demo.order.service.impl;

import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.cart.repository.CartItemRepository;
import com.example.demo.cart.repository.CartRepository;
import com.example.demo.common.dto.PageResponse;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.discount.entity.UserVoucher;
import com.example.demo.discount.entity.UserVoucherId;
import com.example.demo.discount.entity.Voucher;
import com.example.demo.discount.repository.UserVoucherRepository;
import com.example.demo.discount.repository.VoucherRepository;
import com.example.demo.discount.service.VoucherService;
import com.example.demo.order.dto.CancelOrderResponse;
import com.example.demo.order.dto.CheckoutRequest;
import com.example.demo.order.dto.CheckoutResponse;
import com.example.demo.order.dto.OrderDetailResponse;
import com.example.demo.order.dto.OrderItemResponse;
import com.example.demo.order.dto.OrderPreviewRequest;
import com.example.demo.order.dto.OrderPreviewResponse;
import com.example.demo.order.dto.OrderSummaryResponse;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.order.repository.OrderItemRepository;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.order.service.OrderService;
import com.example.demo.product.entity.Product;
import com.example.demo.product.entity.ProductImage;
import com.example.demo.product.entity.ProductVariant;
import com.example.demo.product.repository.ProductVariantRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal STANDARD_SHIPPING_FEE = BigDecimal.valueOf(30000);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherService voucherService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(UUID userId, OrderPreviewRequest request) {
        List<CartItem> cartItems;
        if (request != null && request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            cartItems = cartItemRepository.findByCartUserIdAndIdIn(userId, request.getCartItemIds());
        } else {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
            cartItems = cart.getItems();
        }

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate order preview because no cart items were found");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderPreviewResponse.OrderItemPreviewResponse> itemPreviews = new ArrayList<>();

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();
            String productName = product != null ? product.getName() : "";
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(totalPrice);

            String thumbnailUrl = resolveVariantImageUrl(variant, product != null ? product.getImages() : List.of());

            itemPreviews.add(OrderPreviewResponse.OrderItemPreviewResponse.builder()
                    .productVariantId(variant.getId())
                    .productName(productName)
                    .sku(variant.getSku())
                    .color(variant.getColor())
                    .size(variant.getSize())
                    .thumbnailUrl(thumbnailUrl)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build());
        }

        BigDecimal shippingFee = STANDARD_SHIPPING_FEE;
        BigDecimal discountAmount = BigDecimal.ZERO;
        OrderPreviewResponse.VoucherSummary voucherSummary = null;

        if (request != null && request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            discountAmount = voucherService.calculateDiscount(userId, request.getVoucherCode().trim(), subtotal);
            voucherSummary = OrderPreviewResponse.VoucherSummary.builder()
                    .code(request.getVoucherCode().trim())
                    .discountAmount(discountAmount)
                    .build();
        }

        BigDecimal finalAmount = subtotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        return OrderPreviewResponse.builder()
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .voucher(voucherSummary)
                .items(itemPreviews)
                .build();
    }

    @Override
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByCartUserIdAndIdIn(userId, request.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("No valid cart items found for checkout");
        }

        // Validate stock
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();
            int stock = variant.getQuantity() != null ? variant.getQuantity() : 0;
            if (stock < item.getQuantity()) {
                throw new IllegalArgumentException(String.format("Insufficient stock for item '%s' (SKU: %s). Available: %d, Requested: %d",
                        variant.getProduct() != null ? variant.getProduct().getName() : "",
                        variant.getSku(), stock, item.getQuantity()));
            }
        }

        // Calculate Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal itemTotal = item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal shippingFee = STANDARD_SHIPPING_FEE;
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String code = request.getVoucherCode().trim();
            voucher = voucherRepository.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + code));

            discountAmount = voucherService.calculateDiscount(userId, code, subtotal);

            // Record voucher usage
            Optional<UserVoucher> uvOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId());
            if (uvOpt.isPresent()) {
                UserVoucher uv = uvOpt.get();
                uv.setUsage(uv.getUsage() + 1);
                userVoucherRepository.save(uv);
            } else {
                UserVoucher uv = UserVoucher.builder()
                        .id(new UserVoucherId(userId, voucher.getId()))
                        .user(user)
                        .voucher(voucher)
                        .usage(1)
                        .usageLimit(1)
                        .build();
                userVoucherRepository.save(uv);
            }
        }

        BigDecimal finalAmount = subtotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        // Create Order
        Order order = Order.builder()
                .user(user)
                .voucher(voucher)
                .recipientName(request.getRecipientName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .address(request.getAddress().trim())
                .description(request.getDescription())
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Deduct inventory & create snapshot OrderItems
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();
            String productName = product != null ? product.getName() : "";
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            String thumbnailUrl = resolveVariantImageUrl(variant, product != null ? product.getImages() : List.of());

            // Deduct stock
            variant.setQuantity(variant.getQuantity() - item.getQuantity());
            productVariantRepository.save(variant);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .productName(productName)
                    .sku(variant.getSku())
                    .color(variant.getColor())
                    .size(variant.getSize())
                    .thumbnailUrl(thumbnailUrl)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();

            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        // Remove cart items
        Cart cart = cartItems.get(0).getCart();
        cart.getItems().removeAll(cartItems);
        cartItemRepository.deleteAll(cartItems);

        return CheckoutResponse.builder()
                .orderId(savedOrder.getId())
                .recipientName(savedOrder.getRecipientName())
                .phoneNumber(savedOrder.getPhoneNumber())
                .address(savedOrder.getAddress())
                .subtotalAmount(savedOrder.getSubtotalAmount())
                .shippingFee(savedOrder.getShippingFee())
                .discountAmount(savedOrder.getDiscountAmount())
                .finalAmount(savedOrder.getFinalAmount())
                .status(savedOrder.getStatus())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> orderPage = (status != null)
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);

        Page<OrderSummaryResponse> responsePage = orderPage.map(order -> OrderSummaryResponse.builder()
                .id(order.getId())
                .recipientName(order.getRecipientName())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .build());

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(UUID userId, UUID orderId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getUser().getId().equals(userId) && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productVariantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                        .productName(item.getProductName())
                        .sku(item.getSku())
                        .color(item.getColor())
                        .size(item.getSize())
                        .thumbnailUrl(item.getThumbnailUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        String voucherCode = order.getVoucher() != null ? order.getVoucher().getCode() : null;

        return OrderDetailResponse.builder()
                .id(order.getId())
                .recipientName(order.getRecipientName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .description(order.getDescription())
                .status(order.getStatus())
                .subtotalAmount(order.getSubtotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .voucherCode(voucherCode)
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    public CancelOrderResponse cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only orders in PENDING status can be cancelled. Current status is: " + order.getStatus());
        }

        // Restock inventory
        for (OrderItem item : order.getItems()) {
            if (item.getProductVariant() != null) {
                ProductVariant variant = item.getProductVariant();
                variant.setQuantity(variant.getQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        // Revert voucher usage
        if (order.getVoucher() != null) {
            userVoucherRepository.findByUserIdAndVoucherId(userId, order.getVoucher().getId())
                    .ifPresent(uv -> {
                        uv.setUsage(Math.max(0, uv.getUsage() - 1));
                        userVoucherRepository.save(uv);
                    });
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return CancelOrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
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
