package com.busguard.audit;

import com.busguard.model.AuditRecord;
import com.busguard.model.BusTrip;
import com.busguard.model.ExecutionDecision;
import com.busguard.model.ImpactResult;
import com.busguard.model.PredictionResult;
import com.busguard.model.RecoveryCandidate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditLogger {

    private static final List<AuditRecord> auditLogs = new CopyOnWriteArrayList<>();

    public static AuditRecord logDecision(BusTrip trip, PredictionResult prediction, RecoveryCandidate selectedCandidate, ExecutionDecision decision, ImpactResult impact, String actionStatus) {
        AuditRecord record = new AuditRecord();
        record.setAuditId("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setTimestamp(LocalDateTime.now());
        record.setTripId(trip.getTripId());
        record.setBusId(trip.getBusId());
        record.setRouteId(trip.getRouteId());
        record.setPredictedDelayMin(prediction.getPredictedDelayMinutes());
        record.setRiskLevel(prediction.getRiskLevel());
        record.setSelectedStrategy(selectedCandidate != null ? selectedCandidate.getTitle() : "No Action");
        record.setExecutionDecision(decision);
        record.setActionStatus(actionStatus);
        record.setDelayReductionMin(impact != null ? impact.getDelayReductionMin() : 0.0);
        record.setPercentageImprovement(impact != null ? impact.getPercentageImprovement() : 0.0);
        record.setDecisionRationale(selectedCandidate != null ? selectedCandidate.getRecommendationRationale() : "Default");

        auditLogs.add(0, record); // Prepend for reverse chronological order
        return record;
    }

    public static List<AuditRecord> getAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    public static void clearLogs() {
        auditLogs.clear();
    }
}
