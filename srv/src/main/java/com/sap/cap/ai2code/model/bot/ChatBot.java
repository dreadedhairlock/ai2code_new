package com.sap.cap.ai2code.model.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext.ReturnType;
import cds.gen.mainservice.BotMessages;
import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModel;
import com.sap.cap.ai2code.model.ai.AIModelResolver;
import com.sap.cap.ai2code.service.common.GenericCqnService;
import com.sap.cap.ai2code.service.ai.AIService;
import com.sap.cap.ai2code.service.ai.StreamingCompletedProcessor;
import com.sap.cap.ai2code.service.prompt.PromptService;

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

        List<PromptTexts> prompts = new ArrayList<>();

        try {
            // 1. Get the appropriate AI service based on the AI model type
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());

            // 2. Get previous bot messages
            List<BotMessages> historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());

            // 3. Check if this is the first chat call - if so, need to save prompt messages
            boolean isFirstCall = historyMessages.isEmpty();
            if (isFirstCall) {
                prompts = promptService.getPrompts(botType.getId(), "", "");
                if (prompts != null && !prompts.isEmpty()) {
                    savePromptMessages(prompts);
                    historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());
                    System.out.println("historyMessages: " + historyMessages);
                }
            }

            // 4. Make the streaming AI service call with streaming processor
            return aiService.chatWithAIStreaming(
                    historyMessages,
                    prompts,
                    content,
                    aiModel,
                    null,
                    (completeResponse) -> {
                        try {
                            // Save messages when streaming completes
                            // Save user message
                            genericCqnService.createAndInsertBotMessage(
                                    botInstance.getId(),
                                    content,
                                    "user"
                            );
                            // Small delay to ensure user message is saved first
                            Thread.sleep(500);
                            // Save assistant message
                            genericCqnService.createAndInsertBotMessage(
                                    botInstance.getId(),
                                    completeResponse,
                                    "assistant"
                            );

                        } catch (Exception e) {
                            System.err.println("Failed to save conversation: " + e.getMessage());
                            e.printStackTrace();
                            // updateBotInstanceStatus("F"); // Uncomment if you have this method
                        }
                    }
            );

        } catch (Exception e) {
            throw new BusinessException("Chat failed", e);
        }

    }

    @Override
    public String chat(String content) {

        List<PromptTexts> prompts = new ArrayList<>();

        try {
            // 1. Get the appropriate AI service based on the AI model type
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());

            // 2. Get previous bot messages
            List<BotMessages> historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());

            // 3. Check if this is the first chat call - if so, need to save prompt messages
            if (historyMessages.isEmpty()) {
                prompts = promptService.getPrompts(botType.getId(), "", "");
                if (prompts != null && !prompts.isEmpty()) {
                    savePromptMessages(prompts);
                    historyMessages = genericCqnService.getBotMessagesByBotInstanceId(botInstance.getId());
                    System.out.println("historyMessages: " + historyMessages);
                }
            }

            // 4. Make the actual AI service call with history, prompts, and current content
            String response = aiService.chatWithAI(historyMessages, prompts, content, aiModel);

            return response;

        } catch (Exception e) {
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
