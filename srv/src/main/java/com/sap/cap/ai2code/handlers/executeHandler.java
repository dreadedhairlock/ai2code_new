package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.ai.orchestration.BotInstance;
import cds.gen.mainservice.BotInstancesExecuteContext;

public class executeHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Before(event = BotInstancesExecuteContext.CDS_NAME)
    public void beforeExecute(BotInstancesExecuteContext context, BotInstance botInstance) {
        
    }

    @On(event = BotInstancesExecuteContext.CDS_NAME)
    public void onExecute(BotInstancesExecuteContext context) {
        System.out.println("Executing bot instance with context: " + context.toString());
    }

    @After(event = BotInstancesExecuteContext.CDS_NAME)
    public void afterExecute(BotInstancesExecuteContext context) {

    }

}
