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

    public static BusinessException taskTypeNotFound(String taskTypeID){
        return new BusinessException("TaskType " + taskTypeID + " not found.");
    }

    public static BusinessException botInstanceNotFound(String botInstanceID){
        return new BusinessException("BotInstance " + botInstanceID + " not found.");
    }

    public static BusinessException botTypeNotFound(String botTypeID){
        return new BusinessException("Bot Type " + botTypeID + " not found.");
    }

    public static BusinessException failAdopt(String errorMessage, Exception exception){
        return new BusinessException("Failed to adopt bot messages: " + errorMessage, exception);
    }

    public static BusinessException failExecute(String errorMessage, Exception exception){
        return new BusinessException("Failed to execute bot: " + errorMessage, exception);
    }

    public static BusinessException executeMethodNotFound(){
        return new BusinessException("execute method in class not found!");
    }

    public static BusinessException implementationClassMissing(){
        return new BusinessException("Implementation class not specified in bot type!");
    }

    public static BusinessException codeBotFailExecute(String errorMessage, Exception exception){
        return new BusinessException("Failed to execute code bot: " + errorMessage, exception);
    }

    public static BusinessException functionCallBotFailExecute(String errorMessage, Exception exception){
        return new BusinessException("Failed to execute function call bot: " + errorMessage, exception);
    }

    // TO ADD NEW EXCEPTIONS, FOLLOW THE TEMPLATE AS ABOVE!

}

