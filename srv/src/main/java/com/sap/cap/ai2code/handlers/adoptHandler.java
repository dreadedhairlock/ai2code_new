package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotMessages_;

import com.sap.cap.ai2code.service.BotService;

@Component
@ServiceName("MainService")
public class adoptHandler implements EventHandler {

    @Autowired
    private BotService botService;

    @Before(event = BotMessagesAdoptContext.CDS_NAME, entity = BotMessages_.CDS_NAME)
    public void beforeAdopt(BotMessagesAdoptContext context) {
    }

     @On(event = BotMessagesAdoptContext.CDS_NAME, entity = BotMessages_.CDS_NAME)
    public void onAdopt(BotMessagesAdoptContext context) {
        botService.adopt(context);
    }

    @After(event = BotMessagesAdoptContext.CDS_NAME, entity = BotMessages_.CDS_NAME)
    public void afterAdopt(BotMessagesAdoptContext context) {

    }

}