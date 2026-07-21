package com.smarthiring.dto;

import java.util.List;

public class WalletResponse {
    private Double totalEarnings;
    private Double totalWithdrawn;
    private Double availableBalance;
    private List<WalletTransactionResponse> transactions;

    public WalletResponse(
            Double totalEarnings,
            Double totalWithdrawn,
            Double availableBalance,
            List<WalletTransactionResponse> transactions
    ) {
        this.totalEarnings = totalEarnings;
        this.totalWithdrawn = totalWithdrawn;
        this.availableBalance = availableBalance;
        this.transactions = transactions;
    }

    public Double getTotalEarnings() { return totalEarnings; }
    public Double getTotalWithdrawn() { return totalWithdrawn; }
    public Double getAvailableBalance() { return availableBalance; }
    public List<WalletTransactionResponse> getTransactions() { return transactions; }
}
