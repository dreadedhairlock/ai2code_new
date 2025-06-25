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

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModelResolver;
import com.sap.cap.ai2code.service.bot.BotService;
import com.sap.cap.ai2code.service.common.GenericCqnService;
import com.sap.cap.ai2code.service.context.ContextService;
import com.sap.cap.ai2code.service.prompt.PromptService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for streaming chat endpoints Handles /api/chat/Streaming
 * endpoint for real-time chat streaming
 */
@RestController
@RequestMapping("${cds.odata-v4.endpoint.path:/api}/chat")
@CrossOrigin(origins = "*")
@Slf4j  // Lombok annotation for logging
public class StreamingChatController {

    private final BotService botService;

    public StreamingChatController(BotService botService) {
        this.botService = botService;
    }

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE)
     */
    @PostMapping(value = "/Streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatSAP(@RequestBody ChatRequest chatRequest) {

        // Validate request using Lombok-generated methods
        try {
            chatRequest.validate();
            log.info("Starting streaming chat for bot: {}", chatRequest.getId());
        } catch (IllegalArgumentException e) {
            log.error("Validation failed: {}", e.getMessage());
            throw new BusinessException("Validation failed:" + e.getMessage());
        }

        return botService.chatInStreaming(chatRequest.getId(), chatRequest.getContent());
    }
}

@Data                    // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor       // Generates default constructor
@AllArgsConstructor      // Generates constructor with all parameters
class ChatRequest {

    @NonNull
    private String id;       // botInstanceId

    @NonNull
    private String content;  // User message

    // Custom validation method
    public void validate() {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Bot instance ID is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
    }
}
