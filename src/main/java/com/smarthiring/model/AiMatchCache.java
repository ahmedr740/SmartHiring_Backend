package com.smarthiring.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_match_cache")
public class AiMatchCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cacheKey;

    private Long targetId;
    private Integer aiScore;
    private Integer fallbackScore;
    private String label;

    @Column(length = 1000)
    private String explanation;

    @Column(length = 1000)
    private String strengths;

    @Column(length = 1000)
    private String risks;

    private String recommendedAction;
    private String source;
    private LocalDateTime generatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }

    public Integer getFallbackScore() { return fallbackScore; }
    public void setFallbackScore(Integer fallbackScore) { this.fallbackScore = fallbackScore; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
