package com.example.demo.discount.service;

import com.example.demo.discount.dto.ApplyVoucherRequest;
import com.example.demo.discount.dto.ApplyVoucherResponse;
import com.example.demo.discount.dto.ClaimVoucherRequest;
import com.example.demo.discount.dto.ClaimVoucherResponse;
import com.example.demo.discount.dto.VoucherResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VoucherService {
    List<VoucherResponse> getMyVouchers(UUID userId);

    VoucherResponse getVoucherByCode(UUID userId, String code);

    ClaimVoucherResponse claimVoucher(UUID userId, ClaimVoucherRequest request);

    ApplyVoucherResponse applyVoucher(UUID userId, ApplyVoucherRequest request);

    BigDecimal calculateDiscount(UUID userId, String voucherCode, BigDecimal subtotalAmount);
}
