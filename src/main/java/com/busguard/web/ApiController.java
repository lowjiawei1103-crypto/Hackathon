package com.busguard.web;

import com.busguard.audit.AuditLogger;
import com.busguard.data.DataLoader;
import com.busguard.execution.ExecutionPolicyEngine;
import com.busguard.execution.SimulationStateManager;
import com.busguard.ml.DelayPredictorML;
import com.busguard.ml.LLMPredictor;
import com.busguard.ml.ModelEvaluator;
import com.busguard.model.*;
import com.busguard.recovery.RecoveryOptimizer;
import com.busguard.risk.RiskEngine;
import com.busguard.simulation.BusNetworkSimulator;
import com.busguard.verification.ImpactVerifier;

import java.util.*;

public class ApiController {

    private List<BusTrip> busTrips = new ArrayList<>();
    private final DelayPredictorML predictor = new DelayPredictorML();
    private final LLMPredictor llmPredictor = new LLMPredictor();
    private final RiskEngine riskEngine = new RiskEngine(predictor);
    private final RecoveryOptimizer optimizer = new RecoveryOptimizer();
    private final ExecutionPolicyEngine policyEngine = new ExecutionPolicyEngine();
    private final SimulationStateManager stateManager = new SimulationStateManager();
    private final BusNetworkSimulator simulator = new BusNetworkSimulator();

    public ApiController(String csvPath) {
        try {
            this.busTrips = DataLoader.loadDataset(csvPath);
            System.out.println("BusGuard API Initialized with " + busTrips.size() + " trips from " + csvPath);
        } catch (Exception e) {
            System.err.println("Failed to load dataset: " + e.getMessage());
        }
    }

    public List<BusTrip> getTrips() {
        return busTrips;
    }

    public String handleRequest(String path, String query, String method, String body) {
        try {
            if (path.equals("/api/trips") || path.equals("/api/dataset")) {
                return toJsonTrips(busTrips);
            } else if (path.equals("/api/predictions")) {
                List<PredictionResult> predictions = riskEngine.evaluateAndPrioritizeTrips(busTrips);
                return toJsonPredictions(predictions);
            } else if (path.equals("/api/model-metrics")) {
                ModelEvaluator.EvaluationReport report = ModelEvaluator.evaluateModel(predictor, busTrips);
                return toJsonMetrics(report);
            } else if (path.equals("/api/trip-detail")) {
                String tripId = getQueryParam(query, "tripId");
                return getTripDetailJson(tripId);
            } else if (path.equals("/api/action/approve")) {
                String tripId = getQueryParam(query, "tripId");
                boolean ok = stateManager.approveAction(tripId);
                BusTrip trip = findTrip(tripId);
                if (trip != null) {
                    PredictionResult pred = predictor.predict(trip);
                    RecoveryOptimizer.OptimizationResult opt = optimizer.optimize(trip, pred);
                    RecoveryCandidate selected = opt.getSelectedCandidate();
                    BusNetworkSimulator.SimulationOutcome outcome = simulator.simulateIntervention(trip, pred, selected);
                    ImpactResult impact = ImpactVerifier.verifyImpact(trip, pred, selected, outcome);
                    AuditLogger.logDecision(trip, pred, selected, ExecutionDecision.REQUIRES_APPROVAL, impact, "APPROVED");

                    return generateApprovalSuccessJson(trip, selected, impact, outcome);
                }
                return String.format("{\"success\":%b,\"tripId\":\"%s\",\"status\":\"APPROVED\"}", ok, tripId);
            } else if (path.equals("/api/action/reject")) {
                String tripId = getQueryParam(query, "tripId");
                boolean ok = stateManager.rejectAction(tripId);
                BusTrip trip = findTrip(tripId);
                if (trip != null) {
                    PredictionResult pred = predictor.predict(trip);
                    RecoveryOptimizer.OptimizationResult opt = optimizer.optimize(trip, pred);
                    BusNetworkSimulator.SimulationOutcome outcome = simulator.simulateIntervention(trip, pred, null);
                    ImpactResult impact = ImpactVerifier.verifyImpact(trip, pred, null, outcome);
                    AuditLogger.logDecision(trip, pred, opt.getSelectedCandidate(), ExecutionDecision.REQUIRES_APPROVAL, impact, "REJECTED");
                }
                return String.format("{\"success\":%b,\"tripId\":\"%s\",\"status\":\"REJECTED\"}", ok, tripId);
            } else if (path.equals("/api/simulate-all")) {
                return runFullSimulation();
            } else if (path.equals("/api/audit-logs")) {
                return toJsonAuditLogs(AuditLogger.getAuditLogs());
            } else if (path.equals("/api/system-summary")) {
                return getSystemSummaryJson();
            } else if (path.equals("/api/chat")) {
                return handleChatRequest(body);
            }
        } catch (Exception e) {
            return String.format("{\"error\":\"%s\"}", escapeJson(e.getMessage()));
        }
        return "{\"error\":\"Endpoint not found\"}";
    }

