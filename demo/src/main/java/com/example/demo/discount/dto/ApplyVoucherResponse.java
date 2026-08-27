package com.example.demo.discount.dto;

import com.example.demo.discount.entity.DiscountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyVoucherResponse {
    private UUID voucherId;
    private String voucherCode;
    private DiscountType discountType;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    @JsonProperty("isValid")
    private boolean isValid;
}
