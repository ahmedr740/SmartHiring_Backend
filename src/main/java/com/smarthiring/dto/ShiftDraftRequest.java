package com.smarthiring.dto;

public class ShiftDraftRequest {
    private String managerInput;

    public ShiftDraftRequest() {
    }

    public ShiftDraftRequest(String managerInput) {
        this.managerInput = managerInput;
    }

    public String getManagerInput() { return managerInput; }
    public void setManagerInput(String managerInput) { this.managerInput = managerInput; }
}
