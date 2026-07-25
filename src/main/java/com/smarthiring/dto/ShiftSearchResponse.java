package com.smarthiring.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ShiftSearchResponse {
    private String interpretation;
    private List<ShiftSearchMatch> matches;
    private LocalDateTime generatedAt;
    private String source;

    public ShiftSearchResponse() {
    }

    public ShiftSearchResponse(String interpretation, List<ShiftSearchMatch> matches, LocalDateTime generatedAt, String source) {
        this.interpretation = interpretation;
        this.matches = matches;
        this.generatedAt = generatedAt;
        this.source = source;
    }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public List<ShiftSearchMatch> getMatches() { return matches; }
    public void setMatches(List<ShiftSearchMatch> matches) { this.matches = matches; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
