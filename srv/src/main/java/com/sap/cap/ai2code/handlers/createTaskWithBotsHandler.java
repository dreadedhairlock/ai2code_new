package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.MainService_;

import java.util.UUID;

@Component
@ServiceName("MainService")
public class createTaskWithBotsHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @On(event = CreateTaskWithBotsContext.CDS_NAME)
    public void onCreateTaskWithBots(CreateTaskWithBotsContext context) {
        // Implement the logic to handle the creation of a task with bots
        // This method will be triggered when a CreateTaskWithBots event occurs
        // You can access the context to get details about the task being created
        System.out.println("Creating task with bots: "
                + context.getName() + ", "
                + context.getDescription() + ", "
                + context.getTypeId());
        
        // Add your business logic here
        Tasks task = Tasks.create();
        //task.setId(UUID.randomUUID().toString());
        task.setTypeId(context.getTypeId());
        task.setName(context.getName());
        task.setDescription(context.getDescription());
        task.setIsMain(true);

        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(task);
        Result result = db.run(insert);
        System.out.println(result.single(Tasks.class).toString());
        context.setResult(result.single(Tasks.class));
    
    }
/*
    @After(event = CreateTaskWithBotsContext.CDS_NAME)
    public void afterCreateTaskWithBots(CreateTaskWithBotsContext context) {

        // Implement the logic to handle the creation of a task with bots
        // This method will be triggered when a CreateTaskWithBots event occurs
        // You can access the context to get details about the task being created
        System.out.println("Setting up the task's ID: "
                + context.getName() + ", "
                + context.getDescription() + ", "
                + context.getTypeId());
        
        // Add your business logic here

        Tasks task = Tasks.create();
        //task.setId(UUID.randomUUID().toString());
        task.setTypeId(context.getTypeId());
        task.setName(context.getName());
        task.setDescription(context.getDescription());
        task.setIsMain(true);

        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(task);
        Result result = db.run(insert);
        System.out.println(result.single(Tasks.class).toString());
        context.setResult(result.single(Tasks.class));

    }
*/

}
