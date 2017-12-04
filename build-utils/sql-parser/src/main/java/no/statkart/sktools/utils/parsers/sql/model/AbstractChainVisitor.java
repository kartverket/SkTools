package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Felleslogikk for kjedede-visitorer
 *
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class AbstractChainVisitor<H extends Expression> implements ExpressionVisitor<H> {

    protected final ExpressionVisitor successor; //next in chain

    protected AbstractChainVisitor(ExpressionVisitor<H> successor) {
        this.successor = successor;
    }


    public Object defaultCase(H host, Object param) {
        return host.execute(successor, param);
    }

}
