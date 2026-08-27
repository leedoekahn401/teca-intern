package com.example.demo.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @Schema(description = "UUID of the product variant to add", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;

    @Schema(description = "Quantity to add (minimum 1)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
