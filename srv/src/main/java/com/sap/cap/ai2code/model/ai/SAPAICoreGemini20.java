package com.sap.cap.ai2code.model.ai;

import cds.gen.configservice.ModelConfigs;

// In your SAPAICoreGemini20.java file
public class SAPAICoreGemini20 implements AIModel {

    private ModelConfigs modelConfigs;

    // Add constructor that accepts ModelConfigs
    public SAPAICoreGemini20(ModelConfigs modelConfigs) {
        this.modelConfigs = modelConfigs;
    }

    // Keep the no-args constructor if needed
    public SAPAICoreGemini20() {
    }

    @Override
    public String getModelName() {
        return modelConfigs != null ? modelConfigs.getModelName() : "gemini-2.0";
    }

    @Override
    public ModelConfigs getModelConfigs() {
        return this.modelConfigs;
    }

    // Implement other AIModel interface methods...
}
