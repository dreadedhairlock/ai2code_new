//UNCHECKED
package com.sap.cap.ai2code.handlers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotInstancesExecuteContext.ReturnType;
import cds.gen.mainservice.BotInstances_;

@Component
@ServiceName("MainService")
public class executeHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Before(event = BotInstancesExecuteContext.CDS_NAME)
    public void beforeExecute(BotInstancesExecuteContext context) {
         System.out.println("Preparing to execute bot instance");
        
        // Validate the CQN select query if provided
        if (context.getCqn() != null) {
            System.out.println("Validating CQN query: " + context.getCqn().toString());
        }
    }

    @On(event = BotInstancesExecuteContext.CDS_NAME)
    public void onExecute(BotInstancesExecuteContext context) {
        System.out.println("Executing bot instance with context: " + context.toString());
        ReturnType returnResult = ReturnType.create();
        try {
            // Execute the CQN query to get bot instances
            if (context.getCqn() != null) {
                Result queryResult = db.run(context.getCqn());
                
                queryResult.stream().forEach(row -> {
                    try {
                        BotInstances botInstance = BotInstances.of(row);
                        String botInstanceId = botInstance.getId();
                        
                        // 将BotInstances的status字段设置为R(Running)。
                        updateBotInstanceStatus(botInstanceId, "R");

                        // Get botInstance's BotType
                        String botInstanceType = botInstance.getTypeId();

                        BotTypes botType = getBotType(botInstanceType);
                        
                        String executionResult = "";
                        List<String> taskIds = new ArrayList<>();
                        
                        // F type BotInstance - Function call - incomplete
                        if ("F".equals(botInstanceType)) executionResult = executeFunctionCallBot(botInstance, botType);
                        // C type BotInstance - Code
                        else if ("C".equals(botInstanceType)) executionResult = executeCodeBot(botInstance, botType);
                        // Set the result to be the executionResult string
                        returnResult.setResult(executionResult);
                        // Update bot instance with result
                        updateBotInstanceResult(botInstanceId, executionResult);
                        // 将BotInstances.Status设置为S(Success)。
                        updateBotInstanceStatus(botInstanceId, "S");
                        
                    } catch (Exception e) {
                        System.err.println("Error executing bot instance: " + e.getMessage());
                        // Set status to Failed
                        BotInstances botInstance = BotInstances.of(row);
                        updateBotInstanceStatus(botInstance.getId(), "F");
                        throw new RuntimeException("Bot execution failed", e);
                    }
                });
            }

            context.setResult(returnResult);
            
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
        StringBuilder promptsStringBuilder = new StringBuilder();
        try {
            
            // Get the prompts for maintenance
            List<PromptTexts> prompts = botType.getPrompts();
            System.out.println("Using prompts: " + prompts);

            prompts.forEach(prompt -> {
                String promptContent = prompt.getContent();
                promptsStringBuilder.append(promptContent);
            });

            // Combine all prompts into one string variable.
            String promptsString = promptsStringBuilder.toString();

            // It is unsure what does maintenance meant in this case. Please provide a more detailed explanation.

            
            // Get implementation class
            String implementationClassFromBotType = botType.getImplementationClass();

            if (implementationClassFromBotType == null || implementationClassFromBotType.isEmpty()) {
                throw new IllegalArgumentException("Implementation class not specified for bot type");
            }

            // Get the implementationClass
            Class<?> implementationClass = Class.forName(implementationClassFromBotType);
            System.out.println("Class Name: " + implementationClass.getName());

            // Get all methods
            Method[] methods = implementationClass.getDeclaredMethods();
            // Check if there exists a method called execute in the class
            Boolean methodExists = false;
            for(Method method : methods) {
                if ("execute".equals(method.getName())) {
                    methodExists = true;
                    break;
                }
            }

            // Error handling case
            if(methodExists == false) throw new IllegalArgumentException("No method called execute found in the class");
            
            // Call the AI Function call and execute method
            Object implementationInstance = implementationClass.getDeclaredConstructor().newInstance();
            Method executeMethod = implementationClass.getMethod("execute", Object.class);
            Object executionResult = executeMethod.invoke(implementationInstance);
            
            return executionResult.toString();
            
        } catch (Exception e) {
            System.err.println("Error executing F type bot: " + e.getMessage());
            throw new RuntimeException("Function call bot execution failed", e);
        }
    }

    private String executeCodeBot(BotInstances botInstance, BotTypes botType) {
        System.out.println("Executing C type bot instance: " + botInstance.getId());
        
        try {
            // Get implementation class
            String implementationClassFromBotType = botType.getImplementationClass();

            if (implementationClassFromBotType == null || implementationClassFromBotType.isEmpty()) {
                throw new IllegalArgumentException("Implementation class not specified for bot type");
            }

            // Get the implementationClass
            Class<?> implementationClass = Class.forName(implementationClassFromBotType);
            System.out.println("Class Name: " + implementationClass.getName());

            // Get all methods
            Method[] methods = implementationClass.getDeclaredMethods();
            // Check if there exists a method called execute in the class
            Boolean methodExists = false;
            for(Method method : methods) {
                if ("execute".equals(method.getName())) {
                    methodExists = true;
                    break;
                }
            }

            // Error handling case
            if(methodExists == false) throw new IllegalArgumentException("No method called execute found in the class");
            
            // Call the AI Function call and execute method
            Object implementationInstance = implementationClass.getDeclaredConstructor().newInstance();
            Method executeMethod = implementationClass.getMethod("execute", Object.class);
            Object executionResult = executeMethod.invoke(implementationInstance);
            
            return executionResult.toString();
            
        } catch (Exception e) {
            System.err.println("Error executing C type bot: " + e.getMessage());
            throw new RuntimeException("Custom bot execution failed", e);
        }
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

}
