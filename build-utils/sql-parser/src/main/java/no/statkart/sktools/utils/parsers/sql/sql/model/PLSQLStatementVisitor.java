package no.statkart.sktools.utils.parsers.sql.sql.model;

/**
 * Visitor interface for type
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public interface PLSQLStatementVisitor<T extends PLSQLStatement> extends ExpressionVisitor<T> {
    Object plsqlCase(T host, Object param);
}
