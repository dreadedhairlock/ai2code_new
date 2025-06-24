package com.sap.cap.ai2code.service.impl;

// import com.sap.cap.ai2code.model.tree.TaskBotNode;
// import com.sap.cap.ai2code.model.bot.Bot;
// import com.sap.cap.ai2code.model.task.Task;

import org.springframework.stereotype.Service;

// /**
//  * 任务-Bot统一缓存管理器
//  * 使用树形结构管理Task和BotInstance的层级关系
//  */
@Service
public class TaskBotCacheManager {
    
//     // 全局节点缓存：nodeId -> TaskBotNode
//     private final Map<String, TaskBotNode> nodeCache = new ConcurrentHashMap<>();
    
//     // 根节点缓存：mainTaskId -> rootNode
//     private final Map<String, TaskBotNode> rootCache = new ConcurrentHashMap<>();
    
//     /**
//      * 添加任务节点
//      */
//     public TaskBotNode addTaskNode(Task task, String parentBotInstanceId) {
//         TaskBotNode taskNode = new TaskBotNode(task);
        
//         nodeCache.put(task.getTask().getId(), taskNode);
        
//         // 如果是主任务，添加到根缓存
//         if (task.getTask().getIsMain() != null && task.getTask().getIsMain()) {
//             rootCache.put(task.getTask().getId(), taskNode);
//         } else if (parentBotInstanceId != null) {
//             // 如果有父Bot实例，建立父子关系
//             TaskBotNode parentNode = nodeCache.get(parentBotInstanceId);
//             if (parentNode != null) {
//                 parentNode.addChild(taskNode);
//             }
//         }
        
//         return taskNode;
//     }
    
//     /**
//      * 添加Bot实例节点
//      */
//     public TaskBotNode addBotInstanceNode(Bot bot) {
//         TaskBotNode botNode = new TaskBotNode(bot);
        
//         nodeCache.put(bot.getBotInstance().getId(), botNode);
        
//         // 建立与任务的父子关系
//         String taskId = bot.getBotInstance().getTaskId();
//         if (taskId != null) {
//             TaskBotNode taskNode = nodeCache.get(taskId);
//             if (taskNode != null) {
//                 taskNode.addChild(botNode);
//             }
//         }
        
//         return botNode;
//     }
    
//     /**
//      * 获取节点
//      */
//     public TaskBotNode getNode(String nodeId) {
//         return nodeCache.get(nodeId);
//     }
    
//     /**
//      * 获取任务节点
//      */
//     public TaskBotNode getTaskNode(String taskId) {
//         TaskBotNode node = nodeCache.get(taskId);
//         if (node != null && node.getType() == TaskBotNode.NodeType.TASK) {
//             return node;
//         }
//         return null;
//     }
    
//     /**
//      * 获取Bot实例节点
//      */
//     public TaskBotNode getBotInstanceNode(String botInstanceId) {
//         TaskBotNode node = nodeCache.get(botInstanceId);
//         if (node != null && node.getType() == TaskBotNode.NodeType.BOT_INSTANCE) {
//             return node;
//         }
//         return null;
//     }
    
//     /**
//      * 根据Bot实例ID获取主任务ID
//      */
//     public String getMainTaskId(String botInstanceId) {
//         TaskBotNode botNode = getBotInstanceNode(botInstanceId);
//         if (botNode != null) {
//             return botNode.getMainTaskId();
//         }
//         return null;
//     }
    
//     /**
//      * 根据任务ID和序列号查找Bot实例
//      */
//     public TaskBotNode getBotInstanceByTaskAndSequence(String taskId, int sequence) {
//         TaskBotNode taskNode = getTaskNode(taskId);
//         if (taskNode != null) {
//             return taskNode.findBotBySequence(sequence);
//         }
//         return null;
//     }
    
//     /**
//      * 获取缓存的Task对象
//      */
//     public Task getCachedTask(String taskId) {
//         TaskBotNode node = getTaskNode(taskId);
//         return node != null ? node.getTaskObject() : null;
//     }
    
//     /**
//      * 获取缓存的Bot对象
//      */
//     public Bot getCachedBot(String botInstanceId) {
//         TaskBotNode node = getBotInstanceNode(botInstanceId);
//         return node != null ? node.getBotObject() : null;
//     }
    
//     /**
//      * 更新Bot状态
//      */
//     public void updateBotStatus(String botInstanceId, String statusCode) {
//         TaskBotNode node = getBotInstanceNode(botInstanceId);
//         if (node != null) {
//             node.updateBotStatus(statusCode);
//         }
//     }
    
//     /**
//      * 移除节点及其子树
//      */
//     public void removeNode(String nodeId) {
//         TaskBotNode node = nodeCache.remove(nodeId);
//         if (node != null) {
//             // 递归移除子节点
//             removeSubtree(node);
            
//             // 从父节点中移除
//             if (node.getParent() != null) {
//                 node.getParent().removeChild(nodeId);
//             }
            
//             // 如果是根节点，从根缓存中移除
//             if (node.isMainTask()) {
//                 rootCache.remove(node.getId());
//             }
//         }
//     }
    
//     private void removeSubtree(TaskBotNode node) {
//         for (TaskBotNode child : node.getChildren()) {
//             nodeCache.remove(child.getId());
//             removeSubtree(child);
//         }
//     }
    
//     /**
//      * 清空所有缓存
//      */
//     public void clearAll() {
//         nodeCache.clear();
//         rootCache.clear();
//     }
    
//     /**
//      * 获取缓存统计信息
//      */
//     public Map<String, Integer> getCacheStats() {
//         Map<String, Integer> stats = new ConcurrentHashMap<>();
//         stats.put("totalNodes", nodeCache.size());
//         stats.put("rootNodes", rootCache.size());
        
//         long taskNodes = nodeCache.values().stream()
//             .filter(node -> node.getType() == TaskBotNode.NodeType.TASK)
//             .count();
//         long botNodes = nodeCache.values().stream()
//             .filter(node -> node.getType() == TaskBotNode.NodeType.BOT_INSTANCE)
//             .count();
            
//         stats.put("taskNodes", (int) taskNodes);
//         stats.put("botInstanceNodes", (int) botNodes);
        
//         return stats;
//     }
}