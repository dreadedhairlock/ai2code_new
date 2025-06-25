package com.sap.cap.ai2code.service.execution.functioncall;

import com.sap.cap.ai2code.exception.BusinessException;
import com.sap.cap.ai2code.model.execution.annotation.BotExecutor;
import com.sap.cap.ai2code.model.execution.annotation.ExecuteMethod;
import com.sap.cap.ai2code.model.execution.annotation.ExecuteParameter;
import com.sap.cap.ai2code.model.execution.functioncall.FunctionInfo;
import com.sap.cap.ai2code.service.bot.BotExecution;
import com.sap.cap.ai2code.model.execution.functioncall.ParameterInfo;
import com.sap.cap.ai2code.model.execution.functioncall.ParameterTypeInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class FunctionCallProcessor {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Autowired
    public FunctionCallProcessor(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    /**
     * 扫描并解析所有的 BotExecution 实现类
     * 
     * @return 函数信息列表
     */
    public List<FunctionInfo> extractFunctionInfos() {
        List<FunctionInfo> functionInfos = new ArrayList<>();

        // 获取所有实现了 BotExecution 接口的 Bean
        Map<String, BotExecution> botExecutors = applicationContext.getBeansOfType(BotExecution.class);

        for (Map.Entry<String, BotExecution> entry : botExecutors.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            BotExecutor botExecutor = clazz.getAnnotation(BotExecutor.class);

            if (botExecutor != null && botExecutor.enabled()) {
                functionInfos.addAll(extractFunctionInfosFromClass(clazz, botExecutor));
            }
        }

        return functionInfos;
    }

    /**
     * 从特定的 BotExecution 实例中提取函数信息
     */
    public <T extends BotExecution> List<FunctionInfo> extractFunctionInfosFromInstance(T botExecutionInstance) {
        Class<?> clazz = botExecutionInstance.getClass();
        BotExecutor botExecutor = clazz.getAnnotation(BotExecutor.class);

        if (botExecutor == null || !botExecutor.enabled()) {
            return Collections.emptyList();
        }

        return extractFunctionInfosFromClass(clazz, botExecutor);
    }

    /**
     * 从指定类中提取函数信息
     */
    private List<FunctionInfo> extractFunctionInfosFromClass(Class<?> clazz, BotExecutor botExecutor) {
        List<FunctionInfo> functionInfos = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            ExecuteMethod executeMethod = method.getAnnotation(ExecuteMethod.class);
            if (executeMethod != null) {
                FunctionInfo functionInfo = createFunctionInfo(method, executeMethod, botExecutor);
                functionInfos.add(functionInfo);
            }
        }

        return functionInfos;
    }

    /**
     * 创建函数信息对象
     */
    private FunctionInfo createFunctionInfo(Method method, ExecuteMethod executeMethod, BotExecutor botExecutor) {
        String functionName = executeMethod.operation().isEmpty() ? botExecutor.name() + "_" + method.getName()
                : executeMethod.operation();
        // 标准化函数名称，确保符合 AI 规范
        functionName = sanitizeFunctionName(functionName);

        String description = botExecutor.description().isEmpty() ? "Function from " + botExecutor.name()
                : botExecutor.description();

        List<ParameterInfo> parameters = extractParameterInfos(method);

        return FunctionInfo.builder()
                .name(functionName)
                .description(description)
                .parameters(parameters)
                .method(method)
                .targetClass(method.getDeclaringClass())
                .logExecution(executeMethod.logExecution())
                .build();
    }

    /**
     * 标准化函数名称，确保符合 OpenAI 规范
     * 只保留字母、数字、下划线、点和连字符
     */
    private String sanitizeFunctionName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "unknown_function";
        }

        // 移除或替换不符合规范的字符
        String sanitized = originalName
                .replaceAll("[^a-zA-Z0-9_.-]", "_") // 将不符合规范的字符替换为下划线
                .replaceAll("_{2,}", "_") // 将连续的下划线替换为单个下划线
                .replaceAll("^_+|_+$", ""); // 移除开头和结尾的下划线

        // 确保函数名不为空
        if (sanitized.isEmpty()) {
            sanitized = "function_" + Math.abs(originalName.hashCode());
        }

        // 确保函数名以字母开头
        if (!sanitized.matches("^[a-zA-Z].*")) {
            sanitized = "func_" + sanitized;
        }

        return sanitized;
    }

    /**
     * 提取方法参数信息 - 增强版，支持复杂类型处理
     */
    private List<ParameterInfo> extractParameterInfos(Method method) {
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

            if (executeParam != null) {
                // 获取参数名
                String parameterName = getParameterNameSpringAIStyle(parameter, i);

                // 获取参数类型信息
                ParameterTypeInfo typeInfo = extractParameterTypeInfo(parameter);

                ParameterInfo paramInfo = ParameterInfo.builder()
                        .name(parameterName)
                        .required(executeParam.required())
                        .description(executeParam.description())
                        // 添加类型信息字段
                        .jsonSchemaType(typeInfo.getJsonSchemaType())

                        .type(parameter.getType())
                        .properties(typeInfo.getProperties())

                        .itemType(typeInfo.getItemType())
                        .itemProperties(typeInfo.getItemProperties())

                        .genericType(parameter.getParameterizedType())
                        .build();
                parameterInfos.add(paramInfo);
            }
        }

        return parameterInfos;
    }

    /**
     * 提取参数的详细类型信息
     */
    private ParameterTypeInfo extractParameterTypeInfo(Parameter parameter) {
        Class<?> parameterType = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        var builder = ParameterTypeInfo.builder();

        // 处理基本类型
        if (parameterType == String.class) {
            builder.jsonSchemaType("string");
        } else if (parameterType == Integer.class || parameterType == int.class) {
            builder.jsonSchemaType("integer");
        } else if (parameterType == Boolean.class || parameterType == boolean.class) {
            builder.jsonSchemaType("boolean");
        } else if (parameterType == Double.class || parameterType == double.class ||
                parameterType == Float.class || parameterType == float.class) {
            builder.jsonSchemaType("number");
        }
        // 处理 List 类型
        else if (List.class.isAssignableFrom(parameterType)) {
            builder.jsonSchemaType("array");

            // 提取泛型参数类型
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

                if (actualTypeArguments.length > 0) {
                    Type itemType = actualTypeArguments[0];
                    if (itemType instanceof Class) {
                        Class<?> itemClass = (Class<?>) itemType;
                        builder.itemType(itemClass);

                        // 如果是复杂对象，提取其属性
                        if (isComplexType(itemClass)) {
                            Map<String, Object> itemProperties = extractObjectProperties(itemClass);
                            builder.itemProperties(itemProperties);
                        }
                    }
                }
            }
        }
        // 处理 Map 类型
        else if (Map.class.isAssignableFrom(parameterType)) {
            builder.jsonSchemaType("object");
        }
        // 处理复杂对象类型
        else if (isComplexType(parameterType)) {
            builder.jsonSchemaType("object");
            Map<String, Object> properties = extractObjectProperties(parameterType);
            builder.properties(properties);
        }
        // 默认处理为字符串
        else {
            builder.jsonSchemaType("string");
        }

        return builder.build();
    }

    /**
     * 判断是否为复杂类型（非基本类型、非Java内置类型）
     */
    private boolean isComplexType(Class<?> clazz) {
        return !clazz.isPrimitive() &&
                !clazz.getName().startsWith("java.lang") &&
                !clazz.getName().startsWith("java.util") &&
                !clazz.getName().startsWith("java.time") &&
                clazz != Object.class;
    }

    /**
     * 提取复杂对象的属性信息
     */
    private Map<String, Object> extractObjectProperties(Class<?> clazz) {
        Map<String, Object> properties = new HashMap<>();

        try {
            // 使用 Jackson 的 ObjectMapper 来分析对象结构
            var jsonSchema = objectMapper.getTypeFactory().constructType(clazz);

            // 通过反射获取字段信息
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段和 serialVersionUID
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                        "serialVersionUID".equals(field.getName())) {
                    continue;
                }

                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                Map<String, Object> fieldSchema = new HashMap<>();

                // 设置字段类型
                if (fieldType == String.class) {
                    fieldSchema.put("type", "string");
                } else if (fieldType == Integer.class || fieldType == int.class) {
                    fieldSchema.put("type", "integer");
                } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                    fieldSchema.put("type", "boolean");
                } else if (fieldType == Double.class || fieldType == double.class ||
                        fieldType == Float.class || fieldType == float.class) {
                    fieldSchema.put("type", "number");
                } else if (List.class.isAssignableFrom(fieldType)) {
                    fieldSchema.put("type", "array");

                    // 处理 List 的泛型类型
                    Type genericFieldType = field.getGenericType();
                    if (genericFieldType instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericFieldType;
                        Type[] actualTypes = paramType.getActualTypeArguments();
                        if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                            Class<?> itemClass = (Class<?>) actualTypes[0];
                            if (isComplexType(itemClass)) {
                                fieldSchema.put("items", Map.of("type", "object"));
                            } else {
                                fieldSchema.put("items", Map.of("type", getJsonSchemaType(itemClass)));
                            }
                        }
                    }
                } else if (isComplexType(fieldType)) {
                    fieldSchema.put("type", "object");
                } else {
                    fieldSchema.put("type", "string");
                }

                // 添加字段描述（如果有注解的话）
                addFieldDescription(field, fieldSchema);

                properties.put(fieldName, fieldSchema);
            }

            // 也检查 getter 方法（for Lombok generated getters）
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (isGetterMethod(method)) {
                    String propertyName = getPropertyNameFromGetter(method);
                    if (!properties.containsKey(propertyName)) {
                        Class<?> returnType = method.getReturnType();
                        Map<String, Object> propertySchema = new HashMap<>();
                        propertySchema.put("type", getJsonSchemaType(returnType));
                        properties.put(propertyName, propertySchema);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Failed to extract properties for class: " + clazz.getName() + ", error: " + e.getMessage());
        }

        return properties;
    }

    /**
     * 为字段添加描述信息
     */
    private void addFieldDescription(java.lang.reflect.Field field, Map<String, Object> fieldSchema) {
        // 检查是否有 Jackson 注解
        com.fasterxml.jackson.annotation.JsonProperty jsonProperty = field
                .getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            fieldSchema.put("description", jsonProperty.value());
        }

        // 可以添加其他注解的处理，比如 @Schema, @ApiModelProperty 等
    }

    /**
     * 判断是否为 getter 方法
     */
    private boolean isGetterMethod(Method method) {
        return method.getName().startsWith("get") &&
                method.getParameterCount() == 0 &&
                method.getReturnType() != void.class &&
                !java.lang.reflect.Modifier.isStatic(method.getModifiers());
    }

    /**
     * 从 getter 方法名提取属性名
     */
    private String getPropertyNameFromGetter(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String propertyName = methodName.substring(3);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return methodName;
    }

    /**
     * 获取 JSON Schema 类型
     */
    private String getJsonSchemaType(Class<?> clazz) {
        if (clazz == String.class) {
            return "string";
        } else if (clazz == Integer.class || clazz == int.class) {
            return "integer";
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return "boolean";
        } else if (clazz == Double.class || clazz == double.class ||
                clazz == Float.class || clazz == float.class) {
            return "number";
        } else if (List.class.isAssignableFrom(clazz)) {
            return "array";
        } else if (Map.class.isAssignableFrom(clazz) || isComplexType(clazz)) {
            return "object";
        } else {
            return "string";
        }
    }

    /**
     * 获取参数名 - 优先使用注解的 name 属性
     */
    private String getParameterNameSpringAIStyle(Parameter parameter, int parameterIndex) {
        ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

        // 1. 优先使用注解的 name 属性
        if (executeParam != null) {
            String annotationName = executeParam.name();
            if (annotationName != null && !annotationName.trim().isEmpty()) {
                return annotationName;
            }
        }

        // 2. 如果注解没有 name 属性或为空，使用反射获取参数名
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }

        // 3. 最后使用位置索引
        return "arg" + parameterIndex;
    }

    /**
     * 执行指定的函数调用
     */
    public Object executeFunctionCall(String functionName, Map<String, Object> arguments) throws Exception {
        Optional<FunctionInfo> functionInfoOpt = findFunctionInfo(functionName);
        if (!functionInfoOpt.isPresent()) {
            throw new IllegalArgumentException("Function not found: " + functionName);
        }

        FunctionInfo functionInfo = functionInfoOpt.get();
        Object targetInstance = applicationContext.getBean(functionInfo.getTargetClass());

        // 4. 获取目标方法
        Method method = functionInfo.getMethod();
        // 准备方法参数
        Object[] methodArgs = prepareMethodArguments(method, arguments);

        // 执行方法
        if (functionInfo.isLogExecution()) {
            // TODO: 添加日志记录
        }

        return functionInfo.getMethod().invoke(targetInstance, methodArgs);
    }

    /**
     * 准备方法参数 - 修复版本，正确处理复杂类型转换
     */
    private Object[] prepareMethodArguments(Method method, Map<String, Object> argumentsMap) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

            if (executeParam != null) {
                String paramName = executeParam.name();
                Object argumentValue = argumentsMap.get(paramName);

                // 转换参数类型
                args[i] = convertArgumentToParameterType(argumentValue, parameter);
            }
        }

        return args;
    }

    /**
     * 将参数值转换为正确的参数类型
     */
    private Object convertArgumentToParameterType(Object argumentValue, Parameter parameter) {
        if (argumentValue == null) {
            return null;
        }

        Class<?> parameterType = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        try {
            // 处理 List 类型
            if (List.class.isAssignableFrom(parameterType)) {
                return convertToList(argumentValue, genericType);
            }

            // 处理基本类型
            if (parameterType == String.class) {
                return argumentValue.toString();
            } else if (parameterType == Integer.class || parameterType == int.class) {
                if (argumentValue instanceof Number) {
                    return ((Number) argumentValue).intValue();
                } else {
                    return Integer.parseInt(argumentValue.toString());
                }
            } else if (parameterType == Boolean.class || parameterType == boolean.class) {
                if (argumentValue instanceof Boolean) {
                    return argumentValue;
                } else {
                    return Boolean.parseBoolean(argumentValue.toString());
                }
            } else if (parameterType == Double.class || parameterType == double.class) {
                if (argumentValue instanceof Number) {
                    return ((Number) argumentValue).doubleValue();
                } else {
                    return Double.parseDouble(argumentValue.toString());
                }
            }

            // 如果参数类型就是期望的类型，直接返回
            if (parameterType.isAssignableFrom(argumentValue.getClass())) {
                return argumentValue;
            }

            // 处理复杂对象类型
            return objectMapper.convertValue(argumentValue, parameterType);

        } catch (Exception e) {
            throw new BusinessException("Failed to convert argument to parameter type: " + parameterType.getName(), e);
        }
    }

    /**
     * 转换为 List 类型，正确处理泛型
     */
    private List<?> convertToList(Object argumentValue, Type genericType) {
        if (!(argumentValue instanceof List)) {
            throw new BusinessException("Expected List type but got: " + argumentValue.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        List<Object> rawList = (List<Object>) argumentValue;

        // 获取泛型参数类型
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments.length > 0) {
                Type itemType = actualTypeArguments[0];

                if (itemType instanceof Class) {
                    Class<?> itemClass = (Class<?>) itemType;

                    // 转换列表中的每个元素
                    List<Object> convertedList = new ArrayList<>();
                    for (Object item : rawList) {
                        Object convertedItem = convertToSpecificType(item, itemClass);
                        convertedList.add(convertedItem);
                    }

                    return convertedList;
                }
            }
        }

        // 如果无法确定泛型类型，返回原始列表
        return rawList;
    }

    /**
     * 转换为特定类型
     */
    private Object convertToSpecificType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 如果已经是目标类型，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 处理基本类型
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        }

        // 处理复杂对象类型 - 使用 Jackson 进行转换
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new BusinessException("Failed to convert value to type: " + targetType.getName() +
                    ", value: " + value + ", valueType: " + value.getClass().getName(), e);
        }
    }

    /**
     * 在特定实例上执行函数调用
     * 
     * @param functionName         函数名
     * @param argumentsJson        参数数据
     * @param botExecutionInstance Bot 执行实例
     * @return 执行结果
     */
    public <T extends BotExecution> Object executeFunctionCallOnInstance(String functionName,
            String argumentsJson,
            T botExecutionInstance) throws Exception {
        // 从实例中查找对应的函数信息
        List<FunctionInfo> functionInfos = extractFunctionInfosFromInstance(botExecutionInstance);
        Optional<FunctionInfo> functionInfoOpt = functionInfos.stream()
                .filter(info -> info.getName().equals(functionName))
                .findFirst();

        if (!functionInfoOpt.isPresent()) {
            throw new IllegalArgumentException("Function not found in instance: " + functionName);
        }

        FunctionInfo functionInfo = functionInfoOpt.get();

        Map<String, Object> arguments;
        try {
            arguments = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("Failed to parse function arguments: " + argumentsJson, e);
        }

        // 准备方法参数
        // 4. 获取目标方法
        Method method = functionInfo.getMethod();

        Object[] methodArgs = prepareMethodArguments(method, arguments);

        // 在指定实例上执行方法
        if (functionInfo.isLogExecution()) {
            // TODO: 添加日志记录
            System.out.println("Executing function: " + functionName + " on instance: " +
                    botExecutionInstance.getClass().getSimpleName());
        }

        return functionInfo.getMethod().invoke(botExecutionInstance, methodArgs);
    }

    /**
     * 根据函数名查找对应的函数信息
     */
    public Optional<FunctionInfo> findFunctionInfo(String functionName) {
        return extractFunctionInfos().stream()
                .filter(info -> info.getName().equals(functionName))
                .findFirst();
    }
}