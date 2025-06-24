package com.sap.cap.ai2code.service.prompt;

import java.util.List;

import cds.gen.configservice.PromptTexts;

public interface PromptService {

    public String parse(PromptTexts prompt, String mainTaskId, String botInstanceId);

    public List<PromptTexts> getPrompts(String botTypeId, String mainTaskId, String botInstanceId);

}
