package com.example.demo.discount.controller;

import com.example.demo.auth.dto.ApiResponse;
import com.example.demo.discount.dto.ApplyVoucherRequest;
import com.example.demo.discount.dto.ApplyVoucherResponse;
import com.example.demo.discount.dto.ClaimVoucherRequest;
import com.example.demo.discount.dto.ClaimVoucherResponse;
import com.example.demo.discount.dto.VoucherResponse;
import com.example.demo.discount.service.VoucherService;
import com.example.demo.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers", description = "Endpoints for viewing, claiming, and applying vouchers & discounts")
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Get user vouchers", description = "Retrieves all available and claimed vouchers for the authenticated user.")
    @GetMapping(value = {"", "/my-vouchers"})
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyVouchers(@AuthenticationPrincipal CustomUserDetails currentUser) {
        validateUser(currentUser);
        List<VoucherResponse> response = voucherService.getMyVouchers(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vouchers retrieved successfully", response));
    }

    @Operation(summary = "Get voucher by code", description = "Retrieves voucher details and user claim status by promotional code.")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherByCode(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Voucher code (e.g. SUMMER2026)") @PathVariable String code
    ) {
        validateUser(currentUser);
        VoucherResponse response = voucherService.getVoucherByCode(currentUser.getId(), code);
        return ResponseEntity.ok(ApiResponse.success("Voucher details retrieved successfully", response));
    }

    @Operation(summary = "Claim voucher", description = "Saves/claims a voucher code to the authenticated user's wallet.")
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<ClaimVoucherResponse>> claimVoucher(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ClaimVoucherRequest request
    ) {
        validateUser(currentUser);
        ClaimVoucherResponse response = voucherService.claimVoucher(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Voucher claimed successfully", response));
    }

    @Operation(summary = "Apply voucher", description = "Calculates order discount and final amount when applying a voucher code.")
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyVoucherResponse>> applyVoucher(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ApplyVoucherRequest request
    ) {
        validateUser(currentUser);
        ApplyVoucherResponse response = voucherService.applyVoucher(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Voucher applied successfully", response));
    }

    private void validateUser(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
    }
}
