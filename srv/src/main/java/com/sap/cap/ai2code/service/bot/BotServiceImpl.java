package com.sap.cap.ai2code.service.bot;

// import org.apache.tomcat.util.descriptor.web.ContextService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModel;
import com.sap.cap.ai2code.model.ai.AIModelResolver;
import com.sap.cap.ai2code.model.bot.Bot;
import com.sap.cap.ai2code.model.bot.ChatBot;
import com.sap.cap.ai2code.model.bot.CodingBot;
import com.sap.cap.ai2code.model.bot.FunctionCallingBot;
import com.sap.cap.ai2code.service.common.GenericCqnService;
import com.sap.cap.ai2code.service.context.ContextService;
import com.sap.cap.ai2code.service.prompt.PromptService;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;

@Service
public class BotServiceImpl implements BotService {

    private final AIModelResolver aiModelResolver;
    private final GenericCqnService genericCqnService;
    private final PromptService promptService;
    private final BotExecutionFactoryService botExecutionFactoryService;
    private final ContextService contextService;

    public BotServiceImpl(AIModelResolver aiModelResolver, GenericCqnService genericCqnService,
            PromptService promptService, BotExecutionFactoryService botExecutionFactoryService,
            ContextService contextService) {
        this.aiModelResolver = aiModelResolver;
        this.genericCqnService = genericCqnService;
        this.promptService = promptService;
        this.botExecutionFactoryService = botExecutionFactoryService;
        this.contextService = contextService;
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
        System.out.println("bot ID: " + botInstanceId);
        // Call and return the result of the second chat method
        return chat(botInstanceId, context.getContent());
    }

    @Override
    public BotMessages chat(String botInstanceId, String content) {
        String response;
        // Get cached or create new Bot
        Bot bot = getCurrentBot(botInstanceId);

        updateBotInstanceStatus(bot, "R");

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

            updateBotInstanceStatus(bot, "S");

            return botMessage;

        } catch (BusinessException | InterruptedException e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "F");
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
    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context) {
        String botInstanceId = extractIdFromContext(context);
        return execute(botInstanceId);
    }

    @Override
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId) {
        Bot bot = getCurrentBot(botInstanceId);

        // 更新状态为RUNNING
        updateBotInstanceStatus(bot, "RUNNING");
        try {
            BotInstancesExecuteContext.ReturnType result = bot.execute();

            // 更新状态为SUCCESS
            updateBotInstanceStatus(bot, "SUCCESS");

            // 更新result字段
            updateBotInstanceResult(bot, result.getResult());

            return result;
        } catch (Exception e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "FAILED");
            throw new BusinessException("Execution failed for bot: " + botInstanceId, e);
        }

    }

    @Override
    public ContextNodes adopt(BotMessagesAdoptContext context) {
        // Get MessageID from context
        String messageId = extractMessageIdFromContext(context);
        // Get the associated BotInstanceID from MessageID
        String botInstanceId = genericCqnService.getBotInstanceIdByMessageId(messageId);
        if (botInstanceId == null) {
            throw new BusinessException("No bot instance associated with message: " + messageId);
        }
        return adopt(botInstanceId, messageId);
    }

    @Override
    public ContextNodes adopt(String botInstanceId, String messageId) {
        // adopt 1. Get the current BotMessages entries
        BotMessages botMessage = genericCqnService.getMessageById(botInstanceId, messageId);
        if (botMessage == null) {
            throw new BusinessException("Message not found: " + messageId);
        }
        String messageText = botMessage.getMessage();
        // adopt 2. Get BotInstances entry according to BotMessages.botInstance
        // adopt 3. Get BotTypes entries based on BotInstances.type
        Bot bot = getCurrentBot(botInstanceId);
        updateBotInstanceStatus(bot, "R");
        // 3. Retrieve outputContextPath
        String outputContextPath = genericCqnService.getOutputContextPathByBotInstanceId(botInstanceId);
        if (outputContextPath == null || outputContextPath.isBlank()) {
            updateBotInstanceStatus(bot, "F");
            throw new BusinessException("No outputContextPath configured for botInstance: " + botInstanceId);
        }
        // 4. Retrieve the absolute outputContextPath
        String absoluteOutputContextPath = contextService.getContextFullPath(botInstanceId, outputContextPath);
        // 5. Call the upsertContext method of ContextService to store and return
        // ContextNodes
        ContextNodes node = contextService.upsertContext(botInstanceId, absoluteOutputContextPath, messageText);
        updateBotInstanceStatus(bot, "S");
        return node;
    }

    private Bot createBotInstance(BotInstances botInstance, BotTypes botType, AIModel aiModel) {
        String functionTypeCode = botType.getFunctionTypeCode();

        switch (functionTypeCode) {
            case "A": // AI Chat Bot
                return new ChatBot(botInstance, aiModel, botType, genericCqnService, promptService, aiModelResolver);
            case "F": // Function Calling Bot
                return new FunctionCallingBot(botInstance, aiModel, botType, genericCqnService, promptService,
                        aiModelResolver, botExecutionFactoryService);
            case "C": // Coding Bot
                return new CodingBot(botInstance, aiModel, botType);
            default:
                throw new BusinessException("Unsupported bot function type: " + functionTypeCode);
        }
    }

    private void updateBotInstanceStatus(Bot bot, String status) {
        String botInstanceId = bot.getBotInstance().getId();
        // 使用缓存管理器更新状态
        // cacheManager.updateBotStatus(botInstanceId, status);
        // 同时更新数据库
        genericCqnService.updateBotInstanceStatus(botInstanceId, status);
    }

    private void updateBotInstanceResult(Bot bot, String result) {
        String botInstanceId = bot.getBotInstance().getId();
        System.out.println("Bot update:" + botInstanceId);
        genericCqnService.updateBotInstanceResult(botInstanceId, result);
    }

    private String extractIdFromContext(BotInstancesExecuteContext context) {
        // 从CQN查询中提取ID，需要解析CqnSelect
        // return context.getCqn().ref().segments().get(0).id();
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        // return result.rootKeys().get("ID").toString();
        return result.targetKeys().get("ID").toString();
    }

    private String extractMessageIdFromContext(BotMessagesAdoptContext context) {
        // Extract the message ID from the CQN query
        // Use the CqnAnalyzer class to extract the ID from the CQN query
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        return result.targetKeys().get("ID").toString();
    }
}
