package com.sap.cap.ai2code.service.bot;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModel;
import com.sap.cap.ai2code.model.ai.AIModelResolver;
import com.sap.cap.ai2code.model.bot.Bot;
import com.sap.cap.ai2code.model.bot.ChatBot;
import com.sap.cap.ai2code.service.common.GenericCqnService;
import com.sap.cap.ai2code.service.prompt.PromptService;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstancesExecuteContext.ReturnType;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;

// import org.apache.tomcat.util.descriptor.web.ContextService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class BotServiceImpl implements BotService {

    private final AIModelResolver aiModelResolver;
    private final GenericCqnService genericCqnService;
    private final PromptService promptService;

    public BotServiceImpl(AIModelResolver aiModelResolver, GenericCqnService genericCqnService, PromptService promptService) {
        this.aiModelResolver = aiModelResolver;
        this.genericCqnService = genericCqnService;
        this.promptService = promptService;
    }

    @Override
    public Bot getCurrentBot(String botInstanceId) {

        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);

        BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());

        AIModel aiModel = aiModelResolver.resolveAIModel(botType.getModelId());

        System.out.println("botInstance: " + botInstance);
        System.out.println("botType: " + botType);
        System.out.println("aiModel: " + aiModel);

        Bot bot = createBotInstance(botInstance, botType, aiModel);

        return bot;
    }

    @Override
    public Bot getCurrentBot(String taskId, int sequence) {
        // Query BotInstance by taskId and sequence
        BotInstances botInstance = genericCqnService.getBotInstanceByTaskAndSequence(taskId, sequence);
        return getCurrentBot(botInstance.getId());
    }

    @Override
    public BotMessages chat(BotInstancesChatCompletionContext context) {
        // Get BotInstance's ID
        String botInstanceId = genericCqnService.extractBotInstanceIdFromContext(context);
        // Call and return the result of the second chat method
        return chat(botInstanceId, context.getContent());
    }

    @Override
    public BotMessages chat(String botInstanceId, String content) {
        String response;
        // Get cached or create new Bot
        Bot bot = getCurrentBot(botInstanceId);

        try {
            if (bot instanceof ChatBot chatBot) {
                response = chatBot.chat(content);
            } else {
                throw new BusinessException("Bot is not a ChatBot: " + botInstanceId);
            }

            genericCqnService.createAndInsertBotMessage(botInstanceId, content, "user");

            // 500ms delay to ensure user message got inserted to DB first
            Thread.sleep(500);

            BotMessages botMessage = genericCqnService.createAndInsertBotMessage(botInstanceId, response, "assistant");

            return botMessage;

        } catch (BusinessException | InterruptedException e) {
            // 更新状态为FAILED
            // updateBotInstanceStatus(bot, "F");
            throw new BusinessException("Chat failed for bot: " + botInstanceId, e);
        }
    }

    @Override
    public SseEmitter chatInStreaming(String botInstanceId, String content) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'chatInStreaming'");
    }

    @Override
    public Boolean executeAsync(BotInstancesExecuteContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeAsync'");
    }

    @Override
    public Boolean executeAsync(String botInstanceId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeAsync'");
    }

    @Override
    public ReturnType execute(BotInstancesExecuteContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

    @Override
    public ReturnType execute(String botInstanceId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

    @Override
    public void adopt(BotMessagesAdoptContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'adopt'");
    }

    @Override
    public void adopt(String botInstanceId, String messageId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'adopt'");
    }

    private Bot createBotInstance(BotInstances botInstance, BotTypes botType, AIModel aiModel) {
        String functionTypeCode = botType.getFunctionTypeCode();

        switch (functionTypeCode) {
            case "A" -> {
                // AI Chat Bot
                return new ChatBot(botInstance, aiModel, botType, genericCqnService, promptService, aiModelResolver);
            }
            default ->
                throw new BusinessException("Unsupported bot function type: " + functionTypeCode);
        }
        // case "F": // Function Calling Bot
        //     return new FunctionCallingBot(botInstance, aiModel, botType, genericCqnService, promptService, aiModelResolver, botExecutionFactoryService);
        // case "C": // Coding Bot
        //     return new CodingBot(botInstance, aiModel, botType);
    }
}
