package com.sap.cap.ai2code.model;

import org.springframework.stereotype.Service;
import cds.gen.configservice.ModelConfigs;
import com.sap.cap.ai2code.model.AIModel;
import com.sap.cap.ai2code.model.SAPAICoreGemini20;
import com.sap.cap.ai2code.service.impl.GenericCqnService;
import com.sap.cap.ai2code.service.impl.SAPGeminiAIServiceImpl;
// import com.sap.cap.ai2code.service.impl.DummyAIServiceImpl;
import com.sap.cap.ai2code.service.interfaces.AIService;

/**
 * AI Model Resolver - provides AI models and services based on configuration
 * Currently only supports Gemini 3.5, other models are dummy implementations
 */
@Service
public class AIModelResolver {

    private final GenericCqnService genericCqnService;
    private final SAPGeminiAIServiceImpl sapGeminiService;

    public AIModelResolver(GenericCqnService genericCqnService,
            SAPGeminiAIServiceImpl sapGeminiService) {
        this.genericCqnService = genericCqnService;
        this.sapGeminiService = sapGeminiService;
    }

    /**
     * Resolves AI model by model configuration ID
     */
    public AIModel resolveAIModel(String modelConfigId) {
        ModelConfigs modelConfig = genericCqnService.getModelConfig(modelConfigId);
        return resolveAIModel(modelConfig);
    }

    /**
     * Resolves AI model by model configuration
     */
    public AIModel resolveAIModel(ModelConfigs modelConfigs) {
        switch (modelConfigs.getProvider()) {
            case "SAPAICore-Gemini":
                switch (modelConfigs.getModelName()) {
                    case "gemini-2.0":
                        return new SAPAICoreGemini20(modelConfigs);
                }
                break;
            case "SAPAICore-OpenAI":
            // Dummy implementations for development/testing
            // return new DummyAIModel(modelConfigs, "OpenAI-" + modelConfigs.getModelName());
            case "SAPAICore-Claude":
            // Dummy implementations for development/testing
            // return new DummyAIModel(modelConfigs, "Claude-" + modelConfigs.getModelName());
        }

        throw new IllegalArgumentException("Unsupported model: "
                + modelConfigs.getProvider() + "/" + modelConfigs.getModelName());
    }

    /**
     * Resolves AI service by model configuration
     */
    public AIService resolveAIService(ModelConfigs modelConfigs) {
        switch (modelConfigs.getProvider()) {
            case "SAPAICore-Gemini":
                return sapGeminiService;
            case "SAPAICore-OpenAI":
            case "SAPAICore-Claude":
            default:
                return sapGeminiService; // Default fallback to dummy
        }
    }
}
