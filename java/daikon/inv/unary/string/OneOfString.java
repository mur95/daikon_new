// ***** This file is automatically generated from OneOf.java.jpp

package daikon.inv.unary.string;

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

public final class OneOfString
  extends SingleString
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

  private String [] elts;
  private int num_elts;

  OneOfString (PptSlice ppt) {
    super(ppt);

    elts = new String[dkconfig_size];

    num_elts = 0;

  }

  public static OneOfString instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new OneOfString(ppt);
  }

  protected Object clone() {
    OneOfString result = (OneOfString) super.clone();
    result.elts = (String[]) elts.clone();

    result.num_elts = this.num_elts;

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

  static Comparator comparator = new UtilMDE.NullableStringComparator();

  private void sort_rep() {
    Arrays.sort(elts, 0, num_elts , comparator);
  }

  public String min_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();
    return elts[0];
  }

  public String max_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();
    return elts[num_elts-1];
  }

  // Assumes the other array is already sorted
  public boolean compare_rep(int num_other_elts, String [] other_elts) {
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
      sb.append((( elts[i] ==null) ? "null" : "\"" + UtilMDE.quote( elts[i] ) + "\""));
    }
    sb.append(" }");
    return sb.toString();
  }

  public String repr() {
    return "OneOfString" + varNames() + ": "
      + "falsified=" + falsified
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
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

      return varname + " == " + (( elts[0] ==null) ? "null" : "\"" + UtilMDE.quote( elts[0] ) + "\"");
    } else {
      return varname + " one of " + subarray_rep();
    }
  }

  private boolean is_type() {
    return var().name.hasNodeOfType(VarInfoName.TypeOf.class);
  }

  /*
    public String format_java() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < num_elts; i++) {
    sb.append (" || (" + var().name.java_name() + ".equals(" + (( elts[i] ==null) ? "null" : "\"" + UtilMDE.quote( elts[i] ) + "\"") + ")");
    sb.append (")");
    }
    // trim off the && at the beginning for the first case
    return sb.toString().substring (4);
    }
  */

  public String format_java() {
    //have to take a closer look at this!
    sort_rep();

    String varname = var().name.java_name();

    String result;

    result = "";
    boolean is_type = is_type();
    for (int i=0; i<num_elts; i++) {
      if (i != 0) { result += " || "; }
      result += varname;
      String str = elts[i];
      if (!is_type) {
        result += ".equals(" + (( str ==null) ? "null" : "\"" + UtilMDE.quote( str ) + "\"") + ")";
      } else {
        result += " == ";
        if ((str == null) || "null".equals(str)) {
          result += "== null)";
        } else if (str.startsWith("[")) {
          result += "(" + UtilMDE.classnameFromJvm(str) + ")";
        } else {
          if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length()-1);
          }
          result += "(" + str + ")";
        }
      }
    }

    return result;
  }

  /* IOA */
  public String format_ioa() {
    sort_rep();

    String varname = var().name.ioa_name();

    String result;

    result = "";
    for (int i=0; i<num_elts; i++) {
      if (i != 0) { result += " \\/ ("; }
      result += varname + " = " + (( elts[i] ==null) ? "null" : "\"" + UtilMDE.quote( elts[i] ) + "\"") + ")";
    }
    result += ")";

    /*
    result = "(";
    for (int i=0; i<num_elts; i++) {
      if (i != 0) { result += " \\/ ("; }
      result += varname + " = ";
      String str = elts[i];
      if (!is_type()) {
        result += (( str ==null) ? "null" : "\"" + UtilMDE.quote( str ) + "\"");
      } else {
        if ((str == null) || "null".equals(str)) {
          result += "\\typeof(null)";
        } else if (str.startsWith("[")) {
          result += "\\type(" + UtilMDE.classnameFromJvm(str) + ")";
        } else {
          if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length()-1);
          }
          result += "\\type(" + str + ")";
        }
        result += "***";   // to denote that it's not correct IOA syntax
      }
      result += ")";
    } // end for
    */

    return result;
  }

  private static String format_esc_string2type(String str) {
    if ((str == null) || "null".equals(str)) {
      return "\\typeof(null)";
    } else if (str.startsWith("[")) {
      return "\\type(" + UtilMDE.classnameFromJvm(str) + ")";
    } else {
      if (str.startsWith("\"") && str.endsWith("\"")) {
        str = str.substring(1, str.length()-1);
      }
      return "\\type(" + str + ")";
    }
  }

  public String format_esc() {
    sort_rep();

    String varname = var().name.esc_name();

    String result;

    // We cannot say anything about Strings in ESC, just types (which
    // Daikon stores as Strings).
    if (! is_type()) {
      result = format_unimplemented(OutputFormat.ESCJAVA); // "needs to be implemented"
    }

    // Format   \typeof(theArray) = "[Ljava.lang.Object;"
    //   as     \typeof(theArray) == \type(java.lang.Object[])
    // ... but still ...
    // format   \typeof(other) = "package.SomeClass;"
    //   as     \typeof(other) == \type(package.SomeClass)

    result = "";
    for (int i=0; i<num_elts; i++) {
      if (i != 0) { result += " || "; }
      result += varname + " == " + format_esc_string2type(elts[i]);
    }

    // Inner classes
    result = result.replace('$', '.');

    return result;
  }

  public String format_jml() {

    String varname = var().name.JMLElementCorrector().jml_name();

    String result;

    result = "";
    boolean is_type = is_type();
    for (int i=0; i<num_elts; i++) {
      if (i != 0) { result += " || "; }
      result += varname;
      String str = elts[i];
      if (!is_type) {
        result += ".equals(" + (( str ==null) ? "null" : "\"" + UtilMDE.quote( str ) + "\"") + ")";
      } else {
	result += " == \\type";
	if ((str == null) || "null".equals(str)) {
	  // "null" is not a type... Going to print as Object... should actually not print
	  result += "(java.lang.Object)";
	} else if (str.startsWith("[")) {
	  result += "(" + UtilMDE.classnameFromJvm(str) + ")";
	} else {
	  if (str.startsWith("\"") && str.endsWith("\"")) {
	    str = str.substring(1, str.length()-1);
	  }
	  result += "(" + str + ")";
	}
      }
    }

    return result;
  }

  public String format_simplify() {
    sort_rep();

    String varname = var().name.simplify_name();

    String result;

    result = "";
    boolean is_type = is_type();
    if (!is_type) {
      return "format_simplify " + this.getClass() + " cannot express Strings";
    }
    for (int i=0; i<num_elts; i++) {
      String value = elts[i];
      if (value == null) {
        // do nothing
      } else if (value.startsWith("[")) {
        value = UtilMDE.classnameFromJvm(value);
      } else if (value.startsWith("\"") && value.endsWith("\"")) {
        value = value.substring(1, value.length()-1);
      }
      value = "|T_" + value + "|";
      result += " (EQ " + varname + " " + value + ")";
    }
    if (num_elts > 1) {
      result = "(OR" + result + ")";
    } else {
      // chop leading space
      result = result.substring(1);
    }

    return result;
  }

  public void add_modified(String v, int count) {

    Assert.assertTrue(Intern.isInterned(v));

    for (int i=0; i<num_elts; i++)
      if (elts[i] == v) {

        return;

      }
    if (num_elts == dkconfig_size) {
      destroyAndFlow();
      return;
    }

    if (is_type() && (num_elts == 1)) {
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
    OneOfString other = (OneOfString) o;
    if (num_elts != other.num_elts)
      return false;
    if (num_elts == 0 && other.num_elts == 0)
      return true;

    sort_rep();
    other.sort_rep();

    for (int i=0; i < num_elts; i++) {
      if (! ( elts[i] == other.elts[i] ))
        return false;
    }

    return true;
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof OneOfString) {
      OneOfString other = (OneOfString) o;

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
  public static OneOfString find(PptSlice ppt) {
    Assert.assertTrue(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof OneOfString)
        return (OneOfString) inv;
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
