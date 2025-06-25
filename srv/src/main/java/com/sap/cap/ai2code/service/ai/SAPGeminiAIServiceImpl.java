package com.sap.cap.ai2code.service.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.ai2code.model.ai.AIModel;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class SAPGeminiAIServiceImpl implements AIService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private static final Dotenv dotenv = Dotenv.load();

    private final String model = dotenv.get("GEMINI_MODEL");
    private final String apiKey = dotenv.get("GEMINI_API_KEY");

    public SAPGeminiAIServiceImpl() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model) {
        try {
            // Build conversation context
            List<Map<String, String>> conversationContext = buildConversationContext(messages, prompts, content);

            // Call Gemini API
            return callGeminiAPIWithContext(conversationContext);

        } catch (Exception e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    @Override
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model, ExecutorService executor, StreamingCompletedProcessor streamingCompletionProcessor) {

        SseEmitter emitter = new SseEmitter(30000L);

        new Thread(() -> {
            StringBuilder completeResponse = new StringBuilder();

            try {
                String endpoint = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?key=%s",
                        this.model, apiKey);

                // Build conversation context and create proper request body
                List<Map<String, String>> conversationContext = buildConversationContext(messages, prompts, content);
                String requestBody = buildRequestBody(conversationContext, true);

                System.out.println("Streaming endpoint: " + endpoint);
                System.out.println("Request body: " + requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.contains("\"text\":")) {

                        // Extract just the text content for cleaner streaming
                        String textContent = extractTextFromLine(line);
                        if (textContent != null && !textContent.isEmpty()) {
                            completeResponse.append(textContent);

                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(textContent));

                            System.out.println("Sent chunk: " + textContent);
                        } else {
                            // Fallback: send the raw line if text extraction fails
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(line));
                        }
                    }
                }

                // Process complete response if processor provided
                if (streamingCompletionProcessor != null && completeResponse.length() > 0) {
                    streamingCompletionProcessor.process(completeResponse.toString());
                }

                System.out.println("Streaming completed successfully. Total response: " + completeResponse.toString());
                emitter.complete();

            } catch (Exception e) {
                System.err.println("Streaming failed - Error: " + e.getMessage());
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * Extract text content from a line containing "text": This is a simple
     * extraction method similar to your working controller
     */
    private String extractTextFromLine(String line) {
        try {
            // Try to parse as JSON and extract text
            JsonNode jsonNode = objectMapper.readTree(line);
            JsonNode candidates = jsonNode.get("candidates");

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
        } catch (Exception e) {
            // If JSON parsing fails, return null to use fallback
            System.err.println("Text extraction failed, using fallback: " + e.getMessage());
        }
        return null;
    }

    /**
     * Enhanced streaming chunk parser with better error handling
     */
    private String parseStreamingChunk(JsonNode jsonNode) {
        try {
            // Check if this is an error response first
            if (jsonNode.has("error")) {
                JsonNode error = jsonNode.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : "Unknown error";
                System.err.println("API Error: " + errorMessage);
                return null;
            }

            // Parse normal response
            JsonNode candidates = jsonNode.get("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);

                // Check for finish reason
                if (candidate.has("finishReason")) {
                    String finishReason = candidate.get("finishReason").asText();
                    System.out.println("Stream finished with reason: " + finishReason);
                    return null; // Don't send finish reason as content
                }

                JsonNode content = candidate.get("content");
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
        } catch (Exception e) {
            System.err.println("Failed to parse streaming chunk: " + e.getMessage());
        }
        return null;
    }

    /**
     * Builds conversation context from messages, prompts, and current content
     */
    private List<Map<String, String>> buildConversationContext(List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content) {
        List<Map<String, String>> context = new ArrayList<>();

        // Add system prompts first
        if (prompts != null) {
            for (PromptTexts prompt : prompts) {
                if (prompt.getContent() != null && !prompt.getContent().trim().isEmpty()) {
                    Map<String, String> systemMessage = new HashMap<>();
                    systemMessage.put("role", "system");
                    systemMessage.put("content", prompt.getContent());
                    context.add(systemMessage);
                }
            }
        }

        // Add historical messages
        if (messages != null) {
            for (BotMessages message : messages) {
                if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                    Map<String, String> historyMessage = new HashMap<>();
                    historyMessage.put("role", message.getRole());
                    historyMessage.put("content", message.getMessage());
                    context.add(historyMessage);
                }
            }
        }

        // Add current user message
        if (content != null && !content.trim().isEmpty()) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", content);
            context.add(userMessage);
        }

        return context;
    }

    /**
     * Calls Gemini API with conversation context
     */
    private String callGeminiAPIWithContext(List<Map<String, String>> conversationContext) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey);

        // Build contents array from conversation context
        StringBuilder contentsBuilder = new StringBuilder();
        contentsBuilder.append("\"contents\": [");

        for (int i = 0; i < conversationContext.size(); i++) {
            Map<String, String> message = conversationContext.get(i);
            String role = message.get("role");
            String content = message.get("content").replace("\"", "\\\"").replace("\n", "\\n");

            // Gemini uses "user" and "model" roles, map "assistant" and "system" to "model"
            String geminiRole = "user".equals(role) ? "user" : "model";

            contentsBuilder.append("{")
                    .append("\"role\": \"").append(geminiRole).append("\",")
                    .append("\"parts\": [{\"text\": \"").append(content).append("\"}]")
                    .append("}");

            if (i < conversationContext.size() - 1) {
                contentsBuilder.append(",");
            }
        }
        contentsBuilder.append("]");

        String requestBody = "{" + contentsBuilder.toString() + "}";

        // DEBUG
        System.out.println("AI Request Body: " + requestBody);

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

        return parseGeminiResponse(response.body());
    }

    /**
     * Parses Gemini API response
     */
    private String parseGeminiResponse(String responseBody) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
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

    /**
     * Build request body for Gemini API
     */
    private String buildRequestBody(List<Map<String, String>> conversationContext, boolean isStreaming) {
        StringBuilder contentsBuilder = new StringBuilder();
        contentsBuilder.append("\"contents\": [");

        for (int i = 0; i < conversationContext.size(); i++) {
            Map<String, String> message = conversationContext.get(i);
            String role = message.get("role");
            String content = message.get("content").replace("\"", "\\\"").replace("\n", "\\n");

            // Gemini uses "user" and "model" roles, map "assistant" and "system" to "model"
            String geminiRole = "user".equals(role) ? "user" : "model";

            contentsBuilder.append("{")
                    .append("\"role\": \"").append(geminiRole).append("\",")
                    .append("\"parts\": [{\"text\": \"").append(content).append("\"}]")
                    .append("}");

            if (i < conversationContext.size() - 1) {
                contentsBuilder.append(",");
            }
        }
        contentsBuilder.append("]");

        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{").append(contentsBuilder.toString());

        // Add streaming configuration if needed
        if (isStreaming) {
            requestBody.append(",\"generationConfig\": {\"temperature\": 0.7}");
        }

        requestBody.append("}");

        System.out.println("AI Request Body: " + requestBody.toString());
        return requestBody.toString();
    }
}
