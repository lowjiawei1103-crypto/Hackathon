package com.busguard.data;

import com.busguard.model.BusTrip;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static List<BusTrip> loadDataset(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new DataValidationException("Dataset file not found at path: " + filePath);
        }

        List<BusTrip> trips = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new DataValidationException("Dataset CSV is empty or missing headers.");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(",", -1);
                if (tokens.length < 18) {
                    System.err.println("Warning: Line " + lineNumber + " has insufficient tokens. Skipping.");
                    continue;
                }

                BusTrip trip = new BusTrip();
                trip.setTripId(tokens[0].trim());
                trip.setBusId(tokens[1].trim());
                trip.setRouteId(tokens[2].trim());
                trip.setRouteName(tokens[3].trim());
                
                try {
                    if (!tokens[4].trim().isEmpty()) trip.setScheduledDeparture(LocalDateTime.parse(tokens[4].trim(), FORMATTER));
                    if (!tokens[5].trim().isEmpty()) trip.setObservedDeparture(LocalDateTime.parse(tokens[5].trim(), FORMATTER));
                    
                    trip.setScheduledTravelMin(parseInteger(tokens[6], 30));
                    trip.setObservedTravelMin(parseInteger(tokens[7], 30));

                    if (!tokens[8].trim().isEmpty()) trip.setScheduledArrival(LocalDateTime.parse(tokens[8].trim(), FORMATTER));
                    if (!tokens[9].trim().isEmpty()) trip.setObservedArrival(LocalDateTime.parse(tokens[9].trim(), FORMATTER));

                    trip.setPassengerLoad(parseInteger(tokens[10], 50));
                    trip.setDriverId(tokens[11].trim());
                    trip.setDriverShiftHours(parseDouble(tokens[12], 4.0));
                    trip.setStandbyBusAvailable(Boolean.parseBoolean(tokens[13].trim()));
                    trip.setWeatherCondition(tokens[14].trim());
                    trip.setTrafficCongestionLevel(tokens[15].trim());
                    trip.setLatitude(parseDouble(tokens[16], 3.1390));
                    trip.setLongitude(parseDouble(tokens[17], 101.6869));
                    
                    if (tokens.length > 18) {
                        trip.setDelayCause(tokens[18].trim());
                    } else {
                        trip.setDelayCause("Unspecified Traffic Bottleneck");
                    }

                    trips.add(trip);
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        if (trips.isEmpty()) {
            throw new DataValidationException("No valid bus trip records were parsed from dataset.");
        }

        return trips;
    }

    private static int parseInteger(String val, int defaultVal) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static double parseDouble(String val, double defaultVal) {
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
