package no.statkart.sktools.utils.databasepatcher.exception;

import org.apache.log4j.Logger;

public class OperationalException extends DatabasePatcherException {

    public OperationalException(String message) {
        super(message);
    }

    public OperationalException(Logger logger, String message) {
        super(logger, message);
    }

    public OperationalException(Logger logger, String message, Throwable cause) {
        super(logger, message, cause);
    }

    public OperationalException(Logger logger, String message, String errorCode, Throwable cause) {
        super(logger, message, errorCode, cause);
    }

    public OperationalException(Logger logger, String message, String errorCode, String detailMessage, Throwable cause) {
        super(logger, message, errorCode, detailMessage, cause);
    }

}