    private BusTrip findTrip(String tripId) {
        for (BusTrip t : busTrips) {
            if (t.getTripId().equalsIgnoreCase(tripId)) return t;
        }
        return null;
    }

    private String getQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return "";
    }

    private String getTripDetailJson(String tripId) {
        BusTrip trip = findTrip(tripId);
        if (trip == null && !busTrips.isEmpty()) trip = busTrips.get(0);
        if (trip == null) return "{\"error\":\"Trip not found\"}";

        PredictionResult pred = predictor.predict(trip);
        RecoveryOptimizer.OptimizationResult opt = optimizer.optimize(trip, pred);
        ExecutionDecision decision = policyEngine.evaluateDecision(pred, opt.getSelectedCandidate());
        
        stateManager.recordDecision(trip.getTripId(), decision);
        SimulationStateManager.ActionState actionState = stateManager.getTripState(trip.getTripId());

        BusNetworkSimulator.SimulationOutcome outcome = simulator.simulateIntervention(trip, pred, opt.getSelectedCandidate());
        ImpactResult impact = ImpactVerifier.verifyImpact(trip, pred, opt.getSelectedCandidate(), outcome);

        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format("\"tripId\":\"%s\",", trip.getTripId()));
        sb.append(String.format("\"busId\":\"%s\",", trip.getBusId()));
        sb.append(String.format("\"routeId\":\"%s\",", trip.getRouteId()));
        sb.append(String.format("\"routeName\":\"%s\",", escapeJson(trip.getRouteName())));
        sb.append(String.format("\"passengerLoad\":%d,", trip.getPassengerLoad()));
        sb.append(String.format("\"driverShiftHours\":%.1f,", trip.getDriverShiftHours()));
        sb.append(String.format("\"standbyBusAvailable\":%b,", trip.isStandbyBusAvailable()));
        sb.append(String.format("\"weatherCondition\":\"%s\",", trip.getWeatherCondition()));
        sb.append(String.format("\"trafficCongestionLevel\":\"%s\",", trip.getTrafficCongestionLevel()));
        sb.append(String.format("\"delayCause\":\"%s\",", escapeJson(trip.getDelayCause())));
        
        // Prediction details
        sb.append(String.format("\"predictedDelayMinutes\":%.1f,", pred.getPredictedDelayMinutes()));
        sb.append(String.format("\"delayProbability\":%.2f,", pred.getDelayProbability()));
        sb.append(String.format("\"riskLevel\":\"%s\",", pred.getRiskLevel().name()));
        sb.append(String.format("\"riskColor\":\"%s\",", pred.getRiskLevel().getColor()));
        sb.append(String.format("\"priorityScore\":%.1f,", pred.getPriorityScore()));
        sb.append(String.format("\"primaryFactor\":\"%s\",", escapeJson(pred.getPrimaryFactor())));
        
        String llmInsight = llmPredictor.generateLLMExplanation(trip, pred);
        sb.append(String.format("\"explanationText\":\"%s\",", escapeJson(pred.getExplanationText())));
        sb.append(String.format("\"llmInsight\":\"%s\",", escapeJson(llmInsight)));
        
        // Feature contributions
        sb.append("\"featureContributions\":{");
        int idx = 0;
        for (Map.Entry<String, Double> entry : pred.getFeatureContributions().entrySet()) {
            if (idx++ > 0) sb.append(",");
            sb.append(String.format("\"%s\":%.1f", escapeJson(entry.getKey()), entry.getValue()));
        }
        sb.append("},");

        // Candidates
        sb.append("\"candidates\":[");
        for (int i = 0; i < opt.getAllCandidates().size(); i++) {
            RecoveryCandidate c = opt.getAllCandidates().get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"candidateId\":\"%s\",", c.getCandidateId()));
            sb.append(String.format("\"strategyType\":\"%s\",", c.getStrategyType().name()));
            sb.append(String.format("\"title\":\"%s\",", escapeJson(c.getTitle())));
            sb.append(String.format("\"actionDetails\":\"%s\",", escapeJson(c.getActionDetails())));
            sb.append(String.format("\"feasible\":%b,", c.isFeasible()));
            sb.append(String.format("\"feasibilityReason\":\"%s\",", escapeJson(c.getFeasibilityReason())));
            sb.append(String.format("\"estimatedPostActionDelayMin\":%.1f,", c.getEstimatedPostActionDelayMin()));
            sb.append(String.format("\"expectedDelayReductionMin\":%.1f,", c.getExpectedDelayReductionMin()));
            sb.append(String.format("\"estimatedCostImpact\":%.2f,", c.getEstimatedCostImpact()));
            sb.append(String.format("\"confidenceScore\":%.2f,", c.getConfidenceScore()));
            sb.append(String.format("\"potentialSideEffects\":\"%s\",", escapeJson(c.getPotentialSideEffects())));
            sb.append(String.format("\"recommendationRationale\":\"%s\"", escapeJson(c.getRecommendationRationale())));
            sb.append("}");
        }
        sb.append("],");

        // Selected candidate
        RecoveryCandidate sel = opt.getSelectedCandidate();
        sb.append("\"selectedCandidate\":{");
        sb.append(String.format("\"candidateId\":\"%s\",", sel.getCandidateId()));
        sb.append(String.format("\"strategyType\":\"%s\",", sel.getStrategyType().name()));
        sb.append(String.format("\"title\":\"%s\",", escapeJson(sel.getTitle())));
        sb.append(String.format("\"actionDetails\":\"%s\",", escapeJson(sel.getActionDetails())));
        sb.append(String.format("\"expectedDelayReductionMin\":%.1f", sel.getExpectedDelayReductionMin()));
        sb.append("},");

        // Execution Decision
        sb.append(String.format("\"executionDecision\":\"%s\",", decision.name()));
        sb.append(String.format("\"executionDecisionLabel\":\"%s\",", decision.getLabel()));
        sb.append(String.format("\"executionDecisionColor\":\"%s\",", decision.getColor()));
        sb.append(String.format("\"actionState\":\"%s\",", actionState.name()));

        // Impact verification
        sb.append("\"impact\":{");
        sb.append(String.format("\"baselineDelayMin\":%.1f,", impact.getBaselineDelayMin()));
        sb.append(String.format("\"postActionDelayMin\":%.1f,", impact.getPostActionDelayMin()));
        sb.append(String.format("\"delayReductionMin\":%.1f,", impact.getDelayReductionMin()));
        sb.append(String.format("\"percentageImprovement\":%.1f,", impact.getPercentageImprovement()));
        sb.append(String.format("\"costImpact\":%.2f,", impact.getCostImpact()));
        sb.append(String.format("\"outcomeStatus\":\"%s\",", impact.getOutcomeStatus()));
        sb.append(String.format("\"verificationSummary\":\"%s\"", escapeJson(impact.getVerificationSummary())));
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private String runFullSimulation() {
        AuditLogger.clearLogs();
        int autoCount = 0, reqApprovalCount = 0, blockedCount = 0;
        double totalReduction = 0.0;

        for (BusTrip trip : busTrips) {
            PredictionResult pred = predictor.predict(trip);
            RecoveryOptimizer.OptimizationResult opt = optimizer.optimize(trip, pred);
            ExecutionDecision dec = policyEngine.evaluateDecision(pred, opt.getSelectedCandidate());
            
            stateManager.recordDecision(trip.getTripId(), dec);
            BusNetworkSimulator.SimulationOutcome outcome = simulator.simulateIntervention(trip, pred, opt.getSelectedCandidate());
            ImpactResult impact = ImpactVerifier.verifyImpact(trip, pred, opt.getSelectedCandidate(), outcome);

            String status = "PENDING";
            if (dec == ExecutionDecision.AUTO_EXECUTE) {
                status = "AUTO_EXECUTED";
                autoCount++;
            } else if (dec == ExecutionDecision.REQUIRES_APPROVAL) {
                status = "PENDING_APPROVAL";
                reqApprovalCount++;
            } else {
                status = "BLOCKED";
                blockedCount++;
            }

            totalReduction += impact.getDelayReductionMin();
            AuditLogger.logDecision(trip, pred, opt.getSelectedCandidate(), dec, impact, status);
        }

        return String.format(
            "{\"status\":\"SUCCESS\",\"totalTrips\":%d,\"autoExecuted\":%d,\"requiresApproval\":%d,\"blocked\":%d,\"totalDelaySavedMin\":%.1f}",
            busTrips.size(), autoCount, reqApprovalCount, blockedCount, totalReduction
        );
    }

    private String getSystemSummaryJson() {
        List<PredictionResult> preds = riskEngine.evaluateAndPrioritizeTrips(busTrips);
        int total = preds.size();
        int criticalCount = 0, highCount = 0, mediumCount = 0, lowCount = 0;
        double totalBaselineDelay = 0.0;
        double totalSaved = 0.0;

        for (PredictionResult p : preds) {
            totalBaselineDelay += p.getPredictedDelayMinutes();
            if (p.getRiskLevel() == RiskLevel.CRITICAL) criticalCount++;
            else if (p.getRiskLevel() == RiskLevel.HIGH) highCount++;
            else if (p.getRiskLevel() == RiskLevel.MEDIUM) mediumCount++;
            else lowCount++;
        }

        return String.format(
            "{\"totalTrips\":%d,\"criticalTrips\":%d,\"highRiskTrips\":%d,\"mediumRiskTrips\":%d,\"lowRiskTrips\":%d,\"totalBaselineDelayMin\":%.1f,\"modelMae\":%.2f,\"modelPrecision\":%.2f}",
            total, criticalCount, highCount, mediumCount, lowCount, totalBaselineDelay, predictor.getModelMAE(), predictor.getPrecision()
        );
    }

    private String toJsonTrips(List<BusTrip> trips) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trips.size(); i++) {
            BusTrip t = trips.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"tripId\":\"%s\",", t.getTripId()));
            sb.append(String.format("\"busId\":\"%s\",", t.getBusId()));
            sb.append(String.format("\"routeId\":\"%s\",", t.getRouteId()));
            sb.append(String.format("\"routeName\":\"%s\",", escapeJson(t.getRouteName())));
            sb.append(String.format("\"passengerLoad\":%d,", t.getPassengerLoad()));
            sb.append(String.format("\"weatherCondition\":\"%s\",", t.getWeatherCondition()));
            sb.append(String.format("\"trafficCongestionLevel\":\"%s\",", t.getTrafficCongestionLevel()));
            sb.append(String.format("\"latitude\":%.4f,", t.getLatitude()));
            sb.append(String.format("\"longitude\":%.4f", t.getLongitude()));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonPredictions(List<PredictionResult> predictions) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < predictions.size(); i++) {
            PredictionResult p = predictions.get(i);
            BusTrip t = findTrip(p.getTripId());
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"rank\":%d,", i + 1));
            sb.append(String.format("\"tripId\":\"%s\",", p.getTripId()));
            sb.append(String.format("\"busId\":\"%s\",", t != null ? t.getBusId() : "N/A"));
            sb.append(String.format("\"routeId\":\"%s\",", t != null ? t.getRouteId() : "N/A"));
            sb.append(String.format("\"routeName\":\"%s\",", t != null ? escapeJson(t.getRouteName()) : "N/A"));
            sb.append(String.format("\"predictedDelayMinutes\":%.1f,", p.getPredictedDelayMinutes()));
            sb.append(String.format("\"delayProbability\":%.2f,", p.getDelayProbability()));
            sb.append(String.format("\"riskLevel\":\"%s\",", p.getRiskLevel().name()));
            sb.append(String.format("\"riskColor\":\"%s\",", p.getRiskLevel().getColor()));
            sb.append(String.format("\"priorityScore\":%.1f,", p.getPriorityScore()));
            sb.append(String.format("\"primaryFactor\":\"%s\",", escapeJson(p.getPrimaryFactor())));
            sb.append(String.format("\"explanationText\":\"%s\",", escapeJson(p.getExplanationText())));
            sb.append(String.format("\"actionState\":\"%s\"", stateManager.getTripState(p.getTripId()).name()));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonMetrics(ModelEvaluator.EvaluationReport report) {
        return String.format(
            "{\"mae\":%.2f,\"rmse\":%.2f,\"precision\":%.2f,\"recall\":%.2f,\"f1Score\":%.2f,\"totalEvaluatedTrips\":%d,\"tp\":%d,\"fp\":%d,\"tn\":%d,\"fn\":%d}",
            report.mae, report.rmse, report.precision, report.recall, report.f1Score,
            report.totalEvaluatedTrips, report.truePositives, report.falsePositives, report.trueNegatives, report.falseNegatives
        );
    }

    private String toJsonAuditLogs(List<AuditRecord> logs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < logs.size(); i++) {
            AuditRecord r = logs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"auditId\":\"%s\",", r.getAuditId()));
            sb.append(String.format("\"timestamp\":\"%s\",", r.getTimestamp().toString()));
            sb.append(String.format("\"tripId\":\"%s\",", r.getTripId()));
            sb.append(String.format("\"busId\":\"%s\",", r.getBusId()));
            sb.append(String.format("\"routeId\":\"%s\",", r.getRouteId()));
            sb.append(String.format("\"predictedDelayMin\":%.1f,", r.getPredictedDelayMin()));
            sb.append(String.format("\"riskLevel\":\"%s\",", r.getRiskLevel().name()));
            sb.append(String.format("\"selectedStrategy\":\"%s\",", escapeJson(r.getSelectedStrategy())));
            sb.append(String.format("\"executionDecision\":\"%s\",", r.getExecutionDecision().name()));
            sb.append(String.format("\"actionStatus\":\"%s\",", r.getActionStatus()));
            sb.append(String.format("\"delayReductionMin\":%.1f,", r.getDelayReductionMin()));
            sb.append(String.format("\"percentageImprovement\":%.1f,", r.getPercentageImprovement()));
            sb.append(String.format("\"decisionRationale\":\"%s\"", escapeJson(r.getDecisionRationale())));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateApprovalSuccessJson(BusTrip trip, RecoveryCandidate selected, ImpactResult impact, BusNetworkSimulator.SimulationOutcome outcome) {
        StringBuilder steps = new StringBuilder("[");
        if (selected.getStrategyType() == StrategyType.ROUTE_OPTIMIZATION) {
            steps.append("\"Step 1: Signal Driver of Bus ").append(trip.getBusId()).append(" to divert at Exit 211 onto Expressway Bypass Corridor.\",");
            steps.append("\"Step 2: Notify passengers via bus onboard display that intermediate stops #4 and #5 will be bypassed to skip heavy congestion.\",");
            steps.append("\"Step 3: Re-enter main route schedule at Station 8. Expected delay saving: ").append(impact.getDelayReductionMin()).append(" minutes.\"");
        } else if (selected.getStrategyType() == StrategyType.STANDBY_REASSIGNMENT) {
            steps.append("\"Step 1: Issue immediate depot dispatch command for Standby Bus MYB-DEPOT-99 at KL Sentral Hub.\",");
            steps.append("\"Step 2: Proceed via express bus lane directly to Terminal Station 5.\",");
            steps.append("\"Step 3: Inject Standby Bus into Trip ").append(trip.getTripId()).append(" sequence, absorbing 80% of downstream delay.\"");
        } else if (selected.getStrategyType() == StrategyType.ADAPTIVE_SCHEDULING) {
            steps.append("\"Step 1: Transmit departure adjustment code +5 mins to driver console.\",");
            steps.append("\"Step 2: Update terminal electronic passenger display boards with adjusted 5-minute headway buffer.\",");
            steps.append("\"Step 3: Synchronize trailing bus interval to eliminate bus bunching along Route ").append(trip.getRouteId()).append(".\"");
        } else {
            steps.append("\"Step 1: Retain baseline timetable.\",");
            steps.append("\"Step 2: Monitor vehicle telemetry for route updates.\",");
            steps.append("\"Step 3: Re-evaluate when traffic congestion level changes.\"");
        }
        steps.append("]");

        return String.format(
            "{\"success\":true,\"tripId\":\"%s\",\"busId\":\"%s\",\"routeId\":\"%s\",\"status\":\"SUCCESSFUL_INTERVENTION_APPLIED\"," +
            "\"strategyTitle\":\"%s\",\"baselineDelayMin\":%.1f,\"postActionDelayMin\":%.1f,\"delayReductionMin\":%.1f," +
            "\"percentageImprovement\":%.1f,\"costImpact\":%.2f,\"outcomeStatus\":\"%s\",\"verificationSummary\":\"%s\"," +
            "\"movementSteps\":%s}",
            trip.getTripId(), trip.getBusId(), trip.getRouteId(),
            escapeJson(selected.getTitle()), impact.getBaselineDelayMin(), impact.getPostActionDelayMin(),
            impact.getDelayReductionMin(), impact.getPercentageImprovement(), impact.getCostImpact(),
            impact.getOutcomeStatus(), escapeJson(impact.getVerificationSummary()), steps.toString()
        );
    }

    private String handleChatRequest(String body) {
        String userMessage = "";
        if (body != null && body.contains("\"message\":\"")) {
            int start = body.indexOf("\"message\":\"") + 11;
            int end = body.indexOf("\"", start);
            if (end != -1) {
                userMessage = body.substring(start, end);
            }
        }
        if (userMessage.isEmpty()) {
            userMessage = "Give a summary of current bus delay risks and recovery actions.";
        }

        List<PredictionResult> preds = riskEngine.evaluateAndPrioritizeTrips(busTrips);
        StringBuilder context = new StringBuilder("You are BusGuard AI Dispatch Assistant for Rapid KL buses in Kuala Lumpur. Current live system state:\n");
        context.append("Total active trips: ").append(busTrips.size()).append("\n");
        for (int i = 0; i < Math.min(5, preds.size()); i++) {
            PredictionResult p = preds.get(i);
            BusTrip t = findTrip(p.getTripId());
            context.append(String.format("Trip %s (Bus %s, Route %s): Delay +%.1fm, Risk %s, Cause: %s\n",
                    p.getTripId(), t != null ? t.getBusId() : "N/A", t != null ? t.getRouteId() : "N/A",
                    p.getPredictedDelayMinutes(), p.getRiskLevel().name(), p.getPrimaryFactor()));
        }
        context.append("Be helpful, professional, and concise in assisting bus operations staff.");

        String aiReply = llmPredictor.chatWithAssistant(userMessage, context.toString());
        return String.format("{\"reply\":\"%s\"}", escapeJson(aiReply));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "");
    }
}
