package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cap.ai2code.services.GeminiChatService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.BotInstancesChatCompletionContext;

@Component
@ServiceName("MainService")
public class chatCompletionHandler implements EventHandler {

    @Autowired
    private GeminiChatService geminiChatService;

    /**
     * Handler untuk chatCompletion action
     */
    @On(entity = "MainService.BotInstances", event = "chatCompletion")
    public void onChatCompletion(BotInstancesChatCompletionContext context) {

        String content = context.getContent();
        try {

            if (content == null || content.trim().isEmpty()) {
                context.setResult("Error: No content provided");
            }

            context.setResult(content);

            String response = geminiChatService.chatCompletion(content);

            context.setResult(response);
        } catch (Exception e) {
            context.setResult("Error processing request");
        }
    }
}