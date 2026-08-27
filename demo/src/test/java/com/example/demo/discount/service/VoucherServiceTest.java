package com.example.demo.discount.service;

import com.example.demo.discount.dto.ApplyVoucherRequest;
import com.example.demo.discount.dto.ApplyVoucherResponse;
import com.example.demo.discount.dto.ClaimVoucherRequest;
import com.example.demo.discount.dto.ClaimVoucherResponse;
import com.example.demo.discount.dto.VoucherResponse;
import com.example.demo.discount.entity.DiscountType;
import com.example.demo.discount.entity.UserVoucher;
import com.example.demo.discount.entity.Voucher;
import com.example.demo.discount.repository.UserVoucherRepository;
import com.example.demo.discount.repository.VoucherRepository;
import com.example.demo.discount.service.impl.VoucherServiceImpl;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private User user;
    private Voucher percentageVoucher;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).username("testuser").email("user@example.com").build();

        percentageVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .code("SUMMER2026")
                .discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(15)) // 15%
                .maxDiscountAmount(BigDecimal.valueOf(100000))
                .minimumSpend(BigDecimal.valueOf(300000))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("Should claim active voucher for user")
    void testClaimVoucher() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER2026")).thenReturn(Optional.of(percentageVoucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(user.getId(), percentageVoucher.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ClaimVoucherRequest request = ClaimVoucherRequest.builder().voucherCode("SUMMER2026").build();
        ClaimVoucherResponse response = voucherService.claimVoucher(user.getId(), request);

        assertNotNull(response);
        assertEquals("SUMMER2026", response.getVoucherCode());
        assertEquals(1, response.getUsageLimit());
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    @DisplayName("Should correctly calculate percentage discount with cap")
    void testApplyVoucher_PercentageWithCap() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER2026")).thenReturn(Optional.of(percentageVoucher));
        when(userVoucherRepository.findByUserIdAndVoucherId(user.getId(), percentageVoucher.getId())).thenReturn(Optional.empty());

        // 15% of 1,000,000 is 150,000, capped at maxDiscountAmount of 100,000
        ApplyVoucherRequest request = ApplyVoucherRequest.builder()
                .voucherCode("SUMMER2026")
                .subtotalAmount(BigDecimal.valueOf(1000000))
                .build();

        ApplyVoucherResponse response = voucherService.applyVoucher(user.getId(), request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100000), response.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(900000), response.getFinalAmount());
        assertTrue(response.isValid());
    }

    @Test
    @DisplayName("Should reject voucher if subtotal does not meet minimum spend")
    void testApplyVoucher_BelowMinimumSpend() {
        when(voucherRepository.findByCodeIgnoreCase("SUMMER2026")).thenReturn(Optional.of(percentageVoucher));

        ApplyVoucherRequest request = ApplyVoucherRequest.builder()
                .voucherCode("SUMMER2026")
                .subtotalAmount(BigDecimal.valueOf(200000)) // below 300,000
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                voucherService.applyVoucher(user.getId(), request)
        );
    }
}
