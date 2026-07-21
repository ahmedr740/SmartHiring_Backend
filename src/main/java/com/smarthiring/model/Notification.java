package com.smarthiring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User recipient;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    private String actionUrl;
    private LocalDateTime readAt;
    private Boolean emailEligible;
    private String emailStatus;
    private Integer emailAttempts;
    private LocalDateTime lastEmailAttemptAt;

    @Column(nullable = false, unique = true)
    private String dedupeKey;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (emailEligible == null) {
            emailEligible = false;
        }
        if (emailStatus == null || emailStatus.isBlank()) {
            emailStatus = Boolean.TRUE.equals(emailEligible) ? "PENDING" : "NOT_APPLICABLE";
        }
        if (emailAttempts == null) {
            emailAttempts = 0;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public Boolean getEmailEligible() { return emailEligible; }
    public void setEmailEligible(Boolean emailEligible) { this.emailEligible = emailEligible; }
    public String getEmailStatus() { return emailStatus; }
    public void setEmailStatus(String emailStatus) { this.emailStatus = emailStatus; }
    public Integer getEmailAttempts() { return emailAttempts; }
    public void setEmailAttempts(Integer emailAttempts) { this.emailAttempts = emailAttempts; }
    public LocalDateTime getLastEmailAttemptAt() { return lastEmailAttemptAt; }
    public void setLastEmailAttemptAt(LocalDateTime lastEmailAttemptAt) { this.lastEmailAttemptAt = lastEmailAttemptAt; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
