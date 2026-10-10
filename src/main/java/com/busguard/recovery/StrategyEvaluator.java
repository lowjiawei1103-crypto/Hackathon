package com.busguard.recovery;

import com.busguard.model.BusTrip;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;
import com.busguard.model.StrategyType;

public class StrategyEvaluator {

    public static RecoveryCandidate evaluate(BusTrip trip, PredictionResult prediction, StrategyType strategy) {
        RecoveryCandidate candidate = new RecoveryCandidate();
        candidate.setCandidateId("CAN-" + strategy.name() + "-" + trip.getTripId());
        candidate.setStrategyType(strategy);
        candidate.setTitle(strategy.getTitle());

        double baselineDelay = prediction.getPredictedDelayMinutes();
        StringBuilder feasibilityReason = new StringBuilder();

        boolean feasible = OperationalConstraints.checkFeasibility(trip, strategy, feasibilityReason);
        candidate.setFeasible(feasible);
        candidate.setFeasibilityReason(feasibilityReason.toString());

        if (!feasible) {
            candidate.setEstimatedPostActionDelayMin(baselineDelay);
            candidate.setExpectedDelayReductionMin(0.0);
            candidate.setEstimatedCostImpact(0.0);
            candidate.setConfidenceScore(0.0);
            candidate.setActionDetails("Infeasible Strategy");
            candidate.setPotentialSideEffects("N/A - Rejected before simulation.");
            candidate.setRecommendationRationale("Rejected due to constraint violation: " + feasibilityReason.toString());
            return candidate;
        }

        switch (strategy) {
            case ROUTE_OPTIMIZATION:
                double routeSaving = Math.min(baselineDelay * 0.55, 18.0); // 55% delay reduction
                candidate.setEstimatedPostActionDelayMin(Math.round((baselineDelay - routeSaving) * 10.0) / 10.0);
                candidate.setExpectedDelayReductionMin(Math.round(routeSaving * 10.0) / 10.0);
                candidate.setEstimatedCostImpact(15.00); // Fuel cost for detour in MYR
                candidate.setConfidenceScore(0.85);
                candidate.setActionDetails("Reroute bus via AKLEH / Federal Expressway bypass corridor to skip congestion area.");
                candidate.setPotentialSideEffects("Minor route deviation skips 2 low-volume intermediate bus stops.");
                candidate.setRecommendationRationale("Effective rerouting bypasses primary congestion bottleneck with 55% delay saving.");
                break;

            case STANDBY_REASSIGNMENT:
                double standbySaving = Math.min(baselineDelay * 0.80, 30.0); // 80% delay reduction
                candidate.setEstimatedPostActionDelayMin(Math.round((baselineDelay - standbySaving) * 10.0) / 10.0);
                candidate.setExpectedDelayReductionMin(Math.round(standbySaving * 10.0) / 10.0);
                candidate.setEstimatedCostImpact(45.00); // Standby bus dispatch operational cost in MYR
                candidate.setConfidenceScore(0.92);
                candidate.setActionDetails("Dispatch standby bus MYB-DEPOT-99 from KL Central hub directly to downstream station.");
                candidate.setPotentialSideEffects("Consumes 1 depot standby bus unit for 2 hours.");
                candidate.setRecommendationRationale("High impact intervention recovers 80% of trip delay by injecting fresh vehicle.");
                break;

            case ADAPTIVE_SCHEDULING:
                double adaptiveSaving = Math.min(baselineDelay * 0.35, 10.0); // 35% delay reduction
                candidate.setEstimatedPostActionDelayMin(Math.round((baselineDelay - adaptiveSaving) * 10.0) / 10.0);
                candidate.setExpectedDelayReductionMin(Math.round(adaptiveSaving * 10.0) / 10.0);
                candidate.setEstimatedCostImpact(0.00); // Zero additional financial cost
                candidate.setConfidenceScore(0.78);
                candidate.setActionDetails("Adjust departure headway by +5 minutes for downstream trip sequence to smooth passenger arrival buffer.");
                candidate.setPotentialSideEffects("Slightly increases waiting time for passengers at initial terminal by 5 minutes.");
                candidate.setRecommendationRationale("Zero-cost schedule adjustment reduces cascading delay and prevents bus bunching.");
                break;

            case NO_ACTION:
            default:
                candidate.setEstimatedPostActionDelayMin(baselineDelay);
                candidate.setExpectedDelayReductionMin(0.0);
                candidate.setEstimatedCostImpact(0.0);
                candidate.setConfidenceScore(1.0);
                candidate.setActionDetails("Maintain existing route and timetable without operational intervention.");
                candidate.setPotentialSideEffects("Full predicted delay of " + baselineDelay + " mins will be experienced.");
                candidate.setRecommendationRationale("Baseline option retained when interventions are infeasible or offer insufficient gain.");
                break;
        }

        return candidate;
    }
}
