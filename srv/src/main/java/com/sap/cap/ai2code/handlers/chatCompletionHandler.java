package com.sap.cap.ai2code.handlers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.PromptText_;

@Component
@ServiceName("MainService")
public class chatCompletionHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    private final String apiKey = "AIzaSyBnUu21XsdzPYDgBN0OzzoQmFNrK0QTYi0";
    private final String model = "gemini-2.0-flash";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @On(event = BotInstancesChatCompletionContext.CDS_NAME)
    public void handleChatCompletion(BotInstancesChatCompletionContext context) {
        String content = context.getContent();
        String aiResponse;
        String botTypeId;
        String botInstanceId = null;
        BotMessages aiResponseFinal = null;

        //DEBUG
        System.out.println("====================Handler Start!====================");

        try {
            // 1. Set BotInstance status to Running
            Map<String, String> info = getBotInstanceAndSetRunning(context);
            botTypeId = info.get("botTypeId");
            botInstanceId = info.get("botInstanceId");

            List<BotMessages> messageHistory = getMessageHistory(botInstanceId, botTypeId);
            boolean isFirstConversation = messageHistory.isEmpty();

            // 3. Build message context (history + user)
            List<Map<String, String>> conversationContext = buildConversationContext(
                    messageHistory, content, isFirstConversation);

            // 4. Call AI API with proper context
            aiResponse = callGeminiAPIWithContext(conversationContext);

            // 5. Save messages to database
            aiResponseFinal = saveMessages(botInstanceId, content, aiResponse);

            // 6. Update status to Success
            updateBotInstanceStatus(botInstanceId, "S");

        } catch (Exception e) {
            System.err.println("Error in chat completion: " + e.getMessage());
            aiResponse = "Error: " + e.getMessage();

            // Update status to Failed if we have botInstanceId
            if (botInstanceId != null) {
                updateBotInstanceStatus(botInstanceId, "F");
            }
        }

        System.out.println("Gemini Response: " + aiResponse);
        context.setResult(aiResponseFinal);
    }

    /**
     * Sets the status of a bot instance to "Running" and returns the
     * botInstanceId
     */
    private Map<String, String> getBotInstanceAndSetRunning(BotInstancesChatCompletionContext context) {
        // retrieving a query context
        CqnSelect selectQuery = context.getCqn();
        Result botInstanceResult = db.run(selectQuery);
        String botInstanceId = botInstanceResult.single().get("ID").toString();
        String botTypeid = botInstanceResult.single().get("type_ID").toString();

        // Update status to Running
        CqnUpdate updateQuery = Update.entity(BotInstances_.class)
                .where(b -> b.ID().eq(botInstanceId))
                .data("status_code", "R");
        db.run(updateQuery);

        System.out.println("Set Bot Instance " + botInstanceId + " to Running status");

        Map<String, String> result = new HashMap<>();
        result.put("botTypeId", botTypeid);
        result.put("botInstanceId", botInstanceId);

        // DEBUG
        System.out.println("botTypeid: " + botTypeid);
        System.out.println("botInstanceId: " + botInstanceId);

        return result;
    }

    /**
     * Gets message history for the bot instance
     */
    private List<BotMessages> getMessageHistory(String botInstanceId, String botTypeId) {

        List<BotMessages> messageHistory;

        // Get previous messages for this bot instance
        CqnSelect getMessages = Select.from(BotMessages_.class)
                .where(m -> m.botInstance().ID().eq(botInstanceId))
                .orderBy(m -> m.createdAt().asc());
        Result messagesResult = db.run(getMessages);

        if (messagesResult.list().isEmpty()) {
            // If no previous messages, fallback to system prompt
            messageHistory = getSystemPrompt(botInstanceId, botTypeId);
            System.out.println("No previous messages found. Loaded system prompt.");
        } else {
            // If messages found, use them
            messageHistory = messagesResult.listOf(BotMessages.class);
            System.out.println("Found " + messageHistory.size() + " previous messages.");
        }

        //DEBUG
        System.out.println("Initial Chat: " + messagesResult.list().isEmpty());
        System.out.println("messageHistory: " + messageHistory);

        return messageHistory;
    }

    private List<BotMessages> getSystemPrompt(String botInstanceId, String botTypeId) {
        CqnSelect getPromptText = Select.from(PromptText_.class)
                .columns("name", "content")
                .where(p -> p.botType_ID().eq(botTypeId).and(p.lang_code().eq("en")));

        Result promptTextResult = db.run(getPromptText);

        List<BotMessages> systemPrompts = new ArrayList<>();

        if (!promptTextResult.list().isEmpty()) {
            promptTextResult.list().forEach(row -> {
                String name = row.get("name") != null ? row.get("name").toString() : "";
                String content = row.get("content") != null ? row.get("content").toString() : "";

                BotMessages promptMessage = BotMessages.create();
                promptMessage.setBotInstanceId(botInstanceId);
                promptMessage.setRole("system");
                promptMessage.setMessage(name + ": " + content);
                systemPrompts.add(promptMessage);

                CqnInsert insertMessages = Insert.into(BotMessages_.class).entries(systemPrompts);
                db.run(insertMessages);

                System.out.println("System prompt for botTypeId: " + botTypeId + " > " + systemPrompts);

            });
        } else {
            System.out.println("No prompt text found for botTypeId: " + botTypeId);
        }

        return systemPrompts;

    }

    /**
     * Builds conversation context: system prompt + history + recent user
     * message
     */
    private List<Map<String, String>> buildConversationContext(
            List<BotMessages> messageHistory, String userMessage, boolean isFirstConversation) {

        List<Map<String, String>> context = new ArrayList<>();

        if (!isFirstConversation) {
            // Add historical messages to context
            for (BotMessages msg : messageHistory) {
                context.add(Map.of(
                        "role", msg.getRole(),
                        "content", msg.getMessage()
                ));
            }
        }

        // Add current user message
        context.add(Map.of(
                "role", "user",
                "content", userMessage
        ));

        //DEBUG
        System.out.println("Context for AI: " + context);

        return context;
    }

    /**
     * Calls Gemini API with conversation context
     */
    // curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=GEMINI_API_KEY" \
    //   -H 'Content-Type: application/json' \
    //   -X POST \
    //   -d '{
    //     "contents": [
    //       {
    //         "parts": [
    //           {
    //             "text": "Explain how AI works in a few words"
    //           }
    //         ]
    //       }
    //     ]
    //   }'    
    private String callGeminiAPIWithContext(List<Map<String, String>> conversationContext) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
        );

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

        //DEBUG
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
     * Save messages to database message + assistant response
     */
    private BotMessages saveMessages(String botInstanceId, String userMessage, String aiResponse) {
        List<BotMessages> messagesToInsert = new ArrayList<>();
        // Timestamp for user message
        Instant now = Instant.now();
        // Timestamp for AI response (better result when sort by createdAt)
        Instant delayedNow = now.plusMillis(1000);

        // Add user message
        BotMessages userMsg = BotMessages.create();
        userMsg.setBotInstanceId(botInstanceId);
        userMsg.setRole("user");
        userMsg.setMessage(userMessage);
        userMsg.setCreatedAt(now);
        userMsg.setCreatedBy("user");
        messagesToInsert.add(userMsg);

        // Add assistant message
        BotMessages assistantMsg = BotMessages.create();
        assistantMsg.setBotInstanceId(botInstanceId);
        assistantMsg.setRole("assistant");
        assistantMsg.setMessage(aiResponse);
        assistantMsg.setCreatedAt(delayedNow);
        assistantMsg.setCreatedBy("system");
        assistantMsg.setModifiedAt(delayedNow);
        assistantMsg.setModifiedBy("anonymous");
        messagesToInsert.add(assistantMsg);

        // Batch insert all messages
        if (!messagesToInsert.isEmpty()) {
            CqnInsert insertMessages = Insert.into(BotMessages_.class).entries(messagesToInsert);
            db.run(insertMessages);
            System.out.println("Saved " + messagesToInsert.size() + " messages");
        }

        //DEBUG
        System.out.println("AI Response: " + assistantMsg.getMessage());

        //Return botmessage with ID as API response
        CqnSelect aiResponseWithId = Select.from(BotMessages_.class)
                .where(m -> m.message().eq(aiResponse))
                .orderBy(m -> m.createdAt().asc());
        Result finalAiResponse = db.run(aiResponseWithId);

        return finalAiResponse.first(BotMessages.class).orElse(null);
    }

    /**
     * Updates bot instance status
     */
    private void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        try {
            CqnUpdate updateQuery = Update.entity(BotInstances_.class)
                    .where(b -> b.ID().eq(botInstanceId))
                    .data("status_code", statusCode)
                    .data("modifiedAt", Instant.now())
                    .data("modifiedBy", "system");

            db.run(updateQuery);

            // TO DO: get status code list directly from db
            String statusName = switch (statusCode) {
                case "R" ->
                    "RUNNING";
                case "S" ->
                    "SUCCESS";
                case "F" ->
                    "FAILED";
                default ->
                    statusCode;
            };

            System.out.println("Updated Bot Instance " + botInstanceId + " status to: " + statusName);

        } catch (Exception e) {
            System.err.println("Error updating bot instance status: " + e.getMessage());
        }
    }
}
