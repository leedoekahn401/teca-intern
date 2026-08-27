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
public class UpdateCartItemVariantRequest {

    @Schema(description = "UUID of the new product variant", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;

    @Schema(description = "Optional updated quantity (minimum 1)", example = "2")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
