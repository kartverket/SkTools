package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Represents a PROMPT comment statement
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public class PromptStatement extends Comment {

    @Override
    public Object execute(ExpressionVisitor<Expression> algorithm, Object param) {
        if (algorithm instanceof PromptStatementVisitor) {
            return ((PromptStatementVisitor) algorithm).promptCase(this, param);
        } else {
            return algorithm.defaultCase(this, param);
        }
    }
}
