package com.example.demo.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class OrderPreviewRequest {

    @Schema(description = "List of cart item UUIDs to include in order preview (if null/empty, all cart items used)")
    private List<UUID> cartItemIds;

    @Schema(description = "Optional voucher code to test discount", example = "SUMMER2026")
    private String voucherCode;

    @Schema(description = "Optional shipping address", example = "123 Đường Nguyễn Huệ, Quận 1, TP. HCM")
    private String shippingAddress;
}
