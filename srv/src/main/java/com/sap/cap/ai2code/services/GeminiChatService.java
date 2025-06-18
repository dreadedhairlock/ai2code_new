package com.sap.cap.ai2code.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-pro}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Fungsi utama untuk chat completion
     */
    public String chatCompletion(String content) throws IOException, InterruptedException {

        try {
            // Buat request body - revisi untuk menghapus SYSTEM role
            Map<String, Object> requestBody = new HashMap<>();

            // Gunakan struktur yang lebih sederhana untuk Gemini
            Map<String, Object> contentObj = new HashMap<>();

            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", content);
            parts.add(part);

            contentObj.put("parts", parts);
            // Untuk Gemini 1.5 dan 2.0, role USER adalah default jika tidak disebutkan
            // jadi kita bisa menghilangkan field "role"

            requestBody.put("contents", List.of(contentObj));
            requestBody.put("generationConfig", createGenerationConfig());

            // Convert to JSON
            String requestJson = objectMapper.writeValueAsString(requestBody);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(
                            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                            modelName, apiKey)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // Send request

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            int statusCode = response.statusCode();

            if (statusCode != 200) {
                return "Error from Gemini API: HTTP " + statusCode + " - " + response.body();
            }

            // Parse response
            String responseBody = response.body();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            // Extract text dari response
            if (responseJson.has("candidates") &&
                    responseJson.get("candidates").size() > 0 &&
                    responseJson.get("candidates").get(0).has("content")) {

                String result = responseJson.get("candidates")
                        .get(0)
                        .get("content")
                        .get("parts")
                        .get(0)
                        .get("text")
                        .asText();
                return result;
            }

            // Return error message if no valid response
            return "No response from AI. Error: " + responseJson.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Buat konfigurasi generasi untuk API Gemini
     */
    private Map<String, Object> createGenerationConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("temperature", 0.7);
        config.put("maxOutputTokens", 2048);
        config.put("topP", 0.95);
        config.put("topK", 40);
        return config;
    }
}