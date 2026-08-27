package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryResponse {
    private UUID id;
    private String recipientName;
    private BigDecimal finalAmount;
    private OrderStatus status;
    private Integer itemCount;
    private Instant createdAt;
}
