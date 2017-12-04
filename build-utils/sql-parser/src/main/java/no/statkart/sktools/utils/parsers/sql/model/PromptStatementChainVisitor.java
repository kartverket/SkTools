package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Type for kjedet visitor.
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class PromptStatementChainVisitor<H extends PromptStatement> extends AbstractChainVisitor<H> implements PromptStatementVisitor<H> {

    public PromptStatementChainVisitor(ExpressionVisitor<H> successor) {
        super(successor);
    }
}
