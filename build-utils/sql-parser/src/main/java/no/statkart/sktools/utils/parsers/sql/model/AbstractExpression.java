package no.statkart.sktools.utils.parsers.sql.model;


/**
 * Felles funksjonalitet for alle noder.
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class AbstractExpression implements Expression {

    int lineNumber;

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "line " + lineNumber + "\ttype: " + getClass().getSimpleName();
    }
}
