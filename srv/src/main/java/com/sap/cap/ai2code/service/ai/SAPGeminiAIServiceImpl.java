package com.sap.cap.ai2code.service.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
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
import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModel;
import com.sap.cap.ai2code.model.execution.TaskCreationParam;
import com.sap.cap.ai2code.service.bot.BotExecution;
import com.sap.cap.ai2code.service.execution.functioncall.FunctionCallProcessor;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class SAPGeminiAIServiceImpl implements AIService {

    private final FunctionCallProcessor functionCallProcessor;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private static final Dotenv dotenv = Dotenv.load();

    private final String model = dotenv.get("GEMINI_MODEL");
    private final String apiKey = dotenv.get("GEMINI_API_KEY");

    public SAPGeminiAIServiceImpl(FunctionCallProcessor functionCallProcessor) {
        this.functionCallProcessor = null;
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

    @Override
    public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
            T botExecutionInstance, AIModel aiModel) {

        try {
            // Verifikasi bahwa ini adalah CreateTasksBotExecution
            String className = botExecutionInstance.getClass().getSimpleName();
            System.out.println("Bot Execution class: " + className);

            // Buat instruksi khusus untuk CreateTasksBotExecution
            StringBuilder functionDescriptions = new StringBuilder();
            functionDescriptions.append("You are working with a CreateTasksBotExecution.\n");
            functionDescriptions.append("This bot creates tasks based on user input.\n\n");
            functionDescriptions.append("You should call the execute function using this format EXACTLY:\n\n");
            functionDescriptions.append("FUNCTION_CALL: execute\n");
            functionDescriptions.append("botInstanceId: [bot instance id or any string]\n");
            functionDescriptions.append("taskCreationParams: [JSON array of task parameters]\n");
            functionDescriptions.append("END_FUNCTION_CALL\n\n");
            functionDescriptions.append("Example of taskCreationParams format:\n");
            functionDescriptions.append(
                    "[{\"sequence\": 1, \"name\": \"Task 1\", \"description\": \"Description 1\",\"contextPath\": \"/path/1\"}, ");
            functionDescriptions.append(
                    "{\"sequence\": 2, \"name\": \"Task 2\", \"description\": \"Description 2\",\"contextPath\": \"/path/2\"}]\n\n");

            // Create conversation context
            List<Map<String, String>> conversationContext = new ArrayList<>();

            // Tambahkan instruksi sistem
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", functionDescriptions.toString());
            conversationContext.add(systemMessage);

            // Tambahkan prompts jika ada
            if (prompts != null) {
                for (PromptTexts prompt : prompts) {
                    if (prompt.getContent() != null && !prompt.getContent().trim().isEmpty()) {
                        Map<String, String> promptMessage = new HashMap<>();
                        promptMessage.put("role", "system");
                        promptMessage.put("content", prompt.getContent());
                        conversationContext.add(promptMessage);
                    }
                }
            }

            // Tambahkan riwayat percakapan
            if (messages != null) {
                for (BotMessages message : messages) {
                    if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                        Map<String, String> chatMessage = new HashMap<>();
                        chatMessage.put("role", message.getRole());
                        chatMessage.put("content", message.getMessage());
                        conversationContext.add(chatMessage);
                    }
                }
            }

            // Panggil Gemini API
            String geminiResponse = callGeminiAPI(conversationContext, false);
            System.out.println("Gemini response: " + geminiResponse);

            // Parse respons untuk mencari function call
            String functionCallPattern = "FUNCTION_CALL:\\s*execute[\\s\\S]*?END_FUNCTION_CALL";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(functionCallPattern);
            java.util.regex.Matcher matcher = pattern.matcher(geminiResponse);

            if (matcher.find()) {
                // Ekstrak function call
                String functionCallText = matcher.group(0);
                String[] lines = functionCallText.split("\\n");

                // Ekstrak parameter
                String botInstanceId = null;
                String taskCreationParamsJson = null;

                // Setelah FUNCTION_CALL: execute, cari parameter
                boolean collectingTaskParams = false;
                StringBuilder taskParamsBuilder = new StringBuilder();

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();

                    if (line.startsWith("botInstanceId:")) {
                        botInstanceId = line.substring("botInstanceId:".length()).trim();
                    } else if (line.startsWith("taskCreationParams:")) {
                        // Mulai mengumpulkan JSON array
                        collectingTaskParams = true;

                        // Ambil awal JSON yang mungkin ada di baris saat ini
                        String initialJson = line.substring("taskCreationParams:".length()).trim();
                        taskParamsBuilder.append(initialJson);
                    } else if (collectingTaskParams && !line.equals("END_FUNCTION_CALL")) {
                        // Lanjutkan mengumpulkan JSON array
                        taskParamsBuilder.append(" ").append(line);
                    } else if (line.equals("END_FUNCTION_CALL")) {
                        collectingTaskParams = false;
                    }
                }

                taskCreationParamsJson = taskParamsBuilder.toString().trim();

                System.out.println("Extracted botInstanceId: " + botInstanceId);
                System.out.println("Extracted taskCreationParams: " + taskCreationParamsJson);

                // Validasi dan siapkan parameter
                if (botInstanceId == null || botInstanceId.trim().isEmpty()) {
                    botInstanceId = "default_bot_id";
                }

                // Persiapkan parameter taskCreationParams
                List<TaskCreationParam> taskParams;

                try {
                    // Cleanup JSON: hapus tanda backtick markdown jika ada
                    if (taskCreationParamsJson != null) {
                        taskCreationParamsJson = taskCreationParamsJson
                                .replaceAll("```json", "")
                                .replaceAll("```", "")
                                .trim();
                    }

                    if (taskCreationParamsJson == null || taskCreationParamsJson.trim().isEmpty()) {
                        taskParams = new ArrayList<>();
                    } else {
                        // Parse JSON ke List<TaskCreationParam>
                        taskParams = objectMapper.readValue(
                                taskCreationParamsJson,
                                objectMapper.getTypeFactory().constructCollectionType(
                                        List.class,
                                        com.sap.cap.ai2code.model.execution.TaskCreationParam.class));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing taskCreationParams: " + e.getMessage());
                    e.printStackTrace();
                    throw new BusinessException("Failed to parse taskCreationParams JSON: " + e.getMessage());
                }

                // Panggil metode execute
                try {
                    Method executeMethod = botExecutionInstance.getClass().getDeclaredMethod(
                            "execute", String.class, List.class);

                    executeMethod.setAccessible(true);
                    Object result = executeMethod.invoke(botExecutionInstance, botInstanceId, taskParams);
                    System.out.println("Execute method executed successfully. Result: " + result);

                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        throw new BusinessException("Error executing function: " + cause.getMessage(), cause);
                    } else {
                        throw new BusinessException("Error executing function: " + e.getMessage(), e);
                    }
                }
            }

            // Jika tidak ada function call, kembalikan respons teks biasa
            return geminiResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Function calling with Gemini failed: " + e.getMessage(), e);
        }
    }

}
