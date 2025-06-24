package com.sap.cap.ai2code.service.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.Tasks;
import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.service.common.GenericCqnService;

@Service
public class ContextServiceImpl implements ContextService {

    private final GenericCqnService genericCqnService;

    public ContextServiceImpl(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @Override
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes) {
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        List<Map<String, Object>> rootNodes = new ArrayList<>();

        // 第一步：创建所有节点对象
        for (ContextNodes node : contextNodes) {
            Map<String, Object> treeNode = new HashMap<>();
            treeNode.put("id", node.getId());
            treeNode.put("path", node.getPath());
            treeNode.put("label", node.getLabel());
            treeNode.put("type", node.getType());
            treeNode.put("value", node.getValue());
            treeNode.put("children", new ArrayList<Map<String, Object>>());
            treeNode.put("taskId", node.getTask() != null ? node.getTask().getId() : node.getTaskId());

            nodeMap.put(node.getPath(), treeNode);
        }

        // 第二步：建立父子关系
        for (ContextNodes node : contextNodes) {
            String parentPath = getParentPath(node.getPath());

            if (parentPath != null && nodeMap.containsKey(parentPath)) {
                // 有父节点，添加到父节点的children中
                Map<String, Object> parentNode = nodeMap.get(parentPath);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parentNode.get("children");
                children.add(nodeMap.get(node.getPath()));
            } else {
                // 没有父节点，是根节点
                rootNodes.add(nodeMap.get(node.getPath()));
            }
        }

        return rootNodes;
    }

    @Override
    public String getContextFullPath(String botInstanceId, String subPath) {
        if (subPath.startsWith("SubContext:")) {
            // 处理SubContext:的情况，需要获取任务的上下文路径并拼接
            Tasks task = genericCqnService.getParentTaskByBotInstance(botInstanceId);
            String taskContextPath = task.getContextPath();
            String relativePath = subPath.replace("SubContext:", "");

            // 如果任务上下文路径为空或null，直接返回相对路径
            if (taskContextPath == null || taskContextPath.isEmpty()) {
                return relativePath;
            }

            // 拼接路径，避免重复的点号
            if (taskContextPath.endsWith(".") || relativePath.startsWith(".")) {
                return taskContextPath + relativePath;
            } else {
                return taskContextPath + "." + relativePath;
            }
        } else if (subPath.startsWith("Context:")) {
            // 处理Context:的情况，直接去掉Context:前缀
            return subPath.replace("Context:", "");
        } else {
            // 如果都不包含，直接返回原路径
            return subPath;
        }
    }

    @Override
    public ContextNodes upsertContext(String botInstanceId, String contextPath, String contextValue) {
        // try {
        // 1. 通过botInstanceId获取mainTaskId
        String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);
        return upsertContextWithMainTaskId(mainTaskId, contextPath, contextValue);
        //     // 2. 查询是否已存在相同mainTaskId和contextPath的记录
        //     ContextNodes existingNode = null;
        //     try {
        //         existingNode = genericCqnService.getContextNodeByTaskAndPath(mainTaskId, contextPath);
        //     } catch (Exception e) {
        //         // 如果没找到，existingNode保持为null
        //     }

        //     if (existingNode != null) {
        //         // 3. 如果存在，更新现有记录
        //         return genericCqnService.updateContextNodeValue(existingNode, contextValue);
        //     } else {
        //         // 4. 如果不存在，创建新记录
        //         return genericCqnService.createAndInsertContextNode(mainTaskId,
        //                 contextPath,
        //                 generateLabelFromPath(contextPath),
        //                 "text",
        //                 contextValue);
        //     }
        // } catch (Exception e) {
        //     throw new BusinessException("Failed to upsert context node for botInstanceId: " + botInstanceId +
        //             ", contextPath: " + contextPath, e);
        // }
    }

    @Override
    public ContextNodes upsertContextWithMainTaskId(String mainTaskId, String contextPath, String contextValue) {
        // return upsertContext(botInstanceId, contextPath, null);
        String contextValueInString = contextValue != null ? contextValue.toString() : null;
        try {
            // 1. 通过botInstanceId获取mainTaskId
            // String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);

            // 2. 查询是否已存在相同mainTaskId和contextPath的记录
            ContextNodes existingNode = null;
            try {
                existingNode = genericCqnService.getContextNodeByTaskAndPath(mainTaskId, contextPath);
            } catch (Exception e) {
                // 如果没找到，existingNode保持为null
            }

            if (existingNode != null) {
                // 3. 如果存在，更新现有记录
                return genericCqnService.updateContextNodeValue(existingNode, contextValueInString);
            } else {
                // 4. 如果不存在，创建新记录
                return genericCqnService.createAndInsertContextNode(mainTaskId,
                        contextPath,
                        generateLabelFromPath(contextPath),
                        "text",
                        contextValueInString);
            }

        } catch (Exception e) {
            throw new BusinessException("Failed to upsert context node for mainTaskId: " + mainTaskId
                    + ", contextPath: " + contextPath, e);
        }
    }

    @Override
    public ContextNodes getContextNode(String contextNodeId) {
        return genericCqnService.getContextNodeById(contextNodeId);
    }

    @Override
    public List<ContextNodes> upsertContextBatch(String botInstanceId, Map<String, String> contextPathValueMap) {
        List<ContextNodes> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : contextPathValueMap.entrySet()) {
            try {
                ContextNodes node = upsertContext(botInstanceId, entry.getKey(), entry.getValue());
                results.add(node);
            } catch (Exception e) {
                System.err.println("Failed to upsert context node: " + entry.getKey() + ", error: " + e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<ContextNodes> getContextNodesByPattern(String botInstanceId, String pathPattern) {
        try {
            String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);

            // 处理 [-1] 语法，转换为 SQL LIKE 查询
            if (pathPattern.contains("[-1]")) {
                String likePattern = pathPattern.replace("[-1]", "[%]") + "%";
                // return genericCqnService.getContextNodesByTaskAndPathPattern(mainTaskId,
                // likePattern);
                return null;
            } else {
                // 普通路径查询
                ContextNodes node = genericCqnService.getContextNodeByTaskAndPath(mainTaskId, pathPattern);
                return node != null ? List.of(node) : List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 获取父路径
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 处理数组索引，如 a.b[0].c -> a.b[0]
        int lastDot = path.lastIndexOf(".");
        int lastBracket = path.lastIndexOf("]");

        if (lastDot > lastBracket && lastDot > 0) {
            return path.substring(0, lastDot);
        } else if (lastBracket > 0) {
            // 如果最后是数组索引，需要找到数组前的路径
            int openBracket = path.lastIndexOf("[");
            int dotBeforeBracket = path.lastIndexOf(".", openBracket);
            if (dotBeforeBracket > 0) {
                return path.substring(0, dotBeforeBracket);
            }
        }

        return null;
    }

    /**
     * 从路径生成标签
     */
    private String generateLabelFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "Root";
        }

        // 提取路径的最后一部分作为标签
        String[] parts = path.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 移除数组索引
        lastPart = lastPart.replaceAll("\\[\\d+\\]", "");

        // 首字母大写
        if (!lastPart.isEmpty()) {
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }

        return "Node";
    }
}
