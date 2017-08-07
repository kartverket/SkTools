package no.statkart.sktools.utils.parsers.sql.model;

/**
 * Representerer kommentar som er inlinet i scripts med
 */
public class InlineComment extends LineComment {

   @Override
   public Object execute(ExpressionVisitor<Expression> algorithm, Object param) {
      if (algorithm instanceof LineCommentVisitor) {
         return ((LineCommentVisitor) algorithm).inlineComment(this, param);
      } else {
         return super.execute(algorithm, param);
      }
   }
}
