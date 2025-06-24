package com.sap.cap.ai2code.service.task;

import com.sap.cap.ai2code.model.task.Task;

import cds.gen.mainservice.CreateTaskWithBotsContext;

public interface TaskService {

    public Task createTaskWithBots(CreateTaskWithBotsContext context);

    public Task createTaskWithBots(String name, String description, String taskTypeId);

    public Task createTaskWithBots(String botInstanceId, String name, String description, String contextPath, int sequence);

    public Task createTaskWithBots(String botInstanceId, String name);

    public Task getCurrentTask(String taskId);

    public Task getCurrentTask(String botInstanceId, int sequence);
}
