package no.statkart.sktools.utils.parsers.sql.model;


/**
 * Visitor interface for type
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public interface ExpressionVisitor<H extends Expression> {

    public Object defaultCase(H host, Object param);

}

