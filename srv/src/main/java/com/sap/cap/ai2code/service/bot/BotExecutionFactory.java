package com.sap.cap.ai2code.service.bot;

@FunctionalInterface
public interface BotExecutionFactory {
    public BotExecution create(String botName, String description, String version, boolean enabled);
}
