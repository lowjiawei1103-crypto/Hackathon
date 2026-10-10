package com.busguard.risk;

import com.busguard.ml.DelayPredictorML;
import com.busguard.model.BusTrip;
import com.busguard.model.PredictionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RiskEngine {

    private final DelayPredictorML predictor;

    public RiskEngine(DelayPredictorML predictor) {
        this.predictor = predictor;
    }

    public List<PredictionResult> evaluateAndPrioritizeTrips(List<BusTrip> trips) {
        List<PredictionResult> results = new ArrayList<>();
        if (trips == null) return results;

        for (BusTrip trip : trips) {
            PredictionResult res = predictor.predict(trip);
            results.add(res);
        }

        // Rank trips descending by priority score
        results.sort(Comparator.comparingDouble(PredictionResult::getPriorityScore).reversed());

        return results;
    }
}
