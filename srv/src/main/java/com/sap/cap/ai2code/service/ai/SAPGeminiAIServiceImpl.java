package com.sap.cap.ai2code.service.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import com.sap.cap.ai2code.exception.BusinessException;
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

            // Call Gemini API method
            return callGeminiAPI(conversationContext, false);

        } catch (Exception e) {
            throw new BusinessException("Failed to call Gemini API", e);
        }
    }

    @Override
    public SseEmitter chatWithAIStreaming(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model, ExecutorService executor, StreamingCompletedProcessor streamingCompletionProcessor) {

        // New instance of SseEmitter with a timeout of 30 seconds
        SseEmitter emitter = new SseEmitter(30000L);

        // Build conversation context
        List<Map<String, String>> conversationContext = buildConversationContext(messages, prompts, content);

        // Use the provided executor service, or fall back to the class-level one
        ExecutorService executorToUse = executor != null ? executor : this.executorService;

        // Submit the streaming task to the executor service
        executorToUse.submit(() -> {
            StringBuilder completeResponse = new StringBuilder();

            try {
                // Call streaming method
                callGeminiStreamingAPI(conversationContext, emitter, completeResponse, streamingCompletionProcessor);

            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Call regular Gemini API requests
     */
    private String callGeminiAPI(List<Map<String, String>> conversationContext, boolean isStreaming) throws Exception {
        String requestBody = buildRequestBody(conversationContext, isStreaming);
        String mode = isStreaming ? "streamGenerateContent" : "generateContent";

        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:%s?key=%s",
                model, mode, apiKey);

        System.out.println("API URL: " + url);
        System.out.println("Request Body: " + requestBody);

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
     * Call streaming Gemini API request with callback support
     */
    private void callGeminiStreamingAPI(List<Map<String, String>> conversationContext, SseEmitter emitter, StringBuilder completeResponse, StreamingCompletedProcessor streamingCompletionProcessor) throws Exception {
        String requestBody = buildRequestBody(conversationContext, true);

        String endpoint = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent?key=%s",
                this.model, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes());
            System.err.println("API Error Response: " + errorBody);
            throw new RuntimeException("Streaming API call failed: " + response.statusCode() + " - " + errorBody);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
        String line;
        int lineCount = 0;
        int chunkCount = 0;

        while ((line = reader.readLine()) != null) {
            lineCount++;
            line = line.trim();

            if (!line.isEmpty() && line.contains("\"text\":")) {

                // Extract text content for cleaner streaming
                String textContent = extractTextFromLine(line);
                if (textContent != null && !textContent.isEmpty()) {
                    chunkCount++;
                    completeResponse.append(textContent);

                    emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(textContent));

                } else {
                    emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(line));
                    completeResponse.append(line);
                }
            }
        }

        // Process complete response AFTER streaming finishes
        if (streamingCompletionProcessor != null && completeResponse.length() > 0) {
            streamingCompletionProcessor.process(completeResponse.toString());
        }

        emitter.complete();

    }

    /**
     * Extract text content from a streaming line
     */
    private String extractTextFromLine(String line) {
        try {
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
            System.err.println("Text extraction failed, using fallback: " + e.getMessage());
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
     * Build request body for Gemini API (unified for both regular and
     * streaming)
     */
    private String buildRequestBody(List<Map<String, String>> conversationContext, boolean isStreaming) {
        try {
            // Create the main request object
            Map<String, Object> requestMap = new HashMap<>();

            // Build contents array
            List<Map<String, Object>> contents = new ArrayList<>();

            for (Map<String, String> message : conversationContext) {
                String role = message.get("role");
                String content = message.get("content");

                // Gemini uses "user" and "model" roles, map "assistant" and "system" to "model"
                String geminiRole = "user".equals(role) ? "user" : "model";

                // Create parts array
                List<Map<String, String>> parts = new ArrayList<>();
                Map<String, String> textPart = new HashMap<>();
                textPart.put("text", content); // No manual escaping needed!
                parts.add(textPart);

                // Create content object
                Map<String, Object> contentObj = new HashMap<>();
                contentObj.put("role", geminiRole);
                contentObj.put("parts", parts);

                contents.add(contentObj);
            }

            requestMap.put("contents", contents);

            // Add streaming configuration if needed
            if (isStreaming) {
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.7);
                requestMap.put("generationConfig", generationConfig);
            }

            // Convert to JSON using ObjectMapper (handles all escaping automatically)
            return objectMapper.writeValueAsString(requestMap);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    /**
     * Parse Gemini API response (unified for both regular and streaming)
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
}
