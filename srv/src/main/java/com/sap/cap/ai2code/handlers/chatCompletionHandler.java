package com.sap.cap.ai2code.handlers;

import org.springframework.stereotype.Component;

import com.sap.cap.ai2code.service.bot.BotService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstances_;

@Component
@ServiceName("MainService")
public class chatCompletionHandler implements EventHandler {

    private final BotService botService;

    // Constructor
    public chatCompletionHandler(BotService botService) {
        this.botService = botService;
    }

    @On(entity = BotInstances_.CDS_NAME, event = BotInstancesChatCompletionContext.CDS_NAME)
    public void handleChatCompletion(BotInstancesChatCompletionContext context) {
        context.setResult(botService.chat(context));
    }
}
