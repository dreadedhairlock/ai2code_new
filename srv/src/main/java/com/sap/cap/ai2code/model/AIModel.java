package com.sap.cap.ai2code.model;

// import com.sap.cap.ai2code.model.config.AIServiceConfig;
import cds.gen.configservice.ModelConfigs;

public interface AIModel {

    public String getModelName();

    // public AIServiceConfig parseModelConfigs();
    public ModelConfigs getModelConfigs();
}
