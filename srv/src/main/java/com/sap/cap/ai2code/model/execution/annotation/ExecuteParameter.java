// code\srv\src\main\java\customer\ai2code\service\execution\Parameter.java
package com.sap.cap.ai2code.model.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteParameter {
    /**
     * 参数名称（必须与实际参数名一致）
     */
    String name();

    /**
     * 是否必需参数
     */
    boolean required() default false;

    /**
     * 参数描述
     */
    String description();
}