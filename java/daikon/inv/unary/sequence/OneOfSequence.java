// ***** This file is automatically generated from OneOf.java.jpp

package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.binary.sequenceScalar.*;
import daikon.inv.binary.twoSequence.SubSequence;

import utilMDE.*;

import java.util.*;
import java.io.*;

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class OneOfSequence
  extends SingleScalarSequence
  implements OneOf
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff OneOf invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  /**
   * Positive integer.  Specifies the maximum set size for this type
   * of invariant (x is one of 'n' items).
   **/

  public static int dkconfig_size = 3;

  // Probably needs to keep its own list of the values, and number of each seen.
  // (That depends on the slice; maybe not until the slice is cleared out.
  // But so few values is cheap, so this is quite fine for now and long-term.)

  private long[] [] elts;
  private int num_elts;

  /** Whether the variable's declared type is boolean. **/
  private boolean is_boolean;
  /** Whether the variable's declared type is hashcode. **/
  private boolean is_hashcode;

  OneOfSequence (PptSlice ppt) {
    super(ppt);

    Assert.assertTrue(var().type.isPseudoArray(),
                  "ProglangType must be pseudo-array for OneOfSequence" );

    // Elements are interned, so can test with ==
    // (in the general online case, it's not worth interning).
    elts = new long[dkconfig_size][];

    num_elts = 0;

    is_boolean = (var().file_rep_type.elementType() == ProglangType.BOOLEAN);
    is_hashcode = (var().file_rep_type.elementType() == ProglangType.HASHCODE);
    // System.out.println("is_hashcode=" + is_hashcode + " for " + format()
    //                    + "; file_rep_type=" + var().file_rep_type.format());

  }

  public static OneOfSequence instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new OneOfSequence(ppt);
  }

  protected Object clone() {
    OneOfSequence result = (OneOfSequence) super.clone();
    result.elts = (long[] []) elts.clone();

    for (int i=0; i < num_elts; i++) {
      result.elts[i] = (long[] ) elts[i].clone();
    }

    result.num_elts = this.num_elts;

    result.is_boolean = this.is_boolean;
    result.is_hashcode = this.is_hashcode;

    return result;
  }

  public int num_elts() {
    return num_elts;
  }

  public Object elt() {
    if (num_elts != 1)
      throw new Error("Represents " + num_elts + " elements");

    return elts[0];
  }

  static Comparator comparator = new ArraysMDE.LongArrayComparatorLexical();

  private void sort_rep() {
    Arrays.sort(elts, 0, num_elts , comparator);
  }

  public long[] min_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();
    return elts[0];
  }

  public long[] max_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();
    return elts[num_elts-1];
  }

  // Assumes the other array is already sorted
  public boolean compare_rep(int num_other_elts, long[] [] other_elts) {
    if (num_elts != num_other_elts)
      return false;
    sort_rep();
    for (int i=0; i < num_elts; i++)
      if (! ( elts[i] == other_elts[i] ) ) // elements are interned
        return false;
    return true;
  }

  private String subarray_rep() {
    // Not so efficient an implementation, but simple;
    // and how often will we need to print this anyway?
    sort_rep();
    StringBuffer sb = new StringBuffer();
    sb.append("{ ");
    for (int i=0; i<num_elts; i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(ArraysMDE.toString( elts[i] ));
    }
    sb.append(" }");
    return sb.toString();
  }

  public String repr() {
    return "OneOfSequence" + varNames() + ": "
      + "falsified=" + falsified
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
  }

  private boolean all_nulls(int value_no) {
    long[] seq = elts[value_no];
    for (int i=0; i<seq.length; i++) {
      if (seq[i] != 0) return false;
    }
    return true;
  }
  private boolean no_nulls(int value_no) {
    long[] seq = elts[value_no];
    for (int i=0; i<seq.length; i++) {
      if (seq[i] == 0) return false;
    }
    return true;
  }

  public String format_using(OutputFormat format) {
    sort_rep();
    if (format == OutputFormat.DAIKON) {
      return format_daikon();
    } else if (format == OutputFormat.JAVA) {
      return format_java();
    } else if (format == OutputFormat.IOA) {
      return format_ioa();
    } else if (format == OutputFormat.SIMPLIFY) {
      return format_simplify();
    } else if (format == OutputFormat.ESCJAVA) {
      return format_esc();
    } else if (format == OutputFormat.JML) {
      return format_jml();
    } else {
      return format_unimplemented(format);
    }
  }

  public String format_daikon() {
    String varname = var().name.name();
    if (num_elts == 1) {

      if (is_hashcode) {
        // we only have one value, because add_modified dies if more
        Assert.assertTrue(num_elts == 1);
        long[] value = elts[0];
        if (value.length == 0) {
          return varname + " == []";
        } else if ((value.length == 1) && (value[0] == 0)) {
          return varname + " == [null]";
        } else if (no_nulls(0)) {
          return varname + " contains no nulls and has only one value, of length " + value.length;
        } else if (all_nulls(0)) {
          return varname + " contains only nulls and has only one value, of length " + value.length;
        } else {
          return varname + " has only one value, of length " + value.length;
        }
      } else {
        return varname + " == " + ArraysMDE.toString( elts[0]);
      }

    } else {
      return varname + " one of " + subarray_rep();
    }
  }

  /*
    public String format_java() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < num_elts; i++) {
    sb.append (" || (" + var().name.java_name() + " == " + ArraysMDE.toString( elts[i]));
    sb.append (")");
    }
    // trim off the && at the beginning for the first case
    return sb.toString().substring (4);
    }
  */

  public String format_java() {
    //have to take a closer look at this!
    sort_rep();

    String result;

    String length = "";
    String forall = "";

    if (is_hashcode) {
      if (num_elts == 0)  {
        String classname = this.getClass().toString().substring(6); // remove leading "class"
        result = "warning: method " + classname + ".format_java() needs to be implemented: " + format();
      } else {
        Assert.assertTrue(num_elts == 1);
        // we only have one value, because add_modified dies if more
        long[] value = elts[0];
        if (var().name.isApplySizeSafe()) {
          length = var().name.applySize().java_name() + " == " + value.length;
        }
        if (no_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_java(new VarInfoName[] { var().name });
          forall = form[0] + "(" + form[1] + " != null)" + form[2];
        } else if (all_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_java(new VarInfoName[] { var().name });
          forall = form[0] + "(" + form[1] + " == null)" + form[2];
        }
      }
    }

    if (length == "" && forall == "") { // interned
      String classname = this.getClass().toString().substring(6); // remove leading "class"
      result = "warning: method " + classname + ".format_java() needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(" + length + ") && (" + forall + ")";
    }

    return result;
  }

  /* IOA */
  public String format_ioa() {
    sort_rep();

    String result;

    String length = "";
    String forall = "";

    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      Assert.assertTrue(num_elts == 1);
      long[] value = elts[0];
      if (var().name.isApplySizeSafe()) {
        length = "size("+var().name.ioa_name() + ") = " + value.length;
      }
      VarInfoName.QuantHelper.IOAQuantification quant = new VarInfoName.QuantHelper.IOAQuantification (var ());
      if (no_nulls(0)) {
        forall = quant.getQuantifierExp() + quant.getVarIndexed(0) + " ~= null " + quant.getClosingExp();
      } else if (all_nulls(0)) {
        forall = quant.getQuantifierExp() + quant.getVarIndexed(0) + " = null " + quant.getClosingExp();
      }
    }

    if (length == "" && forall == "") { // interned; can't say anything about size or elements
      String thisclassname = this.getClass().getName();
      result = "warning: " + thisclassname + ".format_ioa()  needs to be implemented: " + format();
    } else if (length == "") { // interned; can't say anything about size
      result = forall;
    } else if ((forall == "") || (elts[0].length == 0)) { // interned; can't say anything about elements
      result = length;
    } else { // Default, can say about both length and elements
      result = "(" + length + ") /\\ (" + forall + ")";
    }

    return result;
  }

  public String format_esc() {
    sort_rep();

    String result;

    String length = "";
    String forall = "";

    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      Assert.assertTrue(num_elts == 1);
      long[] value = elts[0];
      if (var().type.isArray()) {
        if (var().name.isApplySizeSafe()) {
          length = var().name.applySize().esc_name() + " == " + value.length;
        }
        if (no_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_esc(new VarInfoName[] { var().name });
          forall = form[0] + "(" + form[1] + " != null)" + form[2];
        } else if (all_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_esc(new VarInfoName[] { var().name });
          forall = form[0] + "(" + form[1] + " == null)" + form[2];
        }
      }
    }

    if (length == "" && forall == "") { // interned
      return format_unimplemented(OutputFormat.ESCJAVA); // "needs to be implemented"
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(" + length + ") && (" + forall + ")";
    }

    return result;
  }

  public String format_jml() {

    String result;

    String length = "";
    String forall = "";

    if (is_hashcode) {
      if (num_elts == 0)  {
        String classname = this.getClass().toString().substring(6); // remove leading "class"
        result = "warning: method " + classname + ".format_jml() needs to be implemented: " + format();
      } else {
        Assert.assertTrue(num_elts == 1);
        // we only have one value, because add_modified dies if more
        long[] value = elts[0];
        if (var().name.isApplySizeSafe()) {
          length = var().name.applySize().jml_name() + " == " + value.length;
        }
        if (no_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_jml(new VarInfoName[] { var().name });
          forall = form[0] + form[1] + " != null" + form[2];
        } else if (all_nulls(0)) {
          String[] form = VarInfoName.QuantHelper.format_jml(new VarInfoName[] { var().name });
          forall = form[0] + form[1] + " == null" + form[2];
        }
      }
    }

    if (length == "" && forall == "") { // interned
      String classname = this.getClass().toString().substring(6); // remove leading "class"
      result = "warning: method " + classname + ".format_jml() needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(" + length + ") && (" + forall + ")";
    }

    return result;
  }

  public String format_simplify() {
    sort_rep();

    String result;

    String length = "";
    String forall = "";
    if (is_hashcode) {
      // we only have one value, because add_modified dies if more
      Assert.assertTrue(num_elts == 1);
      long[] value = elts[0];
      if (var().name.isApplySizeSafe()) {
        length = "(EQ " + var().name.applySize().simplify_name() + " " + value.length + ")";
      }
      if (no_nulls(0)) {
        String[] form = VarInfoName.QuantHelper.format_simplify(new VarInfoName[] { var().name });
        forall = form[0] + "(NEQ " + form[1] + "  null)" + form[2];
      } else if (all_nulls(0)) {
        String[] form = VarInfoName.QuantHelper.format_simplify(new VarInfoName[] { var().name });
        forall = form[0] + "(EQ " + form[1] + "  null)" + form[2];
      }
    }
    if (length == "" && forall == "") { // interned
      String classname = this.getClass().toString().substring(6); // remove leading "class"
      result = "warning: method " + classname + ".format_simplify() needs to be implemented: " + format();
    } else if (length == "") { // interned
      result = forall;
    } else if (forall == "") { // interned
      result = length;
    } else {
      result = "(AND " + length + " " + forall + ")";
    }

    return result;
  }

  public void add_modified(long[] v, int count) {

    Assert.assertTrue(Intern.isInterned(v));

    for (int i=0; i<num_elts; i++)
      if (elts[i] == v) {

        return;

      }
    if (num_elts == dkconfig_size) {
      destroyAndFlow();
      return;
    }

    if (is_hashcode && (num_elts == 1)) {
      destroyAndFlow();
      return;
    }

    // We are significantly changing our state (not just zeroing in on
    // a constant), so we have to flow a copy before we do so.  We even
    // need to clone if this has 0 elements becuase otherwise, lower
    // ppts will get versions of this with multiple elements once this is
    // expanded.
    cloneAndFlow();

    elts[num_elts] = v;
    num_elts++;

  }

  protected double computeProbability() {
    // This is not ideal.
    if (num_elts == 0) {
      return Invariant.PROBABILITY_UNJUSTIFIED;
    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  // Use isObviousDerived since some isObviousImplied methods already exist.
  public boolean isObviousDerived() {
    // Static constants are necessarily OneOf precisely one value.
    // This removes static constants from the output, which might not be
    // desirable if the user doesn't know their actual value.
    if (var().isStaticConstant()) {
      Assert.assertTrue(num_elts <= 1);
      return true;
    }
    return super.isObviousDerived();
  }

  public boolean isSameFormula(Invariant o)
  {
    OneOfSequence other = (OneOfSequence) o;
    if (num_elts != other.num_elts)
      return false;
    if (num_elts == 0 && other.num_elts == 0)
      return true;

    sort_rep();
    other.sort_rep();

    // All nonzero hashcode values should be considered equal to each other
    //
    // Examples:
    // inv1     inv2     result
    // -------  -------  ------
    // {19,23}  {91,0}   false
    // {19,23}  {91,32}  true
    // {19,0}   {91,0}   true
    // {0,0}    {0,0}    true

    if (is_hashcode && other.is_hashcode) {
      // we only have one value, because add_modified dies if more
      Assert.assertTrue(num_elts == 1 && other.num_elts == 1);

      long[] thisSeq = elts[0];
      long[] otherSeq = other.elts[0];
      if (thisSeq.length != otherSeq.length) {
        return false;
      }

      for (int i=0; i < thisSeq.length; i++) {
        if ((thisSeq[i] == 0 && otherSeq[i] != 0) ||
            (thisSeq[i] != 0 && otherSeq[i] == 0)) {
          return false;
        }
      }

      return true;
    }

    for (int i=0; i < num_elts; i++) {
      if (! ( elts[i] == other.elts[i] ))
        return false;
    }

    return true;
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof OneOfSequence) {
      OneOfSequence other = (OneOfSequence) o;

      for (int i=0; i < num_elts; i++) {
        for (int j=0; j < other.num_elts; j++) {
          if (( elts[i] == other.elts[j] ) ) // elements are interned
            return false;
        }
      }
      return true;
    }

    return false;
  }

  // OneOf invariants that indicate a small set of possible values are
  // uninteresting.  OneOf invariants that indicate exactly one value
  // are interesting.
  public boolean isInteresting() {
    if (num_elts() > 1) {
      return false;
    } else {
      return true;
    }
  }

  // Look up a previously instantiated invariant.
  public static OneOfSequence find(PptSlice ppt) {
    Assert.assertTrue(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof OneOfSequence)
        return (OneOfSequence) inv;
    }
    return null;
  }

  // Interning is lost when an object is serialized and deserialized.
  // Manually re-intern any interned fields upon deserialization.
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    for (int i=0; i < num_elts; i++)
      elts[i] = Intern.intern(elts[i]);
  }

}
