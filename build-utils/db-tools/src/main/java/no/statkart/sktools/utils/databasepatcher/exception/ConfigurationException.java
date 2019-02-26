package no.statkart.sktools.utils.databasepatcher.exception;

import org.slf4j.Logger;

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
}
