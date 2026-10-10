package com.busguard.model;

public class ImpactResult {
    private String tripId;
    private String candidateId;
    private StrategyType strategyType;
    private double baselineDelayMin;
    private double postActionDelayMin;
    private double delayReductionMin;
    private double percentageImprovement;
    private double costImpact;
    private String outcomeStatus; // SUCCESSFUL, PARTIALLY_EFFECTIVE, INEFFECTIVE, INFEASIBLE
    private String verificationSummary;
    private boolean simulatedEstimate;

    public ImpactResult() {}

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public double getBaselineDelayMin() { return baselineDelayMin; }
    public void setBaselineDelayMin(double baselineDelayMin) { this.baselineDelayMin = baselineDelayMin; }

    public double getPostActionDelayMin() { return postActionDelayMin; }
    public void setPostActionDelayMin(double postActionDelayMin) { this.postActionDelayMin = postActionDelayMin; }

    public double getDelayReductionMin() { return delayReductionMin; }
    public void setDelayReductionMin(double delayReductionMin) { this.delayReductionMin = delayReductionMin; }

    public double getPercentageImprovement() { return percentageImprovement; }
    public void setPercentageImprovement(double percentageImprovement) { this.percentageImprovement = percentageImprovement; }

    public double getCostImpact() { return costImpact; }
    public void setCostImpact(double costImpact) { this.costImpact = costImpact; }

    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }

    public String getVerificationSummary() { return verificationSummary; }
    public void setVerificationSummary(String verificationSummary) { this.verificationSummary = verificationSummary; }

    public boolean isSimulatedEstimate() { return simulatedEstimate; }
    public void setSimulatedEstimate(boolean simulatedEstimate) { this.simulatedEstimate = simulatedEstimate; }
}
