package com.example.demo.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteCartItemsRequest {

    @Schema(description = "List of Cart Item UUIDs to remove from cart", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Cart item IDs list cannot be empty")
    private List<UUID> cartItemIds;
}
