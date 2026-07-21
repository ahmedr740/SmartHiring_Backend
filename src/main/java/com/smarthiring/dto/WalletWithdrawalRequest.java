package com.smarthiring.dto;

public class WalletWithdrawalRequest {
    private Double amount;
    private String methodLabel;

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMethodLabel() { return methodLabel; }
    public void setMethodLabel(String methodLabel) { this.methodLabel = methodLabel; }
}
