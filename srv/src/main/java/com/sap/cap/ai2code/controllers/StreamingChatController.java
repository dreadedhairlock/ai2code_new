// File Location: srv/src/main/java/customer/ai2code/controllers/StreamingChatController.java
package com.sap.cap.ai2code.controllers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.service.bot.BotService;

/**
 * REST Controller for streaming chat endpoints Handles /api/chat/Streaming
 * endpoint for real-time chat streaming
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class StreamingChatController {

    @Autowired
    private BotService botService;

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE)
     */
    @PostMapping(value = "/Streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody StreamRequestVO chatRequest) {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                String apiKey = "AIzaSyASQmgwGONMTa9kdAkGCoY-blWiE0a5A7U"; // Ganti dengan API key milikmu
                String endpoint = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:streamGenerateContent?key=%s",
                        apiKey);

                String requestBody = """
                        {
                          "contents": [
                            {
                              "parts": [
                                { "text": "%s" }
                              ]
                            }
                          ]
                        }
                        """.formatted(chatRequest.getContent().replace("\"", "\\\""));

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
                    if (!line.isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(line));
                    }
                }

                emitter.complete();
            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
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

    private String id; // botInstanceId
    private Boolean isActiveEntity; // Entity status
    private String content; // User message
    private String locale; // Language locale

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
