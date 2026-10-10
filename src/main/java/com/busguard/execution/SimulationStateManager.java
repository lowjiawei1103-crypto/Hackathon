package com.busguard.execution;

import com.busguard.model.ExecutionDecision;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationStateManager {

    public enum ActionState {
        PENDING_APPROVAL,
        AUTO_EXECUTED,
        APPROVED,
        REJECTED,
        BLOCKED
    }

    private final Map<String, ActionState> tripActionStates = new ConcurrentHashMap<>();

    public boolean canExecuteTripAction(String tripId) {
        ActionState state = tripActionStates.get(tripId);
        return state == null || state == ActionState.PENDING_APPROVAL;
    }

    public void recordDecision(String tripId, ExecutionDecision decision) {
        switch (decision) {
            case AUTO_EXECUTE:
                tripActionStates.put(tripId, ActionState.AUTO_EXECUTED);
                break;
            case REQUIRES_APPROVAL:
                tripActionStates.put(tripId, ActionState.PENDING_APPROVAL);
                break;
            case BLOCKED:
            default:
                tripActionStates.put(tripId, ActionState.BLOCKED);
                break;
        }
    }

    public boolean approveAction(String tripId) {
        if (tripActionStates.get(tripId) == ActionState.PENDING_APPROVAL) {
            tripActionStates.put(tripId, ActionState.APPROVED);
            return true;
        }
        return false;
    }

    public boolean rejectAction(String tripId) {
        if (tripActionStates.get(tripId) == ActionState.PENDING_APPROVAL) {
            tripActionStates.put(tripId, ActionState.REJECTED);
            return true;
        }
        return false;
    }

    public ActionState getTripState(String tripId) {
        return tripActionStates.getOrDefault(tripId, ActionState.PENDING_APPROVAL);
    }

    public Map<String, ActionState> getAllStates() {
        return tripActionStates;
    }

    public void reset() {
        tripActionStates.clear();
    }
}
