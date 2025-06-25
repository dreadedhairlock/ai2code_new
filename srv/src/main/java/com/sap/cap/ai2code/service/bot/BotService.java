package com.sap.cap.ai2code.service.bot;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.model.bot.Bot;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;

public interface BotService {

    public Bot getCurrentBot(String botInstanceId);

    public Bot getCurrentBot(String taskId, int sequence);

    public BotMessages chat(BotInstancesChatCompletionContext context);

    public BotMessages chat(String botInstanceId, String content);

    public SseEmitter chatInStreaming(String botInstanceId, String content);

    public Boolean executeAsync(BotInstancesExecuteContext context);

    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);

    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);

    public ContextNodes adopt(BotMessagesAdoptContext context);

    public ContextNodes adopt(String botInstanceId, String messageId);
}
