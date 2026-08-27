package com.example.demo.discount.repository;

import com.example.demo.discount.entity.UserVoucher;
import com.example.demo.discount.entity.UserVoucherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, UserVoucherId> {
    List<UserVoucher> findByUserId(UUID userId);

    @Query("SELECT uv FROM UserVoucher uv WHERE uv.user.id = :userId AND uv.voucher.id = :voucherId")
    Optional<UserVoucher> findByUserIdAndVoucherId(@Param("userId") UUID userId, @Param("voucherId") UUID voucherId);

    @Query("SELECT uv FROM UserVoucher uv WHERE uv.user.id = :userId AND LOWER(uv.voucher.code) = LOWER(:code)")
    Optional<UserVoucher> findByUserIdAndVoucherCode(@Param("userId") UUID userId, @Param("code") String code);
}
