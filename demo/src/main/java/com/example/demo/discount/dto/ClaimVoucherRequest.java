package com.example.demo.discount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimVoucherRequest {

    @Schema(description = "Voucher promotional code to claim", example = "SUMMER2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Voucher code is required")
    private String voucherCode;
}
