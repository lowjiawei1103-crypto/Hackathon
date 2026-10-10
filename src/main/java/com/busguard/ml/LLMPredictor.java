package com.busguard.ml;

import com.busguard.model.BusTrip;
import com.busguard.model.PredictionResult;
import com.busguard.rag.RAGRetriever;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LLMPredictor {

    private static final String DEFAULT_GROQ_KEY = "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";
    private String apiKey;
    private final RAGRetriever ragRetriever = new RAGRetriever();

    public LLMPredictor() {
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey == null || envKey.trim().isEmpty()) {
            envKey = System.getenv("GEMINI_API_KEY");
        }
        if (envKey != null && !envKey.trim().isEmpty()) {
            this.apiKey = envKey.trim();
        } else {
            this.apiKey = DEFAULT_GROQ_KEY;
        }
    }

    public LLMPredictor(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : DEFAULT_GROQ_KEY;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String chatWithAssistant(String userMessage, String systemContext) {
        // Retrieve relevant RAG Knowledge Base documents for the user message
        String ragContext = ragRetriever.buildRAGContextString(userMessage, 3);
        String combinedSystemContext = systemContext + "\n\n" + ragContext;

        if (!hasApiKey()) {
            return "AI Assistant (RAG Offline Mode):\n" + ragContext + "\nAnswer: Please configure your Groq or Gemini API key.";
        }

        String[] models = {"llama-3.3-70b-versatile", "llama3-8b-8192", "mixtral-8x7b-32768", "gemma2-9b-it"};
        for (String model : models) {
            String res = tryGroqChat(userMessage, combinedSystemContext, model);
            if (res != null && !res.isEmpty()) {
                return res;
            }
        }

        return "AI Assistant (Local RAG Engine):\n" + ragContext;
    }

    private String tryGroqChat(String userMessage, String systemContext, String modelName) {
        try {
            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String escapedSystem = systemContext.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
            String escapedUser = userMessage.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");

            String jsonPayload = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.3,\"max_tokens\":300}",
                modelName, escapedSystem, escapedUser
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream is = conn.getInputStream()) {
                    String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    int contentIdx = response.indexOf("\"content\": \"");
                    if (contentIdx != -1) {
                        int start = contentIdx + 12;
                        int end = response.indexOf("\"", start);
                        if (end != -1) {
                            return response.substring(start, end)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"");
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public String generateLLMExplanation(BusTrip trip, PredictionResult basePrediction) {
        String sys = "You are an expert AI bus dispatch assistant for Rapid KL in Kuala Lumpur.";
        String user = String.format("Analyze bus %s on route %s (%s). Predicted delay: %.1f min due to %s (%s weather). Give a 2-sentence operational recommendation.",
                trip.getBusId(), trip.getRouteId(), trip.getRouteName(), basePrediction.getPredictedDelayMinutes(), basePrediction.getPrimaryFactor(), trip.getWeatherCondition());
        
        String reply = chatWithAssistant(user, sys);
        if (reply != null && !reply.startsWith("AI Assistant")) {
            return "[Groq AI + RAG Knowledge Base] " + reply;
        }
        return "[ML Engine] " + basePrediction.getExplanationText();
    }
}
