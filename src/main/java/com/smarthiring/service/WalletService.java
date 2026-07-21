package com.smarthiring.service;

import com.smarthiring.dto.WalletResponse;
import com.smarthiring.dto.WalletTransactionResponse;
import com.smarthiring.dto.WalletWithdrawalRequest;
import com.smarthiring.model.MockPayment;
import com.smarthiring.model.User;
import com.smarthiring.model.WalletWithdrawal;
import com.smarthiring.repository.MockPaymentRepository;
import com.smarthiring.repository.WalletWithdrawalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class WalletService {

    private final MockPaymentRepository mockPaymentRepository;
    private final WalletWithdrawalRepository walletWithdrawalRepository;

    public WalletService(
            MockPaymentRepository mockPaymentRepository,
            WalletWithdrawalRepository walletWithdrawalRepository
    ) {
        this.mockPaymentRepository = mockPaymentRepository;
        this.walletWithdrawalRepository = walletWithdrawalRepository;
    }

    public WalletResponse getWallet(User currentUser) {
        requireWorker(currentUser);
        List<MockPayment> payments = mockPaymentRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
        List<WalletWithdrawal> withdrawals = walletWithdrawalRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
        return buildWallet(payments, withdrawals);
    }

    @Transactional
    public WalletResponse withdraw(User currentUser, WalletWithdrawalRequest request) {
        requireWorker(currentUser);
        List<MockPayment> payments = mockPaymentRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
        List<WalletWithdrawal> withdrawals = walletWithdrawalRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
        WalletResponse wallet = buildWallet(payments, withdrawals);

        double requestedAmount = request == null || request.getAmount() == null
                ? wallet.getAvailableBalance()
                : request.getAmount();
        requestedAmount = money(requestedAmount);

        if (requestedAmount <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "There is no available balance to withdraw.");
        }

        if (requestedAmount > wallet.getAvailableBalance()) {
            throw new ResponseStatusException(BAD_REQUEST, "Withdrawal amount is higher than your available balance.");
        }

        WalletWithdrawal withdrawal = new WalletWithdrawal();
        withdrawal.setWorker(currentUser);
        withdrawal.setAmount(requestedAmount);
        withdrawal.setStatus("COMPLETED");
        withdrawal.setMethodLabel(cleanMethodLabel(request == null ? null : request.getMethodLabel()));
        withdrawal.setCompletedAt(LocalDateTime.now());
        walletWithdrawalRepository.save(withdrawal);

        withdrawals = walletWithdrawalRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
        return buildWallet(payments, withdrawals);
    }

    private WalletResponse buildWallet(List<MockPayment> payments, List<WalletWithdrawal> withdrawals) {
        double totalEarnings = payments.stream()
                .filter(payment -> "PAID".equalsIgnoreCase(payment.getStatus()))
                .mapToDouble(payment -> payment.getAmount() == null ? 0d : payment.getAmount())
                .sum();
        double totalWithdrawn = withdrawals.stream()
                .filter(withdrawal -> "COMPLETED".equalsIgnoreCase(withdrawal.getStatus()))
                .mapToDouble(withdrawal -> withdrawal.getAmount() == null ? 0d : withdrawal.getAmount())
                .sum();
        List<WalletTransactionResponse> transactions = new ArrayList<>();

        payments.stream()
                .filter(payment -> "PAID".equalsIgnoreCase(payment.getStatus()))
                .map(payment -> new WalletTransactionResponse(
                        payment.getId(),
                        "EARNING",
                        payment.getShift() == null ? "Shift payment" : payment.getShift().getTitle(),
                        money(payment.getAmount() == null ? 0d : payment.getAmount()),
                        payment.getStatus(),
                        payment.getMethodLabel(),
                        payment.getPaidAt() == null ? payment.getCreatedAt() : payment.getPaidAt(),
                        payment.getPaidAt()
                ))
                .forEach(transactions::add);

        withdrawals.stream()
                .map(withdrawal -> new WalletTransactionResponse(
                        withdrawal.getId(),
                        "WITHDRAWAL",
                        "Wallet withdrawal",
                        money(withdrawal.getAmount() == null ? 0d : withdrawal.getAmount()),
                        withdrawal.getStatus(),
                        withdrawal.getMethodLabel(),
                        withdrawal.getCreatedAt(),
                        withdrawal.getCompletedAt()
                ))
                .forEach(transactions::add);

        transactions.sort(Comparator.comparing(WalletTransactionResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        return new WalletResponse(
                money(totalEarnings),
                money(totalWithdrawn),
                money(Math.max(0d, totalEarnings - totalWithdrawn)),
                transactions
        );
    }

    private void requireWorker(User currentUser) {
        if (!"WORKER".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(FORBIDDEN, "Only workers can access wallet details.");
        }
    }

    private String cleanMethodLabel(String methodLabel) {
        if (methodLabel == null || methodLabel.isBlank()) {
            return "Bank transfer";
        }

        return methodLabel.trim();
    }

    private double money(double amount) {
        return Math.round(amount * 100d) / 100d;
    }
}
