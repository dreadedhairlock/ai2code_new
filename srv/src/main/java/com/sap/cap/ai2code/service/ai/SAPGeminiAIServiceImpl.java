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
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content,
            AIModel model, ExecutorService executor, StreamingCompletedProcessor streamingCompletionProcessor) {

        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout

        // Use provided executor or default one
        ExecutorService actualExecutor = executor != null ? executor : this.executorService;

        actualExecutor.execute(() -> {
            try {
                // Build conversation context
                List<Map<String, String>> conversationContext = buildConversationContext(messages, prompts, content);

                // Call Gemini streaming API
                callGeminiStreamingAPI(conversationContext, emitter, streamingCompletionProcessor);

            } catch (Exception e) {
                System.err.println("Streaming failed: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
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

    private void callGeminiStreamingAPI(List<Map<String, String>> conversationContext,
            SseEmitter emitter, StreamingCompletedProcessor processor) throws Exception {

        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?key=%s",
                model, apiKey);

        String requestBody = buildRequestBody(conversationContext, true);
        System.out.println("Streaming URL: " + url);
        System.out.println("Request Body: " + requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        StringBuilder completeResponse = new StringBuilder();

        try {
            // Gunakan InputStream untuk membaca response streaming
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Streaming API call failed: " + response.statusCode());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6).trim();

                    if (jsonData.equals("[DONE]") || jsonData.isEmpty()) {
                        break;
                    }

                    try {
                        JsonNode jsonNode = objectMapper.readTree(jsonData);
                        String chunk = parseStreamingChunk(jsonNode);

                        if (chunk != null && !chunk.isEmpty()) {
                            completeResponse.append(chunk);
                            emitter.send(SseEmitter.event().data(chunk));
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to parse streaming chunk: " + e.getMessage());
                    }
                }
            }

            if (processor != null) {
                processor.process(completeResponse.toString());
            }

            emitter.complete();

        } catch (Exception e) {
            System.err.println("Streaming error: " + e.getMessage());
            emitter.completeWithError(e);
        }
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

    /**
     * Process streaming response from Gemini API
     */
    private void processStreamingResponse(String responseBody, SseEmitter emitter, StringBuilder completeResponse)
            throws IOException {

        BufferedReader reader = new BufferedReader(new StringReader(responseBody));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6).trim();

                if (jsonData.equals("[DONE]")) {
                    break;
                }

                try {
                    JsonNode jsonNode = objectMapper.readTree(jsonData);
                    String chunk = parseStreamingChunk(jsonNode);

                    if (chunk != null && !chunk.isEmpty()) {
                        completeResponse.append(chunk);
                        emitter.send(chunk);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to parse streaming chunk: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parse individual streaming chunk from Gemini response
     */
    private String parseStreamingChunk(JsonNode jsonNode) {
        try {
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
            System.err.println("Failed to parse streaming chunk: " + e.getMessage());
        }
        return "";
    }

}
