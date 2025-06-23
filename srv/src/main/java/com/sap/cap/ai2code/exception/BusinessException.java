package com.sap.cap.ai2code.exception;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

public class BusinessException extends ServiceException {

    public BusinessException(String message) {
        super(ErrorStatuses.BAD_REQUEST, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(ErrorStatuses.BAD_REQUEST, message, cause);
    }

    public static BusinessException emptyTaskTypeID(){
        return new BusinessException("Task type ID is required.");
    }

    public static BusinessException emptyTaskName(){
        return new BusinessException("Task name is required.");
    }

    public static BusinessException failAPICall(int statusCode, String body){
        return new BusinessException("API call failed: " + statusCode + " - " + body);
    }

}

