package com.busguard;

import com.busguard.data.DataLoader;
import com.busguard.ml.DelayPredictorML;
import com.busguard.ml.ModelEvaluator;
import com.busguard.model.BusTrip;
import com.busguard.web.ApiController;
import com.busguard.web.WebServer;

import java.util.List;

public class BusGuardApp {

    public static void main(String[] args) {
        System.out.println("Starting BusGuard AI — Predictive Bus Delay & Automated Recovery System...");

        String csvPath = "data/rapid_kl_bus_trips.csv";
        if (args.length > 0) {
            csvPath = args[0];
        }

        try {
            // 1. Data Loading & Validation
            List<BusTrip> trips = DataLoader.loadDataset(csvPath);
            System.out.println("Loaded and validated " + trips.size() + " bus trips from dataset.");

            // 2. Machine Learning Model Evaluation
            DelayPredictorML predictor = new DelayPredictorML();
            ModelEvaluator.EvaluationReport report = ModelEvaluator.evaluateModel(predictor, trips);
            System.out.println("\n--- Model Validation Report ---");
            System.out.println(report.toString());

            // 3. Start Web Server
            ApiController controller = new ApiController(csvPath);
            int port = 8080;
            WebServer server = new WebServer(port, controller);
            server.start();

        } catch (Exception e) {
            System.err.println("BusGuard Startup Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
