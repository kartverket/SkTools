package no.statkart.sktools.utils.databasepatcher.exception;

import org.apache.log4j.Logger;

public class NotFoundException extends DatabasePatcherException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Logger logger, String message) {
        super(logger, message);
    }

    public NotFoundException(Logger logger, String message, Throwable cause) {
        super(logger, message, cause);
    }

    public NotFoundException(Logger logger, String message, String errorCode, Throwable cause) {
        super(logger, message, errorCode, cause);
    }

    public NotFoundException(Logger logger, String message, String errorCode, String detailMessage, Throwable cause) {
        super(logger, message, errorCode, detailMessage, cause);
    }
}
