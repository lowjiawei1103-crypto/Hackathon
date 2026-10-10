package com.busguard;

import com.busguard.data.DataLoader;
import com.busguard.execution.ExecutionPolicyEngine;
import com.busguard.execution.SimulationStateManager;
import com.busguard.ml.DelayPredictorML;
import com.busguard.ml.ModelEvaluator;
import com.busguard.model.*;
import com.busguard.recovery.OperationalConstraints;
import com.busguard.recovery.RecoveryOptimizer;
import com.busguard.recovery.StrategyEvaluator;
import com.busguard.risk.PriorityScore;
import com.busguard.risk.RiskEngine;
import com.busguard.simulation.BusNetworkSimulator;
import com.busguard.verification.ImpactVerifier;

import java.util.List;

public class TestRunner {

    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println(" Running BusGuard AI System Unit & Integration Test Suite ");
        System.out.println("=========================================================");

        testDataLoader();
        testDelayPredictorML();
        testModelEvaluator();
        testRiskEngineAndPriority();
        testRecoveryOptimizerAndConstraints();
        testExecutionPolicyEngine();
        testImpactVerifier();
        testRAGKnowledgeBase();
        testEndToEndWorkflow();

        System.out.println("=========================================================");
        System.out.println(String.format(" Test Execution Summary: TOTAL: %d | PASSED: %d | FAILED: %d",
                passedTests + failedTests, passedTests, failedTests));
        System.out.println("=========================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void assertEquals(Object expected, Object actual, String testName) {
        if (expected == null && actual == null) {
            pass(testName);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            pass(testName);
        } else {
            fail(testName, "Expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            pass(testName);
        } else {
            fail(testName, "Expected TRUE condition failed.");
        }
    }

    private static void pass(String testName) {
        passedTests++;
        System.out.println(" [PASS] " + testName);
    }

    private static void fail(String testName, String reason) {
        failedTests++;
        System.err.println(" [FAIL] " + testName + " -> " + reason);
    }

    // 1. DataLoader Tests
    private static void testDataLoader() {
        try {
            List<BusTrip> trips = DataLoader.loadDataset("data/rapid_kl_bus_trips.csv");
            assertTrue(trips != null && trips.size() >= 10, "DataLoader should load at least 10 Rapid KL trips");
            BusTrip trip1 = trips.get(0);
            assertEquals("TRIP-101", trip1.getTripId(), "Trip 1 ID match");
            assertEquals("MYB-7701", trip1.getBusId(), "Trip 1 Bus ID match");
            assertEquals("770", trip1.getRouteId(), "Trip 1 Route ID match");
        } catch (Exception e) {
            fail("testDataLoader", e.getMessage());
        }
    }

    // 2. ML Delay Predictor Tests
    private static void testDelayPredictorML() {
        try {
            DelayPredictorML predictor = new DelayPredictorML();
            BusTrip trip = new BusTrip();
            trip.setTripId("TEST-01");
            trip.setTrafficCongestionLevel("CRITICAL");
            trip.setWeatherCondition("HEAVY_RAIN");
            trip.setPassengerLoad(90);
            trip.setDriverShiftHours(8.0);

            PredictionResult res = predictor.predict(trip);
            assertTrue(res.getPredictedDelayMinutes() > 20.0, "Critical traffic & heavy rain should predict high delay (>20 min)");
            assertEquals(RiskLevel.CRITICAL, res.getRiskLevel(), "Predicted delay should be CRITICAL risk level");
            assertTrue(res.getFeatureContributions().containsKey("Traffic Congestion (CRITICAL)"), "Feature contribution should include Traffic Congestion");
        } catch (Exception e) {
            fail("testDelayPredictorML", e.getMessage());
        }
    }

    // 3. Model Evaluator Tests
    private static void testModelEvaluator() {
        try {
            List<BusTrip> trips = DataLoader.loadDataset("data/rapid_kl_bus_trips.csv");
            DelayPredictorML predictor = new DelayPredictorML();
            ModelEvaluator.EvaluationReport report = ModelEvaluator.evaluateModel(predictor, trips);

            assertTrue(report.totalEvaluatedTrips > 0, "ModelEvaluator should evaluate all loaded trips");
            assertTrue(report.mae >= 0.0, "MAE must be non-negative");
            assertTrue(report.precision >= 0.0 && report.precision <= 1.0, "Precision must be between 0 and 1");
        } catch (Exception e) {
            fail("testModelEvaluator", e.getMessage());
        }
    }

    // 4. Risk Engine & Priority Score Tests
    private static void testRiskEngineAndPriority() {
        try {
            double score = PriorityScore.calculatePriority(25.0, 0.85, 80, RiskLevel.HIGH);
            assertTrue(score > 40.0, "High delay & passenger load should produce high priority score");

            DelayPredictorML predictor = new DelayPredictorML();
            RiskEngine riskEngine = new RiskEngine(predictor);
            List<BusTrip> trips = DataLoader.loadDataset("data/rapid_kl_bus_trips.csv");
            List<PredictionResult> prioritized = riskEngine.evaluateAndPrioritizeTrips(trips);

            assertTrue(prioritized.get(0).getPriorityScore() >= prioritized.get(prioritized.size() - 1).getPriorityScore(),
                    "RiskEngine should rank trips in descending order of priority score");
        } catch (Exception e) {
            fail("testRiskEngineAndPriority", e.getMessage());
        }
    }

    // 5. Recovery Optimizer & Operational Constraints Tests
    private static void testRecoveryOptimizerAndConstraints() {
        try {
            BusTrip trip = new BusTrip();
            trip.setTripId("TEST-02");
            trip.setBusId("MYB-99");
            trip.setStandbyBusAvailable(false); // No standby bus available

            StringBuilder reason = new StringBuilder();
            boolean standbyFeasible = OperationalConstraints.checkFeasibility(trip, StrategyType.STANDBY_REASSIGNMENT, reason);
            assertTrue(!standbyFeasible, "Standby bus reassignment should be INFEASIBLE when standbyBusAvailable is false");
            assertTrue(reason.toString().contains("REJECTED"), "Constraint reason should contain REJECTED keyword");

            // Check recovery optimizer selection
            DelayPredictorML predictor = new DelayPredictorML();
            PredictionResult pred = predictor.predict(trip);
            RecoveryOptimizer optimizer = new RecoveryOptimizer();
            RecoveryOptimizer.OptimizationResult optResult = optimizer.optimize(trip, pred);

            assertTrue(optResult.getSelectedCandidate() != null, "Optimizer should select a candidate action");
            assertTrue(optResult.getSelectedCandidate().getStrategyType() != StrategyType.STANDBY_REASSIGNMENT, "Optimizer must not select infeasible standby bus strategy");
        } catch (Exception e) {
            fail("testRecoveryOptimizerAndConstraints", e.getMessage());
        }
    }

    // 6. Execution Policy Engine Tests
    private static void testExecutionPolicyEngine() {
        try {
            ExecutionPolicyEngine policy = new ExecutionPolicyEngine();
            PredictionResult highRiskPred = new PredictionResult();
            highRiskPred.setRiskLevel(RiskLevel.CRITICAL);
            highRiskPred.setPredictedDelayMinutes(35.0);
            highRiskPred.setDataReliable(true);

            RecoveryCandidate cand = new RecoveryCandidate();
            cand.setStrategyType(StrategyType.STANDBY_REASSIGNMENT);
            cand.setFeasible(true);
            cand.setConfidenceScore(0.95);
            cand.setEstimatedCostImpact(45.0); // Exceeds auto execute cost limit MYR 30

            ExecutionDecision decision = policy.evaluateDecision(highRiskPred, cand);
            assertEquals(ExecutionDecision.REQUIRES_APPROVAL, decision, "Critical risk or high cost action must require operator approval");

            // Test Auto Execute condition
            PredictionResult lowRiskPred = new PredictionResult();
            lowRiskPred.setRiskLevel(RiskLevel.MEDIUM);
            lowRiskPred.setPredictedDelayMinutes(12.0);
            lowRiskPred.setDataReliable(true);

            RecoveryCandidate lowCostCand = new RecoveryCandidate();
            lowCostCand.setStrategyType(StrategyType.ADAPTIVE_SCHEDULING);
            lowCostCand.setFeasible(true);
            lowCostCand.setConfidenceScore(0.90);
            lowCostCand.setEstimatedCostImpact(0.0);

            ExecutionDecision autoDecision = policy.evaluateDecision(lowRiskPred, lowCostCand);
            assertEquals(ExecutionDecision.AUTO_EXECUTE, autoDecision, "Low risk and zero cost action should auto-execute");
        } catch (Exception e) {
            fail("testExecutionPolicyEngine", e.getMessage());
        }
    }

    // 7. Impact Verifier Tests
    private static void testImpactVerifier() {
        try {
            BusTrip trip = new BusTrip();
            trip.setTripId("TEST-03");

            PredictionResult pred = new PredictionResult();
            pred.setPredictedDelayMinutes(20.0);

            RecoveryCandidate candidate = new RecoveryCandidate();
            candidate.setCandidateId("CAN-01");
            candidate.setStrategyType(StrategyType.ROUTE_OPTIMIZATION);
            candidate.setFeasible(true);
            candidate.setEstimatedCostImpact(15.0);

            BusNetworkSimulator.SimulationOutcome outcome = new BusNetworkSimulator.SimulationOutcome(8.0, "Detour successful");

            ImpactResult impact = ImpactVerifier.verifyImpact(trip, pred, candidate, outcome);
            assertEquals(12.0, impact.getDelayReductionMin(), "20 min baseline - 8 min post delay = 12 min reduction");
            assertEquals(60.0, impact.getPercentageImprovement(), "12 min reduction / 20 min baseline = 60.0% improvement");
            assertEquals("SUCCESSFUL", impact.getOutcomeStatus(), "60% improvement should categorize as SUCCESSFUL");
        } catch (Exception e) {
            fail("testImpactVerifier", e.getMessage());
        }
    }

    // 8. RAG Knowledge Base Tests
    private static void testRAGKnowledgeBase() {
        try {
            com.busguard.rag.RAGRetriever rag = new com.busguard.rag.RAGRetriever();
            java.util.List<com.busguard.rag.KnowledgeDocument> docs = rag.retrieveRelevantDocs("standby bus dispatch cost", 2);
            assertTrue(docs != null && !docs.isEmpty(), "RAG Retriever should return relevant documents for query");
            assertTrue(docs.get(0).getContent().contains("45.00") || docs.get(0).getContent().contains("Standby"), "RAG document content should match query context");
            pass("testRAGKnowledgeBase");
        } catch (Exception e) {
            fail("testRAGKnowledgeBase", e.getMessage());
        }
    }

    // 9. End-to-End Workflow Test
    private static void testEndToEndWorkflow() {
        try {
            List<BusTrip> trips = DataLoader.loadDataset("data/rapid_kl_bus_trips.csv");
            DelayPredictorML predictor = new DelayPredictorML();
            RiskEngine riskEngine = new RiskEngine(predictor);
            RecoveryOptimizer optimizer = new RecoveryOptimizer();
            ExecutionPolicyEngine policyEngine = new ExecutionPolicyEngine();
            SimulationStateManager stateManager = new SimulationStateManager();
            BusNetworkSimulator simulator = new BusNetworkSimulator();

            List<PredictionResult> prioritized = riskEngine.evaluateAndPrioritizeTrips(trips);
            BusTrip topTrip = trips.stream().filter(t -> t.getTripId().equals(prioritized.get(0).getTripId())).findFirst().get();

            PredictionResult pred = predictor.predict(topTrip);
            RecoveryOptimizer.OptimizationResult opt = optimizer.optimize(topTrip, pred);
            ExecutionDecision decision = policyEngine.evaluateDecision(pred, opt.getSelectedCandidate());

            stateManager.recordDecision(topTrip.getTripId(), decision);
            BusNetworkSimulator.SimulationOutcome outcome = simulator.simulateIntervention(topTrip, pred, opt.getSelectedCandidate());
            ImpactResult impact = ImpactVerifier.verifyImpact(topTrip, pred, opt.getSelectedCandidate(), outcome);

            assertTrue(impact != null && impact.getDelayReductionMin() >= 0, "End-to-end impact calculation successful");
            pass("testEndToEndWorkflow");
        } catch (Exception e) {
            fail("testEndToEndWorkflow", e.getMessage());
        }
    }
}
