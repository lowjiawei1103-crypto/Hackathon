package com.busguard.simulation;

import com.busguard.model.BusTrip;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;
import com.busguard.model.StrategyType;

public class BusNetworkSimulator {

    public static class SimulationOutcome {
        private double simulatedPostActionDelayMin;
        private String networkEffectNotes;

        public SimulationOutcome(double simulatedPostActionDelayMin, String networkEffectNotes) {
            this.simulatedPostActionDelayMin = simulatedPostActionDelayMin;
            this.networkEffectNotes = networkEffectNotes;
        }

        public double getSimulatedPostActionDelayMin() { return simulatedPostActionDelayMin; }
        public String getNetworkEffectNotes() { return networkEffectNotes; }
    }

    public SimulationOutcome simulateIntervention(BusTrip trip, PredictionResult prediction, RecoveryCandidate candidate) {
        if (candidate == null || candidate.getStrategyType() == StrategyType.NO_ACTION || !candidate.isFeasible()) {
            return new SimulationOutcome(prediction.getPredictedDelayMinutes(), "Baseline simulation: No intervention applied.");
        }

        double baselineDelay = prediction.getPredictedDelayMinutes();
        double simulatedPostDelay;
        String notes;

        switch (candidate.getStrategyType()) {
            case ROUTE_OPTIMIZATION:
                simulatedPostDelay = Math.max(2.0, baselineDelay * 0.45);
                notes = "Simulated expressway detour bypassed 3.2km corridor congestion. Trip duration reduced from " + baselineDelay + " min to " + simulatedPostDelay + " min.";
                break;

            case STANDBY_REASSIGNMENT:
                simulatedPostDelay = Math.max(1.0, baselineDelay * 0.20);
                notes = "Simulated standby bus MYB-DEPOT-99 injected at Hub. Downstream passenger arrival delay eliminated by 80%.";
                break;

            case ADAPTIVE_SCHEDULING:
                simulatedPostDelay = Math.max(4.0, baselineDelay * 0.65);
                notes = "Simulated headway regulation (+4 min shift) restored regular bus spacing and mitigated bus bunching.";
                break;

            case NO_ACTION:
            default:
                simulatedPostDelay = baselineDelay;
                notes = "Baseline simulation retained.";
                break;
        }

        simulatedPostDelay = Math.round(simulatedPostDelay * 10.0) / 10.0;
        return new SimulationOutcome(simulatedPostDelay, notes);
    }
}
