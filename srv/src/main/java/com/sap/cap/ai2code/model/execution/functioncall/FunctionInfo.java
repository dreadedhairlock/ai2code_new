package com.sap.cap.ai2code.model.execution.functioncall;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 函数信息封装类
 */
public class FunctionInfo {
    private String name;
    private String description;
    private List<ParameterInfo> parameters;
    private Method method;
    private Class<?> targetClass;
    private boolean logExecution;
    
    // 私有构造函数，强制使用 Builder
    private FunctionInfo() {}
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<ParameterInfo> getParameters() { return parameters; }
    public Method getMethod() { return method; }
    public Class<?> getTargetClass() { return targetClass; }
    public boolean isLogExecution() { return logExecution; }
    
    public static class Builder {
        private FunctionInfo functionInfo = new FunctionInfo();
        
        public Builder name(String name) {
            functionInfo.name = name;
            return this;
        }
        
        public Builder description(String description) {
            functionInfo.description = description;
            return this;
        }
        
        public Builder parameters(List<ParameterInfo> parameters) {
            functionInfo.parameters = parameters;
            return this;
        }
        
        public Builder method(Method method) {
            functionInfo.method = method;
            return this;
        }
        
        public Builder targetClass(Class<?> targetClass) {
            functionInfo.targetClass = targetClass;
            return this;
        }
        
        public Builder logExecution(boolean logExecution) {
            functionInfo.logExecution = logExecution;
            return this;
        }
        
        public FunctionInfo build() {
            return functionInfo;
        }
    }
}