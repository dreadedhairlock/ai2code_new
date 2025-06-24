package com.sap.cap.ai2code.service.impl;

import org.apache.tomcat.util.descriptor.web.ContextService;
import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.model.Bot;
import com.sap.cap.ai2code.model.bot.CodingBot;
import com.sap.cap.ai2code.service.BotService;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;
import com.sap.cap.ai2code.service.impl.GenericCqnService;
import com.sap.cap.ai2code.exception.BusinessException;

@Service
public class BotServiceImpl implements BotService{
    private final GenericCqnService genericCqnService;
    private final ContextService contextService;

    public BotServiceImpl(
            GenericCqnService genericCqnService,
            ContextService contextService) {
        this.genericCqnService = genericCqnService;
        this.contextService = contextService;

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

    private String extractMessageIdFromContext(BotMessagesAdoptContext context) {
        // Extract the message ID from the CQN query
        // Use the CqnAnalyzer class to extract the ID from the CQN query
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult result = cqnAnalyzer.analyze(context.getCqn().ref());
        return result.targetKeys().get("ID").toString();
    }

    @Override
    public Bot getCurrentBot(String botInstanceId) {
        // 从数据库查询BotInstance
        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);

        // 查询关联的BotType
        BotTypes botType = genericCqnService.getBotTypeById(botInstance.getTypeId());
        // Create a corresponding Bot instance based on BotType
        Bot bot = createBotInstance(botInstance, botType);
        return bot;
    }

    @Override
    public Bot getCurrentBot(String taskId, int sequence) {
        // // 使用缓存管理器查找
        // TaskBotNode botNode = cacheManager.getBotInstanceByTaskAndSequence(taskId, sequence);
        // if (botNode != null) {
        //     return botNode.getBotObject();
        // }

        // 根据taskId和sequence查询BotInstance
        BotInstances botInstance = genericCqnService.getBotInstanceByTaskAndSequence(taskId, sequence);
        return getCurrentBot(botInstance.getId());
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

        updateBotInstanceStatus(bot, "RUNNING");

        // 3. Retrieve outputContextPath
        String outputContextPath = genericCqnService.getOutputContextPathByBotInstanceId(botInstanceId);
        if (outputContextPath == null || outputContextPath.isBlank()) {
            updateBotInstanceStatus(bot, "FAILED");
            throw new BusinessException("No outputContextPath configured for botInstance: " + botInstanceId);

        }

        // 4. Retrieve the absolute outputContextPath
        String absoluteOutputContextPath = contextService.getContextFullPath(botInstanceId, outputContextPath);
        // 5. Call the upsertContext method of ContextService to store and return ContextNodes
        ContextNodes node = contextService.upsertContext(botInstanceId, absoluteOutputContextPath, messageText);

        updateBotInstanceStatus(bot, "SUCCESS");

        return node;

    }

    
    private Bot createBotInstance(BotInstances botInstance, BotTypes botType) {
        String functionTypeCode = botType.getFunctionTypeCode();

        switch (functionTypeCode) {
            // case "A": // AI Chat Bot
            //     return new ChatBot(botInstance, aiModel, botType, genericCqnService, promptService, aiModelResolver);
            // case "F": // Function Calling Bot
            //     return new FunctionCallingBot(botInstance, aiModel, botType, genericCqnService, promptService,
            //             aiModelResolver, botExecutionFactoryService);
            case "C": // Coding Bot
                return new CodingBot(botInstance, botType);
            default:
                throw new BusinessException("Unsupported bot function type: " + functionTypeCode);
        }
    }

        private void updateBotInstanceStatus(Bot bot, String status) {
        String botInstanceId = bot.getBotInstance().getId();
        // // 使用缓存管理器更新状态
        // cacheManager.updateBotStatus(botInstanceId, status);
        // 同时更新数据库
        genericCqnService.updateBotInstanceStatus(botInstanceId, status);
    }

}
