package com.sap.cap.ai2code.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.model.Task;
import com.sap.cap.ai2code.service.TaskService;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import static cds.gen.configservice.ConfigService_.BOT_TYPES;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.TaskTypes_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private PersistenceService db;

    @Autowired
    private GenericCqnService genericCqnService;

    @Override
    public Task createTaskWithBots(CreateTaskWithBotsContext context) {
        // Validate task type exists
        validateTaskType(context.getTypeId());
        
        // Create main task
        Tasks createdTask = createMainTask(context.getName(), context.getDescription(), context.getTypeId());
        
        // Create bot instances and context nodes
        createBotInstancesForTask(createdTask);
        createInitialContextNode(createdTask);
        
        return convertToTaskModel(createdTask);
    }

    @Override
    public Task createTaskWithBots(String name, String description, String taskTypeId) {
        // Validate task type exists
        validateTaskType(taskTypeId);
        
        // Create main task
        Tasks createdTask = createMainTask(name, description, taskTypeId);
        
        // Create bot instances and context nodes
        createBotInstancesForTask(createdTask);
        createInitialContextNode(createdTask);
        
        return convertToTaskModel(createdTask);
    }

    @Override
    public Task createTaskWithBots(String botInstanceId, String name, String description, String contextPath, int sequence) {
        // Get the bot instance to determine task type
        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);
        String taskTypeId = getBotInstanceTaskTypeId(botInstance);
        
        // Create sub task
        Tasks createdTask = createSubTask(name, description, contextPath, sequence, botInstanceId, taskTypeId);
        
        return convertToTaskModel(createdTask);
    }

    @Override
    public Task createTaskWithBots(String botInstanceId, String name) {
        // Get the bot instance to determine context
        BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);
        String taskTypeId = getBotInstanceTaskTypeId(botInstance);
        
        // Create sub task with default values
        String description = "Generated task for " + name;
        String contextPath = "generated.task." + System.currentTimeMillis();
        int sequence = getNextSequenceForBotInstance(botInstanceId);
        
        Tasks createdTask = createSubTask(name, description, contextPath, sequence, botInstanceId, taskTypeId);
        
        return convertToTaskModel(createdTask);
    }

    @Override
    public Task getCurrentTask(String taskId) {
        Tasks task = genericCqnService.getTaskById(taskId);
        return convertToTaskModel(task);
    }

    @Override
    public Task getCurrentTask(String botInstanceId, int sequence) {
        Tasks task = genericCqnService.getTaskByBotInstanceAndSequence(botInstanceId, sequence);
        return convertToTaskModel(task);
    }

    // Private helper methods

    private void validateTaskType(String typeId) {
        CqnSelect select = Select.from(TaskTypes_.CDS_NAME)
                .columns(TaskTypes_.ID)
                .where(t -> t.get("ID").eq(typeId));
        Result result = db.run(select);

        if (result.rowCount() == 0) {
            throw new IllegalArgumentException("Task type with ID " + typeId + " not found.");
        }
    }

    private Tasks createMainTask(String name, String description, String taskTypeId) {
        Tasks task = Tasks.create();
        
        System.out.println("Creating main task: " + name + ", " + description + ", " + taskTypeId);
        
        // Set task properties
        task.setTypeId(taskTypeId);
        task.setName(name);
        task.setDescription(description);
        task.setIsMain(true);
        task.setContextPath(null);
        task.setSequence(0);

        // Insert task
        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(task);
        Result result = db.run(insert);

        Tasks createdTask = result.single(Tasks.class);
        System.out.println("Main task created successfully: " + createdTask.getName() + 
                          ", ID: " + createdTask.getId());
        
        return createdTask;
    }

    private Tasks createSubTask(String name, String description, String contextPath, int sequence, String botInstanceId, String taskTypeId) {
        Tasks task = genericCqnService.createAndInsertSubTask(name, description, contextPath, sequence, botInstanceId, taskTypeId);
        
        System.out.println("Sub task created successfully: " + task.getName() + 
                          ", ID: " + task.getId() + ", BotInstance: " + botInstanceId);
        
        return task;
    }

    private void createBotInstancesForTask(Tasks task) {
        String taskId = task.getId();
        
        // Get Bot Types for this task type
        CqnSelect select = Select.from(BOT_TYPES)
                .where(b -> b.taskType_ID().eq(task.getTypeId()));
        Result selectResult = db.run(select);
        
        System.out.println("Bot types for task " + taskId + ": " + selectResult.toJson());

        // Create bot instances for each bot type
        selectResult.stream().forEach(row -> {
            BotInstances botInstance = BotInstances.create();
            
            // Set bot instance properties
            botInstance.setSequence(row.getPath("sequence"));
            botInstance.setTypeId(row.getPath("ID")); 
            botInstance.setStatusCode("C"); // Created status
            botInstance.setTaskId(taskId);
            
            // Insert bot instance
            CqnInsert insertBot = Insert.into(BotInstances_.CDS_NAME).entry(botInstance);
            Result insertBotResult = db.run(insertBot);
            
            System.out.println("BotInstance created: " + insertBotResult.toJson());
        });
    }

    private void createInitialContextNode(Tasks task) {
        String taskId = task.getId();
        
        // Create initial context node for task description
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setTaskId(taskId);
        contextNode.setPath("report.requirement");
        contextNode.setLabel("Task Description");
        contextNode.setType("String");
        contextNode.setValue(task.getDescription());

        CqnInsert insertContextNode = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
        Result insertContextNodeResult = db.run(insertContextNode);
        
        System.out.println("ContextNode created: " + insertContextNodeResult.toJson());
    }

    private String getBotInstanceTaskTypeId(BotInstances botInstance) {
        // Get the task associated with this bot instance
        Tasks task = genericCqnService.getTaskById(botInstance.getTaskId());
        return task.getTypeId();
    }

    private int getNextSequenceForBotInstance(String botInstanceId) {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    private Task convertToTaskModel(Tasks cdsTask) {
        if (cdsTask == null) {
            return null;
        }
        
        // Create Task implementation that wraps the CDS Tasks entity
        return new TaskImpl(cdsTask);
    }
    
    // Inner class implementing the Task interface
    private static class TaskImpl implements Task {
        private final Tasks cdsTask;
        
        public TaskImpl(Tasks cdsTask) {
            this.cdsTask = cdsTask;
        }
        
        @Override
        public Tasks getTask() {
            return cdsTask;
        }
    }
}