package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Type for kjedet visitor.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public abstract class LineCommentChainVisitor<H extends LineComment> extends AbstractChainVisitor<H> implements LineCommentVisitor<H> {

    protected LineCommentChainVisitor(ExpressionVisitor<H> successor) {
        super(successor);
    }
}
