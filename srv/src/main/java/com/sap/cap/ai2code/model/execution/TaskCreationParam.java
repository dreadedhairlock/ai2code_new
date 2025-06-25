package com.sap.cap.ai2code.model.execution;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task creation parameter model
 * Contains only core fields: sequence, name, description, contextPath
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreationParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Execution sequence for the task
     */
    private Integer sequence;

    /**
     * Task name
     */
    private String name;

    /**
     * Task description
     */
    private String description;

    /**
     * Context path for the task
     * Optional field, used to specify where task context should be stored
     */
    private String contextPath;
}