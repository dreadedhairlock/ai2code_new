package com.sap.cap.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.Task;
import com.sap.cap.ai2code.service.TaskService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.Tasks;

@Component
@ServiceName("MainService")
public class createTaskWithBotsHandler implements EventHandler {

    @Autowired
    private TaskService taskService;

    @Before(event = CreateTaskWithBotsContext.CDS_NAME)
    public void beforeCreateTaskWithBots(CreateTaskWithBotsContext context) {
        // Basic validation - the actual validation is now handled in the service layer
        if (context.getTypeId() == null || context.getTypeId().trim().isEmpty()) throw BusinessException.emptyTaskTypeID();
        if (context.getName() == null || context.getName().trim().isEmpty()) throw BusinessException.emptyTaskName();
        
        System.out.println("Before creating task with bots: " + context.getName());
    }

    @On(event = CreateTaskWithBotsContext.CDS_NAME)
    public void onCreateTaskWithBots(CreateTaskWithBotsContext context) {
        System.out.println("Processing createTaskWithBots request: " + 
                          context.getName() + ", " + context.getDescription() + ", " + context.getTypeId());
        
        // Delegate to the service layer
        Task createdTask = taskService.createTaskWithBots(context);
        
        // Convert Task model back to CDS Tasks entity for the context result
        Tasks cdsTask = convertToTasks(createdTask);
        context.setResult(cdsTask);
        
        // System.out.println("Task creation completed successfully: " + 
        //                   createdTask.getName() + ", ID: " + createdTask.getId());
    }

    /**
     * Convert Task model to CDS Tasks entity
     */
    private Tasks convertToTasks(Task task) {
        // Since Task interface wraps Tasks entity, just return the wrapped entity
        return task.getTask();
    }
}