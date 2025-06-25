package com.sap.cap.ai2code.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.GetContextNodesTreeContext;

@Component
@ServiceName("MainService")
public class getContextNodesTreeHandler implements EventHandler {

    private final PersistenceService db;

    public getContextNodesTreeHandler(PersistenceService db) {
        this.db = db;
    }

    @On(event = GetContextNodesTreeContext.CDS_NAME)
    public void onGetContextNodesTree(GetContextNodesTreeContext context) {
        // Retrieve the taskId parameter from the action context
        String taskId = context.getTaskId();

        // Build a CQN query to fetch context nodes associated with the task ID
        CqnSelect query = Select.from(ContextNodes_.class)
                .where(cn -> cn.get("task_ID").eq(taskId));

        // Execute the query
        Result result_db = db.run(query);

        // Debug logs to verify input and result
        System.out.println("UUID: " + taskId);
        System.out.println("Result: " + result_db);

        // Map the result rows to a list of simplified objects
        List<Map<String, Object>> items = result_db.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("ID", row.get("ID"));
                    item.put("path", row.get("path"));
                    item.put("label", row.get("label"));
                    item.put("type", row.get("type"));
                    item.put("value", row.get("value"));
                    return item;
                })
                .collect(Collectors.toList());

        // Convert items to flat list of nodes using path as key
        List<Map<String, Object>> nodes = createFlatNodesUsingPath(items);

        // Wrap the result into a response object
        Map<String, Object> response = new HashMap<>();
        response.put("nodes", nodes);

        // Set the response on the action context
        GetContextNodesTreeContext.ReturnType result = GetContextNodesTreeContext.ReturnType.of(response);
        context.setResult(result);
    }

    /**
     * Creates a flat list of nodes (both folders and data items) using path as the
     * key.
     * The output conforms to the revised action schema without unnecessary
     * attributes.
     */
    private List<Map<String, Object>> createFlatNodesUsingPath(List<Map<String, Object>> items) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Set<String> processedFolderPaths = new HashSet<>();

        // STEP 1: Extract all unique paths that need folders
        Set<String> folderPaths = new LinkedHashSet<>();

        for (Map<String, Object> item : items) {
            String path = (String) item.get("path");
            if (path != null && !path.isEmpty()) {
                // Add all parent paths as folders
                String[] segments = path.split("\\.");
                StringBuilder currentPath = new StringBuilder();

                for (int i = 0; i < segments.length; i++) {
                    if (i > 0)
                        currentPath.append(".");
                    currentPath.append(segments[i]);
                    folderPaths.add(currentPath.toString());
                }
            }
        }

        // Sort paths by depth to process parent folders first
        List<String> sortedPaths = new ArrayList<>(folderPaths);
        sortedPaths.sort(Comparator.comparingInt(p -> p.split("\\.").length));

        // STEP 2: Create folder nodes for each unique path
        for (String folderPath : sortedPaths) {
            if (processedFolderPaths.contains(folderPath)) {
                continue; // Skip already processed paths
            }

            String[] segments = folderPath.split("\\.");
            String folderName = segments[segments.length - 1];

            Map<String, Object> folderNode = new HashMap<>();
            // No ID for folders typically
            folderNode.put("path", folderPath);
            folderNode.put("label", folderName);
            folderNode.put("type", "folder");
            folderNode.put("isFolder", true);

            nodes.add(folderNode);
            processedFolderPaths.add(folderPath);
        }

        // STEP 3: Add data items
        for (Map<String, Object> item : items) {
            Map<String, Object> dataNode = new HashMap<>();
            dataNode.put("ID", item.get("ID"));
            dataNode.put("path", item.get("path"));
            dataNode.put("label", item.get("label"));
            dataNode.put("type", item.get("type"));
            dataNode.put("value", item.get("value"));
            dataNode.put("isFolder", false);

            nodes.add(dataNode);
        }
        System.out.println(nodes);
        return nodes;
    }
    // /**
    // * Returns the parent path of a given dot-separated path.
    // */
    // private String getParentPath(String path) {
    // int lastDotIndex = path.lastIndexOf(".");
    // return lastDotIndex > 0 ? path.substring(0, lastDotIndex) : null;
    // }

    // /**
    // * Simple generic pair class to assist with traversals or mappings.
    // */
    // class Pair<K, V> {
    // private K key;
    // private V value;

    // public Pair(K key, V value) {
    // this.key = key;
    // this.value = value;
    // }

    // public K getKey() {
    // return key;
    // }

    // public V getValue() {
    // return value;
    // }
    // }

    // // Ganti seluruh logika tree building Anda dengan ini:

    // // Map untuk menyimpan semua nodes (folder + data)
    // Map<String, Map<String, Object>> allNodes = new HashMap<>();
    // Map<String, Set<String>> parentChildren = new HashMap<>();

    // // Proses setiap item dari database
    // items.forEach(item -> {
    // String fullPath = (String) item.get("path");
    // List<String> pathParts = Arrays.asList(fullPath.split("\\."))
    // .stream().filter(part -> !part.isEmpty()).collect(Collectors.toList());

    // String currentPath = "";

    // // Buat semua folder nodes di path
    // for (int i = 0; i < pathParts.size(); i++) {
    // String part = pathParts.get(i);
    // String previousPath = currentPath;
    // currentPath = currentPath.isEmpty() ? part : currentPath + "." + part;

    // // Buat folder node jika belum ada
    // if (!allNodes.containsKey(currentPath)) {
    // Map<String, Object> folderNode = new HashMap<>();
    // folderNode.put("ID", null);
    // folderNode.put("path", currentPath);
    // folderNode.put("label", part);
    // folderNode.put("type", null);
    // folderNode.put("value", null);
    // folderNode.put("children", new ArrayList<Map<String, Object>>());
    // folderNode.put("isFolder", true);
    // allNodes.put(currentPath, folderNode);
    // }

    // // Track parent-child relationship
    // if (!previousPath.isEmpty()) {
    // parentChildren.computeIfAbsent(previousPath, k -> new
    // HashSet<>()).add(currentPath);
    // }
    // }

    // // Buat data node (leaf node) untuk item ini
    // Map<String, Object> dataNode = new HashMap<>();
    // dataNode.put("ID", item.get("ID"));
    // dataNode.put("path", fullPath);
    // dataNode.put("label", item.get("label"));
    // dataNode.put("type", item.get("type"));
    // dataNode.put("value", item.get("value"));
    // dataNode.put("children", new ArrayList<Map<String, Object>>());
    // dataNode.put("isFolder", false);

    // // Tambahkan data node ke parent folder
    // if (allNodes.containsKey(fullPath)) {
    // List<Map<String, Object>> children = (List<Map<String, Object>>)
    // allNodes.get(fullPath).get("children");
    // children.add(dataNode);
    // }
    // });

    // // Build hierarchy - populate children untuk setiap node
    // parentChildren.forEach((parentPath, childrenPaths) -> {
    // if (allNodes.containsKey(parentPath)) {
    // List<Map<String, Object>> children = (List<Map<String, Object>>)
    // allNodes.get(parentPath)
    // .get("children");
    // childrenPaths.forEach(childPath -> {
    // if (allNodes.containsKey(childPath)) {
    // // Periksa apakah child sudah ada
    // boolean exists = children.stream()
    // .anyMatch(child -> childPath.equals(child.get("path")));
    // if (!exists) {
    // children.add(allNodes.get(childPath));
    // }
    // }
    // });
    // }
    // });

    // // Dapatkan root nodes (nodes yang tidak punya parent)
    // List<Map<String, Object>> rootNodes = allNodes.values().stream()
    // .filter(node -> {
    // String nodePath = (String) node.get("path");
    // return !nodePath.contains("."); // Root level nodes
    // })
    // .collect(Collectors.toList());

    // // Response
    // Map<String, Object> response = new HashMap<>();
    // response.put("nodes", rootNodes);

    // GetContextNodesTreeContext.ReturnType result =
    // GetContextNodesTreeContext.ReturnType.of(response);
    // context.setResult(result);
    // System.out.println(rootNodes);

    // // Struktur untuk menyimpan tree yang sudah diproses
    // Map<String, Map<String, Object>> treeMap = new HashMap<>();

    // // Proses item menjadi struktur tree
    // items.forEach(item -> {
    // // Split path menggunakan dot notation
    // String path = (String) item.get("path");
    // List<String> pathParts = Arrays.asList(path.split("\\."));
    // pathParts = pathParts.stream().filter(part ->
    // !part.isEmpty()).collect(Collectors.toList());

    // String currentPath = "";

    // // Buat hierarchy untuk setiap level
    // for (int i = 0; i < pathParts.size(); i++) {
    // String part = pathParts.get(i);
    // String previousPath = currentPath;
    // currentPath = currentPath.isEmpty() ? part : currentPath + "." + part;

    // // Buat node jika belum ada
    // if (!treeMap.containsKey(currentPath)) {
    // Map<String, Object> node = new HashMap<>();
    // node.put("path", currentPath);
    // node.put("label", part);
    // node.put("children", new ArrayList<Map<String, Object>>());
    // node.put("isFolder", true);
    // treeMap.put(currentPath, node);
    // }

    // // Link dengan parent
    // if (!previousPath.isEmpty() && treeMap.containsKey(previousPath)) {
    // Map<String, Object> parent = treeMap.get(previousPath);
    // Map<String, Object> current = treeMap.get(currentPath);

    // List<Map<String, Object>> children = (List<Map<String, Object>>)
    // parent.get("children");

    // // Periksa apakah child sudah ada dalam list
    // boolean childExists = children.stream()
    // .anyMatch(child -> current.get("path").equals(child.get("path")));

    // if (!childExists) {
    // children.add(current);
    // }
    // }
    // }

    // // Tambahkan item data ke node terakhir
    // String finalPath = (String) item.get("path");
    // if (treeMap.containsKey(finalPath)) {
    // Map<String, Object> leafNode = new HashMap<>();
    // leafNode.put("ID", item.get("ID"));
    // leafNode.put("path", item.get("path"));
    // leafNode.put("type", item.get("type"));
    // leafNode.put("label", item.get("label"));
    // leafNode.put("value", item.get("value"));
    // leafNode.put("children", new ArrayList<>());
    // leafNode.put("isFolder", false);

    // List<Map<String, Object>> children = (List<Map<String, Object>>)
    // treeMap.get(finalPath).get("children");
    // children.add(leafNode);
    // }
    // });

    // // Dapatkan root nodes
    // List<Map<String, Object>> rootNodes = treeMap.values().stream()
    // .filter(node -> {
    // String nodePath = (String) node.get("path");
    // String parentPath = nodePath.contains(".")
    // ? nodePath.substring(0, nodePath.lastIndexOf("."))
    // : "";
    // return parentPath.isEmpty() || !treeMap.containsKey(parentPath);
    // })
    // .collect(Collectors.toList());

    // // Buat result object
    // GetContextNodesTreeContext.ReturnType response =
    // GetContextNodesTreeContext.ReturnType.create();

    // // Convert rootNodes langsung menggunakan reflection/mapping CAP
    // List<GetContextNodesTreeContext.ReturnType.Nodes> convertedNodes =
    // rootNodes.stream()
    // .map(nodeMap -> GetContextNodesTreeContext.ReturnType.Nodes.of(nodeMap))
    // .collect(Collectors.toList());

    // response.setNodes(convertedNodes);
    // context.setResult(response);
    // System.out.println("test:" + response);
    // System.out.println("root:" + rootNodes);

    // GetContextNodesTreeContext.ReturnType response =
    // GetContextNodesTreeContext.ReturnType.create();
    // response.setNodes(rootNodes);
    // context.setResult(response);

    // // Siapkan respons
    // Map<String, Object> response = new HashMap<>();
    // response.put("nodes", rootNodes);

    // Set hasil ke context
    // context.setResult(response);
}
