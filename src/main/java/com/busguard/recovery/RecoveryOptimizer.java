package com.busguard.recovery;

import com.busguard.model.BusTrip;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;
import com.busguard.model.StrategyType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecoveryOptimizer {

    public static class OptimizationResult {
        private List<RecoveryCandidate> allCandidates;
        private RecoveryCandidate selectedCandidate;
        private String selectionRationale;

        public OptimizationResult(List<RecoveryCandidate> allCandidates, RecoveryCandidate selectedCandidate, String selectionRationale) {
            this.allCandidates = allCandidates;
            this.selectedCandidate = selectedCandidate;
            this.selectionRationale = selectionRationale;
        }

        public List<RecoveryCandidate> getAllCandidates() { return allCandidates; }
        public RecoveryCandidate getSelectedCandidate() { return selectedCandidate; }
        public String getSelectionRationale() { return selectionRationale; }
    }

    public OptimizationResult optimize(BusTrip trip, PredictionResult prediction) {
        List<RecoveryCandidate> candidates = new ArrayList<>();

        // Generate and evaluate candidates for all strategy types
        for (StrategyType strategy : StrategyType.values()) {
            RecoveryCandidate candidate = StrategyEvaluator.evaluate(trip, prediction, strategy);
            candidates.add(candidate);
        }

        // Filter feasible candidates that satisfy minimum delay reduction threshold
        List<RecoveryCandidate> feasibleCandidates = new ArrayList<>();
        for (RecoveryCandidate c : candidates) {
            if (c.isFeasible()) {
                feasibleCandidates.add(c);
            }
        }

        // Find the best candidate according to Net Operational Objective:
        // Score = Delay Reduction (mins) - (Cost / 10.0)
        RecoveryCandidate bestCandidate = candidates.stream()
                .filter(c -> c.getStrategyType() == StrategyType.NO_ACTION)
                .findFirst()
                .orElse(candidates.get(0));

        double bestScore = 0.0; // Baseline net gain is 0

        for (RecoveryCandidate candidate : feasibleCandidates) {
            if (candidate.getStrategyType() == StrategyType.NO_ACTION) continue;

            double delayReduction = candidate.getExpectedDelayReductionMin();
            if (delayReduction < OperationalConstraints.MIN_DELAY_REDUCTION_THRESHOLD) {
                continue; // Skip negligible improvements
            }

            double netBenefitScore = delayReduction - (candidate.getEstimatedCostImpact() / 15.0);
            if (netBenefitScore > bestScore) {
                bestScore = netBenefitScore;
                bestCandidate = candidate;
            }
        }

        String rationale;
        if (bestCandidate.getStrategyType() == StrategyType.NO_ACTION) {
            rationale = "Retained No-Action Baseline. Alternatives were either infeasible or offered less than minimum threshold (" + OperationalConstraints.MIN_DELAY_REDUCTION_THRESHOLD + " mins) delay reduction.";
        } else {
            rationale = String.format("Selected %s. Delivers %.1f mins delay reduction with net objective score of %.2f. Replaced baseline.",
                    bestCandidate.getTitle(), bestCandidate.getExpectedDelayReductionMin(), bestScore);
        }

        return new OptimizationResult(candidates, bestCandidate, rationale);
    }
}
