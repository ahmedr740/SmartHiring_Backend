package com.smarthiring.dto;

import java.time.LocalDateTime;

public class WalletTransactionResponse {
    private Long id;
    private String type;
    private String title;
    private Double amount;
    private String status;
    private String methodLabel;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public WalletTransactionResponse(
            Long id,
            String type,
            String title,
            Double amount,
            String status,
            String methodLabel,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.status = status;
        this.methodLabel = methodLabel;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public Double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMethodLabel() { return methodLabel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
