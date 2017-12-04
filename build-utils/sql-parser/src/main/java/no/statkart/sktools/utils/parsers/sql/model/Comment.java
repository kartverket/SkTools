package no.statkart.sktools.utils.parsers.sql.model;

/**
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class Comment extends AbstractExpression {

    protected String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
