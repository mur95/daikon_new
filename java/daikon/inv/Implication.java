package daikon.inv;

import daikon.*;
import daikon.inv.DiscardInfo;

import java.util.*;

import utilMDE.*;

// Here Implication is reimplemented as an extension of the new general
// Joiner class

/**
 * The Implication invariant class is used internally within Daikon to
 * handle invariants that are only true when certain other conditions are
 * also true (splitting).
 **/
public class Implication
  extends Joiner
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20030822L;

  public Invariant predicate() { return left; }
  public Invariant consequent() { return right; }
  public boolean iff;

  protected Implication(PptSlice ppt, Invariant predicate, Invariant consequent, boolean iff) {
    super(ppt, predicate, consequent);
    this.iff = iff;
  }

  /**
   * Creates a new Implication Invariant from the predicate,
   * consequent and the boolean iff and adds it to the PptTopLevel.
   *
   * @return null if predicate and the consequent are the same, or if
   * the PptTopLevel already contains this Implication.
   **/
   public static Implication makeImplication(PptTopLevel ppt,
                                             Invariant predicate,
                                             Invariant consequent,
                                             boolean iff)
  {
    if (predicate.isSameInvariant(consequent)) {
      return null;
    }

    // Don't add this Implication to the program point if the program
    // point already has this implication.  This is slow and dumb; we
    // should use hashing for O(1) check instead.
    for (Iterator i = ppt.joiner_view.invs.iterator(); i.hasNext(); ) {
      Invariant nextInv = (Invariant)i.next();
      if (!(nextInv instanceof Implication)) continue;
      Implication existing = (Implication) nextInv;
      if (existing.iff != iff) continue;
      if (existing.right.getClass() != consequent.getClass()) continue;
      if (existing.left.getClass() != predicate.getClass()) continue;
      // Why not instead check var_info indices plus isSameFormula?
      // Should use PptSlice identity check instead?
      if (! existing.right.format().equals(consequent.format())) continue;
      if (! existing.left.format().equals(predicate.format())) continue;
      return null;
    }

    // System.out.println("Adding implication: ");
    // System.out.println("Predicate: " + predicate.format_using(OutputFormat.JML));
    // System.out.println("Consequent: " + consequent.format_using(OutputFormat.JML));
    Implication result = new Implication(ppt.joiner_view, predicate, consequent, iff);
    return result;
  }

  protected double computeConfidence() {
    double pred_conf = left.computeConfidence();
    double cons_conf = right.computeConfidence();
    if ((pred_conf == CONFIDENCE_NEVER)
        || (cons_conf == CONFIDENCE_NEVER)) {
      return CONFIDENCE_NEVER;
    }
    return confidence_and(pred_conf, cons_conf);
  }

  public String repr() {
    return "[Implication: " + left.repr()
      + " => " + right.repr() + "]";
  }

  public String format_using(OutputFormat format) {
    String pred_fmt = left.format_using(format);
    String consq_fmt = right.format_using(format);
    if (format == OutputFormat.DAIKON || format == OutputFormat.JML) {
      String arrow = (iff ? "  <==>  " : "  ==>  "); // "interned"
      return "(" + pred_fmt + ")" + arrow + "(" + consq_fmt + ")";
    } else if (format == OutputFormat.IOA) {
      String arrow = (iff ? "  <=>  " : "  =>  ");
      return "(" + pred_fmt + ")" + arrow + "(" + consq_fmt + ")";
    } else if (format == OutputFormat.ESCJAVA) {
      String arrow = (iff ? "  ==  " : "  ==>  "); // "interned"
      return "(" + pred_fmt + ")" + arrow + "(" + consq_fmt + ")";
    } else if (format == OutputFormat.JAVA) {
      String mid = (iff ? " == " : " || !"); // "interned"
      return "(" + consq_fmt + ")" + mid + "(" + pred_fmt + ")";
    } else if (format == OutputFormat.SIMPLIFY) {
      String cmp = (iff ? "IFF" : "IMPLIES");
      return "(" + cmp + " " + pred_fmt + " " + consq_fmt + ")";
    } else if (format == OutputFormat.DBCJAVA) {
      if ( iff )
        // RRN: I'm not sure if this actually works for DBC;
        //      there seems to be no dedicated biconditional, however.
        // CP: it should work if expressions always return boolean
        //     which I am fixing to make sure of.
        return "((" + pred_fmt + ") == (" + consq_fmt + "))";
      else
        return "(" + pred_fmt + " $implies " + consq_fmt + ")";
    } else {
      return format_unimplemented(format);
    }
  }

  public DiscardInfo isObviousStatically(VarInfo[] vis) {
    return right.isObviousStatically(vis);
  }

  public boolean isSameFormula(Invariant other) {
    Implication other_implic = (Implication)other;
    // Guards are necessary because the contract of isSameFormula states
    // that the argument is of the same class as the receiver.
    return (((predicate().getClass() == other_implic.predicate().getClass())
            && predicate().isSameFormula(other_implic.predicate()))
            && ((consequent().getClass() == other_implic.consequent().getClass())
            && consequent().isSameFormula(other_implic.consequent())));
  }

  /* [INCR]
  public boolean hasOnlyConstantVariables() {
    // The old version of this code only looked at the predicate, but
    // it was almost never used, since the OnlyConstantVariablesFilter
    // had a separate check for "inv instanceof Implication" that
    // looked at both the predicate and the consequent. I also can't
    // think of a reason why it would make sense to only check the
    // predicate. -SMcC
    return consequent.hasOnlyConstantVariables()
      || predicate.hasOnlyConstantVariables();
  }
  */ // ... [INCR]

  // An implication is only interesting if both the predicate and
  // consequent are interesting
  public boolean isInteresting() {
    return (predicate().isInteresting() && consequent().isInteresting());
  }

  // If a constant managed to appear in a predicate, that's
  // interesting enough for us.
  public boolean hasUninterestingConstant() {
    return consequent().hasUninterestingConstant();
  }

  public boolean isAllPrestate() {
    return predicate().isAllPrestate() && consequent().isAllPrestate();
  }
}
