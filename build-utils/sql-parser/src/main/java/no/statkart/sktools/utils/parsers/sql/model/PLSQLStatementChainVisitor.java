package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Chained visitor for type.
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class PLSQLStatementChainVisitor<H extends PLSQLStatement> extends AbstractChainVisitor<H> implements PLSQLStatementVisitor<H> {
    protected PLSQLStatementChainVisitor(ExpressionVisitor<H> successor) {
        super(successor);
    }
}
