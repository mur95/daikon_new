//
// Generated by JTB 1.3.2
//

package jtb.syntaxtree;

// Grammar production:
// f0 -> "<"
// f1 -> TypeParameter()
// f2 -> ( "," TypeParameter() )*
// f3 -> ">"
public class TypeParameters implements Node {
   static final long serialVersionUID = 20050923L;

   private Node parent;
   public NodeToken f0;
   public TypeParameter f1;
   public NodeListOptional f2;
   public NodeToken f3;

   public TypeParameters(NodeToken n0, TypeParameter n1, NodeListOptional n2, NodeToken n3) {
      f0 = n0;
      if ( f0 != null ) f0.setParent(this);
      f1 = n1;
      if ( f1 != null ) f1.setParent(this);
      f2 = n2;
      if ( f2 != null ) f2.setParent(this);
      f3 = n3;
      if ( f3 != null ) f3.setParent(this);
   }

   public TypeParameters(TypeParameter n0, NodeListOptional n1) {
      f0 = new NodeToken("<");
      if ( f0 != null ) f0.setParent(this);
      f1 = n0;
      if ( f1 != null ) f1.setParent(this);
      f2 = n1;
      if ( f2 != null ) f2.setParent(this);
      f3 = new NodeToken(">");
      if ( f3 != null ) f3.setParent(this);
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
