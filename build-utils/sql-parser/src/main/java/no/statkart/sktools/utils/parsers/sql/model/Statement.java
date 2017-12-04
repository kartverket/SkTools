package no.statkart.sktools.utils.parsers.sql.model;

/**
 * @author Leif Lislegård
 * @since 0.1
 */
public abstract class Statement extends AbstractExpression {

    protected String sql;


    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
