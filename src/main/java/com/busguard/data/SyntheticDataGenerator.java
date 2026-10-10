package com.busguard.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SyntheticDataGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void generate10kDataset(String outputPath) throws Exception {
        File file = new File(outputPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        String[] routes = {
            "770|Mid Valley to Subang Jaya",
            "B115|KL Sentral to Complex Keramat",
            "750|Central Market to Shah Alam Sek 14",
            "300|Pandan Indah to Lebuh Ampang",
            "780|Pasar Seni to Kota Damansara",
            "600|Hab Puchong Utama to Pasar Seni",
            "250|LRT Wangsa Maju to Terminal Lebuh Ampang",
            "450|Hentian Kajang to Hub Lebuh Pudu",
            "100|Hub Lebuh Pudu to Kuala Selangor",
            "590|Hab Bandar Sungai Long to Hub Lebuh Pudu",
            "T801|LRT Sri Rampai to Section 10 Wangsa Maju",
            "772|Subang Suria to Pasar Seni",
            "640|Hub Pasar Seni to Taman Sri Sentosa",
            "802|LRT Kelana Jaya to Kota Damansara"
        };

        String[] weatherConditions = {"CLEAR", "LIGHT_RAIN", "MODERATE_RAIN", "HEAVY_RAIN", "STORM"};
        String[] trafficLevels = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
        String[] delayCauses = {
            "Federal Highway Congestion & Bottleneck",
            "Jalan Tun Sambanthan Bus Lane Obstruction",
            "Traffic Signal Synchronization Failure",
            "Ampang Corridor Construction Work",
            "Normal Operating Conditions",
            "LDP Interchange Vehicle Breakdown",
            "Jalan Pahang Bus Stop Crowding",
            "Cheras Highway Heavy Peak Traffic",
            "On Time Schedule",
            "Sungai Besi Toll Queue Backlog",
            "Subang Ramp Heavy Rain Waterlogging",
            "Klang Bus Hub Passenger Surge"
        };

        Random rand = new Random(42); // Fixed seed for reproducibility

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("trip_id,bus_id,route_id,route_name,scheduled_departure,observed_departure,scheduled_travel_min,observed_travel_min,scheduled_arrival,observed_arrival,passenger_load,driver_id,driver_shift_hours,standby_bus_available,weather_condition,traffic_congestion_level,latitude,longitude,delay_cause\n");

            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 10, 6, 0, 0);

            for (int i = 1; i <= 10000; i++) {
                String tripId = String.format("TRIP-%05d", i);
                String busId = String.format("MYB-%04d", rand.nextInt(9000) + 1000);
                String driverId = String.format("DRV-%03d", rand.nextInt(900) + 100);

                String routeInfo = routes[rand.nextInt(routes.length)];
                String[] routeParts = routeInfo.split("\\|");
                String routeId = routeParts[0];
                String routeName = routeParts[1];

                LocalDateTime schedDep = baseTime.plusMinutes(rand.nextInt(720)); // Spanning 12 hours
                
                String traffic = trafficLevels[rand.nextInt(trafficLevels.length)];
                String weather = weatherConditions[rand.nextInt(weatherConditions.length)];
                
                // Realistic departure delay calculation
                int depDelayMin = 0;
                if (traffic.equals("CRITICAL")) depDelayMin = rand.nextInt(25) + 5;
                else if (traffic.equals("HIGH")) depDelayMin = rand.nextInt(15);
                else depDelayMin = rand.nextInt(5);

                LocalDateTime obsDep = schedDep.plusMinutes(depDelayMin);

                int schedTravelMin = rand.nextInt(40) + 20; // 20-60 mins
                int travelExtra = 0;

                if (traffic.equals("CRITICAL")) travelExtra += rand.nextInt(35) + 15;
                else if (traffic.equals("HIGH")) travelExtra += rand.nextInt(20) + 5;
                else if (traffic.equals("MEDIUM")) travelExtra += rand.nextInt(10);

                if (weather.contains("HEAVY") || weather.contains("STORM")) travelExtra += rand.nextInt(15) + 5;

                int obsTravelMin = schedTravelMin + travelExtra;

                LocalDateTime schedArr = schedDep.plusMinutes(schedTravelMin);
                LocalDateTime obsArr = obsDep.plusMinutes(obsTravelMin);

                int load = rand.nextInt(85) + 15; // 15% - 100%
                double shiftHours = Math.round((rand.nextDouble() * 8.0 + 1.0) * 10.0) / 10.0;
                boolean standby = rand.nextDouble() > 0.35; // 65% chance available

                // KL coordinates center: 3.1390, 101.6869
                double lat = 3.0000 + (rand.nextDouble() * 0.25);
                double lng = 101.5500 + (rand.nextDouble() * 0.30);

                String cause = delayCauses[rand.nextInt(delayCauses.length)];
                if (travelExtra + depDelayMin < 5) cause = "On Time Schedule";

                String line = String.format(
                    "%s,%s,%s,\"%s\",%s,%s,%d,%d,%s,%s,%d,%s,%.1f,%b,%s,%s,%.4f,%.4f,\"%s\"\n",
                    tripId, busId, routeId, routeName,
                    schedDep.format(FORMATTER), obsDep.format(FORMATTER),
                    schedTravelMin, obsTravelMin,
                    schedArr.format(FORMATTER), obsArr.format(FORMATTER),
                    load, driverId, shiftHours, standby,
                    weather, traffic, lat, lng, cause
                );
                writer.write(line);
            }
        }
        System.out.println("Successfully generated 10,000 synthetic Rapid KL records at: " + outputPath);
    }

    public static void main(String[] args) {
        try {
            generate10kDataset("data/rapid_kl_10k_synthetic.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
