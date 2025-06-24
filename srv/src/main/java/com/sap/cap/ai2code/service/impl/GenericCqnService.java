package com.sap.cap.ai2code.service.impl;

import org.springframework.stereotype.Service;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;

import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;

import java.util.List;
import java.util.UUID;

import com.sap.cap.ai2code.exception.BusinessException;

@Service
public class GenericCqnService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;

    private final TaskBotCacheManager cacheManager;

    public GenericCqnService(
            EntityService entityService,
            MainService mainService,
            ConfigService configService,
            TaskBotCacheManager cacheManager) {
        this.entityService = entityService;
        this.mainService = mainService;
        this.configService = configService;
        this.cacheManager = cacheManager;
    }

    // 原有查询方法...
    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(configService, select, ModelConfigs.class,
                "ModelConfig not found: " + modelConfigId);
    }

    public BotInstances getBotInstanceById(String botInstanceId) {
        var select = Select.from(BotInstances_.class).where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);
    }

    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        var select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId).and(b.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);
    }

    public BotTypes getBotTypeById(String typeId) {
        var select = Select.from(BotTypes_.class).where(b -> b.ID().eq(typeId));
        return entityService.selectSingle(configService, select, BotTypes.class,
                "BotType not found: " + typeId);
    }

    public List<BotTypes> getBotTypesByTaskType(String taskTypeId) {
        var select = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskTypeId))
                .orderBy(b -> b.sequence().asc());
        return entityService.selectList(configService, select, BotTypes.class);
    }

    public Tasks getTaskById(String taskId) {
        var select = Select.from(Tasks_.class).where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
    }

    public Tasks getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId).and(t.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);
    }

    // 根据BotInstance ID获取任务详情（33160）
    public Tasks getTaskByBotInstance(String botInstanceId) {
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId);
    }

    // 创建和插入方法 - 包含业务逻辑
    public Tasks createAndInsertMainTask(String name, String description, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(true);
        newTask.setContextPath("");
        newTask.setSequence(0);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    public Tasks createAndInsertSubTask(String name, String description, String contextPath,
            int sequence, String botInstanceId, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(false); // 子任务
        newTask.setContextPath(contextPath);
        newTask.setSequence(sequence);
        newTask.setBotInstanceId(botInstanceId);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    public BotInstances createAndInsertBotInstance(String taskId, BotTypes botType) {
        BotInstances botInstance = BotInstances.create();
        botInstance.setId(UUID.randomUUID().toString());
        botInstance.setSequence(botType.getSequence());
        botInstance.setTypeId(botType.getId());
        botInstance.setStatusCode("C"); // Created
        botInstance.setTaskId(taskId);

        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
        return botInstance;
    }

    public ContextNodes createAndInsertContextNode(String taskId, String path, String label,
            String type, String value) {
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setId(UUID.randomUUID().toString());
        contextNode.setTaskId(taskId);
        contextNode.setPath(path);
        contextNode.setLabel(label);
        contextNode.setType(type);
        contextNode.setValue(value);

        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
        return contextNode;
    }

    // 更新方法
    public void updateBotInstance(BotInstances botInstance) {
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    public void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceResult(String botInstanceId, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceStatusAndResult(String botInstanceId, String statusCode, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    public ContextNodes getContextNodeById(String contextNodeId) {
        var select = Select.from(ContextNodes_.class).where(c -> c.ID().eq(contextNodeId));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found: " + contextNodeId);
    }

    // 修改为根据taskId和contextPath查询
    public ContextNodes getContextNodeByTaskAndPath(String taskId, String contextPath) {
        var select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId).and(c.path().eq(contextPath)));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found for task: " + taskId + ", path: " + contextPath);
    }

    // 更新ContextNode的业务方法
    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // 更新ContextNode的完整信息
    public ContextNodes updateContextNode(ContextNodes existingNode, String label, String type, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setLabel(label);
        existingNode.setType(type);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // 根据path生成友好的label
    public String generateLabelFromPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "Context";
        }

        // 将路径转换为友好的标签
        String[] parts = contextPath.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 将驼峰命名转换为可读格式
        return lastPart.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ")
                .replaceAll("-", " ")
                .toLowerCase()
                .replaceAll("\\b\\w", "");
    }

    // public void insertContextNode(ContextNodes contextNode) {
    // entityService.insert(mainService, null, ContextNodes_.class, contextNode,
    // true);
    // }

    // 原有的简单插入方法保留，但标记为内部使用
    private void insertTask(Tasks task) {
        entityService.insert(mainService, null, Tasks_.class, task, true);
    }

    private void insertBotInstance(BotInstances botInstance) {
        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
    }

    private void insertContextNode(ContextNodes contextNode) {
        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
    }

    /**
     * 根据BotType ID查询所有PromptTexts
     */
    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        var select = Select.from(PromptTexts_.class)
                .where(p -> p.botType_ID().eq(botTypeId));
        // .orderBy(p -> p.sequence().asc());
        return entityService.selectList(configService, select, PromptTexts.class);
    }

    /**
     * 根据path查询ContextNode（不指定taskId）
     */
    // public ContextNodes getContextNodeByPath(String contextPath) {
    // var select = Select.from(ContextNodes_.class)
    // .where(c -> c.path().eq(contextPath));
    // return entityService.selectSingle(mainService, select, ContextNodes.class,
    // "ContextNode not found for path: " + contextPath);
    // }

    /**
     * 根据botInstanceId获取主任务ID
     */
    public String getMainTaskId(String botInstanceId) {
        // 使用缓存管理器获取主任务ID
        // String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
        // if (mainTaskId != null) {
        //     return mainTaskId;
        // }

        // 如果缓存中没有，执行原有逻辑
        // 1. 通过botInstanceId获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取当前任务ID
        String currentTaskId = botInstance.getTaskId();

        // 3. 循环查找，直到找到主任务
        while (currentTaskId != null) {
            Tasks currentTask = getTaskById(currentTaskId);

            // 4. 如果是主任务，返回该任务ID
            if (currentTask.getIsMain() != null && currentTask.getIsMain()) {
                return currentTaskId;
            }

            // 5. 如果不是主任务，通过botInstanceId找到父任务
            String parentBotInstanceId = currentTask.getBotInstanceId();

            if (parentBotInstanceId == null || parentBotInstanceId.isEmpty()) {
                // 如果没有父BotInstance，说明这可能就是顶层任务
                // 但不是主任务，这种情况可能是数据错误
                throw new BusinessException(
                        "Found top-level task but it's not marked as main task: " + currentTaskId);
            }

            // 6. 获取父BotInstance的任务ID
            BotInstances parentBotInstance = getBotInstanceById(parentBotInstanceId);
            currentTaskId = parentBotInstance.getTaskId();
        }

        // 如果遍历完还没找到主任务，抛出异常
        throw new BusinessException("Main task not found for botInstanceId: " + botInstanceId);
    }

    /**
     * 创建并插入单条BotMessage（指定角色）
     */
    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRole(role);

        entityService.insert(mainService, null, BotMessages_.class, botMessage, true);
        return botMessage;
    }

    /**
     * 判断是否是第一次调用
     */
    public boolean isFirstCall(String botInstanceId) {
        try {
            // 查询该Bot实例是否有历史消息
            List<BotMessages> messages = getBotMessagesByBotInstanceId(botInstanceId);
            return messages == null || messages.isEmpty();
        } catch (Exception e) {
            // 如果查询失败，默认认为是第一次调用
            return true;
        }
    }

    /**
     * 根据BotInstance ID查询所有BotMessages
     */
    public List<BotMessages> getBotMessagesByBotInstanceId(String botInstanceId) {
        var select = Select.from(BotMessages_.class)
                .where(b -> b.botInstance_ID().eq(botInstanceId))
                .orderBy(b -> b.createdAt().asc());
        return entityService.selectList(mainService, select, BotMessages.class);
    }

    /**
     * Retrieve BotInstanceID based on BotMessageID
     */
    public String getBotInstanceIdByMessageId(String messageId) {
        CqnSelect selectBotInstance = Select.from(BotMessages_.class)
                .columns(BotInstances_.ID, BotInstances_.TYPE_ID, BotInstances_.TASK_ID)
                .where(m -> m.ID().eq(messageId));

        BotMessages message = entityService.selectSingle(
                mainService,
                selectBotInstance,
                BotMessages.class,
                "Message not found: " + messageId // 添加错误消息参数
        );

        return message != null ? message.getBotInstanceId() : null;
    }

    /**
     * Get Message based on BotInstanceID and MessageID
     */
    public BotMessages getMessageById(String botInstanceId, String messageId) {
        CqnSelect selectMessage = Select.from(BotMessages_.class)
                        .where(m -> m.ID().eq(messageId)
                        .and(m.botInstance_ID().eq(botInstanceId)));
        return entityService.selectSingle(
                mainService,
                selectMessage,
                BotMessages.class,
                String.format("Message %s not found in bot instance %s", messageId, botInstanceId));
    }

    /**
     * 根据BotInstance ID获取输出上下文路径
     */
    public String getOutputContextPathByBotInstanceId(String botInstanceId) {
        // 1. 获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取关联的BotType
        BotTypes botType = getBotTypeById(botInstance.getTypeId());

        // 3. 返回配置的输出路径
        return botType.getOutputContextPath();
    }

    public String getTaskIdByBotInstanceId(String botInstanceId) {
        // 构建查询语句
        CqnSelect select = Select.from(BotInstances_.class)
                .columns(b -> b.task_ID())
                .where(b -> b.ID().eq(botInstanceId));

        // 查询结果列表
        BotInstances instance = entityService.selectSingle(mainService, select, BotInstances.class,String.format("Parent task not found for bot instance %s", botInstanceId));

        // 检查并返回结果
        // if (instance.isEmpty() || instances.get(0).getTaskId() == null) {
        //     throw new IllegalStateException("No taskId found for botInstanceId: " + botInstanceId);
        // }

        return instance.getTaskId();
    }

    /**
     * 根据BotInstance ID获取父任务详情
     * 这个方法用于获取创建当前BotInstance的父任务（即包含该BotInstance的任务）
     */
    public Tasks getParentTaskByBotInstance(String botInstanceId) {
        try {
            // 1. 先获取当前BotInstance
            // BotInstances botInstance = getBotInstanceById(botInstanceId);

            // 2. 获取BotInstance所属的任务
            String taskId = getTaskIdByBotInstanceId(botInstanceId);

            // 3. 获取该任务
            Tasks currentTask = getTaskById(taskId);

            // 4. 如果当前任务有父任务ID，则获取父任务
            // if (currentTask.getParentTaskId() != null && !currentTask.getParentTaskId().isEmpty()) {
            //     return getTaskById(currentTask.getParentTaskId());
            // } else {
            //     // 5. 如果没有父任务，返回当前任务本身
            //     return currentTask;
            // }
            return currentTask;

        } catch (Exception e) {
            throw new BusinessException("Failed to get parent task for botInstance: " + botInstanceId, e);
        }
    }

}
