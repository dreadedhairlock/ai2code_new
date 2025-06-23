package com.sap.cap.ai2code.exception;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

// Rename from AIServiceException.java
public class BusinessException extends ServiceException {
    public BusinessException(String message) {
        super(ErrorStatuses.BAD_REQUEST, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(ErrorStatuses.BAD_REQUEST, message, cause);
    }

    public static BusinessException notFound(String entity) {
        return new BusinessException(entity + "_not_found");
    }

    public static BusinessException parsingError(String type, Throwable cause) {
        return new BusinessException("Error_When_Parsing_" + type, cause);
    }
}