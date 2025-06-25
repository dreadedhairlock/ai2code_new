package com.sap.cap.ai2code.service.common;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.MainService;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;

/**
 * Generic CQN Service providing domain-specific data access operations for
 * ai2code entities. Handles complex business queries and entity relationships.
 */
@Service
public class GenericCqnService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;

    public GenericCqnService(
            EntityService entityService,
            MainService mainService,
            ConfigService configService) {
        this.entityService = entityService;
        this.mainService = mainService;
        this.configService = configService;
    }

    // ===== MODEL CONFIG OPERATIONS =====
    /**
     * Get model configuration by ID
     */
    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(configService, select, ModelConfigs.class,
                "ModelConfig not found: " + modelConfigId);
    }

    // ===== BOT TYPE OPERATIONS =====
    /**
     * Get bot type by ID
     */
    public BotTypes getBotTypeById(String typeId) {
        CqnSelect select = Select.from(BotTypes_.class)
                .where(b -> b.ID().eq(typeId));
        return entityService.selectSingle(configService, select, BotTypes.class,
                "BotType not found: " + typeId);
    }

    /**
     * Get all bot types for a task type, ordered by sequence
     */
    public List<BotTypes> getBotTypesByTaskType(String taskTypeId) {
        CqnSelect select = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskTypeId))
                .orderBy(b -> b.sequence().asc());
        return entityService.selectList(configService, select, BotTypes.class);
    }

    /**
     * Get all prompt texts for a bot type
     */
    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        CqnSelect select = Select.from(PromptTexts_.class)
                .where(p -> p.botType_ID().eq(botTypeId));
        return entityService.selectList(configService, select, PromptTexts.class);
    }

    // ===== TASK OPERATIONS =====
    /**
     * Get task by ID
     */
    public Tasks getTaskById(String taskId) {
        CqnSelect select = Select.from(Tasks_.class)
                .where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
    }

    /**
     * Get task by bot instance and sequence
     */
    public Tasks getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        CqnSelect select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId)
                        .and(t.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);
    }

    /**
     * Get task by bot instance ID
     */
    public Tasks getTaskByBotInstance(String botInstanceId) {
        CqnSelect select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId);
    }

    /**
     * Create and insert main task
     */
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

    /**
     * Create and insert sub task
     */
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

    // ===== BOT INSTANCE OPERATIONS =====
    /**
     * Extracts the ID value from the CQN (Core Query Notation) string
     * representation
     */
    public String extractBotInstanceIdFromContext(BotInstancesChatCompletionContext context) {
        String result = context.getCqn().toString();
        Pattern pattern = Pattern.compile("\"val\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(result);

        if (matcher.find()) {
            String botInstanceId = matcher.group(1);
            return botInstanceId;
        } else {
            // Log the CQN string to help debug the pattern
            throw new BusinessException("Could not extract bot instance ID from CQN: " + result);
        }
    }

    /**
     * Get bot instance by ID
     */
    public BotInstances getBotInstanceById(String botInstanceId) {
        CqnSelect select = Select.from(BotInstances_.class)
                .where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);
    }

    /**
     * Get bot instance by task and sequence
     */
    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        CqnSelect select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId)
                        .and(b.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);
    }

    /**
     * Create and insert bot instance
     */
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

    /**
     * Update bot instance status
     */
    public void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        botInstance.setModifiedAt(Instant.now());
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    /**
     * Update bot instance result
     */
    public void updateBotInstanceResult(String botInstanceId, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setResult(result);
        botInstance.setModifiedAt(Instant.now());
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    /**
     * Update bot instance status and result
     */
    public void updateBotInstanceStatusAndResult(String botInstanceId, String statusCode, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        botInstance.setResult(result);
        botInstance.setModifiedAt(Instant.now());
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    /**
     * Get output context path by bot instance ID
     */
    public String getOutputContextPathByBotInstanceId(String botInstanceId) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        BotTypes botType = getBotTypeById(botInstance.getTypeId());
        return botType.getOutputContextPath();
    }

    // ===== BOT MESSAGE OPERATIONS =====
    /**
     * Get all bot messages by bot instance ID, ordered by creation time
     */
    public List<BotMessages> getBotMessagesByBotInstanceId(String botInstanceId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .where(b -> b.botInstance_ID().eq(botInstanceId))
                .orderBy(b -> b.createdAt().asc());
        return entityService.selectList(mainService, select, BotMessages.class);
    }

    /**
     * Get message by ID and bot instance ID
     */
    public BotMessages getMessageById(String botInstanceId, String messageId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .where(m -> m.ID().eq(messageId)
                        .and(m.botInstance_ID().eq(botInstanceId)));
        return entityService.selectSingle(mainService, select, BotMessages.class,
                String.format("Message %s not found in bot instance %s", messageId, botInstanceId));
    }

    /**
     * Get bot instance ID by message ID
     */
    public String getBotInstanceIdByMessageId(String messageId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .columns(m -> m.botInstance_ID())
                .where(m -> m.ID().eq(messageId));

        BotMessages message = entityService.selectSingle(mainService, select, BotMessages.class,
                "Message not found: " + messageId);

        return message != null ? message.getBotInstanceId() : null;
    }

    /**
     * Create and insert bot message with specified role
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
     * Check if this is the first call for a bot instance (no previous messages)
     */
    public boolean isFirstCall(String botInstanceId) {
        try {
            List<BotMessages> messages = getBotMessagesByBotInstanceId(botInstanceId);
            return messages == null || messages.isEmpty();
        } catch (Exception e) {
            // If query fails, assume it's the first call
            return true;
        }
    }

    // ===== CONTEXT NODE OPERATIONS =====
    /**
     * Get context node by ID
     */
    public ContextNodes getContextNodeById(String contextNodeId) {
        CqnSelect select = Select.from(ContextNodes_.class)
                .where(c -> c.ID().eq(contextNodeId));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found: " + contextNodeId);
    }

    /**
     * Get context node by task and path
     */
    public ContextNodes getContextNodeByTaskAndPath(String taskId, String contextPath) {
        CqnSelect select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId)
                        .and(c.path().eq(contextPath)));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found for task: " + taskId + ", path: " + contextPath);
    }

    /**
     * Get context node by task and path (returns Optional)
     */
    public Optional<ContextNodes> getContextNodeByTaskAndPathOptional(String taskId, String contextPath) {
        CqnSelect select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId)
                        .and(c.path().eq(contextPath)));
        return entityService.selectSingleOptional(mainService, select, ContextNodes.class);
    }

    /**
     * Create and insert context node
     */
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

    /**
     * Update context node value
     */
    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(Instant.now());
        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    /**
     * Update context node with complete information
     */
    public ContextNodes updateContextNode(ContextNodes existingNode, String label, String type, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setLabel(label);
        existingNode.setType(type);
        existingNode.setModifiedAt(Instant.now());
        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // ===== BUSINESS LOGIC OPERATIONS =====
    /**
     * Find the main task ID by traversing the task hierarchy from a bot
     * instance
     */
    public String getMainTaskId(String botInstanceId) {
        // 1. Get the bot instance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. Get current task ID
        String currentTaskId = botInstance.getTaskId();

        // 3. Traverse up the hierarchy until we find the main task
        while (currentTaskId != null) {
            Tasks currentTask = getTaskById(currentTaskId);

            // 4. If this is the main task, return its ID
            if (currentTask.getIsMain() != null && currentTask.getIsMain()) {
                return currentTaskId;
            }

            // 5. If not main task, find parent task through bot instance
            String parentBotInstanceId = currentTask.getBotInstanceId();

            if (parentBotInstanceId == null || parentBotInstanceId.isEmpty()) {
                // Top-level task but not marked as main - data inconsistency
                throw new BusinessException(
                        "Found top-level task but it's not marked as main task: " + currentTaskId);
            }

            // 6. Get parent bot instance's task ID
            BotInstances parentBotInstance = getBotInstanceById(parentBotInstanceId);
            currentTaskId = parentBotInstance.getTaskId();
        }

        // If we traversed everything and found no main task
        throw new BusinessException("Main task not found for botInstanceId: " + botInstanceId);
    }

    /**
     * Get task ID by bot instance ID
     */
    public String getTaskIdByBotInstanceId(String botInstanceId) {
        CqnSelect select = Select.from(BotInstances_.class)
                .columns(b -> b.task_ID())
                .where(b -> b.ID().eq(botInstanceId));

        BotInstances instance = entityService.selectSingle(mainService, select, BotInstances.class,
                String.format("Bot instance not found: %s", botInstanceId));

        return instance.getTaskId();
    }

    /**
     * Get parent task by bot instance
     */
    public Tasks getParentTaskByBotInstance(String botInstanceId) {
        try {
            String taskId = getTaskIdByBotInstanceId(botInstanceId);
            return getTaskById(taskId);
        } catch (Exception e) {
            throw new BusinessException("Failed to get parent task for botInstance: " + botInstanceId, e);
        }
    }

    // ===== UTILITY METHODS =====
    /**
     * Generate a friendly label from a context path
     */
    public String generateLabelFromPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "Context";
        }

        // Convert path to friendly label
        String[] parts = contextPath.split("\\.");
        String lastPart = parts[parts.length - 1];

        // Convert camelCase to readable format
        String label = lastPart.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ")
                .replaceAll("-", " ")
                .toLowerCase();

        // Capitalize the first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : label.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
