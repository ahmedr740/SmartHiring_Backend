package com.smarthiring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_payments")
public class MockPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "shift_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private Shift shift;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    @JsonIgnoreProperties({"password"})
    private User worker;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    @JsonIgnoreProperties({"password"})
    private User manager;

    private Double amount;
    private String status;
    private String methodLabel;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public User getWorker() { return worker; }
    public void setWorker(User worker) { this.worker = worker; }

    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMethodLabel() { return methodLabel; }
    public void setMethodLabel(String methodLabel) { this.methodLabel = methodLabel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
