//
// Generated by JTB 1.3.2
//

package jtb.syntaxtree;

// Grammar production:
// f0 -> <IDENTIFIER>
// f1 -> [ Arguments() ]
// f2 -> [ ClassOrInterfaceBody(false) ]
public class EnumConstant implements Node {
   static final long serialVersionUID = 20050923L;

   private Node parent;
   public NodeToken f0;
   public NodeOptional f1;
   public NodeOptional f2;

   public EnumConstant(NodeToken n0, NodeOptional n1, NodeOptional n2) {
      f0 = n0;
      if ( f0 != null ) f0.setParent(this);
      f1 = n1;
      if ( f1 != null ) f1.setParent(this);
      f2 = n2;
      if ( f2 != null ) f2.setParent(this);
   }

   public void accept(jtb.visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(jtb.visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
   public <R> R accept(jtb.visitor.GJNoArguVisitor<R> v) {
      return v.visit(this);
   }
   public <A> void accept(jtb.visitor.GJVoidVisitor<A> v, A argu) {
      v.visit(this,argu);
   }
   public void setParent(Node n) { parent = n; }
   public Node getParent()       { return parent; }
}
