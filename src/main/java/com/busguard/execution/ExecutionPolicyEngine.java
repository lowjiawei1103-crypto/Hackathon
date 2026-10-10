package com.busguard.execution;

import com.busguard.model.ExecutionDecision;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;
import com.busguard.model.RiskLevel;
import com.busguard.model.StrategyType;

public class ExecutionPolicyEngine {

    private double minAutoExecutionConfidence = 0.82;
    private double maxAutoExecutionDelayLimitMin = 30.0;
    private double maxAutoExecutionCostLimitMYR = 30.0;

    public ExecutionDecision evaluateDecision(PredictionResult prediction, RecoveryCandidate selectedCandidate) {
        if (selectedCandidate == null || !selectedCandidate.isFeasible()) {
            return ExecutionDecision.BLOCKED;
        }

        // Strategy is No-Action -> Auto-Execute baseline
        if (selectedCandidate.getStrategyType() == StrategyType.NO_ACTION) {
            return ExecutionDecision.AUTO_EXECUTE;
        }

        // Safety Condition 1: Check prediction data reliability
        if (!prediction.isDataReliable()) {
            return ExecutionDecision.REQUIRES_APPROVAL;
        }

        // Safety Condition 2: Critical Risk level or high delay requires human operator approval
        if (prediction.getRiskLevel() == RiskLevel.CRITICAL || prediction.getPredictedDelayMinutes() > maxAutoExecutionDelayLimitMin) {
            return ExecutionDecision.REQUIRES_APPROVAL;
        }

        // Safety Condition 3: High financial cost or high impact strategy requires approval
        if (selectedCandidate.getEstimatedCostImpact() > maxAutoExecutionCostLimitMYR) {
            return ExecutionDecision.REQUIRES_APPROVAL;
        }

        // Safety Condition 4: Model confidence score check
        if (selectedCandidate.getConfidenceScore() < minAutoExecutionConfidence) {
            return ExecutionDecision.REQUIRES_APPROVAL;
        }

        // If all safety and confidence conditions pass -> Auto Execute
        return ExecutionDecision.AUTO_EXECUTE;
    }

    public double getMinAutoExecutionConfidence() { return minAutoExecutionConfidence; }
    public void setMinAutoExecutionConfidence(double minAutoExecutionConfidence) { this.minAutoExecutionConfidence = minAutoExecutionConfidence; }

    public double getMaxAutoExecutionDelayLimitMin() { return maxAutoExecutionDelayLimitMin; }
    public void setMaxAutoExecutionDelayLimitMin(double maxAutoExecutionDelayLimitMin) { this.maxAutoExecutionDelayLimitMin = maxAutoExecutionDelayLimitMin; }

    public double getMaxAutoExecutionCostLimitMYR() { return maxAutoExecutionCostLimitMYR; }
    public void setMaxAutoExecutionCostLimitMYR(double maxAutoExecutionCostLimitMYR) { this.maxAutoExecutionCostLimitMYR = maxAutoExecutionCostLimitMYR; }
}
