package com.example.demo.discount.service.impl;

import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.discount.dto.ApplyVoucherRequest;
import com.example.demo.discount.dto.ApplyVoucherResponse;
import com.example.demo.discount.dto.ClaimVoucherRequest;
import com.example.demo.discount.dto.ClaimVoucherResponse;
import com.example.demo.discount.dto.VoucherResponse;
import com.example.demo.discount.entity.DiscountType;
import com.example.demo.discount.entity.UserVoucher;
import com.example.demo.discount.entity.UserVoucherId;
import com.example.demo.discount.entity.Voucher;
import com.example.demo.discount.repository.UserVoucherRepository;
import com.example.demo.discount.repository.VoucherRepository;
import com.example.demo.discount.service.VoucherService;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getMyVouchers(UUID userId) {
        List<Voucher> allVouchers = voucherRepository.findAll();
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserId(userId);

        Map<UUID, UserVoucher> userVoucherMap = userVouchers.stream()
                .collect(Collectors.toMap(uv -> uv.getVoucher().getId(), uv -> uv, (v1, v2) -> v1));

        return allVouchers.stream()
                .map(v -> {
                    UserVoucher uv = userVoucherMap.get(v.getId());
                    boolean isClaimed = (uv != null);
                    int usageLeft = isClaimed ? Math.max(0, uv.getUsageLimit() - uv.getUsage()) : 1;

                    return VoucherResponse.builder()
                            .id(v.getId())
                            .code(v.getCode())
                            .discountType(v.getDiscountType())
                            .value(v.getValue())
                            .maxDiscountAmount(v.getMaxDiscountAmount())
                            .minimumSpend(v.getMinimumSpend())
                            .validFrom(v.getValidFrom())
                            .validUntil(v.getValidUntil())
                            .isClaimed(isClaimed)
                            .usageLeft(usageLeft)
                            .description(buildDescription(v))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(UUID userId, String code) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + code));

        Optional<UserVoucher> uvOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId());
        boolean isClaimed = uvOpt.isPresent();
        int usageLeft = isClaimed ? Math.max(0, uvOpt.get().getUsageLimit() - uvOpt.get().getUsage()) : 1;

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .value(voucher.getValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minimumSpend(voucher.getMinimumSpend())
                .validFrom(voucher.getValidFrom())
                .validUntil(voucher.getValidUntil())
                .isClaimed(isClaimed)
                .usageLeft(usageLeft)
                .description(buildDescription(voucher))
                .build();
    }

    @Override
    public ClaimVoucherResponse claimVoucher(UUID userId, ClaimVoucherRequest request) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(request.getVoucherCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + request.getVoucherCode()));

        Instant now = Instant.now();
        if (now.isAfter(voucher.getValidUntil())) {
            throw new IllegalArgumentException("This voucher has expired");
        }

        Optional<UserVoucher> existingUv = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId());
        if (existingUv.isPresent()) {
            UserVoucher uv = existingUv.get();
            if (uv.getUsage() >= uv.getUsageLimit()) {
                throw new IllegalArgumentException("You have already used the maximum limit for this voucher");
            }
            return ClaimVoucherResponse.builder()
                    .voucherCode(voucher.getCode())
                    .usageLimit(uv.getUsageLimit())
                    .build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserVoucher userVoucher = UserVoucher.builder()
                .id(new UserVoucherId(userId, voucher.getId()))
                .user(user)
                .voucher(voucher)
                .usage(0)
                .usageLimit(1)
                .build();

        userVoucherRepository.save(userVoucher);

        return ClaimVoucherResponse.builder()
                .voucherCode(voucher.getCode())
                .usageLimit(1)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyVoucherResponse applyVoucher(UUID userId, ApplyVoucherRequest request) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(request.getVoucherCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + request.getVoucherCode()));

        validateVoucherEligibility(userId, voucher, request.getSubtotalAmount());

        BigDecimal discountAmount = calculateDiscountAmount(voucher, request.getSubtotalAmount());
        BigDecimal finalAmount = request.getSubtotalAmount().subtract(discountAmount).max(BigDecimal.ZERO);

        return ApplyVoucherResponse.builder()
                .voucherId(voucher.getId())
                .voucherCode(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .subtotalAmount(request.getSubtotalAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .isValid(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(UUID userId, String voucherCode, BigDecimal subtotalAmount) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(voucherCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with code: " + voucherCode));

        validateVoucherEligibility(userId, voucher, subtotalAmount);
        return calculateDiscountAmount(voucher, subtotalAmount);
    }

    private void validateVoucherEligibility(UUID userId, Voucher voucher, BigDecimal subtotalAmount) {
        Instant now = Instant.now();
        if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidUntil())) {
            throw new IllegalArgumentException("Voucher is not valid at this time");
        }

        if (voucher.getMinimumSpend() != null && subtotalAmount.compareTo(voucher.getMinimumSpend()) < 0) {
            throw new IllegalArgumentException(String.format("Minimum spend of %.2f is required to use voucher %s",
                    voucher.getMinimumSpend(), voucher.getCode()));
        }

        Optional<UserVoucher> uvOpt = userVoucherRepository.findByUserIdAndVoucherId(userId, voucher.getId());
        if (uvOpt.isPresent()) {
            UserVoucher uv = uvOpt.get();
            if (uv.getUsage() >= uv.getUsageLimit()) {
                throw new IllegalArgumentException(String.format("You have reached the usage limit for voucher %s", voucher.getCode()));
            }
        }
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal subtotalAmount) {
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotalAmount.multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getValue().min(subtotalAmount);
        }
        return discount;
    }

    private String buildDescription(Voucher v) {
        if (v.getDiscountType() == DiscountType.PERCENTAGE) {
            String maxDesc = v.getMaxDiscountAmount() != null ? " tối đa " + v.getMaxDiscountAmount() : "";
            return String.format("Giảm %s%%%s cho đơn từ %s", v.getValue().stripTrailingZeros().toPlainString(), maxDesc, v.getMinimumSpend());
        } else {
            return String.format("Giảm %s cho đơn từ %s", v.getValue(), v.getMinimumSpend());
        }
    }
}
