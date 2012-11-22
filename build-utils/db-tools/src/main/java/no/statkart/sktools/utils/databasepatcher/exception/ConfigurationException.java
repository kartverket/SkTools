package no.statkart.sktools.utils.databasepatcher.exception;

import org.apache.log4j.Logger;

public class ConfigurationException extends DatabasePatcherException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Logger logger, String message) {
        super(logger, message);
    }

    public ConfigurationException(Logger logger, String message, Throwable cause) {
        super(logger, message, cause);
    }

    public ConfigurationException(Logger logger, String message, String errorCode, Throwable cause) {
        super(logger, message, errorCode, cause);
    }

    public ConfigurationException(Logger logger, String message, String errorCode, String detailMessage, Throwable cause) {
        super(logger, message, errorCode, detailMessage, cause);
    }

}
