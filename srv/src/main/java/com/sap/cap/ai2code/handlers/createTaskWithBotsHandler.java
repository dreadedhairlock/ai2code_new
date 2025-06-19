package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import static cds.gen.configservice.ConfigService_.BOT_TYPES;
import cds.gen.mainservice.TaskTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;

@Component
@ServiceName("MainService")
public class createTaskWithBotsHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Before(event = CreateTaskWithBotsContext.CDS_NAME)
    public void beforeCreateTaskWithBots(CreateTaskWithBotsContext context) {
        // 1. Query the entries in the TaskTypes table in ConfigService and its botTypes according to typeId
        CqnSelect select = Select.from(TaskTypes_.CDS_NAME)
                            .columns(TaskTypes_.ID)
                            .where(t -> t.get("ID").eq(context.getTypeId()));
        Result result = db.run(select);

        if (result.rowCount() == 0) {
            throw new IllegalArgumentException("Task type with ID " + context.getTypeId() + " not found.");
        }
    }

    @On(event = CreateTaskWithBotsContext.CDS_NAME)
    public void onCreateTaskWithBots(CreateTaskWithBotsContext context) {
        // 2. Create an entry MainService.Tasks.
        Tasks task = Tasks.create();

        System.out.println("Creating task with bots: "
                            + context.getName() + ", "
                            + context.getDescription() + ", "
                            + context.getTypeId()); 

        // Type is set to the TaskType found by typeId
        task.setTypeId(context.getTypeId());
        // Name
        task.setName(context.getName());
        // Description
        task.setDescription(context.getDescription());
        // Set isMain to true 
        task.setIsMain(true);
        // Set contextPath to empty
        task.setContextPath(null);
        // Set the sequence to empty or 0
        task.setSequence(0);

        // insert task
        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(task);
        Result result = db.run(insert);

        // 5. Return the created Tasks entry to the front end
        context.setResult(result.single(Tasks.class));

        System.out.println("Task created successfully: "
        + context.getResult().getName() + ", "
        + context.getResult().getDescription() + ", "
        + context.getResult().getTypeId());
    }

    @After(event = CreateTaskWithBotsContext.CDS_NAME)
    public void afterCreateTaskWithBots(CreateTaskWithBotsContext context) {

        String TaskID = context.getResult().getId();
        // Get Type ID from Bot Types
        CqnSelect select = Select.from(BOT_TYPES)
                        .where(b -> b.taskType_ID()
                        .eq(context.getResult()
                        .getTypeId()));
        Result selectResult = db.run(select);
        System.out.println("Bot types: " + selectResult.toJson());


        // 3. Create the corresponding number of MainService.BotInstances 
        selectResult.stream().forEach(
            row -> {
                BotInstances botInstance = BotInstances.create();
                // Sequence is set to ConfigService.BotTypes.sequence
                botInstance.setSequence(row.getPath("sequence"));
                // Type is set to ConfigService.BotTypes
                botInstance.setTypeId(row.getPath("ID"));
                // status is set to BotInstanceStatus.code.C (Created)
                botInstance.setStatusCode("C");
                // taskID is set
                botInstance.setTaskId(TaskID);
                CqnInsert insertBot = Insert.into(BotInstances_.CDS_NAME)
                                    .entry(botInstance);
                Result insertBotResult = db.run(insertBot);
                System.out.println("BotInstance Result: " + insertBotResult);
            }
        );

        // 4. Create a new MainService.ContextNodes
            ContextNodes contextNode = ContextNodes.create();
            // task
            contextNode.setTaskId(TaskID);
            // path
            contextNode.setPath(context.getResult().getDescription());
            // label
            contextNode.setLabel(context.getResult().getDescription());
            // type
            contextNode.setType(context.getResult().getDescription());
            // value
            contextNode.setValue(context.getResult().getDescription());

            CqnInsert insertContextNode = Insert.into(ContextNodes_.CDS_NAME)
                                            .entry(contextNode);
            Result insertContextNodeResult = db.run(insertContextNode);
            System.out.println("ContextNode Result: " + insertContextNodeResult);
    }

}