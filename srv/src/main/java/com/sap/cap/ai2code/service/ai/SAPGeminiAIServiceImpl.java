package com.sap.cap.ai2code.service.ai;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
// import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
// import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.ai.AIModel;
import com.sap.cap.ai2code.model.execution.functioncall.FunctionInfo;
import com.sap.cap.ai2code.model.execution.functioncall.ParameterInfo;
// import com.sap.cap.ai2code.service.ai.AIService;
import com.sap.cap.ai2code.service.bot.BotExecution;
import com.sap.cap.ai2code.service.execution.functioncall.FunctionCallProcessor;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotMessages;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class SAPGeminiAIServiceImpl implements AIService {
    private final FunctionCallProcessor functionCallProcessor;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Dotenv dotenv = Dotenv.load();

    private final String model = dotenv.get("GEMINI_MODEL");
    private final String apiKey = dotenv.get("GEMINI_API_KEY");

    public SAPGeminiAIServiceImpl(FunctionCallProcessor functionCallProcessor) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.functionCallProcessor = functionCallProcessor;
    }

    @Override
    public String chatWithAI(List<BotMessages> messages, List<PromptTexts> prompts, String content, AIModel model) {
        try {
            // Build conversation context
            List<Map<String, String>> conversationContext = buildConversationContext(messages, prompts, content);

            // Call Gemini API
            return callGeminiAPIWithContext(conversationContext);

        } catch (Exception e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    /**
     * Builds conversation context from messages, prompts, and current content
     */
    private List<Map<String, String>> buildConversationContext(List<BotMessages> messages,
            List<PromptTexts> prompts,
            String content) {
        List<Map<String, String>> context = new ArrayList<>();

        // Add system prompts first
        if (prompts != null) {
            for (PromptTexts prompt : prompts) {
                if (prompt.getContent() != null && !prompt.getContent().trim().isEmpty()) {
                    Map<String, String> systemMessage = new HashMap<>();
                    systemMessage.put("role", "system");
                    systemMessage.put("content", prompt.getContent());
                    context.add(systemMessage);
                }
            }
        }

        // Add historical messages
        if (messages != null) {
            for (BotMessages message : messages) {
                if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                    Map<String, String> historyMessage = new HashMap<>();
                    historyMessage.put("role", message.getRole());
                    historyMessage.put("content", message.getMessage());
                    context.add(historyMessage);
                }
            }
        }

        // Add current user message
        if (content != null && !content.trim().isEmpty()) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", content);
            context.add(userMessage);
        }

        return context;
    }

    /**
     * Calls Gemini API with conversation context
     */
    private String callGeminiAPIWithContext(List<Map<String, String>> conversationContext) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey);

        // Build contents array from conversation context
        StringBuilder contentsBuilder = new StringBuilder();
        contentsBuilder.append("\"contents\": [");

        for (int i = 0; i < conversationContext.size(); i++) {
            Map<String, String> message = conversationContext.get(i);
            String role = message.get("role");
            String content = message.get("content").replace("\"", "\\\"").replace("\n", "\\n");

            // Gemini uses "user" and "model" roles, map "assistant" and "system" to "model"
            String geminiRole = "user".equals(role) ? "user" : "model";

            contentsBuilder.append("{")
                    .append("\"role\": \"").append(geminiRole).append("\",")
                    .append("\"parts\": [{\"text\": \"").append(content).append("\"}]")
                    .append("}");

            if (i < conversationContext.size() - 1) {
                contentsBuilder.append(",");
            }
        }
        contentsBuilder.append("]");

        String requestBody = "{" + contentsBuilder.toString() + "}";

        // DEBUG
        System.out.println("AI Request Body: " + requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API call failed: " + response.statusCode() + " - " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    /**
     * Parses Gemini API response
     */
    private String parseGeminiResponse(String responseBody) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        JsonNode candidates = jsonResponse.get("candidates");

        if (candidates != null && candidates.size() > 0) {
            JsonNode content = candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
            }
        }

        return "No response from AI";
    }

    @Override
    public <T extends BotExecution> Object functionCalling(List<BotMessages> messages, List<PromptTexts> prompts,
            T botExecutionInstance, AIModel aiModel) {

        try {
            // 1. Ekstrak informasi fungsi dari botExecutionInstance
            List<FunctionInfo> functionInfos = functionCallProcessor
                    .extractFunctionInfosFromInstance(botExecutionInstance);

            if (functionInfos.isEmpty()) {
                throw new BusinessException("No executable methods found in bot instance: "
                        + botExecutionInstance.getClass().getSimpleName());
            }

            // Log informasi tentang class yang sedang digunakan
            System.out.println("Bot Execution class: " + botExecutionInstance.getClass().getName());
            System.out.println("Bot Execution simple name: " + botExecutionInstance.getClass().getSimpleName());

            // Kasus khusus untuk CreateTasksBotExecution
            boolean isCreateTasksBot = botExecutionInstance.getClass().getSimpleName()
                    .equals("CreateTasksBotExecution");

            // 2. Buat deskripsi berbasis teks tentang fungsi yang tersedia
            StringBuilder functionDescriptions = new StringBuilder();

            // Untuk kasus CreateTasksBotExecution, berikan instruksi yang lebih spesifik
            if (isCreateTasksBot) {
                functionDescriptions.append("You are working with a CreateTasksBotExecution bot.\n");
                functionDescriptions.append("This bot has one primary function called 'execute' to create tasks.\n\n");
                functionDescriptions.append("You can call the execute function like this:\n\n");
                functionDescriptions.append("FUNCTION_CALL: execute\n");
                functionDescriptions.append("botInstanceId: [bot instance id]\n");
                functionDescriptions.append("taskCreationParams: [JSON array of task creation parameters]\n");
                functionDescriptions.append("END_FUNCTION_CALL\n\n");
                functionDescriptions.append("Example of taskCreationParams format:\n");
                functionDescriptions.append(
                        "[{\"sequence\": 1, \"name\": \"Task 1\", \"description\": \"Description 1\", \"contextPath\": \"/path/1\"}, ");
                functionDescriptions.append(
                        "{\"sequence\": 2, \"name\": \"Task 2\", \"description\": \"Description 2\", \"contextPath\": \"/path/2\"}]\n\n");
                functionDescriptions.append(
                        "DO NOT use any other function name like 'Create_Tasks_Bot_Execution_execute'. Use ONLY 'execute'.\n\n");
            } else {
                // Standard behavior for other bot types
                functionDescriptions.append("You can call these functions:\n\n");

                for (FunctionInfo function : functionInfos) {
                    functionDescriptions.append("Function: ").append(function.getName()).append("\n");
                    functionDescriptions.append("Description: ").append(function.getDescription()).append("\n");
                    functionDescriptions.append("Parameters:\n");

                    for (ParameterInfo param : function.getParameters()) {
                        functionDescriptions.append("  - ").append(param.getName());
                        functionDescriptions.append(" (Type: ").append(param.getType().getSimpleName()).append(")");
                        if (param.isRequired()) {
                            functionDescriptions.append(" [Required]");
                        }
                        functionDescriptions.append(": ").append(param.getDescription()).append("\n");
                    }
                    functionDescriptions.append("\n");
                }
            }

            // Instruksi format umum
            functionDescriptions.append("To call a function, respond EXACTLY in this format:\n");
            functionDescriptions.append("FUNCTION_CALL: functionName\n");
            functionDescriptions.append("PARAM_NAME_1: param_value_1\n");
            functionDescriptions.append("PARAM_NAME_2: param_value_2\n");
            functionDescriptions.append("END_FUNCTION_CALL\n\n");

            if (!isCreateTasksBot) {
                functionDescriptions.append("For example:\n");
                functionDescriptions.append("FUNCTION_CALL: calculateSum\n");
                functionDescriptions.append("A: 5\n");
                functionDescriptions.append("B: 10\n");
                functionDescriptions.append("END_FUNCTION_CALL\n\n");
            }

            // 3. Buat konteks percakapan dengan deskripsi fungsi sebagai instruksi sistem
            List<Map<String, String>> conversationContext = new ArrayList<>();

            // Tambahkan pesan sistem dengan instruksi function calling
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", functionDescriptions.toString());
            conversationContext.add(systemMessage);

            // Tambahkan prompts sistem jika ada
            if (prompts != null) {
                for (PromptTexts prompt : prompts) {
                    if (prompt.getContent() != null && !prompt.getContent().trim().isEmpty()) {
                        Map<String, String> promptMessage = new HashMap<>();
                        promptMessage.put("role", "system");
                        promptMessage.put("content", prompt.getContent());
                        conversationContext.add(promptMessage);
                    }
                }
            }

            // Tambahkan riwayat percakapan
            if (messages != null) {
                for (BotMessages message : messages) {
                    if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                        Map<String, String> chatMessage = new HashMap<>();
                        chatMessage.put("role", message.getRole());
                        chatMessage.put("content", message.getMessage());
                        conversationContext.add(chatMessage);
                    }
                }
            }

            // 4. Panggil Gemini API dengan konteks percakapan yang sudah disiapkan
            String geminiResponse = callGeminiAPIWithContext(conversationContext);
            System.out.println("Gemini response: " + geminiResponse);

            // 5. Parse respons untuk mencari format function call
            String functionCallPattern = "FUNCTION_CALL:\\s*([^\\n]+)[\\s\\S]*?END_FUNCTION_CALL";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(functionCallPattern);
            java.util.regex.Matcher matcher = pattern.matcher(geminiResponse);

            if (matcher.find()) {
                // Ekstrak nama fungsi dan parameter
                String functionCallText = matcher.group(0);
                String[] lines = functionCallText.split("\\n");

                String functionNameRaw = lines[0].substring("FUNCTION_CALL:".length()).trim();
                System.out.println("Function name from AI: " + functionNameRaw);

                // Untuk CreateTasksBotExecution, selalu gunakan 'execute'
                String functionName = isCreateTasksBot ? "execute" : functionNameRaw;

                Map<String, String> parameters = new HashMap<>();

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.equals("END_FUNCTION_CALL")) {
                        break;
                    }

                    int separatorIndex = line.indexOf(":");
                    if (separatorIndex > 0) {
                        String paramName = line.substring(0, separatorIndex).trim();
                        String paramValue = line.substring(separatorIndex + 1).trim();
                        parameters.put(paramName, paramValue);
                    }
                }

                System.out.println("Parameters: " + parameters);

                // Cari metode yang sesuai
                Method methodToInvoke;

                if (isCreateTasksBot) {
                    methodToInvoke = findExactMethodByName(botExecutionInstance.getClass(), "execute");
                    if (methodToInvoke == null) {
                        throw new BusinessException("Execute method not found in CreateTasksBotExecution");
                    }
                } else {
                    methodToInvoke = findMethodByName(botExecutionInstance.getClass(), functionName);
                    if (methodToInvoke == null) {
                        throw new BusinessException("Method not found: " + functionName);
                    }
                }

                System.out.println("Method to invoke: " + methodToInvoke.getName());

                // Persiapkan parameter untuk invokasi
                Object[] methodParams = new Object[methodToInvoke.getParameterCount()];
                java.lang.reflect.Parameter[] methodParameters = methodToInvoke.getParameters();

                for (int i = 0; i < methodParameters.length; i++) {
                    String paramName = methodParameters[i].getName();
                    String paramValue = parameters.get(paramName);
                    Class<?> paramType = methodParameters[i].getType();

                    System.out.println("Param " + i + ": " + paramName + " (" + paramType.getSimpleName() + ")");

                    if (paramValue != null) {
                        System.out.println("Value: " + paramValue);

                        // Handling khusus untuk List<TaskCreationParam>
                        if (List.class.isAssignableFrom(paramType) &&
                                paramName.equals("taskCreationParams") &&
                                isCreateTasksBot) {
                            try {
                                // Gunakan Jackson untuk parsing JSON array ke List
                                List<?> paramList = objectMapper.readValue(
                                        paramValue,
                                        objectMapper.getTypeFactory().constructCollectionType(
                                                List.class,
                                                com.sap.cap.ai2code.model.execution.TaskCreationParam.class));
                                methodParams[i] = paramList;
                            } catch (Exception e) {
                                System.err.println("Error parsing taskCreationParams: " + e.getMessage());
                                // Fallback jika parsing gagal, buat list kosong
                                methodParams[i] = new ArrayList<>();
                            }
                        } else {
                            // Konversi parameter normal
                            methodParams[i] = convertStringToType(paramValue, paramType);
                        }
                    } else {
                        System.out.println("No value provided, using default");
                        methodParams[i] = getDefaultValue(paramType);
                    }
                }

                // Eksekusi fungsi
                methodToInvoke.setAccessible(true);
                try {
                    Object result = methodToInvoke.invoke(botExecutionInstance, methodParams);
                    System.out.println("Function executed successfully. Result: " + result);
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BusinessException("Error executing function: " + e.getMessage(), e);
                }
            }

            // 6. Jika tidak ada function call, kembalikan respons teks biasa
            return geminiResponse;

        } catch (Exception e) {
            e.printStackTrace(); // Log stack trace untuk debugging
            throw new BusinessException("Function calling with Gemini failed: " + e.getMessage(), e);
        }
    }

    private Object convertStringToType(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        try {
            System.out.println("Converting string to " + targetType.getSimpleName() + ": " + value);

            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType.isArray()) {
                // Konversi string ke array
                try {
                    // Coba parse sebagai JSON array terlebih dahulu
                    return objectMapper.readValue(value, targetType);
                } catch (Exception e) {
                    // Fallback ke metode splitting sederhana (format CSV)
                    String[] elements = value.split(",");
                    Class<?> componentType = targetType.getComponentType();
                    Object array = java.lang.reflect.Array.newInstance(componentType, elements.length);

                    for (int i = 0; i < elements.length; i++) {
                        Object convertedValue = convertStringToType(elements[i].trim(), componentType);
                        java.lang.reflect.Array.set(array, i, convertedValue);
                    }
                    return array;
                }
            } else if (List.class.isAssignableFrom(targetType)) {
                // Coba parse sebagai JSON array terlebih dahulu
                try {
                    // Untuk List generik, kita harus menebak tipe elemen
                    // Ini lebih kompleks dan mungkin memerlukan informasi tambahan
                    // Untuk saat ini, coba gunakan Object
                    return objectMapper.readValue(value,
                            objectMapper.getTypeFactory().constructCollectionType(
                                    List.class, Object.class));
                } catch (Exception e) {
                    // Fallback ke metode splitting sederhana
                    String[] elements = value.split(",");
                    List<String> list = new ArrayList<>();
                    for (String element : elements) {
                        list.add(element.trim());
                    }
                    return list;
                }
            } else {
                // Untuk tipe kompleks, gunakan Jackson untuk deserialisasi JSON
                try {
                    return objectMapper.readValue(value, targetType);
                } catch (Exception e) {
                    System.err.println("Error deserializing to " + targetType.getName() + ": " + e.getMessage());
                    throw new RuntimeException(
                            "Failed to convert value to type " + targetType.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting '" + value + "' to " + targetType.getName() + ": " + e.getMessage());
            throw new RuntimeException(
                    "Failed to convert value to type " + targetType.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Mencari metode dengan nama persis atau dengan normalisasi
     */
    private Method findMethodByName(Class<?> clazz, String methodName) {
        // Log nama fungsi yang diminta untuk debugging
        System.out.println("Looking for method: " + methodName);
        System.out.println("In class: " + clazz.getSimpleName());

        // Tampilkan semua metode yang tersedia di kelas
        System.out.println("Available methods in " + clazz.getSimpleName() + ":");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.println("- " + method.getName());
        }

        // Kasus khusus untuk CreateTasksBotExecution
        if (clazz.getSimpleName().equals("CreateTasksBotExecution")) {
            System.out.println("Special handling for CreateTasksBotExecution");
            // Selalu gunakan metode execute untuk kelas ini
            Method executeMethod = findExactMethodByName(clazz, "execute");
            if (executeMethod != null) {
                System.out.println("Using execute method from CreateTasksBotExecution");
                return executeMethod;
            }
        }

        // 1. Coba cari dengan nama persis
        Method exactMatch = findExactMethodByName(clazz, methodName);
        if (exactMatch != null) {
            System.out.println("Found exact match: " + exactMatch.getName());
            return exactMatch;
        }

        // 2. Jika nama yang diberikan adalah "Create_Tasks_Bot_Execution_execute",
        // gunakan "execute"
        if ("Create_Tasks_Bot_Execution_execute".equals(methodName) ||
                methodName.toLowerCase().contains("create") && methodName.toLowerCase().contains("task")) {
            Method executeMethod = findExactMethodByName(clazz, "execute");
            if (executeMethod != null) {
                System.out.println("Mapping " + methodName + " to execute");
                return executeMethod;
            }
        }

        // 3. Untuk nama metode lain dengan format X_Y_Z_execute, coba ambil execute
        if (methodName.toLowerCase().endsWith("_execute") ||
                methodName.toLowerCase().endsWith("execute")) {
            Method executeMethod = findExactMethodByName(clazz, "execute");
            if (executeMethod != null) {
                System.out.println("Found execute method for name ending with _execute");
                return executeMethod;
            }
        }

        // 4. Coba cari tanpa underscore
        Method noUnderscoreMatch = findExactMethodByName(clazz, methodName.replace("_", ""));
        if (noUnderscoreMatch != null) {
            System.out.println("Found match without underscores: " + noUnderscoreMatch.getName());
            return noUnderscoreMatch;
        }

        // 5. Coba cari dalam CamelCase
        Method camelCaseMatch = findExactMethodByName(clazz, toCamelCase(methodName));
        if (camelCaseMatch != null) {
            System.out.println("Found camelCase match: " + camelCaseMatch.getName());
            return camelCaseMatch;
        }

        // Jika tidak ditemukan di kelas saat ini, periksa parent class
        if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
            return findMethodByName(clazz.getSuperclass(), methodName);
        }

        // Jika semua upaya gagal, kembalikan null
        System.out.println("No method found for: " + methodName);
        return null;
    }

    /**
     * Mencari metode dengan nama persis
     */
    private Method findExactMethodByName(Class<?> clazz, String methodName) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding method: " + e.getMessage());
        }
        return null;
    }

    /**
     * Mengubah snake_case ke camelCase
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Mendapatkan nilai default untuk tipe primitif
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class)
            return false;
        if (type == char.class)
            return '\0';
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0.0f;
        if (type == double.class)
            return 0.0d;
        return null; // Untuk tipe non-primitif
    }

}
