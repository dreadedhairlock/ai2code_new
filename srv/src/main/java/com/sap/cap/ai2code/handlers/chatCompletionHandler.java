package com.sap.cap.ai2code.handlers;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotMessages;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@ServiceName("MainService")
public class chatCompletionHandler implements EventHandler {

    private final String apiKey = "AIzaSyDyE_D4ej7SljvLAV5vWMmkQxg5OjGv5r4";
    private final String model = "gemini-2.0-flash";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @On(event = BotInstancesChatCompletionContext.CDS_NAME)
    public void handleChatCompletion(BotInstancesChatCompletionContext context) {
        String content = context.getContent();

        String aiResponse;
        try {
            // Call Gemini AI API
            aiResponse = callGeminiAPI(content);
        } catch (Exception e) {
            System.err.println("Error calling AI API: " + e.getMessage());
            aiResponse = "Error: " + e.getMessage();
        }

        // Create BotMessages response
        BotMessages response = BotMessages.create();
        response.setRole("assistant");
        response.setMessage(aiResponse);
        response.setRagData("AI response from Gemini API");

        System.out.println("gemini");
        System.out.println(aiResponse);

        context.setResult(response);
    }

    private String callGeminiAPI(String userMessage) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
        );

        // Create request body
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }]
            }
            """, userMessage.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API call failed: " + response.statusCode() + " - " + response.body());
        }

        // Parse response
        JsonNode jsonResponse = objectMapper.readTree(response.body());
        JsonNode candidates = jsonResponse.get("candidates");

        if (candidates != null && candidates.size() > 0) {
            JsonNode content = candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
            }
        }

        return "No response from AI";
    }
}
