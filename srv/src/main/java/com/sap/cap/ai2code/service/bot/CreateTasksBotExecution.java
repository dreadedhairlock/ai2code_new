package com.sap.cap.ai2code.service.bot;

import com.sap.cap.ai2code.service.bot.BotExecution;

import java.util.ArrayList;
import java.util.List;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.execution.TaskCreationParam;
import com.sap.cap.ai2code.model.execution.annotation.BotExecutor;
import com.sap.cap.ai2code.model.execution.annotation.ExecuteMethod;
import com.sap.cap.ai2code.model.execution.annotation.ExecuteParameter;
import com.sap.cap.ai2code.model.task.Task;
import com.sap.cap.ai2code.service.task.TaskService;

@BotExecutor(name = "Create Tasks Bot Execution", description = "Implementation for creating tasks in the bot execution framework", version = "1.0", enabled = true)
public class CreateTasksBotExecution implements BotExecution {

    // 任务服务实例
    private final TaskService taskService;

    public CreateTasksBotExecution(TaskService taskService) {
        // 默认构造函数
        this.taskService = taskService;
    }

    @ExecuteMethod
    public List<String> execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(name = "taskCreationParams", description = "Array of task parameters") List<TaskCreationParam> taskCreationParams
    /**
     * 1.
     * sequence
     * 2.name
     * 3.description
     * 4.
     * contextPath
     */
    ) {

        List<String> tasks = new ArrayList<>();
        // throw new BusinessException("Unimplemented method 'execute'");
        // 调用BotService.createTaskWithBots(param);
        if (taskCreationParams == null || taskCreationParams.isEmpty()) {
            throw new BusinessException("Task creation parameters cannot be null or empty");
        }
        if (botInstanceId == null || botInstanceId.isEmpty()) {
            throw new BusinessException("Bot instance ID cannot be null or empty");
        }

        // 遍历任务创建参数，创建任务
        for (TaskCreationParam param : taskCreationParams) {
            if (param.getSequence() == null || param.getName() == null || param.getDescription() == null) {
                throw new BusinessException("Task creation parameters must include sequence, name, and description");
            }
            // 调用任务服务创建任务
            tasks.add(taskService.createTaskWithBots(botInstanceId, param.getName(), param.getDescription(),
                    param.getContextPath(), param.getSequence()).getTask().getId());
        }
        return tasks;
    }
}