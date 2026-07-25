package com.smarthiring.dto;

import java.time.LocalDateTime;

public class ShiftDraftResponse {
    private String title;
    private String description;
    private String requirements;
    private String roleNeeded;
    private Double pay;
    private String date;
    private String startTime;
    private String endTime;
    private String location;
    private String assumptions;
    private LocalDateTime generatedAt;
    private String source;

    public ShiftDraftResponse() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getRoleNeeded() { return roleNeeded; }
    public void setRoleNeeded(String roleNeeded) { this.roleNeeded = roleNeeded; }

    public Double getPay() { return pay; }
    public void setPay(Double pay) { this.pay = pay; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAssumptions() { return assumptions; }
    public void setAssumptions(String assumptions) { this.assumptions = assumptions; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
