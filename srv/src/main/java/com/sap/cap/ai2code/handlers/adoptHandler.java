package com.sap.cap.ai2code.handlers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sap.cap.ai2code.service.bot.BotService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.ContextNodes;

@Component
@ServiceName("MainService")
public class adoptHandler implements EventHandler {

    private final BotService botService;

    // Constructor
    public adoptHandler(BotService botService) {
        this.botService = botService;
    }

    @On(event = BotMessagesAdoptContext.CDS_NAME, entity = BotMessages_.CDS_NAME)
    public void onAdopt(BotMessagesAdoptContext context) {
        try {
            // // If your BotService.adopt() returns a single ContextNodes:
            // ContextNodes resultNode = botService.adopt(context);

            // // Return type should be List<ContextNodes> not single ContextNodes
            ContextNodes resultNode = botService.adopt(context);
            List<ContextNodes> resultNodes = List.of(resultNode);
            context.setResult(resultNodes);
            System.out.println("Result Nodes: " + resultNodes);

        } catch (Exception e) {
            System.err.println("Error in adopt handler: " + e.getMessage());
            throw new RuntimeException("Failed to adopt bot messages: " + e.getMessage(), e);
        }

        /**
         * This is the code before modularization
         */
        // List<ContextNodes> resultNodes = new ArrayList<>(); // set array
        // try {
        //     // Get BotMessage information
        //     CqnSelect selectQuery = context.getCqn();
        //     // 1. Get the current BotMessages entries
        //     Result botMessagesResult = db.run(selectQuery);
        //     botMessagesResult.stream().forEach(
        //             row -> {
        //                 System.out.println("Row: " + row);
        //                 String botInstanceId = row.getPath("botInstance_ID");
        //                 String botMessageId = row.getPath("ID");
        //                 String botMessage = row.getPath("message");
        //                 System.out.println("BotInstance with ID: " + botInstanceId);
        //                 System.out.println("BotMessage with ID: " + botMessageId);
        //                 try {
        //                     // 2. Get BotInstances entry according to BotMessages.botInstance
        //                     CqnSelect selectBotInstance = Select.from(BotInstances_.CDS_NAME)
        //                             .columns(BotInstances_.ID, BotInstances_.TYPE_ID, BotInstances_.TASK_ID)
        //                             .byId(botInstanceId);
        //                     Result botInstanceResult = db.run(selectBotInstance);
        //                     if (botInstanceResult.rowCount() == 0) {
        //                         throw new IllegalArgumentException(
        //                                 "BotInstance with ID " + botInstanceId + " not found.");
        //                     }
        //                     // select single BotInstance
        //                     BotInstances botInstance = botInstanceResult.single(BotInstances.class);
        //                     String botTypeId = botInstance.getTypeId();
        //                     System.out.println("BotType with ID: " + botTypeId);
        //                     // 3. Get BotTypes entries based on BotInstances.type
        //                     CqnSelect selectBotType = Select.from(BotTypes_.CDS_NAME)
        //                             .columns(BotTypes_.ID, BotTypes_.CONTEXT_TYPE_CODE, BotTypes.OUTPUT_CONTEXT_PATH,
        //                                     BotTypes_.TASK_TYPE_ID)
        //                             .where(b -> b.get("ID").eq(botTypeId));
        //                     Result botTypeResult = db.run(selectBotType);
        //                     if (botTypeResult.rowCount() == 0) {
        //                         throw new IllegalArgumentException("BotType with ID " + botTypeId + " not found.");
        //                     }
        //                     BotTypes botType = botTypeResult.single(BotTypes.class);
        //                     // 4. Store the content of this message in the ContextNodes entry
        //                     ContextNodes contextNode = ContextNodes.create();
        //                     // Path: outputContextPath set according to BotTypes
        //                     String path = botType.getOutputContextPath();
        //                     System.out.println("path: " + path);
        //                     String cleanedPath = path.contains("SubContext:") ? path.replaceFirst("SubContext:", "")
        //                             : path;
        //                     System.out.println("path: " + cleanedPath);
        //                     contextNode.setPath(cleanedPath);
        //                     // Label: using bot message content as label
        //                     String labelValue = botMessage != null && botMessage.length() > 200
        //                             ? botMessage.substring(0, 200)
        //                             : botMessage;
        //                     contextNode.setLabel(labelValue);
        //                     // Type: contextType set according to BotTypes
        //                     contextNode.setType(botType.getContextTypeCode());
        //                     // Value: BotMessages.message/Convert to the corresponding format according to
        //                     // the AI function call
        //                     String messageValue = processMessageValue(botMessage, botType.getContextType());
        //                     contextNode.setValue(messageValue);
        //                     // Set task ID from bot instance
        //                     contextNode.setTaskId(botInstance.getTaskId());
        //                     // Insert the context node
        //                     CqnInsert insertContextNode = Insert.into(ContextNodes_.CDS_NAME).entry(contextNode);
        //                     Result insertResult = db.run(insertContextNode);
        //                     ContextNodes createdNode = insertResult.single(ContextNodes.class);
        //                     resultNodes.add(createdNode);
        //                     // 5. Set the status field of BotInstances to S (Success)
        //                     CqnUpdate updateBotInstance = Update.entity(BotInstances_.CDS_NAME)
        //                             .byId(botInstanceId)
        //                             .data(BotInstances.STATUS_CODE, "S");
        //                     db.run(updateBotInstance);
        //                     System.out.println("Successfully processed BotInstance ID: " + botInstanceId);
        //                 } catch (Exception e) {
        //                     // Error handling: Set the status field of BotInstances to F (Failed)
        //                     try {
        //                         CqnUpdate updateBotInstanceFailed = Update.entity(BotInstances_.CDS_NAME)
        //                                 .byId(botInstanceId)
        //                                 .data(BotInstances.STATUS_CODE, "F");
        //                         db.run(updateBotInstanceFailed);
        //                         System.err.println("Failed to process BotMessage for BotInstance ID: " + botInstanceId +
        //                                 ". Error: " + e.getMessage());
        //                     } catch (Exception updateException) {
        //                         System.err.println("Failed to update BotInstance status to Failed: "
        //                                 + updateException.getMessage());
        //                     }
        //                 }
        //             });
        //     // 6. Returns the ContextNodes entries
        //     context.setResult(resultNodes);
        //     System.out.println("Result Nodes: " + resultNodes);
        // } catch (Exception e) {
        //     System.err.println("Error in adopt handler: " + e.getMessage());
        //     throw new RuntimeException("Failed to adopt bot messages: " + e.getMessage(), e);
        // }
    }

    // /**
    //  * Processes the message value according to the context type.
    //  * This is a placeholder implementation; adjust logic as needed.
    //  */
    // private String processMessageValue(String message, Object contextType) {
    //     // Example: just return the message as-is, or add logic based on contextType
    //     return message;
    // }
}
