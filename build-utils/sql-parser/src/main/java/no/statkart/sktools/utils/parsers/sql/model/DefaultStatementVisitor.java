package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Visitor interface for type
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public interface DefaultStatementVisitor<T extends DefaultStatement> extends ExpressionVisitor<T> {

    Object defaultStatementCase(DefaultStatement host, Object param);

}
