package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Visitor interface for type
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public interface PromptStatementVisitor<T extends PromptStatement> extends ExpressionVisitor<T> {
    Object promptCase(T host, Object param);
}
