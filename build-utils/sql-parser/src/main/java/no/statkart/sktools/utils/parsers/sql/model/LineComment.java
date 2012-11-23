package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Representerer kommentar i sql filer. En linje som starter med -- og ikke ligger inne i en statement betraktes som kommentar
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class LineComment extends Comment {

    @Override
    public Object execute(ExpressionVisitor<Expression> algorithm, Object param) {
        if (algorithm instanceof LineCommentVisitor) {
            return ((LineCommentVisitor) algorithm).commentCase(this, param);
        } else {
            return algorithm.defaultCase(this, param);
        }
    }
}
