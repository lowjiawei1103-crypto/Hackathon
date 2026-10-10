package com.busguard.ml;

import com.busguard.model.BusTrip;
import java.util.List;

public class ModelEvaluator {

    public static class EvaluationReport {
        public double mae;
        public double rmse;
        public double precision;
        public double recall;
        public double f1Score;
        public int truePositives;
        public int falsePositives;
        public int trueNegatives;
        public int falseNegatives;
        public int totalEvaluatedTrips;

        @Override
        public String toString() {
            return String.format(
                "Model Evaluation Metrics:\n" +
                " - Evaluated Trips: %d\n" +
                " - MAE (Mean Absolute Error): %.2f min\n" +
                " - RMSE (Root Mean Squared Error): %.2f min\n" +
                " - Precision: %.2f\n" +
                " - Recall: %.2f\n" +
                " - F1-Score: %.2f\n" +
                " - Confusion Matrix: [TP: %d, FP: %d, FN: %d, TN: %d]",
                totalEvaluatedTrips, mae, rmse, precision, recall, f1Score,
                truePositives, falsePositives, falseNegatives, trueNegatives
            );
        }
    }

    public static EvaluationReport evaluateModel(DelayPredictorML predictor, List<BusTrip> trips) {
        EvaluationReport report = new EvaluationReport();
        if (trips == null || trips.isEmpty()) return report;

        double totalAbsoluteError = 0.0;
        double totalSquaredError = 0.0;
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (BusTrip trip : trips) {
            double actualDelay = trip.calculateActualDelayMinutes();
            double predictedDelay = predictor.predict(trip).getPredictedDelayMinutes();

            double err = Math.abs(actualDelay - predictedDelay);
            totalAbsoluteError += err;
            totalSquaredError += (err * err);

            boolean actualIsDelayed = actualDelay >= 15.0;
            boolean predictedIsDelayed = predictedDelay >= 15.0;

            if (actualIsDelayed && predictedIsDelayed) tp++;
            else if (!actualIsDelayed && predictedIsDelayed) fp++;
            else if (!actualIsDelayed && !predictedIsDelayed) tn++;
            else if (actualIsDelayed && !predictedIsDelayed) fn++;
        }

        int n = trips.size();
        report.totalEvaluatedTrips = n;
        report.mae = Math.round((totalAbsoluteError / n) * 100.0) / 100.0;
        report.rmse = Math.round(Math.sqrt(totalSquaredError / n) * 100.0) / 100.0;
        report.truePositives = tp;
        report.falsePositives = fp;
        report.trueNegatives = tn;
        report.falseNegatives = fn;

        double prec = (tp + fp > 0) ? (double) tp / (tp + fp) : 1.0;
        double rec = (tp + fn > 0) ? (double) tp / (tp + fn) : 1.0;
        double f1 = (prec + rec > 0) ? 2 * (prec * rec) / (prec + rec) : 0.0;

        report.precision = Math.round(prec * 100.0) / 100.0;
        report.recall = Math.round(rec * 100.0) / 100.0;
        report.f1Score = Math.round(f1 * 100.0) / 100.0;

        return report;
    }
}
