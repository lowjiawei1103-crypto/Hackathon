package com.busguard.model;

public class RecoveryCandidate {
    private String candidateId;
    private StrategyType strategyType;
    private String title;
    private String actionDetails;
    private boolean feasible;
    private String feasibilityReason;
    private double estimatedPostActionDelayMin;
    private double expectedDelayReductionMin;
    private double estimatedCostImpact;
    private double confidenceScore;
    private String potentialSideEffects;
    private String recommendationRationale;

    public RecoveryCandidate() {}

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getActionDetails() { return actionDetails; }
    public void setActionDetails(String actionDetails) { this.actionDetails = actionDetails; }

    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }

    public String getFeasibilityReason() { return feasibilityReason; }
    public void setFeasibilityReason(String feasibilityReason) { this.feasibilityReason = feasibilityReason; }

    public double getEstimatedPostActionDelayMin() { return estimatedPostActionDelayMin; }
    public void setEstimatedPostActionDelayMin(double estimatedPostActionDelayMin) { this.estimatedPostActionDelayMin = estimatedPostActionDelayMin; }

    public double getExpectedDelayReductionMin() { return expectedDelayReductionMin; }
    public void setExpectedDelayReductionMin(double expectedDelayReductionMin) { this.expectedDelayReductionMin = expectedDelayReductionMin; }

    public double getEstimatedCostImpact() { return estimatedCostImpact; }
    public void setEstimatedCostImpact(double estimatedCostImpact) { this.estimatedCostImpact = estimatedCostImpact; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getPotentialSideEffects() { return potentialSideEffects; }
    public void setPotentialSideEffects(String potentialSideEffects) { this.potentialSideEffects = potentialSideEffects; }

    public String getRecommendationRationale() { return recommendationRationale; }
    public void setRecommendationRationale(String recommendationRationale) { this.recommendationRationale = recommendationRationale; }
}
