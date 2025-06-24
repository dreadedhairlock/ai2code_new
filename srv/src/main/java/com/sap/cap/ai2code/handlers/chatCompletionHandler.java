package com.sap.cap.ai2code.handlers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.ai2code.service.bot.BotService;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.PromptText_;

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
