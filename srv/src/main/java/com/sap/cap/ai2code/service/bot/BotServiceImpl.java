package com.sap.cap.ai2code.service.bot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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
import com.sap.cap.ai2code.service.prompt.PromptService;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;

@Service
public class BotServiceImpl implements BotService {

    private final AIModelResolver aiModelResolver;
    private final GenericCqnService genericCqnService;
    private final PromptService promptService;

    public BotServiceImpl(AIModelResolver aiModelResolver, GenericCqnService genericCqnService,
            PromptService promptService) {
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
        String botInstanceId = genericCqnService.extractBotInstanceIdFromContext(context);
        return execute(botInstanceId);
    }

    @Override
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId) {
        Bot bot = getCurrentBot(botInstanceId);
        BotTypes botType;
        String functionTypeCode;
        try {
            Field botTypeField = bot.getClass().getDeclaredField("botType");
            botTypeField.setAccessible(true);
            Object botTypeObj = botTypeField.get(bot);

            if (botTypeObj instanceof BotTypes) {
                botType = (BotTypes) botTypeObj;
                functionTypeCode = botType.getFunctionTypeCode();
            } else {
                throw new BusinessException("Invalid Bot Type");
            }
        } catch (Exception e) {
            throw new BusinessException("Execution failed for bot: " + botInstanceId, e);
        }

        // 创建返回对象
        BotInstancesExecuteContext.ReturnType returnValue = BotInstancesExecuteContext.ReturnType.create();

        // 更新状态为RUNNING
        updateBotInstanceStatus(bot, "R");

        try {
            switch (functionTypeCode) {
                case "F": {
                    List<PromptTexts> prompts = botType.getPrompts();
                    System.out.println("prompt: " + prompts);
                    String implementationClassFromBotType = botType.getImplementationClass();

                    if (implementationClassFromBotType == null ||
                            implementationClassFromBotType.isEmpty())
                        throw BusinessException.implementationClassMissing();

                    // Get the implementationClass
                    Class<?> implementationClass = Class.forName(implementationClassFromBotType);
                    System.out.println("Class Name: " + implementationClass.getName());

                    // Get all methods
                    Method[] methods = implementationClass.getDeclaredMethods();

                    // Check if there exists a method called execute in the class
                    Boolean methodExists = false;
                    for (Method method : methods) {
                        if ("execute".equals(method.getName())) {
                            methodExists = true;
                            break;
                        }
                    }

                    if (methodExists == false)
                        throw BusinessException.executeMethodNotFound();

                    // Call the AI Function call and execute method
                    Object implementationInstance = implementationClass.getDeclaredConstructor().newInstance();
                    Method executeMethod = implementationClass.getMethod("execute",
                            Object.class);
                    String executionResult = executeMethod.invoke(implementationInstance).toString();

                    System.out.println("result: " + executionResult);
                    updateBotInstanceResult(bot, executionResult);
                    updateBotInstanceStatus(bot, "S");

                    // 设置返回值
                    returnValue.setResult(executionResult);
                    System.out.println("test" + returnValue);
                    return returnValue;
                }
                case "C": {
                    List<PromptTexts> prompts = botType.getPrompts();
                    System.out.println("prompt: " + prompts);

                    // 如果有返回值，设置
                    returnValue.setResult("Custom bot result"); // 根据实际情况修改

                    return returnValue;
                }
                default: {
                    // 默认情况下的返回值
                    returnValue.setResult("Unsupported bot type: " + functionTypeCode);
                    return returnValue;
                }
            }

            // 如果switch没有返回，则返回一个空对象
            // return returnValue;

        } catch (Exception e) {
            // 更新状态为FAILED
            updateBotInstanceStatus(bot, "F");
            throw new BusinessException("Execution failed for bot: " + botInstanceId, e);
        }
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
            case "A": // AI Chat Bot
                return new ChatBot(botInstance, aiModel, botType, genericCqnService, promptService, aiModelResolver);
            case "F": // Function Calling Bot
                return new FunctionCallingBot(botInstance, aiModel, botType);
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
}
