package com.sap.cap.ai2code.service.prompt;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sap.cap.ai2code.service.common.GenericCqnService;

import cds.gen.configservice.PromptTexts;

@Service
public class PromptServiceImpl implements PromptService {

    private final GenericCqnService genericCqnService;

    public PromptServiceImpl(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @Override
    public String parse(PromptTexts prompt, String mainTaskId, String botInstanceId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parse'");
    }

    @Override
    public List<PromptTexts> getPrompts(String botTypeId, String botInstanceId) {
        return genericCqnService.getPromptTextsByBotType(botTypeId);
        // throw new UnsupportedOperationException("Unimplemented method 'getPrompts'");
    }

}
