package no.statkart.sktools.utils.databasepatcher.exception;

import org.apache.log4j.Logger;

/**
 * Thrown to indicate that a system error has occured which has been caused either
 * by a fault in the configuration (see {@link ConfigurationException})
 * or is due to an abnormal situation in the operational environment
 * (see {@link OperationalException}).
 */
public abstract class DatabasePatcherException extends RuntimeException {
    private String extraDetailMessage;
    private String errorCode;

    /**
     * Indikerer om exception er håndtert i en VM. Benyttes for eksempel for å unngå å vise samme feilmeldingsdialog
     * til brukeren flere ganger. Er transient slik at håndtering av feilen f.eks. på tjeneren ikke forhindrer håndtering
     * på klienten.
     */
    private transient boolean handled;

    public DatabasePatcherException(String message) {
        super(message);
    }

    public DatabasePatcherException(Logger logger, String message) {
        super(message);
        if( logger != null )
            logger.error(message);
    }

    public DatabasePatcherException(Logger logger, String message, Throwable cause) {
        super(message, cause);
        if( logger != null )
            logger.error(message, cause);
    }

    public DatabasePatcherException(Logger logger, String message, String errorCode, Throwable cause) {
        super(message, cause);
        if( logger != null )
            logger.error(message + " ErrorCode: " + errorCode, cause);
        this.errorCode = errorCode;
    }

    public DatabasePatcherException(Logger logger, String message, String errorCode, String detailMessage, Throwable cause) {
        super(message, cause);
        if( logger != null )
            logger.error(message + " ErrorCode: " + errorCode + " Details: " + detailMessage, cause);
        this.errorCode = errorCode;
        this.extraDetailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return extraDetailMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getCompleteMessage() {
        return errorCode + ": " + getMessage() + "\n" + extraDetailMessage;
    }

    /**
     * Appends a message to the possibly existing <code>detailMessage</code> of this exception.
     *
     * @param detailMessage a string to be append to the detail message
     */
    public void appendDetailMessage(String detailMessage){
        if (detailMessage==null) {
            this.extraDetailMessage = detailMessage;
        } else {
            this.extraDetailMessage = this.extraDetailMessage + "\n " + detailMessage + "\n";
        }
    }

    /**
     * Er denne feilen allerede håndtert av applikasjonen? På klienten vil normal håndtering være å vise en
     * feilmeldingsdialog til brukeren og så kaste feilen videre for å terminere aksjonen og evt. også logging
     * i Java Web Start sin log-fil.
     *
     * Håndtering av en feil betyr håndtering innenfor en Java VM. Typisk vil tjeneren og klienten kunne ha forskjellig
     * håndtering, tjeneren kan logge til spesiell logg-fil mens klienten f.eks. viser en dialog.
     *
     * @return true dersom denne systemfeilen allerede er "håndtert" av programmet
     */
    public boolean isHandled() {
        return handled;
    }

    /**
     * Angi om denne systemfeilen allerede er håndtert av applikasjonen (i kjørende Java VM).
     * @param b om håndtert
     */
    public void setHandled(boolean b) {
        this.handled = b;
    }
}
