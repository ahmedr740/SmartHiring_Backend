package com.smarthiring.repository;

import com.smarthiring.model.WalletWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletWithdrawalRepository extends JpaRepository<WalletWithdrawal, Long> {
    List<WalletWithdrawal> findAllByWorkerIdOrderByCreatedAtDesc(Long workerId);
}
