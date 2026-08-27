package com.example.demo.discount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyVoucherRequest {

    @Schema(description = "Voucher promotional code to apply", example = "SUMMER2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    @Schema(description = "Order subtotal amount before discount", example = "500000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Subtotal amount is required")
    @DecimalMin(value = "0.00", message = "Subtotal amount must be greater than or equal to 0")
    private BigDecimal subtotalAmount;
}
