package no.statkart.sktools.utils.parsers.sql.sql.model;

/**
 * Type for kjedet visitor.
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class DefaultStatementChainVisitor<H extends DefaultStatement> extends AbstractChainVisitor<H> implements DefaultStatementVisitor<H> {

    protected DefaultStatementChainVisitor(ExpressionVisitor<H> successor) {
        super(successor);
    }

}
