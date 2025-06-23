package com.sap.cap.ai2code.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;

// import com.sap.cap.ai2code.exception.BusinessException;

@Service
public class GenericCqnService {

    @Autowired
    private PersistenceService db;

    // Query methods for configuration entities
    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.CDS_NAME)
                .where(m -> m.get("ID").eq(modelConfigId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("ModelConfig not found: " + modelConfigId);
        }
        return result.single(ModelConfigs.class);
    }

    public BotTypes getBotTypeById(String typeId) {
        CqnSelect select = Select.from(BotTypes_.CDS_NAME)
                .where(b -> b.get("ID").eq(typeId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("BotType not found: " + typeId);
        }
        return result.single(BotTypes.class);
    }

    public List<BotTypes> getBotTypesByTaskType(String taskTypeId) {
        CqnSelect select = Select.from(BotTypes_.CDS_NAME)
                .where(b -> b.get("taskType_ID").eq(taskTypeId))
                .orderBy(b -> b.get("sequence").asc());
        Result result = db.run(select);
        return result.listOf(BotTypes.class);
    }

    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        CqnSelect select = Select.from(PromptTexts_.CDS_NAME)
                .where(p -> p.get("botType_ID").eq(botTypeId));
        Result result = db.run(select);
        return result.listOf(PromptTexts.class);
    }

    // Query methods for main service entities
    public Tasks getTaskById(String taskId) {
        CqnSelect select = Select.from(Tasks_.CDS_NAME)
                .where(t -> t.get("ID").eq(taskId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("Task not found: " + taskId);
        }
        return result.single(Tasks.class);
    }

    public Tasks getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        CqnSelect select = Select.from(Tasks_.CDS_NAME)
                .where(t -> t.get("botInstance_ID").eq(botInstanceId)
                        .and(t.get("sequence").eq(sequence)));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);
        }
        return result.single(Tasks.class);
    }

    public Tasks getTaskByBotInstance(String botInstanceId) {
        CqnSelect select = Select.from(Tasks_.CDS_NAME)
                .where(t -> t.get("botInstance_ID").eq(botInstanceId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("Task not found for botInstance: " + botInstanceId);
        }
        return result.single(Tasks.class);
    }

    public BotInstances getBotInstanceById(String botInstanceId) {
        CqnSelect select = Select.from(BotInstances_.CDS_NAME)
                .where(b -> b.get("ID").eq(botInstanceId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("BotInstance not found: " + botInstanceId);
        }
        return result.single(BotInstances.class);
    }

    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        CqnSelect select = Select.from(BotInstances_.CDS_NAME)
                .where(b -> b.get("task_ID").eq(taskId)
                        .and(b.get("sequence").eq(sequence)));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("BotInstance not found for task: " + taskId + ", sequence: " + sequence);
        }
        return result.single(BotInstances.class);
    }

    public ContextNodes getContextNodeById(String contextNodeId) {
        CqnSelect select = Select.from(ContextNodes_.CDS_NAME)
                .where(c -> c.get("ID").eq(contextNodeId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("ContextNode not found: " + contextNodeId);
        }
        return result.single(ContextNodes.class);
    }

    public ContextNodes getContextNodeByTaskAndPath(String taskId, String contextPath) {
        CqnSelect select = Select.from(ContextNodes_.CDS_NAME)
                .where(c -> c.get("task_ID").eq(taskId)
                        .and(c.get("path").eq(contextPath)));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("ContextNode not found for task: " + taskId + ", path: " + contextPath);
        }
        return result.single(ContextNodes.class);
    }

    public List<BotMessages> getBotMessagesByBotInstanceId(String botInstanceId) {
        CqnSelect select = Select.from(BotMessages_.CDS_NAME)
                .where(b -> b.get("botInstance_ID").eq(botInstanceId))
                .orderBy(b -> b.get("createdAt").asc());
        Result result = db.run(select);
        return result.listOf(BotMessages.class);
    }

    // Create and insert methods
    public Tasks createAndInsertMainTask(String name, String description, String taskTypeId) {
        System.out.println("Creating main task: " + name + ", " + description + ", " + taskTypeId);
        
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(true);
        newTask.setContextPath("");
        newTask.setSequence(0);
        newTask.setTypeId(taskTypeId);

        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(newTask);
        Result result = db.run(insert);
        
        Tasks createdTask = result.single(Tasks.class);
        System.out.println("Task created successfully: " + createdTask.getName());
        return createdTask;
    }

    public Tasks createAndInsertSubTask(String name, String description, String contextPath,
            int sequence, String botInstanceId, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(false);
        newTask.setContextPath(contextPath);
        newTask.setSequence(sequence);
        newTask.setBotInstanceId(botInstanceId);
        newTask.setTypeId(taskTypeId);

        CqnInsert insert = Insert.into(Tasks_.CDS_NAME).entry(newTask);
        Result result = db.run(insert);
        return result.single(Tasks.class);
    }

    public BotInstances createAndInsertBotInstance(String taskId, BotTypes botType) {
        BotInstances botInstance = BotInstances.create();
        botInstance.setId(UUID.randomUUID().toString());
        botInstance.setSequence(botType.getSequence());
        botInstance.setTypeId(botType.getId());
        botInstance.setStatusCode("C"); // Created
        botInstance.setTaskId(taskId);

        CqnInsert insert = Insert.into(BotInstances_.CDS_NAME).entry(botInstance);
        Result result = db.run(insert);
        return result.single(BotInstances.class);
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

        CqnInsert insert = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
        Result result = db.run(insert);
        return result.single(ContextNodes.class);
    }

    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRole(role);

        CqnInsert insert = Insert.into(BotMessages_.CDS_NAME).entry(botMessage);
        Result result = db.run(insert);
        return result.single(BotMessages.class);
    }

    // Update methods
    public void updateBotInstance(BotInstances botInstance) {
        CqnInsert update = Insert.into(BotInstances_.CDS_NAME).entry(botInstance);
        db.run(update);
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

    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(java.time.Instant.now());

        CqnInsert update = Insert.into(ContextNodes_.CDS_NAME).entry(existingNode);
        db.run(update);
        return existingNode;
    }

    public ContextNodes updateContextNode(ContextNodes existingNode, String label, String type, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setLabel(label);
        existingNode.setType(type);
        existingNode.setModifiedAt(java.time.Instant.now());

        CqnInsert update = Insert.into(ContextNodes_.CDS_NAME).entry(existingNode);
        db.run(update);
        return existingNode;
    }

    public String getTaskIdByBotInstanceId(String botInstanceId) {
        CqnSelect select = Select.from(BotInstances_.CDS_NAME)
                .columns(b -> b.get("task_ID"))
                .where(b -> b.get("ID").eq(botInstanceId));
        Result result = db.run(select);
        
        if (result.rowCount() == 0) {
            // throw new BusinessException("No taskId found for botInstanceId: " + botInstanceId);
        }
        
        BotInstances instance = result.single(BotInstances.class);
        return instance.getTaskId();
    }

    public String getOutputContextPathByBotInstanceId(String botInstanceId) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        BotTypes botType = getBotTypeById(botInstance.getTypeId());
        return botType.getOutputContextPath();
    }

    public boolean isFirstCall(String botInstanceId) {
        try {
            List<BotMessages> messages = getBotMessagesByBotInstanceId(botInstanceId);
            return messages == null || messages.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    public String generateLabelFromPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "Context";
        }

        String[] parts = contextPath.split("\\.");
        String lastPart = parts[parts.length - 1];

        String processed = lastPart.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ")
                .replaceAll("-", " ")
                .toLowerCase();

        // Capitalize first letter of each word
        String[] words = processed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}