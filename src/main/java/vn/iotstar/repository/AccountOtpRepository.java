package vn.iotstar.repository;

import java.util.Optional;
import java.time.LocalDateTime;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import vn.iotstar.entity.AccountOtp;

public interface AccountOtpRepository extends JpaRepository<AccountOtp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountOtp> findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(Integer userId, String purpose);

    long countByUser_IdAndPurposeAndCreatedAtAfter(Integer userId, String purpose, LocalDateTime createdAfter);

    void deleteByUser_Id(Integer userId);
}
