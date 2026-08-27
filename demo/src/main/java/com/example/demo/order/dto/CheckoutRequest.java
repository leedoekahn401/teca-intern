package com.example.demo.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class CheckoutRequest {

    @Schema(description = "Full name of the recipient", example = "Nguyễn Văn A", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @Schema(description = "Contact phone number of recipient", example = "0987654321", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Schema(description = "Detailed delivery address", example = "123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Address is required")
    private String address;

    @Schema(description = "Optional order notes/delivery instructions", example = "Giao hàng giờ hành chính, gọi trước khi đến")
    private String description;

    @Schema(description = "Optional promotional voucher code to apply", example = "SUMMER2026")
    private String voucherCode;

    @Schema(description = "List of cart item UUIDs to checkout and convert into order", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Cart item IDs cannot be empty")
    private List<UUID> cartItemIds;
}
