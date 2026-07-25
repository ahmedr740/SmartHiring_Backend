package com.smarthiring.dto;

public class ShiftSearchRequest {
    private String query;

    public ShiftSearchRequest() {
    }

    public ShiftSearchRequest(String query) {
        this.query = query;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
