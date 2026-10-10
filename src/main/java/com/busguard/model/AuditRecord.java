package com.busguard.model;

import java.time.LocalDateTime;

public class AuditRecord {
    private String auditId;
    private LocalDateTime timestamp;
    private String tripId;
    private String busId;
    private String routeId;
    private double predictedDelayMin;
    private RiskLevel riskLevel;
    private String selectedStrategy;
    private ExecutionDecision executionDecision;
    private String actionStatus; // EXECUTED_AUTO, APPROVED, REJECTED, BLOCKED
    private double delayReductionMin;
    private double percentageImprovement;
    private String decisionRationale;

    public AuditRecord() {}

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public double getPredictedDelayMin() { return predictedDelayMin; }
    public void setPredictedDelayMin(double predictedDelayMin) { this.predictedDelayMin = predictedDelayMin; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getSelectedStrategy() { return selectedStrategy; }
    public void setSelectedStrategy(String selectedStrategy) { this.selectedStrategy = selectedStrategy; }

    public ExecutionDecision getExecutionDecision() { return executionDecision; }
    public void setExecutionDecision(ExecutionDecision executionDecision) { this.executionDecision = executionDecision; }

    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }

    public double getDelayReductionMin() { return delayReductionMin; }
    public void setDelayReductionMin(double delayReductionMin) { this.delayReductionMin = delayReductionMin; }

    public double getPercentageImprovement() { return percentageImprovement; }
    public void setPercentageImprovement(double percentageImprovement) { this.percentageImprovement = percentageImprovement; }

    public String getDecisionRationale() { return decisionRationale; }
    public void setDecisionRationale(String decisionRationale) { this.decisionRationale = decisionRationale; }
}
