package com.smarthiring.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MatchRecommendationResponse {
    private Long targetId;
    private Integer rank;
    private Integer aiScore;
    private Integer fallbackScore;
    private String label;
    private String explanation;
    private List<String> strengths;
    private List<String> risks;
    private String recommendedAction;
    private LocalDateTime generatedAt;
    private String source;

    public MatchRecommendationResponse() {
    }

    public MatchRecommendationResponse(
            Long targetId,
            Integer rank,
            Integer aiScore,
            Integer fallbackScore,
            String label,
            String explanation,
            List<String> strengths,
            List<String> risks,
            String recommendedAction,
            LocalDateTime generatedAt,
            String source
    ) {
        this.targetId = targetId;
        this.rank = rank;
        this.aiScore = aiScore;
        this.fallbackScore = fallbackScore;
        this.label = label;
        this.explanation = explanation;
        this.strengths = strengths;
        this.risks = risks;
        this.recommendedAction = recommendedAction;
        this.generatedAt = generatedAt;
        this.source = source;
    }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }

    public Integer getFallbackScore() { return fallbackScore; }
    public void setFallbackScore(Integer fallbackScore) { this.fallbackScore = fallbackScore; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
