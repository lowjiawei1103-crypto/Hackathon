package com.busguard.model;

import java.time.LocalDateTime;

public class BusTrip {
    private String tripId;
    private String busId;
    private String routeId;
    private String routeName;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime observedDeparture;
    private int scheduledTravelMin;
    private int observedTravelMin;
    private LocalDateTime scheduledArrival;
    private LocalDateTime observedArrival;
    private int passengerLoad;
    private String driverId;
    private double driverShiftHours;
    private boolean standbyBusAvailable;
    private String weatherCondition;
    private String trafficCongestionLevel;
    private double latitude;
    private double longitude;
    private String delayCause;

    public BusTrip() {}

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(LocalDateTime scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }

    public LocalDateTime getObservedDeparture() { return observedDeparture; }
    public void setObservedDeparture(LocalDateTime observedDeparture) { this.observedDeparture = observedDeparture; }

    public int getScheduledTravelMin() { return scheduledTravelMin; }
    public void setScheduledTravelMin(int scheduledTravelMin) { this.scheduledTravelMin = scheduledTravelMin; }

    public int getObservedTravelMin() { return observedTravelMin; }
    public void setObservedTravelMin(int observedTravelMin) { this.observedTravelMin = observedTravelMin; }

    public LocalDateTime getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(LocalDateTime scheduledArrival) { this.scheduledArrival = scheduledArrival; }

    public LocalDateTime getObservedArrival() { return observedArrival; }
    public void setObservedArrival(LocalDateTime observedArrival) { this.observedArrival = observedArrival; }

    public int getPassengerLoad() { return passengerLoad; }
    public void setPassengerLoad(int passengerLoad) { this.passengerLoad = passengerLoad; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public double getDriverShiftHours() { return driverShiftHours; }
    public void setDriverShiftHours(double driverShiftHours) { this.driverShiftHours = driverShiftHours; }

    public boolean isStandbyBusAvailable() { return standbyBusAvailable; }
    public void setStandbyBusAvailable(boolean standbyBusAvailable) { this.standbyBusAvailable = standbyBusAvailable; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public String getTrafficCongestionLevel() { return trafficCongestionLevel; }
    public void setTrafficCongestionLevel(String trafficCongestionLevel) { this.trafficCongestionLevel = trafficCongestionLevel; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDelayCause() { return delayCause; }
    public void setDelayCause(String delayCause) { this.delayCause = delayCause; }

    public double calculateActualDelayMinutes() {
        if (observedArrival != null && scheduledArrival != null) {
            long diffSeconds = java.time.Duration.between(scheduledArrival, observedArrival).getSeconds();
            return Math.max(0, diffSeconds / 60.0);
        }
        int departureDelay = 0;
        if (observedDeparture != null && scheduledDeparture != null) {
            departureDelay = (int) Math.max(0, java.time.Duration.between(scheduledDeparture, observedDeparture).toMinutes());
        }
        int travelExtra = Math.max(0, observedTravelMin - scheduledTravelMin);
        return departureDelay + travelExtra;
    }
}
