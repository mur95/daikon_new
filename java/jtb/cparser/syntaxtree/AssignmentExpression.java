//
// Generated by JTB 1.1.2
//

package jtb.cparser.syntaxtree;

// Grammar production:
// f0 -> UnaryExpression() AssignmentOperator() AssignmentExpression()
//       | ConditionalExpression()
public class AssignmentExpression implements Node {
  static final long serialVersionUID = 20050923L;

   public NodeChoice f0;

   public AssignmentExpression(NodeChoice n0) {
      f0 = n0;
   }

   public void accept(jtb.cparser.visitor.Visitor v) {
      v.visit(this);
   }
}
