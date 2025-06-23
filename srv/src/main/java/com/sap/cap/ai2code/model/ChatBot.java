package com.sap.cap.ai2code.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstancesExecuteContext.ReturnType;
import cds.gen.mainservice.BotMessages;
import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.AIModelResolver;
import com.sap.cap.ai2code.service.impl.GenericCqnService;
import com.sap.cap.ai2code.service.interfaces.AIService;
import com.sap.cap.ai2code.service.interfaces.PromptService;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatBot implements Bot {

    private BotInstances botInstance;
    private AIModel aiModel;
    private BotTypes botType;

    // Service dependencies
    private GenericCqnService genericCqnService;
    private PromptService promptService;
    private AIModelResolver aiModelResolver;

    // Constructor that matches what BotServiceImpl is calling
    public ChatBot(BotInstances botInstance, AIModel aiModel, BotTypes botType,
            GenericCqnService genericCqnService, PromptService promptService,
            AIModelResolver aiModelResolver) {
        this.botInstance = botInstance;
        this.aiModel = aiModel;
        this.botType = botType;
        this.genericCqnService = genericCqnService;
        this.promptService = promptService;
        this.aiModelResolver = aiModelResolver;
    }

    @Override
    public ReturnType execute() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

    @Override
    public Boolean executeAsync() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeAsync'");
    }

    @Override
    public Boolean stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

    @Override
    public Boolean resume() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resume'");
    }

    @Override
    public Boolean cancel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancel'");
    }

    @Override
    public SseEmitter chatInStreaming(String content) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chatInStreaming'");
    }

    @Override
    public String chat(String content) {
        List<PromptTexts> prompts = new ArrayList<>();
        try {
            // 1. Get the appropriate AI service based on the AI model type
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());

            // 2. Check if this is the first chat call - if so, need to save prompt messages
            boolean isFirstCall = genericCqnService.isFirstCall(botInstance.getId());
            if (isFirstCall) {
                // Get prompts for this bot type and instance
                prompts = promptService.getPrompts(botType.getId(), "", "");
                if (prompts != null && !prompts.isEmpty()) {
                    savePromptMessages(prompts);
                }
            }

            // 3. Retrieve chat history messages for this bot instance
            List<BotMessages> historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());

            // 4. Make the actual AI service call with history, prompts, and current content
            String response = aiService.chatWithAI(historyMessages, prompts, content, aiModel);

            return response;

        } catch (Exception e) {
            System.err.println("Chat failed for bot: " + botInstance.getId() + ", error: " + e.getMessage());
            throw new BusinessException("Chat failed", e);
        }
    }

    @Override
    public BotInstances getBotInstance() {
        return this.botInstance;
    }

    /**
     *
     */
    private void savePromptMessages(List<PromptTexts> prompts) {
        for (PromptTexts prompt : prompts) {
            if (prompt.getContent() != null && !prompt.getContent().isEmpty()) {

                genericCqnService.createAndInsertBotMessage(botInstance.getId(), prompt.getContent(), "system");
            }
        }
    }

    // @Override
    // public AIModel getAiModel() {
    //     return this.aiModel;
    // }
}
