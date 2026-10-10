package com.busguard.model;

public enum StrategyType {
    ROUTE_OPTIMIZATION("Route / Departure-Time Optimization", "Reroute bus via express corridor or bypass known bottlenecks."),
    STANDBY_REASSIGNMENT("Standby-Bus Reassignment", "Deploy available depot standby bus to inject directly into downstream schedule."),
    ADAPTIVE_SCHEDULING("Adaptive Departure & Headway Regulation", "Regulate departure interval (headway) to prevent bus bunching and delay cascading."),
    NO_ACTION("No-Action Baseline", "Retain current operational plan when alternatives are infeasible or offer negligible gain.");

    private final String title;
    private final String description;

    StrategyType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
