package com.example.demo.order.service;

import com.example.demo.common.dto.PageResponse;
import com.example.demo.order.dto.CancelOrderResponse;
import com.example.demo.order.dto.CheckoutRequest;
import com.example.demo.order.dto.CheckoutResponse;
import com.example.demo.order.dto.OrderDetailResponse;
import com.example.demo.order.dto.OrderPreviewRequest;
import com.example.demo.order.dto.OrderPreviewResponse;
import com.example.demo.order.dto.OrderSummaryResponse;
import com.example.demo.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderPreviewResponse previewOrder(UUID userId, OrderPreviewRequest request);

    CheckoutResponse checkout(UUID userId, CheckoutRequest request);

    PageResponse<OrderSummaryResponse> getOrders(UUID userId, OrderStatus status, Pageable pageable);

    OrderDetailResponse getOrderDetail(UUID userId, UUID orderId, boolean isAdmin);

    CancelOrderResponse cancelOrder(UUID userId, UUID orderId);
}
