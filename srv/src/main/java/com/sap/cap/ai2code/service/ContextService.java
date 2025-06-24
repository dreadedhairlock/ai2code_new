package com.sap.cap.ai2code.service;

import java.util.List;
import java.util.Map;

import cds.gen.mainservice.ContextNodes;

public interface ContextService {
    /**
     * 将上下文节点列表构建为层次结构
     */
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes);

    /**
     * 获取完整的上下文路径（处理SubContext相对路径）
     */
    public String getContextFullPath(String botInstanceId, String subPath);

    /**
     * 创建或更新上下文节点
     */
    public ContextNodes upsertContext(String botInstanceId, String contextPath, String contextValue);

    public ContextNodes upsertContextWithMainTaskId(String mainTaskId, String contextPath, String contextValue);

    /**
     * 获取上下文节点
     */
    public ContextNodes getContextNode(String contextNodeId);

    /**
     * 批量更新/创建上下文节点
     */
    public List<ContextNodes> upsertContextBatch(String botInstanceId, Map<String, String> contextPathValueMap);

    /**
     * 根据路径模式查询上下文节点（支持数组语法）
     */
    public List<ContextNodes> getContextNodesByPattern(String botInstanceId, String pathPattern);
}
