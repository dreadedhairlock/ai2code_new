package com.sap.cap.ai2code.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.model.Bot;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessagesAdoptContext;

public interface BotService {
    public Bot getCurrentBot(String botInstanceId);
    public Bot getCurrentBot(String taskId, int sequence);

    public String chat(BotInstancesChatCompletionContext context);
    public String chat(String botInstanceId, String content);

    public SseEmitter chatInStreaming(String botInstanceId, String content);

    public Boolean executeAsync(BotInstancesExecuteContext context);
    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);

    public void adopt(BotMessagesAdoptContext context);
    public void adopt(String botInstanceId, String messageId);
}
