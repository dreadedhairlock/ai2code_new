// File Location: srv/src/main/java/customer/ai2code/controllers/StreamingChatController.java
package com.sap.cap.ai2code.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.service.bot.BotService;
import com.sap.cap.ai2code.exception.BusinessException;

/**
 * REST Controller for streaming chat endpoints Handles /api/chat/Streaming
 * endpoint for real-time chat streaming
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Configure CORS as needed
public class StreamingChatController {

    @Autowired
    private BotService botService;

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE)
     *
     * @param request The streaming request payload
     * @return SseEmitter for real-time streaming
     */
    @PostMapping(value = "/Streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody StreamRequestVO request) {

        try {
            // Extract parameters from request
            String botInstanceId = request.getId();
            String content = request.getContent();

            // Validate input
            if (botInstanceId == null || botInstanceId.isEmpty()) {
                throw new BusinessException("Bot instance ID is required");
            }

            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException("Content is required");
            }

            // Delegate to bot service for streaming
            return botService.chatInStreaming(botInstanceId, content);

        } catch (Exception e) {
            // Return error via SSE
            SseEmitter errorEmitter = new SseEmitter();
            errorEmitter.completeWithError(e);
            return errorEmitter;
        }
    }

    /**
     * Health check endpoint for streaming service
     */
    @GetMapping("/streaming/health")
    public String healthCheck() {
        return "Streaming service is running";
    }

    /**
     * Get streaming configuration
     */
    @GetMapping("/streaming/config")
    public StreamingConfigResponse getStreamingConfig() {
        return StreamingConfigResponse.builder()
                .streamingEnabled(true)
                .timeout(30000L)
                .maxConnections(100)
                .build();
    }
}

/**
 * Request payload for streaming chat
 */
class StreamRequestVO {

    private String id;              // botInstanceId
    private Boolean isActiveEntity; // Entity status
    private String content;         // User message
    private String locale;          // Language locale

    // Constructors
    public StreamRequestVO() {
    }

    public StreamRequestVO(String id, Boolean isActiveEntity, String content, String locale) {
        this.id = id;
        this.isActiveEntity = isActiveEntity;
        this.content = content;
        this.locale = locale;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsActiveEntity() {
        return isActiveEntity;
    }

    public void setIsActiveEntity(Boolean isActiveEntity) {
        this.isActiveEntity = isActiveEntity;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return "StreamRequestVO{"
                + "id='" + id + '\''
                + ", isActiveEntity=" + isActiveEntity
                + ", content='" + content + '\''
                + ", locale='" + locale + '\''
                + '}';
    }
}

/**
 * Response for streaming configuration
 */
class StreamingConfigResponse {

    private boolean streamingEnabled;
    private long timeout;
    private int maxConnections;

    public static StreamingConfigResponseBuilder builder() {
        return new StreamingConfigResponseBuilder();
    }

    // Getters and Setters
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public static class StreamingConfigResponseBuilder {

        private boolean streamingEnabled;
        private long timeout;
        private int maxConnections;

        public StreamingConfigResponseBuilder streamingEnabled(boolean streamingEnabled) {
            this.streamingEnabled = streamingEnabled;
            return this;
        }

        public StreamingConfigResponseBuilder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public StreamingConfigResponseBuilder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public StreamingConfigResponse build() {
            StreamingConfigResponse response = new StreamingConfigResponse();
            response.setStreamingEnabled(this.streamingEnabled);
            response.setTimeout(this.timeout);
            response.setMaxConnections(this.maxConnections);
            return response;
        }
    }
}
