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
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoucherResponse {
    private UUID id;
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumSpend;
    private Instant validFrom;
    private Instant validUntil;

    @JsonProperty("isClaimed")
    private Boolean isClaimed;

    private Integer usageLeft;
    private String description;
}
