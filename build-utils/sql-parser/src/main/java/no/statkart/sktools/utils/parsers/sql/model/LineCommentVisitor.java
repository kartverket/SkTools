package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Visitor interface for type
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public interface LineCommentVisitor<T extends LineComment> extends ExpressionVisitor<T> {
    Object commentCase(T host, Object param);
}
