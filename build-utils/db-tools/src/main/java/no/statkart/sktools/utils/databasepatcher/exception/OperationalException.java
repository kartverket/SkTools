package no.statkart.sktools.utils.databasepatcher.exception;

import org.slf4j.Logger;

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
}
