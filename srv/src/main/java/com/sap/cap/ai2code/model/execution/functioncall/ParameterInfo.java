package com.sap.cap.ai2code.model.execution.functioncall;

import lombok.Builder;
import lombok.Data;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 参数信息
 */
@Data
@Builder
public class ParameterInfo {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数类型
     */
    private Class<?> type;

    /**
     * 对象属性定义 (for complex object parameters)
     */
    private Map<String, Object> properties;

    /**
     * 泛型类型
     */
    private Type genericType;

    /**
     * 是否必需
     */
    private boolean required;

    /**
     * 参数描述
     */
    private String description;

    /**
     * JSON Schema 类型
     */
    private String jsonSchemaType;

    /**
     * 数组元素类型 (for List parameters)
     */
    private Class<?> itemType;

    /**
     * 对象属性定义 (for complex object parameters)
     */
    private Map<String, Object> itemProperties;

}