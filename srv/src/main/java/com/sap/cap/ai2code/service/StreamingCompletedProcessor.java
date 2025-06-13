package com.sap.cap.ai2code.service;

@FunctionalInterface
public interface StreamingCompletedProcessor {
    /**
     * 处理流式聊天完成事件
     * 
     * @param completeReply 完成的回复内容
     */
    void process(String completeReply);
}
