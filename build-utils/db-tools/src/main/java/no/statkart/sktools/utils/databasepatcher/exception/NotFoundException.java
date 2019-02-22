package no.statkart.sktools.utils.databasepatcher.exception;

import org.slf4j.Logger;

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
}
