package com.busguard.model;

import java.util.Map;

public class PredictionResult {
    private String tripId;
    private double predictedDelayMinutes;
    private double delayProbability;
    private RiskLevel riskLevel;
    private double priorityScore;
    private String primaryFactor;
    private String explanationText;
    private Map<String, Double> featureContributions;
    private boolean dataReliable;
    private String warningMessage;

    public PredictionResult() {}

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public double getPredictedDelayMinutes() { return predictedDelayMinutes; }
    public void setPredictedDelayMinutes(double predictedDelayMinutes) { this.predictedDelayMinutes = predictedDelayMinutes; }

    public double getDelayProbability() { return delayProbability; }
    public void setDelayProbability(double delayProbability) { this.delayProbability = delayProbability; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }

    public String getPrimaryFactor() { return primaryFactor; }
    public void setPrimaryFactor(String primaryFactor) { this.primaryFactor = primaryFactor; }

    public String getExplanationText() { return explanationText; }
    public void setExplanationText(String explanationText) { this.explanationText = explanationText; }

    public Map<String, Double> getFeatureContributions() { return featureContributions; }
    public void setFeatureContributions(Map<String, Double> featureContributions) { this.featureContributions = featureContributions; }

    public boolean isDataReliable() { return dataReliable; }
    public void setDataReliable(boolean dataReliable) { this.dataReliable = dataReliable; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}
