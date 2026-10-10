package com.busguard.model;

public enum ExecutionDecision {
    AUTO_EXECUTE("Auto Executed", "#10B981", "High confidence & safe limits satisfied. Action applied automatically in simulation."),
    REQUIRES_APPROVAL("Requires Approval", "#F59E0B", "Action carries operational impact or uncertainties. Waiting for operator authorization."),
    BLOCKED("Blocked", "#EF4444", "Infeasible strategy or hard operational constraint violation. Baseline retained.");

    private final String label;
    private final String color;
    private final String description;

    ExecutionDecision(String label, String color, String description) {
        this.label = label;
        this.color = color;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
}
