package com.smarthiring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    @JsonIgnoreProperties({"password"})
    private User worker; // the worker applying

    @ManyToOne
    @JoinColumn(name = "shift_id")
    @JsonIgnoreProperties({"password"})
    private Shift shift; // the shift being applied for

    private String status; // PENDING, ACCEPTED, REJECTED
    private Integer workerRating;
    private String workerReview;
    private LocalDateTime workerRatedAt;
    private Integer managerRating;
    private String managerReview;
    private LocalDateTime managerRatedAt;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getWorker() { return worker; }
    public void setWorker(User worker) { this.worker = worker; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getWorkerRating() { return workerRating; }
    public void setWorkerRating(Integer workerRating) { this.workerRating = workerRating; }

    public String getWorkerReview() { return workerReview; }
    public void setWorkerReview(String workerReview) { this.workerReview = workerReview; }

    public LocalDateTime getWorkerRatedAt() { return workerRatedAt; }
    public void setWorkerRatedAt(LocalDateTime workerRatedAt) { this.workerRatedAt = workerRatedAt; }

    public Integer getManagerRating() { return managerRating; }
    public void setManagerRating(Integer managerRating) { this.managerRating = managerRating; }

    public String getManagerReview() { return managerReview; }
    public void setManagerReview(String managerReview) { this.managerReview = managerReview; }

    public LocalDateTime getManagerRatedAt() { return managerRatedAt; }
    public void setManagerRatedAt(LocalDateTime managerRatedAt) { this.managerRatedAt = managerRatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
