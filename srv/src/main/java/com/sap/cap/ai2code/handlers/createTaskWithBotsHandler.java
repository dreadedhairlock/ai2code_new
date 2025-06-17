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
import cds.gen.configservice.TaskTypes_;
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

        System.out.println("Preparing to create task with bots: "
                + context.getName() + ", "
                + context.getDescription() + ", "
                + context.getTypeId());
        
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
        // Implement the logic to handle the creation of a task with bots
        // This method will be triggered when a CreateTaskWithBots event occurs
        // You can access the context to get details about the task being created
        System.out.println("Creating task with bots: "
                + context.getName() + ", "
                + context.getDescription() + ", "
                + context.getTypeId());
        
        // 创建一条目MainService.Tasks。
        Tasks task = Tasks.create();
        // Type 设置为typeId查找到的TaskType
        task.setTypeId(context.getTypeId());
        // Name
        task.setName(context.getName());
        // Description
        task.setDescription(context.getDescription());
        // 将isMain设置为true
        task.setIsMain(true);
        // contextPath设置为空
        task.setContextPath(null);
        // sequence设置为空或0
        task.setSequence(0);

        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(task);
        Result result = db.run(insert);
        context.setResult(result.single(Tasks.class));
    }

    @After(event = CreateTaskWithBotsContext.CDS_NAME)
    public void afterCreateTaskWithBots(CreateTaskWithBotsContext context) {
        // Implement any post-processing logic after creating a task with bots
        // This method will be triggered after the CreateTaskWithBots event occurs
        // You can log the created task or perform additional actions here
        System.out.println("Task created successfully: "
                + context.getResult().getName() + ", "
                + context.getResult().getDescription() + ", "
                + context.getResult().getTypeId());
        String TaskID = context.getResult().getId();
        CqnSelect select = Select.from(BOT_TYPES).where(b -> b.taskType_ID().eq(context.getResult().getTypeId()));
        Result selectResult = db.run(select);
        System.out.println("Bot types: " + selectResult.toJson());
        // 根据ConfigService.TaskTypes.botTypes的条目数，创建相应条目数的MainService.BotInstances。
        selectResult.stream().forEach(
            row -> {
                BotInstances botInstance = BotInstances.create();
                // Sequence 设置为 ConfigService.BotTypes.sequence
                botInstance.setSequence(row.getPath("sequence"));
                // Type 设置为ConfigService.BotTypes
                botInstance.setTypeId(row.getPath("ID"));
                // status设置为BotInstanceStatus.code.C (Created)
                botInstance.setStatusCode("C");
                botInstance.setTaskId(TaskID);
                CqnInsert insertBot = Insert.into(BotInstances_.CDS_NAME).entry(botInstance);
                Result insertBotResult = db.run(insertBot);
            }
        );

        // 将description和第二步得到的Tasks.Id，创建一条MainService.ContextNodes。
            ContextNodes contextNode = ContextNodes.create();
            contextNode.setTaskId(TaskID);
            contextNode.setPath(context.getResult().getDescription());
            contextNode.setLabel(context.getResult().getDescription());
            contextNode.setValue(context.getResult().getDescription());
            CqnInsert insertContextNode = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
            Result insertBotResult = db.run(insertContextNode);
    }

}
