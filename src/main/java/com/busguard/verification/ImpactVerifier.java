package com.busguard.verification;

import com.busguard.model.BusTrip;
import com.busguard.model.ImpactResult;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;
import com.busguard.simulation.BusNetworkSimulator;

public class ImpactVerifier {

    public static ImpactResult verifyImpact(BusTrip trip, PredictionResult prediction, RecoveryCandidate candidate, BusNetworkSimulator.SimulationOutcome outcome) {
        ImpactResult result = new ImpactResult();
        result.setTripId(trip.getTripId());
        result.setCandidateId(candidate != null ? candidate.getCandidateId() : "BASELINE");
        result.setStrategyType(candidate != null ? candidate.getStrategyType() : com.busguard.model.StrategyType.NO_ACTION);

        double baseline = prediction.getPredictedDelayMinutes();
        double postAction = outcome.getSimulatedPostActionDelayMin();
        double reduction = Math.max(0.0, baseline - postAction);
        reduction = Math.round(reduction * 10.0) / 10.0;

        // Division-by-zero protection (NFR-25)
        double percentage = 0.0;
        if (baseline > 0.0001) {
            percentage = (reduction / baseline) * 100.0;
            percentage = Math.round(percentage * 10.0) / 10.0;
        }

        result.setBaselineDelayMin(baseline);
        result.setPostActionDelayMin(postAction);
        result.setDelayReductionMin(reduction);
        result.setPercentageImprovement(percentage);
        result.setCostImpact(candidate != null ? candidate.getEstimatedCostImpact() : 0.0);
        result.setSimulatedEstimate(true);

        // Outcome categorization (FR-35)
        if (candidate == null || !candidate.isFeasible()) {
            result.setOutcomeStatus("INFEASIBLE");
            result.setVerificationSummary("Candidate strategy was infeasible or violated operational constraints.");
        } else if (percentage >= 50.0) {
            result.setOutcomeStatus("SUCCESSFUL");
            result.setVerificationSummary(String.format("Highly successful intervention: Delay reduced by %.1f mins (%.1f%% improvement). %s", reduction, percentage, outcome.getNetworkEffectNotes()));
        } else if (percentage >= 20.0) {
            result.setOutcomeStatus("PARTIALLY_EFFECTIVE");
            result.setVerificationSummary(String.format("Partially effective intervention: Delay reduced by %.1f mins (%.1f%% improvement). %s", reduction, percentage, outcome.getNetworkEffectNotes()));
        } else {
            result.setOutcomeStatus("INEFFECTIVE");
            result.setVerificationSummary(String.format("Ineffective intervention: Delay reduced by only %.1f mins (%.1f%% improvement). %s", reduction, percentage, outcome.getNetworkEffectNotes()));
        }

        return result;
    }
}
