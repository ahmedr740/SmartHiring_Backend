package com.smarthiring.dto;

public class ShiftSearchMatch {
    private Long shiftId;
    private String reason;

    public ShiftSearchMatch() {
    }

    public ShiftSearchMatch(Long shiftId, String reason) {
        this.shiftId = shiftId;
        this.reason = reason;
    }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
