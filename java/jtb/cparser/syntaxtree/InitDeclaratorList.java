//
// Generated by JTB 1.1.2
//

package jtb.cparser.syntaxtree;

// Grammar production:
// f0 -> InitDeclarator()
// f1 -> ( "," InitDeclarator() )*
public class InitDeclaratorList implements Node {
  static final long serialVersionUID = 20050923L;

   public InitDeclarator f0;
   public NodeListOptional f1;

   public InitDeclaratorList(InitDeclarator n0, NodeListOptional n1) {
      f0 = n0;
      f1 = n1;
   }

   public void accept(jtb.cparser.visitor.Visitor v) {
      v.visit(this);
   }
}
