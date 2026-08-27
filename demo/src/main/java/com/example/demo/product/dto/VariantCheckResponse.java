package com.example.demo.product.dto;

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
public class VariantCheckResponse {
    private UUID variantId;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer stockQuantity;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private String imageUrl;
}
