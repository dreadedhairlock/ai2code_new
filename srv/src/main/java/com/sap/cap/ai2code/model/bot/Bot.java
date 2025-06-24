package com.sap.cap.ai2code.model.bot;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;

public interface Bot {

    public BotInstancesExecuteContext.ReturnType execute();

    public Boolean executeAsync();

    public Boolean stop();

    public Boolean resume();

    public Boolean cancel();

    public SseEmitter chatInStreaming(String content);

    public String chat(String content);

    public BotInstances getBotInstance();


    // public AIModel getAIModel();
}
