package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Interface for alle noder i AST treet
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public interface Expression {

    Object execute(ExpressionVisitor<Expression> algorithm, Object param);

    public int getLineNumber();
}
