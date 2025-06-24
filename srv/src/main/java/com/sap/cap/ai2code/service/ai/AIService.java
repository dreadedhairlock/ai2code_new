package com.sap.cap.ai2code.service.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.sap.cap.ai2code.model.ai.AIModel;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import jakarta.annotation.Nonnull;
// import com.sap.cap.ai2code.service.StreamingCompletedProcessor;
// import com.sap.cap.ai2code.service.BotExecution;

public interface AIService {

    public String chatWithAI(
            List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content,
            AIModel model);

    // public SseEmitter chatWithAIStreaming(
    //                 List<BotMessages> messages,
    //                 List<PromptTexts> prompts,
    //                 String content,
    //                 AIModel model,
    //                 ExecutorService executor,
    //                 StreamingCompletedProcessor streamingCompletionProcessor);
    // public <T extends BotExecution> String functionCalling(
    //                 List<BotMessages> messages,
    //                 List<PromptTexts> prompts,
    //                 // FunctionCalls functionCall,
    //                 Class<T> botExecutClazz,
    //                 AIModel model);
    // /**
    //  * Send a chunk to the emitter
    //  *
    //  * @param emitter The emitter to send the chunk to
    //  * @param chunk   The chunk to send
    //  */
    // public static void send(@Nonnull final SseEmitter emitter, @Nonnull final String chunk) {
    //         try {
    //                 emitter.send(chunk);
    //         } catch (final IOException e) {
    //                 emitter.completeWithError(e);
    //         }
    // }
}
