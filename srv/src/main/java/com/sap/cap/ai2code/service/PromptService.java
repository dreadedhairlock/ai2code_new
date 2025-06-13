package com.sap.cap.ai2code.service;

import java.util.List;

import cds.gen.configservice.PromptTexts;

public interface PromptService {
    /**
     * 
     * @param prompt     提示词
     * @param mainTaskId 主任务ID
     * @return
     */
    public String parse(PromptTexts prompt, String mainTaskId, String botInstanceId);

    /**
     * 
     * @param botTypeId
     * @param mainTaskId
     * @return
     */
    public List<PromptTexts> getPrompts(String botTypeId, String mainTaskId, String botInstanceId);

}