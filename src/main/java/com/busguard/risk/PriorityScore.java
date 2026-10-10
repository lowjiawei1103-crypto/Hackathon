package com.busguard.risk;

import com.busguard.model.RiskLevel;

public class PriorityScore {

    /**
     * Priority Score Formula (FR-08):
     * Priority Score = (Predicted Delay Mins * 0.4) + (Delay Probability * 25.0) + (Passenger Load % * 0.25) + (Risk Severity Rank * 5.0)
     */
    public static double calculatePriority(double delayMinutes, double probability, int passengerLoad, RiskLevel riskLevel) {
        double score = (delayMinutes * 0.40) + (probability * 25.0) + (passengerLoad * 0.25) + (riskLevel.getSeverityRank() * 5.0);
        return Math.round(score * 10.0) / 10.0;
    }
}
