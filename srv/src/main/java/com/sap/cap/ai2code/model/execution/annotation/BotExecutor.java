package com.sap.cap.ai2code.model.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotExecutor {
    String name();
    String description() default "";
    String version() default "1.0";
    boolean enabled() default true;
}