package com.sap.cap.ai2code.model.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteMethod {
    String operation() default "";
    // int timeout() default 30;
    boolean logExecution() default true;
}