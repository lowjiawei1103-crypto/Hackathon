package com.busguard.recovery;

import com.busguard.model.BusTrip;
import com.busguard.model.StrategyType;

public class OperationalConstraints {

    public static final double MAX_DRIVER_SHIFT_HOURS = 9.0;
    public static final double MIN_DELAY_REDUCTION_THRESHOLD = 3.0; // Minutes required to justify action

    public static boolean checkFeasibility(BusTrip trip, StrategyType strategy, StringBuilder reasonOut) {
        if (trip == null) {
            reasonOut.append("Trip data unavailable.");
            return false;
        }

        switch (strategy) {
            case ROUTE_OPTIMIZATION:
                // Check if alternative bypass corridors exist
                if (trip.getTrafficCongestionLevel() != null && trip.getTrafficCongestionLevel().equalsIgnoreCase("CRITICAL") && trip.getPassengerLoad() > 95) {
                    reasonOut.append("High passenger load & severe corridor blockages limits alternative detour routes.");
                    return false;
                }
                reasonOut.append("Alternative expressway bypass available and feasible.");
                return true;

            case STANDBY_REASSIGNMENT:
                // Hard Constraint: Standby bus must be available in depot
                if (!trip.isStandbyBusAvailable()) {
                    reasonOut.append("REJECTED: No standby bus available in depot for assignment.");
                    return false;
                }
                // Hard Constraint: Driver shift hours limit
                if (trip.getDriverShiftHours() >= MAX_DRIVER_SHIFT_HOURS) {
                    reasonOut.append("REJECTED: Replacement driver exceeds maximum allowable shift duration (" + trip.getDriverShiftHours() + "h >= " + MAX_DRIVER_SHIFT_HOURS + "h).");
                    return false;
                }
                reasonOut.append("Standby bus MYB-DEPOT-99 available; driver shift within limits.");
                return true;

            case ADAPTIVE_SCHEDULING:
                reasonOut.append("Feasible headway adjustment of +4 mins available to stagger departure.");
                return true;

            case NO_ACTION:
                reasonOut.append("Baseline default strategy always feasible.");
                return true;

            default:
                reasonOut.append("Unknown strategy type.");
                return false;
        }
    }
}
