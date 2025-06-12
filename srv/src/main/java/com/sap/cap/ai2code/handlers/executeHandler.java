//UNCHECKED
package com.sap.cap.ai2code.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.ai.orchestration.BotInstance;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;

@Component
@ServiceName("MainService")
public class executeHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Before(event = BotInstancesExecuteContext.CDS_NAME)
    public void beforeExecute(BotInstancesExecuteContext context, BotInstance botInstance) {
        System.out.println("Preparing to execute bot instance");
        
        // Validate the CQN select query if provided
        if (context.getCqn() != null) {
            System.out.println("Validating CQN query: " + context.getCqn().toString());
        }
    }

    @On(event = BotInstancesExecuteContext.CDS_NAME)
    public void onExecute(BotInstancesExecuteContext context) {
        System.out.println("Executing bot instance with context: " + context.toString());

        Collection<ContextNodes> resultNodes = new ArrayList<>();

        try {
            // Execute the CQN query to get bot instances
            if (context.getCqn() != null) {
                Result queryResult = db.run(context.getCqn());
                
                queryResult.stream().forEach(row -> {
                    try {
                        BotInstances botInstance = BotInstances.of(row);
                        String botInstanceId = botInstance.getId();
                        
                        // Set status to Running
                        updateBotInstanceStatus(botInstanceId, "R");
                        
                        // Get bot type information
                        BotTypes botType = getBotType(botInstance.getTypeId());
                        
                        String executionResult = "";
                        List<String> taskIds = new ArrayList<>();
                        
                        if ("F".equals(botType.getTaskType())) {
                            // F type BotInstance - Function call
                            executionResult = executeFunctionCallBot(botInstance, botType);
                        } else if ("C".equals(botType.getTaskType())) {
                            // C type BotInstance - Custom implementation
                            executionResult = executeCustomBot(botInstance, botType);
                        }
                        
                        // Update bot instance with result
                        updateBotInstanceResult(botInstanceId, executionResult);
                        
                        // Create context nodes from result
                        ContextNodes contextNode = createContextNode(botInstance, executionResult);
                        if (contextNode != null) {
                            resultNodes.add(contextNode);
                        }
                        
                        // Set status to Success
                        updateBotInstanceStatus(botInstanceId, "S");
                        
                        // Notify frontend (placeholder for actual implementation)
                        notifyFrontend(botInstance);
                        
                    } catch (Exception e) {
                        System.err.println("Error executing bot instance: " + e.getMessage());
                        // Set status to Failed
                        BotInstances botInstance = BotInstances.of(row);
                        updateBotInstanceStatus(botInstance.getId(), "F");
                        throw new RuntimeException("Bot execution failed", e);
                    }
                });
            }
            
            context.setResult((BotInstancesExecuteContext.ReturnType) resultNodes);
            
        } catch (Exception e) {
            System.err.println("Error in bot execution: " + e.getMessage());
            throw new RuntimeException("Bot execution process failed", e);
        }

        
    }
   private BotTypes getBotType(String typeId) {
        CqnSelect select = Select.from(BotTypes_.CDS_NAME).byId(typeId);
        Result result = db.run(select);
        if (result.rowCount() == 0) {
            throw new IllegalArgumentException("Bot type with ID " + typeId + " not found.");
        }
        return result.single(BotTypes.class);
    }

    private String executeFunctionCallBot(BotInstances botInstance, BotTypes botType) {
        System.out.println("Executing F type bot instance: " + botInstance.getId());
        
        try {
            // Get the prompts for maintenance
            // String prompts = botTypes.getPrompts();
            // System.out.println("Using prompts: " + prompts);
            
            // Get implementation class
            String implementationClass = botType.getImplementationClass();
            if (implementationClass == null || implementationClass.isEmpty()) {
                throw new IllegalArgumentException("Implementation class not specified for bot type");
            }
            
            // Call the AI Function call and execute method
            String result = executeImplementationClass(implementationClass, botInstance , null);
            
            // Write to ContextNodes entry through outputContextPath
            String outputContextPath = botType.getOutputContextPath();
            if (outputContextPath != null && !outputContextPath.isEmpty()) {
                writeToContextPath(botInstance, outputContextPath, result);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error executing F type bot: " + e.getMessage());
            throw new RuntimeException("Function call bot execution failed", e);
        }
    }

    private String executeCustomBot(BotInstances botInstance, BotTypes botType) {
        System.out.println("Executing C type bot instance: " + botInstance.getId());
        
        try {
            // Get implementation class
            String implementationClass = botType.getImplementationClass();
            if (implementationClass == null || implementationClass.isEmpty()) {
                throw new IllegalArgumentException("Implementation class not specified for bot type");
            }
            
            // Execute the execute method in the implementation class
            String result = executeImplementationClass(implementationClass, botInstance, null);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error executing C type bot: " + e.getMessage());
            throw new RuntimeException("Custom bot execution failed", e);
        }
    }

    private String executeImplementationClass(String implementationClass, BotInstances botInstance, String prompts) {
        // Placeholder for actual implementation class execution
        // In a real implementation, you would use reflection to instantiate and call the execute method
        System.out.println("Executing implementation class: " + implementationClass);
        System.out.println("Bot instance ID: " + botInstance.getId());
        if (prompts != null) {
            System.out.println("With prompts: " + prompts);
        }
        
        // This would be replaced with actual reflection-based execution
        // Example: Class<?> clazz = Class.forName(implementationClass);
        // Object instance = clazz.newInstance();
        // Method executeMethod = clazz.getMethod("execute", BotInstances.class, String.class);
        // return (String) executeMethod.invoke(instance, botInstance, prompts);
        
        return "Execution result from " + implementationClass;
    }

    private void writeToContextPath(BotInstances botInstance, String outputContextPath, String result) {
        System.out.println("Writing to context path: " + outputContextPath);
        
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setTaskId(botInstance.getTaskId());
        contextNode.setPath(outputContextPath);
        contextNode.setLabel("Bot Result");
        contextNode.setValue(result);
        
        CqnInsert insert = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
        db.run(insert);
    }

    private ContextNodes createContextNode(BotInstances botInstance, String result) {
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setTaskId(botInstance.getTaskId());
        contextNode.setPath("bot_result_" + botInstance.getId());
        contextNode.setLabel("Bot Execution Result");
        contextNode.setValue(result);
        
        CqnInsert insert = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
        Result insertResult = db.run(insert);
        
        return insertResult.single(ContextNodes.class);
    }

    private void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        CqnUpdate update = Update.entity(BotInstances_.CDS_NAME)
            .byId(botInstanceId)
            .data(BotInstances.STATUS_CODE, statusCode);
        db.run(update);
        
        System.out.println("Updated bot instance " + botInstanceId + " status to: " + statusCode);
    }

    private void updateBotInstanceResult(String botInstanceId, String result) {
        CqnUpdate update = Update.entity(BotInstances_.CDS_NAME)
            .byId(botInstanceId)
            .data(BotInstances.RESULT, result);
        db.run(update);
        
        System.out.println("Updated bot instance " + botInstanceId + " result: " + result);
    }

    private void notifyFrontend(BotInstances botInstance) {
        // Placeholder for frontend notification
        // In a real implementation, this might use WebSocket, Server-Sent Events, or other notification mechanisms
        System.out.println("Notifying frontend of updates for bot instance: " + botInstance.getId());
        System.out.println("Frontend should update ContextNode and Task/Instance tree");
    }

    @After(event = BotInstancesExecuteContext.CDS_NAME)
    public void afterExecute(BotInstancesExecuteContext context) {
        System.out.println("Bot instance execution completed");
        
        BotInstancesExecuteContext.ReturnType results = context.getResult();
        if (results != null && results instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) results;
            System.out.println("Created " + collection.size() + " context nodes");
            collection.forEach(node -> {
                if (node instanceof ContextNodes) {
                    ContextNodes contextNode = (ContextNodes) node;
                    System.out.println("Context node: " + contextNode.getPath() + " = " + contextNode.getValue());
                }
            });
        }
    }

}
