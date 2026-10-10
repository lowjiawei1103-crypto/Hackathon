package com.busguard.model;

public enum RiskLevel {
    LOW("Low", "#10B981", 1),
    MEDIUM("Medium", "#F59E0B", 2),
    HIGH("High", "#EF4444", 3),
    CRITICAL("Critical", "#8B5CF6", 4);

    private final String label;
    private final String color;
    private final int severityRank;

    RiskLevel(String label, String color, int severityRank) {
        this.label = label;
        this.color = color;
        this.severityRank = severityRank;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
    public int getSeverityRank() { return severityRank; }

    public static RiskLevel fromDelayMinutes(double delayMinutes) {
        if (delayMinutes >= 35) return CRITICAL;
        if (delayMinutes >= 20) return HIGH;
        if (delayMinutes >= 8) return MEDIUM;
        return LOW;
    }
}
