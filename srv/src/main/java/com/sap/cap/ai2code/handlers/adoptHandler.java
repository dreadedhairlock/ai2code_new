package com.sap.cap.ai2code.handlers;

import java.util.ArrayList;
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
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;

@Component
@ServiceName("MainService")
public class adoptHandler implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Before(event = BotMessagesAdoptContext.CDS_NAME)
    public void beforeExecute(BotMessagesAdoptContext context, BotInstance botInstance) {
    }

@On(event = BotMessagesAdoptContext.CDS_NAME, entity = BotMessages_.CDS_NAME)
    public void onAdopt(BotMessagesAdoptContext context) {
        List<ContextNodes> resultNodes = new ArrayList<>();
        
        try {
            // Get the CQN from context to identify which BotMessages to process
            CqnSelect selectQuery = context.getCqn();
            
            // 1. Get the current BotMessages entries
            Result botMessagesResult = db.run(selectQuery);
            
            for (Object row : botMessagesResult) {
                BotMessages botMessage = (BotMessages) row;
                String botInstanceId = botMessage.getBotInstanceId();
                
                try {
                    // 2. Get BotInstances entry according to BotMessages.botInstance
                    CqnSelect selectBotInstance = Select.from(BotInstances_.CDS_NAME)
                            .columns(BotInstances_.ID, BotInstances_.TYPE_ID)
                            .byId(botInstanceId);
                    Result botInstanceResult = db.run(selectBotInstance);
                    
                    if (botInstanceResult.rowCount() == 0) {
                        throw new IllegalArgumentException("BotInstance with ID " + botInstanceId + " not found.");
                    }
                    
                    BotInstances botInstance = botInstanceResult.single(BotInstances.class);
                    String botTypeId = botInstance.getTypeId();
                    
                    // 3. Get BotTypes entries based on BotInstances.type
                    CqnSelect selectBotType = Select.from(BotTypes_.CDS_NAME)
                            .columns(BotTypes_.ID, BotTypes_.CONTEXT_TYPE_CODE)
                            .byId(botTypeId);
                    Result botTypeResult = db.run(selectBotType);
                    
                    if (botTypeResult.rowCount() == 0) {
                        throw new IllegalArgumentException("BotType with ID " + botTypeId + " not found.");
                    }
                    
                    BotTypes botType = botTypeResult.single(BotTypes.class);
                    
                    // 4. Store the content of this message in the ContextNodes entry
                    ContextNodes contextNode = ContextNodes.create();
                    
                        // Path: outputContextPath set according to BotTypes
                        contextNode.setPath(botType.getOutputContextPath());
                        
                        // Label: using bot message content as label
                        contextNode.setLabel(botMessage.getMessage());
                        
                        // Type: contextType set according to BotTypes
                        contextNode.setType(botType.getContextTypeCode());
                        
                        // Value: BotMessages.message/Convert to the corresponding format according to the AI function call
                        String messageValue = processMessageValue(botMessage.getMessage(), botType.getContextType());
                        contextNode.setValue(messageValue);
                    
                    // Set task ID from bot instance
                    contextNode.setTaskId(botInstance.getTaskId());
                    
                    // Insert the context node
                    CqnInsert insertContextNode = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
                    Result insertResult = db.run(insertContextNode);
                    
                    ContextNodes createdNode = insertResult.single(ContextNodes.class);
                    resultNodes.add(createdNode);
                    
                    // 5. Set the status field of BotInstances to S (Success)
                    CqnUpdate updateBotInstance = Update.entity(BotInstances_.CDS_NAME)
                            .byId(botInstanceId)
                            .data(BotInstances.STATUS_CODE, "S");
                    db.run(updateBotInstance);
                    
                    System.out.println("Successfully processed BotMessage ID: " + botMessage.getId() + 
                                     " for BotInstance ID: " + botInstanceId);
                    
                } catch (Exception e) {
                    // Error handling: Set the status field of BotInstances to F (Failed)
                    try {
                        CqnUpdate updateBotInstanceFailed = Update.entity(BotInstances_.CDS_NAME)
                                .byId(botInstanceId)
                                .data(BotInstances.STATUS_CODE, "F");
                        db.run(updateBotInstanceFailed);
                        
                        System.err.println("Failed to process BotMessage for BotInstance ID: " + botInstanceId + 
                                         ". Error: " + e.getMessage());
                    } catch (Exception updateException) {
                        System.err.println("Failed to update BotInstance status to Failed: " + updateException.getMessage());
                    }
                    
                }
            }
            
            // 6. Returns the ContextNodes entries
            context.setResult(resultNodes);
            
        } catch (Exception e) {
            System.err.println("Error in adopt handler: " + e.getMessage());
            throw new RuntimeException("Failed to adopt bot messages: " + e.getMessage(), e);
        }
    }
    /**
     * Processes the message value according to the context type.
     * This is a placeholder implementation; adjust logic as needed.
     */
    private String processMessageValue(String message, Object contextType) {
        // Example: just return the message as-is, or add logic based on contextType
        return message;
    }

    @After(event = BotMessagesAdoptContext.CDS_NAME)
    public void afterExecute(BotMessagesAdoptContext context) {

    }

}
