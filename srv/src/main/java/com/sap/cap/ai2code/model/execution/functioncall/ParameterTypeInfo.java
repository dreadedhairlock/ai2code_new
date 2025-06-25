package com.sap.cap.ai2code.model.execution.functioncall;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 参数类型信息
 */
@Data
@Builder
public class ParameterTypeInfo {
    
    /**
     * JSON Schema 类型 (string, integer, boolean, number, array, object)
     */
    private String jsonSchemaType;
    
    /**
     * 数组元素类型 (for array types)
     */
    private Class<?> itemType;
    
    /**
     * 对象属性 (for object types)
     */
    private Map<String, Object> properties;
    
    /**
     * 数组元素属性 (for array of objects)
     */
    private Map<String, Object> itemProperties;
}