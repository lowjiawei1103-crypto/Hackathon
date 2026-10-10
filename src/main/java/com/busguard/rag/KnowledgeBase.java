package com.busguard.rag;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeBase {

    private final List<KnowledgeDocument> documents = new ArrayList<>();

    public KnowledgeBase() {
        loadRapidKlDomainKnowledge();
    }

    private void loadRapidKlDomainKnowledge() {
        // SOP 1: Standby Bus Dispatch Rules
        documents.add(new KnowledgeDocument(
            "KB-SOP-001", "Dispatch SOP", "Rapid KL Standby Bus Dispatch Policy",
            "Standby buses stationed at KL Sentral, Bandar Tasik Selatan, and Subang Depot can be dispatched to inject directly into downstream trips when delay exceeds 20 minutes. " +
            "Cost per standby dispatch is MYR 45.00. Dispatch requires standby bus availability = true and driver shift hours < 9.0h."
        ));

        // SOP 2: Driver Shift Limit Constraints
        documents.add(new KnowledgeDocument(
            "KB-SOP-002", "Driver Safety", "Rapid KL Driver Fatigue & Shift Limitation Rule",
            "Under SPAD/APAD safety regulations, Rapid KL bus drivers must not operate vehicles beyond 9.0 continuous shift hours. " +
            "If a driver reaches 7.5 hours, fatigue risk indicators trigger (+3.2 min delay penalty per extra hour). " +
            "Any recovery action assigning a driver with >= 9.0h shift hours is hard-blocked as infeasible."
        ));

        // SOP 3: Federal Highway Corridor Rerouting
        documents.add(new KnowledgeDocument(
            "KB-SOP-003", "Corridor Profile", "Federal Highway (Route 770 & 772) Traffic Bottlenecks",
            "Federal Highway between Mid Valley and Subang Jaya experiences severe morning bottleneck congestion (07:30 - 09:30). " +
            "When traffic level is CRITICAL, buses should be rerouted via NPE Expressway or AKLEH bypass. " +
            "Expressway rerouting incurs fuel cost of MYR 15.00 but saves an estimated 55% of predicted delay (up to 18 mins)."
        ));

        // SOP 4: Heavy Rain & Flash Flood Playbook
        documents.add(new KnowledgeDocument(
            "KB-SOP-004", "Weather Playbook", "Monsoon Flash Flood & Rain Mitigation Protocol",
            "Heavy rain (>= 15mm/h) in Klang Valley slows average bus travel speeds by 35% and increases stop dwell times. " +
            "Under flood alerts at Ampang Corridor or LDP Interchange, buses must switch to elevated bypass routes. " +
            "Headway regulation should be widened by +4 to +6 minutes to prevent bus bunching."
        ));

        // SOP 5: Adaptive Headway & Bus Bunching
        documents.add(new KnowledgeDocument(
            "KB-SOP-005", "Timetable Regulation", "Bus Bunching Prevention & Headway Adjustment",
            "Bus bunching occurs when a delayed lead bus accumulates heavy passenger queues while the trailing bus runs empty behind it. " +
            "Adaptive departure scheduling shifts departure interval by +5 mins for downstream trips, restoring regular bus spacing at zero financial cost."
        ));

        // SOP 6: Operating Cost & Financial Transparency
        documents.add(new KnowledgeDocument(
            "KB-SOP-006", "Financial Rules", "Intervention Cost Impact & Financial Calculation Rules",
            "Operating cost impacts must be explicitly labelled as simulated estimates. " +
            "Route optimization detour cost: MYR 15.00 (fuel & toll). Standby bus dispatch cost: MYR 45.00 (driver wage & depot mileage). " +
            "Adaptive headway scheduling cost: MYR 0.00."
        ));

        // SOP 7: LDP Interchange & Highway Breakdown Playbook
        documents.add(new KnowledgeDocument(
            "KB-SOP-007", "Incident Protocol", "Damansara-Puchong Expressway (LDP Route 600) Vehicle Breakdown Protocol",
            "Route 600 (Hab Puchong Utama to Pasar Seni) is prone to vehicle breakdown bottlenecks near Kelana Jaya LRT. " +
            "When breakdown is detected, dispatch a replacement bus from Kelana Jaya Depot immediately to bypass the obstruction."
        ));

        // SOP 8: Execution Policy Thresholds
        documents.add(new KnowledgeDocument(
            "KB-SOP-008", "Execution Safety", "Confidence-Based Automatic Execution Policy",
            "Interventions auto-execute when confidence score >= 82%, delay < 30 min, and cost < MYR 30.00. " +
            "Interventions require manual human operator approval if risk is CRITICAL, cost > MYR 30.00, or driver shift > 7.5h."
        ));
    }

    public List<KnowledgeDocument> getDocuments() {
        return documents;
    }
}
