package daikon;

import daikon.derive.*;
import daikon.derive.unary.*;
import daikon.derive.binary.*;
import daikon.inv.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.string.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.stringsequence.*;
import daikon.inv.ternary.threeScalar.*;
import daikon.split.*;
import daikon.split.dsaa.*;
import daikon.split.griesLisp.*;
import daikon.split.weissDsaaMDE.*;

import java.util.*;
import com.oroinc.text.regex.*;

import utilMDE.*;




// All information about a single program point.
// A Ppt may also represent just part of the data: a disjunction.
// This probably doesn't do any direct computation, instead deferring that
// to its views that are slices.

public class PptTopLevel extends Ppt {

  // do we need both a num_tracevars for the number of variables in the
  // tracefile and a num_non_derived_vars for the number of variables
  // actually passed off to this Ppt?  The ppt wouldn't use num_tracevars,
  // but it would make sense to store it here anyway.

  // These values are -1 if not yet set (can that happen?).
  int num_declvars;             // number of variables in the decl file
  int num_tracevars;            // number of variables in the trace file
  int num_orig_vars;            // number of _orig vars
  int num_static_constant_vars; // these don't appear in the trace file
  // invariant:  num_declvars == num_tracevars + num_orig_vars

  // Indicates whether derived variables have been introduced.
  // First number: invariants are computed up to this index, non-inclusive
  // Remaining numbers: values have been derived from up to these indices

  // invariant:  len(var_infos) >= invariants_index >= derivation_index[0]
  //   >= derivation_index[1] >= ...
  int[] derivation_indices = new int[derivation_passes+1];

  transient VarValuesOrdered values;
  // these are used only when values is null
  int values_num_samples;
  int values_num_mod_non_missing_samples;
  int values_num_values;
  String values_tuplemod_samples_summary;

  PptTopLevel entry_ppt;        // null if this isn't an exit point
  PptTopLevel object_ppt;       // null if this doesn't contribute to the object ppt

  // These accessors are for abstract methods declared in Ppt
  public int num_samples() {
    return (values == null) ? values_num_samples : values.num_samples(); }
  public int num_mod_non_missing_samples() {
      return ((values == null)
              ? values_num_mod_non_missing_samples
              : values.num_mod_non_missing_samples()); }
  // WARNING!  This is the number of distinct ValueTuple objects,
  // which can be as much as 2^arity times as many as the number of
  // distinct tuples of values.
  public int num_values() {
    return (values == null) ? values_num_values : values.num_values(); }
  // public int num_missing() { return values.num_missing; }
  public String tuplemod_samples_summary() {
      return ((values == null)
              ? values_tuplemod_samples_summary
              : values.tuplemod_samples_summary());
  }
  public void set_values_null() {
    values_num_samples = num_samples();
    values_num_mod_non_missing_samples = num_mod_non_missing_samples();
    values_num_values = num_values();
    values_tuplemod_samples_summary = tuplemod_samples_summary();
    values = null;
  }
  // This method is added as somewhat of a hack for Melissa's gui.  In the gui,
  // PptTopLevel are stored as nodes in a tree.  Swing obtains the string to
  // display in the actualy JTree by calling toString().  For class-level Ppt's,
  // we just want "Sort" instead of "Sort:::CLASS".  For other Ppt's, we want
  // "EXIT184" instead of "Sort.swapReferences([Ljava/lang/Object;II)V:::EXIT184"
  public String toString() {
    if (name.indexOf( "CLASS" ) != -1) { // if this is a class Ppt
      return name.substring( 0, name.indexOf( FileIO.ppt_tag_separator ));
    else
      return name.substring( name.indexOf( FileIO.ppt_tag_separator ) + FileIO.ppt_tag_separator.length());
  }





  // These are now in PptSlice objects instead.
  // Invariants invs;

  PptTopLevel(String name, VarInfo[] var_infos) {
    this.name = name;
    this.var_infos = var_infos;
    int val_idx = 0;
    num_static_constant_vars = 0;
    for (int i=0; i<var_infos.length; i++) {
      VarInfo vi = var_infos[i];
      vi.varinfo_index = i;
      if (vi.is_static_constant) {
        vi.value_index = -1;
        num_static_constant_vars++;
      } else {
        vi.value_index = val_idx;
        val_idx++;
      }
      vi.ppt = this;
    }
    for (int i=0; i<var_infos.length; i++) {
      VarInfo vi = var_infos[i];
      Assert.assert((vi.value_index == -1) || (!vi.is_static_constant));
    }

    values = new VarValuesOrdered();
    views = new HashSet();
    views_cond = new Vector();

    num_declvars = var_infos.length;
    num_tracevars = val_idx;
    num_orig_vars = 0;
    Assert.assert(num_static_constant_vars == num_declvars - num_tracevars);
    // System.out.println("Created PptTopLevel " + name + ": "
    //                    + "num_static_constant_vars=" + num_static_constant_vars
    //                    + ",num_declvars=" + num_declvars
    //                    + ",num_tracevars=" + num_tracevars);
  }

  // // This is used when merging two sets of data, to create Ppts that were
  // // missing in one of them.
  // PptTopLevel(PptTopLevel other) {
  //   name = other.name;
  //   var_infos = other.var_infos;
  //   derivation_indices = new int[other.derivation_indices.length];
  //   System.arraycopy(other.derivation_indices, 0, derivation_indices, 0, derivation_indices.length);
  //   values = new VarValuesOrdered();
  //   // views = new WeakHashMap();
  //   views = new HashSet();
  // }

  // Accessing data
  int num_vars() {
    return var_infos.length;
  }
  Iterator var_info_iterator() {
    return Arrays.asList(var_infos).iterator();
  }



  boolean compatible(Ppt other) {
    // This insists that the var_infos lists are identical.  The Ppt
    // copy constructor does reuse the var_infos field.
    return (var_infos == other.var_infos);
  }

  // // Strangely, this modifies "other" but not "this".  That looks wrong.
  // // Perhaps this should operate over VarValues objects or some such?
  // void merge(PptTopLevel other) {
  //   // ensure that the two program points are compatible
  //   Assert.assert(compatible(other));
  //
  //   Set other_entries = other.values.entrySet();
  //   for (Iterator itor = other_entries.iterator() ; itor.hasNext() ;) {
  //     Map.Entry other_entry = (Map.Entry) itor.next();
  //     ValueTuple value_tuple = (ValueTuple) other_entry.getKey();
  //     int new_count = (values.get(value_tuple)
  //                      + VarValues.getValue(other_entry));
  //     // equivalent but less efficient:  values.put(value_tuple, new_count);
  //     VarValues.setValue(other_entry, new_count);
  //   }



  ///////////////////////////////////////////////////////////////////////////
  /// Adding variables
  ///

  // Some of this should perhaps be moved up into Ppt.

  // I'm not using a Vector for the var_infos field, even though that would
  // simplify adding new elements, because I want static typechecking and I
  // don't want the overheads of lots of casts and of extra space for
  // Vector objects.

  private void addVarInfo(VarInfo vi) {
    VarInfo[] vis = new VarInfo[] { vi };
    addVarInfos(vis);
  }

  private void addVarInfos(VarInfo[] vis) {
    if (vis.length == 0)
      return;
    int old_length = var_infos.length;
    VarInfo[] new_var_infos = new VarInfo[var_infos.length + vis.length];
    System.arraycopy(var_infos, 0, new_var_infos, 0, old_length);
    System.arraycopy(vis, 0, new_var_infos, old_length, vis.length);
    for (int i=old_length; i<new_var_infos.length; i++) {
      VarInfo vi = new_var_infos[i];
       vi.varinfo_index = i;
       vi.value_index = i - num_static_constant_vars;
       vi.ppt = this;
    }
    var_infos = new_var_infos;
  }

  private void addVarInfos(Vector v) {
    int size = v.size();
    if (size == 0)
      return;
    int old_length = var_infos.length;
    VarInfo[] new_var_infos = new VarInfo[var_infos.length + v.size()];
    System.arraycopy(var_infos, 0, new_var_infos, 0, old_length);
    for (int i=0; i<size; i++) {
      VarInfo vi = (VarInfo) v.elementAt(i);
      new_var_infos[i+old_length] = vi;
      vi.varinfo_index = i+old_length;
      vi.value_index = i+old_length - num_static_constant_vars;
      vi.ppt = this;
    }
    var_infos = new_var_infos;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Finding an object ppt for a given ppt
  ///

  // TODO : Can we generalize this?  Maybe have a list of
  // "controlling" ppts, which would include :::OBJECT :::CLASS_STATIC
  // and others as the front ends evolved?
  
  void compute_object_ppt(PptMap all_ppts)
  {
    object_ppt = (PptTopLevel) all_ppts.get(object_ppt_name());
  }

  String object_ppt_name() {
    // e.g. package.Class.method(args)R:::ENTER ->
    //      package.Class:::OBJECT
    int dot_posn = name.lastIndexOf('.');
    if (dot_posn < 0)
      return null;
    return name.substring(0, dot_posn) + FileIO.object_tag;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Adding special variables
  ///

  // Given a program point, if it represents a function exit, then
  // return the corresponding function entry point.  The result is
  // cached in the entry_ppt slot, to prevent repeating this expensive
  // computation.

  void compute_entry_ppt(PptMap all_ppts) {
    entry_ppt = (PptTopLevel) all_ppts.get(entry_ppt_name());
  }

  final static PatternMatcher re_matcher = Global.regexp_matcher;
  public final static Pattern exit_tag_regexp;
  static {
    try {
      exit_tag_regexp = Global.regexp_compiler.compile(FileIO.exit_tag + "[0-9]*$");
    } catch (Exception e) {
      throw new Error(e.toString());
    }
  }

  String entry_ppt_name() {
    if (!re_matcher.contains(name, exit_tag_regexp))
      return null;
    MatchResult match = re_matcher.getMatch();
    int match_begin = match.beginOffset(0);
    String fn_name = name.substring(0, match_begin);

    return (fn_name + FileIO.enter_tag).intern();
  }

  // Add "_orig" variables to the program point.
  // Derivation should not yet have occurred for the entry program point.
  void add_orig_vars(PptTopLevel entry_ppt) {

    VarInfo[] begin_vis = entry_ppt.var_infos;
    num_orig_vars = begin_vis.length - entry_ppt.num_static_constant_vars;
    Assert.assert(num_orig_vars == entry_ppt.num_tracevars);
    // Don't bother to include the constants.
    VarInfo[] new_vis = new VarInfo[num_orig_vars];
    int new_vis_index = 0;
    for (int i=0; i<begin_vis.length; i++) {
      VarInfo vi = begin_vis[i];
      if (vi.isStaticConstant() || vi.isDerived())
	continue;
      new_vis[new_vis_index] = new VarInfo("orig(" + vi.name + ")",
                                           vi.type, vi.rep_type,
                                           vi.comparability.makeAlias(vi.name));
      new_vis_index++;
    }
    Assert.assert(new_vis_index == num_orig_vars);
    addVarInfos(new_vis);
  }



  /// Possibly just blow this off; I'm not sure I care about it.
  /// In any event, leave it until later.
  //
  // void add_invocation_count_vars() {
  //
  //   // Add invocation counts
  //   if (compute_invocation_counts) {
  //     for ppt in fns_to_process {
  //       these_var_infos = fn_var_infos[ppt];
  //       for callee in fn_invocations.keys() {
  // 	calls_var_name = "calls(%s)" % callee;
  // 	these_var_infos.append(var_info(calls_var_name, "integral", "always", len(these_var_infos)));
  // 	these_values.append(fn_invocations[callee]);
  // 	current_var_index++;
  //       }
  //     }
  //   }
  //
  //       (ppt_sans_suffix, ppt_suffix) = (string.split(ppt, ":::", 1) + [""])[0:2]
  //       if ((ppt_suffix != "EXIT")
  // 	  and (ppt_suffix[0:4] != "EXIT")):
  // 	  continue
  //       these_var_infos = fn_var_infos[ppt]
  //       entry_ppt = ppt_sans_suffix + ":::ENTER"
  //       for vi in fn_var_infos[entry_ppt][0:fn_truevars[entry_ppt]]:
  // 	  these_var_infos.append(var_info("orig(" + vi.name + ")", vi.type, comparability_make_alias(vi.name, vi.comparability), len(these_var_infos)))
  //
  // }


  ///////////////////////////////////////////////////////////////////////////
  /// Derived variables
  ///

  // Convenience function for PptConditional initializer (which can't
  // contain statements but can call a function).
  public VarInfo[] trace_and_orig_and_const_vars() {
    // Not ArraysMDE.subarray(var_infos, 0, num_tracevars + num_orig_vars)
    // because its result Object[] cannot be cast to VarInfo[].
    int total_vars = num_tracevars + num_orig_vars + num_static_constant_vars;
    VarInfo[] result = new VarInfo[total_vars];
    System.arraycopy(var_infos, 0, result, 0, total_vars);
    return result;
  }


  // This is here because I think it doesn't make sense to derive except
  // from a PptTopLevel (and possibly a PptConditional?).  Perhaps move it
  // later.

  public static boolean worthDerivingFrom(VarInfo vi) {
    // This prevents derivation from ever occurring on
    // derived variables.  Ought to put this under the
    // control of the individual Derivation objects.

    // System.out.println("worthDerivingFrom(" + vi.name + "): "
    //                    + "derivedDepth=" + vi.derivedDepth()
    //                    + ", isCanonical=" + vi.isCanonical()
    //                    + ", canBeMissing=" + vi.canBeMissing);
    return ((vi.derivedDepth() < 2)
            && (vi.isCanonical())
            && (!vi.canBeMissing));

    // Testing for being canonical is going to be a touch tricky when we
    // integrate derivation and inference, because when something becomes
    // non-canonical we'll have to go back and derive from it, etc.  It's
    // almost as if that is a new variable appearing.  But it did appear in
    // the list until it was found to be equal to another and removed from
    // the list!  I need to decide whether the time savings of not
    // processing the non-canonical variables are worth the time and
    // complexity of making variables non-canonical and possibly canonical
    // again.

    // return (vi.isCanonical()
    //         // This prevents derivation from ever occurring on
    //         // derived variables.  Ought to put this under the
    //         // control of the individual Derivation objects.
    //         && !vi.isDerived());
    // Should add this (back) in:
	    // && !vi.always_missing()
	    // && !vi.always_equal_to_null();
  }


  // There are just two passes of interest right now...
  final static int derivation_passes = 2;

  // To verify that these are all the factories of interest, do
  // cd ~/research/invariants/daikon/; search -i -n 'extends.*derivationfactory'

  transient UnaryDerivationFactory[][] unaryDerivations
    = new UnaryDerivationFactory[][] {
      // pass1
       { new SequenceLengthFactory() },
      // pass2
       { new SequenceInitialFactory(),
         new SequenceMinMaxSumFactory(), } };

  transient BinaryDerivationFactory[][] binaryDerivations
    = new BinaryDerivationFactory[][] {
      // pass1
      { },
      // pass2
      { new SequenceScalarSubscriptFactory(),
        new SequenceStringSubscriptFactory(), }
    };


  /**
   * This does no inference; it just calls deriveVariablesOnePass once per pass.
   * It returns a Vector of Derivation objects (or are they VarInfos?).<p>
   *
   * If derivation_index == (a, b, c) and n = len(var_infos), then
   * the body of this loop:
   * <li>
   *   <ul> does pass1 introduction for b..a
   *   <ul> does pass2 introduction for c..b
   * </li>
   * and afterward, derivation_index == (n, a, b).
   **/
  public Vector derive() {
    Assert.assert(ArraysMDE.sorted_descending(derivation_indices));

    Vector result = new Vector();
    for (int pass=1; pass<=derivation_passes; pass++) {
      int this_di = derivation_indices[pass];
      int last_di = derivation_indices[pass-1];
      if (Global.debugDerive)
        System.out.println("pass=" + pass + ", range=" + this_di + ".." + last_di);
      if (this_di == last_di) {
        if (Global.debugDerive) {
          System.out.println("No pass " + pass + " derivation to do");
        }
	continue;
      }
      result.addAll(deriveVariablesOnePass(this_di, last_di,
					   unaryDerivations[pass-1],
					   binaryDerivations[pass-1]));
    }
    // shift values in derivation_indices:  convert [a,b,c] into [n,a,b]
    for (int i=derivation_passes; i>0; i--)
      derivation_indices[i] = derivation_indices[i-1];
    derivation_indices[0] = var_infos.length + result.size();

    if (Global.debugDerive) {
      System.out.println(name + ": derived " + result.size() + " new variables; "
                         + "new derivation_indices: "
                         + ArraysMDE.toString(derivation_indices));
      // Alternately, and probably more usefully
      for (int i=0; i<result.size(); i++) {
        // System.out.println("  " + ((Derivation)result.elementAt(i)));
        System.out.println("  " + ((Derivation)result.elementAt(i)).getVarInfo().name);
      }
    }
    return result;
  }


  /**
   * This routine does one "pass"; that is, it adds some set of derived
   * variables, according to the functions that are passed in.  All the
   * results involve VarInfo objects at indices i such that vi_index_min <=
   * i < vi_index_limit (and possibly also involve other VarInfos).
   **/
  Vector deriveVariablesOnePass(int vi_index_min, int vi_index_limit, UnaryDerivationFactory[] unary, BinaryDerivationFactory[] binary) {

    if (Global.debugDerive)
      System.out.println("deriveVariablesOnePass: vi_index_min=" + vi_index_min
                         + ", vi_index_limit=" + vi_index_limit
                         + ", unary.length=" + unary.length
                         + ", binary.length=" + binary.length);

    Vector result = new Vector();

    for (int i=vi_index_min; i<vi_index_limit; i++) {
      VarInfo vi = var_infos[i];
      if (!worthDerivingFrom(vi)) {
        if (Global.debugDerive) {
          System.out.println("Unary: not worth deriving from " + vi.name);
        }
	continue;
      }
      for (int di=0; di<unary.length; di++) {
	UnaryDerivationFactory d = unary[di];
        UnaryDerivation[] uderivs = d.instantiate(vi);
        if (uderivs != null) {
          for (int udi=0; udi<uderivs.length; udi++) {
            result.add(uderivs[udi]);
          }
        }
      }
    }

    // I want to get all pairs such that at least one of the elements is
    // under consideration, but I want to generate each such pair only
    // once.  This probably isn't the most efficient technique, but it's
    // probably adequate and is not excessively complicated or excessively
    // slow.
    for (int i1=0; i1<var_infos.length; i1++) {
      VarInfo vi1 = var_infos[i1];
      if (!worthDerivingFrom(vi1)) {
        if (Global.debugDerive) {
          System.out.println("Binary first VarInfo: not worth deriving from " + vi1.name);
        }
	continue;
      }
      boolean target1 = (i1 >= vi_index_min) && (i1 < vi_index_limit);
      int i2_min = (target1 ? i1+1 : Math.max(i1+1, vi_index_min));
      int i2_limit = (target1 ? var_infos.length : vi_index_limit);
      // if (Global.debugDerive)
      //   System.out.println("i1=" + i1
      //                      + ", i2_min=" + i2_min
      //                      + ", i2_limit=" + i2_limit);
      for (int i2=i2_min; i2<i2_limit; i2++) {
	VarInfo vi2 = var_infos[i2];
	if (!worthDerivingFrom(vi2)) {
          if (Global.debugDerive) {
            System.out.println("Binary: not worth deriving from ("
                               + vi1.name + "," + vi2.name + ")");
          }
          continue;
        }
	// if ((!target1) && (ArraysMDE.indexOfEq(var_infos, vi2) == -1))
	//   // Do nothing if neither of these variables is under consideration.
	//   continue;
	for (int di=0; di<binary.length; di++) {
	  BinaryDerivationFactory d = binary[di];
          BinaryDerivation[] bderivs = d.instantiate(vi1, vi2);
          if (bderivs != null) {
            for (int bdi=0; bdi<bderivs.length; bdi++) {
              result.add(bderivs[bdi]);
            }
          }
	}
      }
    }

    for (int i=0; i<result.size(); i++) {
      Assert.assert(result.elementAt(i) instanceof Derivation
                    // , "Non-Derivation " + result.elementAt(i) + " at index " + i
                    );
    }

    return result;
  }


  ///
  /// Adding derived variables
  ///

  // This doesn't compute what the derived variables should be, just adds
  // them after being computed.

  // derivs is a Vector of Derivation objects
  void addDerivedVariables(Vector derivs) {
    Derivation[] derivs_array
      = (Derivation[]) derivs.toArray(new Derivation[0]);
    addDerivedVariables(derivs_array);
  }

  void addDerivedVariables(Derivation[] derivs) {

    VarInfo[] vis = new VarInfo[derivs.length];
    for (int i=0; i<derivs.length; i++) {
      vis[i] = derivs[i].getVarInfo();
    }
    addVarInfos(vis);

    // Since I am only modifying members, not making new objects, and since
    // I am using an Eq hash table, I don't need to rehash.
    values.extend(derivs);
  }




  ///////////////////////////////////////////////////////////////////////////
  /// Manipulating values
  ///

  void add(ValueTuple vt, int count) {
    // System.out.println("PptTopLevel " + name + ": add " + vt);
    Assert.assert(vt.size() == var_infos.length - num_static_constant_vars);

    values.add(vt, count);

    // Add to all the views
    // for (Iterator itor = views.keySet().iterator() ; itor.hasNext() ; ) {
    for (Iterator itor = views.iterator() ; itor.hasNext() ; ) {
      PptSlice view = (PptSlice) itor.next();
      view.add(vt, count);
      if (view.invs.size() == 0)
        itor.remove();
    }

  }

  // void process() {
  //   throw new Error("To implement");
  // }

  // boolean contains(ValueTuple vt) {
  //   return values.containsKey(vt);
  // }

  // Iterator entrySet() {
  //   return values.entrySet().iterator();
  // }



  ///////////////////////////////////////////////////////////////////////////
  ///
  ///

  // This function is called to jump-start processing; it creates all the
  // views (and thus invariants) and derived variables.  Afterward, we just
  // check those invariants.  (That might require us to add more derived
  // variables and invariants, though -- for instance, if an invariant that
  // had suppressed a derived variable becomes falsified.  An equality
  // invariant is an example of such an invariant.)

  // As of 10/25/99, we don't do the second part:  just read the entire
  // data file before doing any processing.

  // The way this works is:
  //  * do derivation by stages
  //  * all inference must be performed over a variable before it may be
  //    derived from.  This implies that as soon as we derive a variable,
  //    we should do all inference over it.
  //  * inference could be by stages, too:  first equality invariants,
  //    then all others.  Probably do this soon.


  public void initial_processing() {
    if (Global.debugPptTopLevel)
      System.out.println("initial_processing for " + name);

    // I probably can't do anything about it if this is called
    // subsequently; but I should be putting off initial_processing for
    // each program point until it has many samples anyway.
    if (num_samples() == 0)
      return;

    derivation_indices = new int[derivation_passes+1];
    // Extraneous, since the array is initialized to all zeros.
    // Arrays.fill(derivation_passes, 0);
    // Not num_tracevars because we also care about _orig, etc.
    instantiate_views(0, var_infos.length);

    // Eventually, integrate derivation and inference.  That will require
    // incrementally adding new variables, slices, and invariants.  For
    // now, don't bother:  I want to just get something working first.
    while (derivation_indices[derivation_passes] < var_infos.length) {
      Vector derivations = derive();

      // Using "addVarInfos(derivations)" would do only part of the work.
      addDerivedVariables(derivations);

      Assert.assert(derivation_indices[0] == var_infos.length);
      instantiate_views(var_infos.length - derivations.size(), var_infos.length);
    }
    if (Global.debugPptTopLevel)
      System.out.println("Done with initial_processing, " + var_infos.length
                         + " variables");
    Assert.assert(derivation_indices[derivation_passes] == var_infos.length);
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Creating invariants
  ///

  // Is a Vector if we are adding views; this obviates the need for a
  // "boolean adding_views" variable.
  private Vector views_to_remove_deferred = null;
  // This is to avoid making a new vector every time through the loop;
  // just reuse this one.  (This probably isn't such a big deal.)
  private Vector vtrd_cache = new Vector(2);

  // I can't decide which loop it's more efficient to make the inner loop.

  // Vector of PptSlice.
  // Maybe this should return the rejected views.
  public void addViews(Vector slices_vector_) {
    if (slices_vector_.isEmpty())
      return;

    // Don't modify the actual parameter
    Vector slices_vector = (Vector) slices_vector_.clone();

    // This might be a brand-new Slice, and instantiate_invariants for this
    // pass might not have come up with any invariants.
    for (Iterator itor = slices_vector.iterator(); itor.hasNext(); ) {
      PptSlice slice = (PptSlice) itor.next();
      if (slice.invs.size() == 0)
        // removes the element from slices_vector
        itor.remove();
    }

    // use an array because iterating over it will be more efficient, I suspect.
    PptSlice[] slices;
    int num_slices;

    // This is also duplicated below.
    views_to_remove_deferred = vtrd_cache;
    slices = (PptSlice[]) slices_vector.toArray(new PptSlice[0]);
    num_slices = slices.length;

    // System.out.println("Adding views for " + name);
    // for (int i=0; i<slices.length; i++) {
    //   System.out.println("  View: " + slices[i].name);
    // }
    // values.dump();

    // System.out.println("Number of samples for " + name + ": "
    //                    + values.num_samples()
    //                    + ", number of values: " + values.num_values());
    // If I recorded mod bits in value.ValueSet(), I could use it here instead.
    for (Iterator vt_itor = values.sampleIterator(); vt_itor.hasNext(); ) {
      VarValuesOrdered.ValueTupleCount entry = (VarValuesOrdered.ValueTupleCount) vt_itor.next();
      ValueTuple vt = entry.value_tuple;
      int count = entry.count;
      for (int i=0; i<num_slices; i++) {
        // System.out.println("" + slices[i] + " .add(" + vt + ", " + count + ")");
        slices[i].add(vt, count);
      }
      if (views_to_remove_deferred.size() > 0) {
        // Inefficient, but easy to code.
        Assert.assert(slices_vector.containsAll(views_to_remove_deferred));
        slices_vector.removeAll(views_to_remove_deferred);
        views_to_remove_deferred.clear();
        if (slices_vector.size() == 0)
          break;
        slices = (PptSlice[]) slices_vector.toArray(new PptSlice[0]);
        num_slices = slices.length;
      }
    }

    views.addAll(slices_vector);
    for (int i=0; i<num_slices; i++) {
      slices[i].already_seen_all = true;
      // System.out.println("Now " + slices[i].name + " has seen it all");
    }

    views_to_remove_deferred = null;
  }

  // Old version with the other loop outermost
  // private boolean adding_views;
  // private Ppt view_to_remove_deferred = null;
  // // Vector of PptSlice.
  // // Maybe this should return the rejected views.
  // void addViews(Vector slices) {
  //   adding_views = true;
  //   // Since I more valuetuples than program points, and program points are
  //   // likely to disappear midway through processing, put the program point
  //   // loop outermost.  (Does this make sense?  I'm not sure...)
  //   for (Iterator slice_itor = slices.iterator() ; slice_itor.hasNext() ; ) {
  //     Ppt this_slice = (Ppt) slice_itor.next();
  //     Assert.assert(view_to_remove_deferred == null);
  //     for (Iterator vt_itor = values.entrySet().iterator() ;
  //          ((view_to_remove_deferred == null) && vt_itor.hasNext()) ;
  //          /* no increment in for loop */ ) {
  //       Map.Entry entry = (Map.Entry) vt_itor.next();
  //       ValueTuple vt = (ValueTuple) entry.getKey();
  //       int count = ((Integer) entry.getValue()).intValue();
  //       this_slice.add(vt, count);
  //     }
  //     if (view_to_remove_deferred != null) {
  //       Assert.assert(view_to_remove_deferred == this_slice);
  //       // removes this_slice from slice_itor
  //       slice_itor.remove();
  //     }
  //   }
  //   views.addAll(slices);
  //   adding_views = false;
  // }


  public void removeView(Ppt slice) {
    // System.out.println("removeView " + slice.name + " " + slice);
    if (views_to_remove_deferred != null) {
      views_to_remove_deferred.add(slice);
    } else {
      boolean removed = views.remove(slice);
      Assert.assert(removed);
    }
  }


  // A slice is a specific kind of view, but we don't call this
  // findView because it doesn't find an arbitrary view.
  public PptSlice findSlice(VarInfo v) {
    for (Iterator itor = views.iterator() ; itor.hasNext() ; ) {
      PptSlice view = (PptSlice) itor.next();
      if ((view.arity == 1) && (v == view.var_infos[0]))
        return view;
    }
    return null;
  }

  public PptSlice findSlice(VarInfo v1, VarInfo v2) {
    Assert.assert(v1.varinfo_index < v2.varinfo_index);
    for (Iterator itor = views.iterator() ; itor.hasNext() ; ) {
      PptSlice view = (PptSlice) itor.next();
      if ((view.arity == 2)
          && (v1 == view.var_infos[0])
          && (v2 == view.var_infos[1]))
        return view;
    }
    return null;
  }

  public PptSlice findSlice(VarInfo v1, VarInfo v2, VarInfo v3) {
    Assert.assert(v1.varinfo_index < v2.varinfo_index);
    Assert.assert(v2.varinfo_index < v3.varinfo_index);
    for (Iterator itor = views.iterator() ; itor.hasNext() ; ) {
      PptSlice view = (PptSlice) itor.next();
      if ((view.arity == 3)
          && (v1 == view.var_infos[0])
          && (v2 == view.var_infos[1])
          && (v3 == view.var_infos[2]))
        return view;
    }
    return null;
  }

  // At present, this needs to occur after deriving variables, because
  // I haven't integrated derivation and inference yet.
  // (This function doesn't exactly belong in this part of the file.)

  // Should return a list of the views created, perhaps.

  // This doesn't work because we have a Vector of Derivation objects, not
  // a vector of VarInfo objects.
  //   // Convenience version.
  //   void instantiate_views(Vector vars) {
  //     if (vars.size() == 0)
  //       return;
  //
  //     for (int i=0; i<vars.size()-1; i++)
  //       Assert.assert( ((VarInfo)vars.elementAt(i)).varinfo_index
  //                      == ((VarInfo)vars.elementAt(i+1)).varinfo_index - 1 );
  //     instantiate_views( ((VarInfo)vars.elementAt(0)).varinfo_index,
  //                        ((VarInfo)vars.elementAt(vars.size()-1)).varinfo_index + 1 );
  //   }


  /**
   * Install views (and thus invariants).
   * This function does cause all invariants over the new views to be computed.
   * The installed views and invariants will all have at least one element with
   * index i such that vi_index_min <= i < vi_index_limit.
   * (However, we also assume that vi_index_limit == var_infos.length.)
   **/
  void instantiate_views(int vi_index_min, int vi_index_limit) {
    if (Global.debugInfer)
      System.out.println("instantiate_views: " + this.name
                         + ", vi_index_min=" + vi_index_min
                         + ", vi_index_limit=" + vi_index_limit
                         + ", var_infos.length=" + var_infos.length);

    // It might pay to instantiate views for variables one at a time, to
    // save work.  I'm not sure, but it does seem plausible.
    // This test prevents that for now.
    Assert.assert(var_infos.length == vi_index_limit);


    // Stage invariant detection:
    //  1. unary constant (skip if canBeMissing)
    //  2. binary equal
    //     (skip if canBeMissing;
    //     only if types are the same/compatible;
    //     can do this sequentially rather than all at once)
    //      * set canonicalness
    //  3. all other unary
    //     (skip if non-canonical, constant, canBeMissing)
    //  4. all other binary
    //     (skip if non-canonical or missing; sometimes skip if constant)
    //  5. ternary
    // A very slightly tricky thing is not duplicating an already-existing
    // view during the "all other" phases.

    if (vi_index_min == vi_index_limit)
      return;

//     if var_values == {}:
//         # This function was never executed
//         return

    // used only for debugging
    int old_num_vars = var_infos.length;
    int old_num_views = views.size();

    /// 1. unary constant

    // Unary slices/invariants.
    Vector unary_views = new Vector(vi_index_limit-vi_index_min);
    for (int i=vi_index_min; i<vi_index_limit; i++) {
      if (Daikon.invariants_check_canBeMissing && var_infos[i].canBeMissing) {
        if (Global.debugDerive) {
          System.out.println("In binary equality, " + var_infos[i].name + " can be missing");
        }
        continue;
      }
      // I haven't computed any invariants over it yet -- how am I to know
      // whether it's canonical??
      // if (!var_infos[i].isCanonical())
      //   continue;
      PptSlice1 slice1 = new PptSlice1(this, var_infos[i]);
      // first pass of unary invariant instantiation
      slice1.instantiate_invariants(1);
      unary_views.add(slice1);
    }
    addViews(unary_views);
    set_dynamic_constant_slots(unary_views);

    // Now some elements of unary_views are installed, but others are not
    // and are incomplete, Because we stopped adding new values as soon as
    // they were found not to hold.  We discard them later, but for now we
    // want to remember all the ones we tried.


    /// 2. binary equality

    // Binary slices/invariants.
    Vector binary_views = new Vector();
    for (int i1=0; i1<vi_index_limit; i1++) {
      if (Daikon.invariants_check_canBeMissing && var_infos[i1].canBeMissing) {
        if (Global.debugDerive) {
          System.out.println("In binary equality, " + var_infos[i1].name + " can be missing");
        }
        continue;
      }
      // I can check canonicalness only if we've already computed
      // invariants over it.
      if ((i1 < vi_index_min) && (!var_infos[i1].isCanonical())) {
        if (Global.debugDerive) {
          System.out.println("Skipping non-canonical non-target variable1 "
                             + var_infos[i1].name);
        }
        continue;
      }
      boolean target1 = (i1 >= vi_index_min) && (i1 < vi_index_limit);
      int i2_min = (target1 ? i1+1 : Math.max(i1+1, vi_index_min));
      if (Global.debugInfer)
        System.out.println("instantiate_views"
                           + "(" + vi_index_min + "," + vi_index_limit + ")"
                           + " i1=" + i1 + " (" + var_infos[i1].name + ")"
                           + ", i2_min=" + i2_min
                           );
      for (int i2=i2_min; i2<vi_index_limit; i2++) {
        // System.out.println("Trying binary instantiate_views"
        //                    + " i1=" + i1 + " (" + var_infos[i1].name + "),"
        //                    + " i2=" + i2 + " (" + var_infos[i2].name + ")");
        if (Daikon.invariants_check_canBeMissing && var_infos[i2].canBeMissing) {
          if (Global.debugDerive) {
            System.out.println("In binary equality vs. " + var_infos[i1].name
                               + ", " + var_infos[i2].name + " can be missing");
          }
          continue;
        }
        // I can check canonicalness only if we've already computed
        // invariants over it.
        if ((i2 < vi_index_min) && (!var_infos[i2].isCanonical())) {
          if (Global.debugDerive) {
            System.out.println("Skipping non-canonical non-target variable2 "
                               + var_infos[i2].name);
          }
          continue;
        }
        // We'll take care of this elsewhere, in part because putting
        // everything in binary_views simplifies things later on.
        // // For equality invariants (only), skip if differing types.
        // if (var_infos[i1].rep_type != var_infos[i2].rep_type) {
        //   continue;
        // }

        PptSlice slice2 = new PptSlice2(this, var_infos[i1], var_infos[i2]);
        // System.out.println("Created PptSlice2 " + slice2.name);
        slice2.instantiate_invariants(1);
        binary_views.add(slice2);
      }
    }
    addViews(binary_views);
    set_equal_to_slots(binary_views, vi_index_min, vi_index_limit);

    // 3. all other unary invariants
    if (Global.debugPptTopLevel)
      System.out.println(unary_views.size() + " unary views for pass 2 instantiate_invariants");
    Vector unary_views_pass2 = new Vector(unary_views.size());
    for (int i=0; i<unary_views.size(); i++) {
      PptSlice1 unary_view = (PptSlice1) unary_views.elementAt(i);
      Assert.assert(unary_view.arity == 1);
      VarInfo var = unary_view.var_infos[0];
      if (!var.isCanonical()) {
        if (Global.debugPptTopLevel)
          System.out.println("Skipping pass 2 unary instantiate_invariants: "
                             + var.name + " is not canonical");
        continue;
      }
      if (unary_view.var_info.isConstant())
        continue;
      if (views.contains(unary_view)) {
        // There is only one type of unary invariant in pass 1:
        // OneOf{Scalar,Sequence}.  It must have been successful, or this
        // view wouldn't have been installed.
        Assert.assert(unary_view.invs.size() == 1);
        Invariant inv = (Invariant) unary_view.invs.elementAt(0);
        Assert.assert((inv instanceof OneOfScalar)
                      || (inv instanceof OneOfString)
                      || (inv instanceof OneOfSequence)
                      || (inv instanceof OneOfStringSequence));
      } else {
        // The old one was a failure (and so saw only a subset of all the
        // values); recreate it.
        unary_view = new PptSlice1(this, var);
        unary_views_pass2.add(unary_view);
      }
      unary_view.instantiate_invariants(2);
    }
    addViews(unary_views_pass2);
    // Save some space
    for (int i=0; i<unary_views.size(); i++) {
      ((PptSlice) unary_views.elementAt(i)).clear_cache();
    }
    for (int i=0; i<unary_views_pass2.size(); i++) {
      ((PptSlice) unary_views_pass2.elementAt(i)).clear_cache();
    }
    unary_views = null;
    unary_views_pass2 = null;

    // 4. all other binary invariants
    Vector binary_views_pass2 = new Vector(binary_views.size());
    for (int i=0; i<binary_views.size(); i++) {
      PptSlice2 binary_view = (PptSlice2) binary_views.elementAt(i);
      Assert.assert(binary_view.arity == 2);
      VarInfo var1 = binary_view.var_infos[0];
      VarInfo var2 = binary_view.var_infos[1];
      if (!var1.isCanonical())
        continue;
      if (!var2.isCanonical())
        continue;
      if (var1.isConstant())
        continue;
      if (var2.isConstant())
        continue;
      if (views.contains(binary_view)) {
        // There is only one type of binary invariant in pass 1:
        // {Int,Seq}Comparison.  It must have been successful, or this view
        // wouldn't have been installed.
        Assert.assert(binary_view.invs.size() == 1);
        Invariant inv = (Invariant) binary_view.invs.elementAt(0);
        Assert.assert(inv instanceof Comparison);
      } else {
        // The old one was a failure (and so saw only a subset of all the
        // values); recreate it.
        binary_view = new PptSlice2(this, var1, var2);
        binary_views_pass2.add(binary_view);
      }
      binary_view.instantiate_invariants(2);
    }
    addViews(binary_views_pass2);
    // Compute exact_nonunary_invariants, then save some space
    set_exact_nonunary_invariants_slots(binary_views);
    set_exact_nonunary_invariants_slots(binary_views_pass2);
    binary_views = null;
    binary_views_pass2 = null;

    // 5. ternary invariants
    if (! Daikon.disable_ternary_invariants) {
      Vector ternary_views = new Vector();
      for (int i1=0; i1<vi_index_limit; i1++) {
        VarInfo var1 = var_infos[i1];
        if (Daikon.invariants_check_canBeMissing && var1.canBeMissing) {
          if (Global.debugDerive) {
            System.out.println("In ternary, " + var1.name + " can be missing");
          }
          continue;
        }
        if (!var1.isCanonical())
          continue;
        if (var1.isConstant())
          continue;

        boolean target1 = (i1 >= vi_index_min) && (i1 < vi_index_limit);
        for (int i2=i1+1; i2<vi_index_limit; i2++) {
          VarInfo var2 = var_infos[i2];
          if (Daikon.invariants_check_canBeMissing && var2.canBeMissing) {
            if (Global.debugDerive) {
              System.out.println("In ternary vs. " + var1.name
                                 + ", " + var2.name + " can be missing");
            }
            continue;
          }
          if (!var2.isCanonical())
            continue;
          if (var2.isConstant())
            continue;
          if (var1.hasExactInvariant(var2)) {
            // This number isn't quite right:  it depends on what the type
            // of var3 will be.  But leave it as a conservative
            // approximation (because we would save this many for many
            // different var3).
            Global.subexact_noninstantiated_invariants
              += ThreeScalarFactory.max_instantiate;
            continue;
          }

          boolean target2 = (i2 >= vi_index_min) && (i2 < vi_index_limit);
          int i3_min = ((target1 || target2) ? i2+1 : Math.max(i2+1, vi_index_min));
          if (Global.debugInfer)
            System.out.println("instantiate_views"
                               + "(" + vi_index_min + "," + vi_index_limit + ")"
                               + " i1=" + i1
                               + " i2=" + i2
                               + ", i3_min=" + i3_min
                               );
          for (int i3=i3_min; i3<vi_index_limit; i3++) {
            Assert.assert(((i1 >= vi_index_min) && (i1 < vi_index_limit))
                          || ((i2 >= vi_index_min) && (i2 < vi_index_limit))
                          || ((i3 >= vi_index_min) && (i3 < vi_index_limit)));
            Assert.assert((i1 < i2) && (i2 < i3));
            VarInfo var3 = var_infos[i3];
            if (Daikon.invariants_check_canBeMissing && var3.canBeMissing) {
              if (Global.debugDerive) {
                System.out.println("In ternary vs. ("
                                   + var1.name + "," + var2.name + ")"
                                   + ", " + var2.name + " can be missing");
              }
              continue;
            }
            if (!var3.isCanonical())
              continue;
            if (var3.isConstant())
              continue;
            if (var1.hasExactInvariant(var3)
                || var2.hasExactInvariant(var3)) {
              // No invariants if any of the types is array.
              if (var1.rep_type.isArray()
                || var2.rep_type.isArray()
                || var3.rep_type.isArray())
                continue;
              Global.subexact_noninstantiated_invariants
                += ThreeScalarFactory.max_instantiate;
              continue;
            }

            // (For efficiency, I could move this earlier.  But that's not
            // completely fair, so I won't for now.)
            // For now, only ternary invariants not involving any arrays
            if (var1.rep_type.isArray()
                || var2.rep_type.isArray()
                || var3.rep_type.isArray())
              continue;

            PptSlice3 slice3 = new PptSlice3(this, var1, var2, var3);
            slice3.instantiate_invariants(1);
            slice3.instantiate_invariants(2);
            ternary_views.add(slice3);
          }
        }
      }
      addViews(ternary_views);
      set_exact_nonunary_invariants_slots(ternary_views);
    }


    if (Global.debugPptTopLevel)
      System.out.println(views.size() - old_num_views + " new views for " + name);

    // This method didn't add any new variables.
    Assert.assert(old_num_vars == var_infos.length);

    // now unary_views, binary_views, and ternary_views get garbage-collected.
  }

  // Set the dynamic_constant slots of all the new variables.
  void set_dynamic_constant_slots(Vector unary_views) {
    for (int i=0; i<unary_views.size(); i++) {
      PptSlice1 unary_view = (PptSlice1) unary_views.elementAt(i);
      // System.out.println("set_dynamic_constant_slots " + unary_view.name + " " + views.contains(unary_view));
      Assert.assert(unary_view.arity == 1);
      // If this view has been installed in the views slot (ie, it has not
      // been eliminated already).
      if (views.contains(unary_view)) {
        // There is only one type of unary invariant in pass 1:
        // OneOf{Scalar,Sequence}.  It must have been successful, or this
        // view wouldn't have been installed.
        Assert.assert(unary_view.invs.size() == 1);
        Invariant inv = (Invariant) unary_view.invs.elementAt(0);
        inv.finished = true;
        // unary_view.already_seen_all = true;
        OneOf one_of = (OneOf) inv;
        // System.out.println("num_elts: " + one_of.num_elts());
        if (one_of.num_elts() == 1) {
          // System.out.println("Constant " + inv.ppt.name + " " + one_of.var().name + " because of " + unary_view.name);
	  // Should be Long, not Integer.
	  Assert.assert(! (one_of.elt() instanceof Integer));
          one_of.var().dynamic_constant = one_of.elt();
          one_of.var().is_dynamic_constant = true;
          // System.out.println("set dynamic_constant to " + one_of.elt());
        }
      } else {
        unary_view.clear_cache();
      }
    }
  }

  static final boolean debug_equal_to = false;
  // static final boolean debug_equal_to = true;

  // Set the equal_to slots of all the new variables.
  void set_equal_to_slots(Vector binary_views, int vi_index_min, int vi_index_limit) {
    for (int i=0; i<binary_views.size(); i++) {
      PptSlice2 binary_view = (PptSlice2) binary_views.elementAt(i);
      Assert.assert(binary_view.arity == 2);

      if (binary_view.debugged) {
        System.out.println("Binary view " + binary_view.name + " has "
                           + (views.contains(binary_view) ? "not " : "") + "been eliminated.");
      }
      // If binary_view has been installed (hasn't yet been eliminated)
      if (views.contains(binary_view)) {
        // There is only one type of binary invariant in pass 1:
        // {Int,Seq}Comparison.  It must have been successful, or this view
        // wouldn't have been installed.
        Assert.assert(binary_view.invs.size() == 1);
        Invariant inv = (Invariant) binary_view.invs.elementAt(0);
        inv.finished = true;
        // binary_view.already_seen_all = true;
        Assert.assert(inv instanceof Comparison);
        // Not "inv.format" because that is null if not justified.
        // System.out.println("Is " + (IsEquality.it.accept(inv) ? "" : "not ")
        //                    + "equality: " + inv.repr());
        if (IsEquality.it.accept(inv)) {
          VarInfo var1 = binary_view.var_infos[0];
          VarInfo var2 = binary_view.var_infos[1];
          Assert.assert(var1.varinfo_index < var2.varinfo_index);
          // System.out.println("found equality: " + var1.name + " = " + var2.name);
          // System.out.println("var1.equal_to="
          //                    + ((var1.equal_to == null) ? "null" : var1.equal_to.name)
          //                    + ", var2.equal_to="
          //                    + ((var2.equal_to == null) ? "null" : var2.equal_to.name));
          if ((var1.equal_to == null) && (var2.equal_to != null)) {
            var1.equal_to = var2.equal_to;
            if (debug_equal_to) {
              System.out.println("Setting " + var1.name + ".equal_to = " + var1.equal_to.name);
            }
          } else if ((var1.equal_to != null) && (var2.equal_to == null)) {
            var2.equal_to = var1.equal_to;
            if (debug_equal_to) {
              System.out.println("Setting " + var2.name + ".equal_to = " + var2.equal_to.name);
            }
          } else if ((var1.equal_to == null) && (var2.equal_to == null)) {
            // Can this cause the canonical version to not be the lowest-
            // numbered version?  I don't think so, because of the ordering
            // in which we are examining pairs.
            var1.equal_to = var1;
            var2.equal_to = var1;
          } else {
            // This is implied by the if-then sequence.
            // Assert.assert((var1.equal_to != null) && (var2.equal_to != null));
            Assert.assert(((Daikon.check_program_types
                            && (! var1.type.comparable(var2.type)))
                           || (var1.equal_to == var2.equal_to))
                          // This is ordinarily commented out, to save
                          // time in the common case.
                          , "Variables not equal: " + var1.name + " (= " + var1.equal_to.name + "), " + var2.name + " (= " + var2.equal_to.name + ") at " + name
                          );
            Assert.assert(var1.equal_to.varinfo_index <= var1.varinfo_index);
            Assert.assert(var2.equal_to.varinfo_index <= var2.varinfo_index);
          }
        }
      } else {
        binary_view.clear_cache();
      }
    }
    for (int i=vi_index_min; i<vi_index_limit; i++) {
      VarInfo vi = var_infos[i];
      if (vi.equal_to == null) {
        if (debug_equal_to) {
          System.out.println("Lonesome canonical var " + vi.varinfo_index + ": " + vi.name);
        }
        vi.equal_to = vi;
      }
    }
  }

  // Compute exact_nonunary_invariants
  void set_exact_nonunary_invariants_slots(Vector nonunary_views) {
    for (int j=0; j<nonunary_views.size(); j++) {
      PptSlice nonunary_view = (PptSlice) nonunary_views.elementAt(j);
      for (int k=0; k<nonunary_view.invs.size(); k++) {
        Invariant inv = (Invariant) nonunary_view.invs.elementAt(k);
        if (inv.isExact() && inv.justified()) {
          nonunary_view.var_infos[0].exact_nonunary_invariants.add(inv);
        } else if (inv.isExact()) {
          // The inv.format() is going to return null
          // because the invariant is not justified!
          System.out.println("Exact but not justified: " + inv.format() + " " + inv.repr());
        }
      }
      nonunary_view.clear_cache();
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Creating conditioned views
  ///

  // This apparently can't appear in PptConditional, lest it never get called.
  // I guess PptConditional isn't instantiated unless it needs to be, but
  // it doesn't need to be unless GriesLisp has been instantiated already.
  static {
    // Would it be enough to say "GriesLisp dummy = null;"?  I'm not sure.
    // This does work, though.
    new SplitterList4Dsaa();
    new GriesLisp();
    new WeissDsaaMDE();
  }

  public void addConditions(Splitter[] splits) {
    if ((splits == null) || (splits.length == 0)) {
      if (Global.debugPptSplit)
        System.out.println("No splits for " + name);
      return;
    }

    Vector pconds_vector = new Vector(2 * splits.length);
    for (int i=0; i<splits.length; i++) {
      PptConditional cond1 = new PptConditional(this, splits[i], false);
      if (! cond1.splitter_valid()) {
        if (Global.debugPptSplit)
          System.out.println("Splitter not valid: " + cond1.name);
        continue;
      }
      pconds_vector.add(cond1);
      PptConditional cond2 = new PptConditional(this, splits[i], true);
      Assert.assert(cond2.splitter_valid());
      pconds_vector.add(cond2);
    }
    PptConditional[] pconds
      = (PptConditional[]) pconds_vector.toArray(new PptConditional[0]);
    int num_pconds = pconds.length;
    Assert.assert((num_pconds/2)*2 == num_pconds); // ensure divisibility by 2
    int num_splits = num_pconds/2;

    for (int i=0; i<num_pconds; i+=2) {
      Assert.assert(! pconds[i].splitter_inverse);
      Assert.assert(pconds[i+1].splitter_inverse);
      Assert.assert(pconds[i+1].splitter.condition().equals(pconds[i].splitter.condition()));
    }

    int trimlength = num_tracevars + num_orig_vars;

    int[][] cumulative_modbits = new int[num_pconds][trimlength];
    for (int i=0; i<num_pconds; i++) {
      Arrays.fill(cumulative_modbits[i], 1);
    }

    for (Iterator vt_itor = values.sampleIterator(); vt_itor.hasNext(); ) {
      VarValuesOrdered.ValueTupleCount entry = (VarValuesOrdered.ValueTupleCount) vt_itor.next();
      ValueTuple vt = entry.value_tuple;
      int count = entry.count;
      // I do not want to use the same ValueTuple every time through the pconds
      // loop because the inserted ValueTuple will be modified in place.
      // It's OK to reuse its elements, though.
      ValueTuple vt_trimmed = vt.trim(trimlength);
      int[] trimmed_mods = vt_trimmed.mods;
      Object[] trimmed_vals = vt_trimmed.vals;
      for (int i=0; i<num_pconds; i+=2) {
        // I really only have to do one of these (depending on which way
        // the split goes), unless the splitter throws an error, in which
        // case I need to have done both.  Thus, do both, to be on the safe
        // side.
        ValueTuple.orModsInto(cumulative_modbits[i], trimmed_mods);
        ValueTuple.orModsInto(cumulative_modbits[i+1], trimmed_mods);
        boolean splitter_test;
        boolean split_exception = false;
        // System.out.println("Testing " + pconds[i].name);
        // This try block is tight so it doesn't accidentally catch
        // other errors.
        try {
          splitter_test = pconds[i].splitter.test(vt);
        } catch (Exception e) {
          // e.printStackTrace();
          // If an exception is thrown, don't put the data on either side
          // of the split.
          split_exception = true;
          splitter_test = false; // to pacify the Java compiler
        }
        if (! split_exception) {
          // System.out.println("Result = " + splitter_test);
          int index = (splitter_test ? i : i+1);
          // Do not reuse cum_mods!  It might itself be the
          // canonical version (returned by Intern.intern), and then
          // modifications would be bad.  Instead, create a new array.
          int[] cum_mods = cumulative_modbits[index];
          int[] new_mods = (int[]) trimmed_mods.clone();
          // This is somewhat like orModsInto, but not exactly.
          for (int mi=0; mi<trimlength; mi++) {
            if ((cum_mods[mi] == ValueTuple.MODIFIED)
                && (new_mods[mi] != ValueTuple.MISSING)) {
              new_mods[mi] = ValueTuple.MODIFIED;
              cum_mods[mi] = ValueTuple.UNMODIFIED;
            }
          }
          // System.out.println("Adding (count " + count + ") to " + pconds[index].name);
          pconds[index].add_nocheck(ValueTuple.makeFromInterned(trimmed_vals,
                                                                Intern.intern(new_mods)),
                                    count);
          // I don't want to do "Arrays.fill(cum_mods, 0)" because where
          // the value was missing, we didn't use up the modification bit.
          // I've already fixed it up above, anyway.
        }
      }
    }


    int parent_num_samples = num_samples();
    for (int i=0; i<num_pconds; i++) {
      // Don't bother with this conditioned view if it contains all or no samples.
      int this_num_samples = pconds[i].num_samples();
      if ((this_num_samples > 0) && (this_num_samples < parent_num_samples)) {
        views_cond.add(pconds[i]);
      } else {
        if (Global.debugPptSplit)
          System.out.println("Omitting " + pconds[i].name + ": "
                             + this_num_samples + "/" + parent_num_samples
                             + " samples");
        // Unconditional output, because it's too confusing otherwise.
        if (this_num_samples == parent_num_samples) {
          System.out.println("Condition always satisfied: "
                             + pconds[i].name + " == " + this.name);
        }
      }
    }
    if (Global.debugPptSplit)
      System.out.println("" + views_cond.size() + " views on " + this.name);
    for (int i=0; i<views_cond.size(); i++) {
      PptConditional pcond = (PptConditional) views_cond.elementAt(i);
      pcond.initial_processing();
    }

  }


  ///////////////////////////////////////////////////////////////////////////
  /// Hiding object invariants
  ///

  public Invariant findMatchingObjectInvariant(Invariant inv)
  {
    Assert.assert(inv.ppt.parent == this);

    // Our invariants can't be implied by object invariants unless
    // this ppt is contributing to some object invariant
    if (object_ppt == null) {
      return null;
    }

    // Try to match inv against all object invariants
    Iterator object_invs = object_ppt.invariants_vector().iterator();
  IS_OBJ_INV_OUTER:
    while (object_invs.hasNext()) {
      Invariant obj_inv = (Invariant) object_invs.next();

      // Can't be the same if they aren't the same type
      if (!inv.getClass().equals(obj_inv.getClass())) {
	continue;
      }

      // Can't be the same if they aren't the same formula
      if (!inv.isSameFormula(obj_inv)) {
	continue;
      }

      // The variable names much match up, in order

      VarInfo[] vars = inv.ppt.var_infos;
      VarInfo[] obj_vars = obj_inv.ppt.var_infos;

      Assert.assert(vars.length == obj_vars.length); // due to inv type match already
      for (int i=0; i < vars.length; i++) {
	VarInfo var = vars[i];
	VarInfo obj_var = obj_vars[i];

	// Do the easy check first
	if (var.name.equals(obj_var.name)) {
	  continue;
	}

	// Now check for aliasing problems

	// The names "match" iff there is an intersection of the names
	// of aliased variables
	Vector all_obj_vars = obj_var.canonicalRep().equalTo();
	all_obj_vars.add(obj_var.canonicalRep());
	Vector all_obj_vars_names = new Vector(all_obj_vars.size());
	for (Iterator iter = all_obj_vars.iterator(); iter.hasNext(); ) {
	  VarInfo elt = (VarInfo) iter.next();
	  all_obj_vars_names.add(elt.name);
	}
	Vector all_vars = new Vector(var.canonicalRep().equalTo());
	all_vars.add(var.canonicalRep());
	boolean name_matched = false;
	for (Iterator iter = all_vars.iterator(); !name_matched && iter.hasNext(); ) {
	  VarInfo elt = (VarInfo) iter.next();
	  name_matched = all_obj_vars_names.contains(elt.name);
	}
	if (!name_matched) {
	  continue IS_OBJ_INV_OUTER;
	}
      }

      // the type, formula, and vars all matched
      return obj_inv;
    }

    // no more candidates in set of object invs
    return null;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Printing invariants
  ///

  // As of 1/9/2000, this is only used in print_invariants.
  public Vector invariants_vector() {
    Vector result = new Vector();
    for (Iterator views_itor = views.iterator(); views_itor.hasNext(); ) {
      PptSlice slice = (PptSlice) views_itor.next();
      result.addAll(slice.invs);
    }
    return result;
  }



  // In original (Python) implementation, known as print_invariants_ppt.
  // I may still want to integrate some more of its logic here.
  /**
   * Print invariants for a single program point.
   * Does no output if no samples or no views.
   **/
  public void print_invariants_maybe() {
    if (num_samples() == 0)
      return;
    if (views.size() == 0) {
      if (! (this instanceof PptConditional)) {
        // Presumably all the views that were originally there were deleted
        // because no invariants remained in any of them.
        System.out.println("[No views for " + name + "]");
      }
      return;
    }
    System.out.println("===========================================================================");
    print_invariants();

    for (int i=0; i<views_cond.size(); i++) {
      PptConditional pcond = (PptConditional) views_cond.elementAt(i);
      pcond.print_invariants_maybe();
    }

  }

  boolean check_modbits () {
    // This test is wrong for PptTopLevel because something is considered
    // unmodified only if none of its values are modified.  The test is
    // appropriate for PptSlice because we don't put missing values
    // there.

    // // The value "0" can be had for missing samples.
    // if (num_mod_non_missing_samples() < num_values() - 1) {
    //   throw new Error("Bad mod bits in dtrace file:\n"
    //                   + "num_mod_non_missing_samples()=" + num_mod_non_missing_samples()
    //                   + ", num_values()=" + num_values() + "\n"
    //                   + "for " + name + "\n"
    //                   + tuplemod_samples_summary() + "\n"
    //                   + "Consider running modbit-munge.pl");
    // }

    return true;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Printing invariants
  ///

  static Comparator icfp = new Invariant.InvariantComparatorForPrinting();

  /** Print invariants for a single program point. */
  public void print_invariants() {
    int num_samps = num_samples();
    System.out.println(name + "  " + num_samps
                       + " sample" + ((num_samps == 1) ? "" : "s"));
    System.out.println("    Samples breakdown: "
		       + tuplemod_samples_summary());
    System.out.print("    Variables:");
    for (int i=0; i<var_infos.length; i++)
      System.out.print(" " + var_infos[i].name);
    System.out.println();

    Assert.assert(check_modbits());

    if (Global.debugPrintInvariants) {
      // System.out.println("Views:");
      // for (Iterator itor = views.iterator(); itor.hasNext(); ) {
      //   PptSlice slice = (PptSlice) itor.next();
      //   System.out.println("  " + slice.name);
      //   for (int i=0; i<slice.invs.size(); i++) {
      //     Invariant inv = (Invariant) slice.invs.elementAt(i);
      //     System.out.println("    " + inv.repr());
      //   }
      // }

      System.out.println("    Variables:");
      for (int i=0; i<var_infos.length; i++) {
        VarInfo vi = var_infos[i];
        PptTopLevel ppt_tl = (PptTopLevel) vi.ppt;
        PptSlice slice1 = ppt_tl.findSlice(vi);
        System.out.print("      " + vi.name
                         + " constant=" + vi.isConstant()
                         + " canonical=" + vi.isCanonical()
                         + " equal_to=" + vi.equal_to.name);
        if (slice1 == null) {
          System.out.println(", no slice");
        } else {
          System.out.println(" slice=" + slice1
                           + "=" + slice1.name
                           + " num_values=" + slice1.num_values()
                           + " num_samples=" + slice1.num_samples());
          // slice1.values_cache.dump();
        }
      }
    }

    // First, do the equality invariants.  They don't show up in the below
    // because one of the two variables is non-canonical!
    // This technique is a bit non-orthogonal, but probably fine.
    // We might do no output if all the other variables are vacuous.
    // We should have already equal_to for each VarInfo.
    for (int i=0; i<var_infos.length; i++) {
      if (! var_infos[i].isCanonical()) {
        Global.non_canonical_variables++;
      } else if (Daikon.invariants_check_canBeMissing && var_infos[i].canBeMissing) {
        Global.can_be_missing_variables++;
      } else {
        Global.canonical_variables++;
      }
      if (var_infos[i].isDerived()) {
        Global.derived_variables++;
      }
    }
    for (int i=0; i<var_infos.length; i++) {
      VarInfo vi = var_infos[i];
      if (vi.isCanonical()) {
        Vector equal_vars = vi.equalToNonobvious();
        if (equal_vars.size() > 0) {
          StringBuffer sb = new StringBuffer(vi.name);
          for (int j=0; j<equal_vars.size(); j++) {
            VarInfo other = (VarInfo) equal_vars.elementAt(j);
            sb.append(" = ");
            sb.append(other.name);
          }
          PptTopLevel ppt_tl = (PptTopLevel) vi.ppt;
          PptSlice slice1 = ppt_tl.findSlice(vi);
          if (slice1 != null) {
            int num_values = slice1.num_values();
            int num_samples = slice1.num_samples();
            sb.append("\t\t(" + num_values + " value"
                      + ((num_values == 1) ? "" : "s") + ", "
                      + num_samples + " sample"
                      + ((num_samples == 1) ? "" : "s") + ")");
          } else {
            sb.append("\t\t(no slice)");
          }
          System.out.println(sb.toString());
        }
      }
    }

    // I could instead sort the PptSlice objects, then sort the invariants
    // in eahc PptSlice.  That would be more efficient, but this is
    // probably not a bottleneck anyway.
    Vector invs_vector = invariants_vector();
    Invariant[] invs_array = (Invariant[]) invs_vector.toArray(new Invariant[0]);
    Arrays.sort(invs_array, icfp);

    Global.non_falsified_invariants += invs_array.length;
    for (int ia_index = 0; ia_index<invs_array.length; ia_index++) {
      Invariant inv = invs_array[ia_index];
      int num_vals = inv.ppt.num_values();
      int inv_num_samps = inv.ppt.num_samples();
      String num_values_samples =
        "\t\t(" + num_vals + " value" + ((num_vals == 1) ? "" : "s") + ", "
        + inv_num_samps + " sample" + ((inv_num_samps == 1) ? "" : "s") + ")";

      // I could imagine printing information about the PptSlice
      // if it has changed since the last Invariant I examined.
      PptSlice slice = inv.ppt;
      if (Global.debugPrintInvariants) {
        System.out.println("Slice: " + slice.varNames() + "  "
                           + slice.num_samples() + " samples");
        System.out.println("    Samples breakdown: "
                           + slice.tuplemod_samples_summary());
        // slice.values_cache.dump();
      }
      Assert.assert(slice.check_modbits());

      // It's hard to know in exactly what order to do these checks that
      // eliminate some invariants from consideration.  Which is cheapest?
      // Which is most often successful?

      // Note exception (below) for OneOf invariants.
      int num_mod_non_missing_samples = inv.ppt.num_mod_non_missing_samples();
      if ((inv instanceof OneOf) && (((OneOf) inv).num_elts() > num_mod_non_missing_samples)) {
        System.out.println("Modbit problem:  more values (" + ((OneOf) inv).num_elts() + ") than modified samples (" + num_mod_non_missing_samples + ")");
      }

      // We print a OneOf invariant even if there are not very many
      // modified samples.  If the variable takes on only one value, maybe
      // it is only set once (or a few times).
      if ((num_mod_non_missing_samples < Invariant.min_mod_non_missing_samples)
          && (! (inv instanceof OneOf))) {
        if (Global.debugPrintInvariants) {
          System.out.println("  [Only " + inv.ppt.num_mod_non_missing_samples() + " modified non-missing samples (" + inv.ppt.num_samples() + " total samples): " + inv.repr() + " ]");
        }
        Global.too_few_samples_invariants++;
        continue;
      }

      if (IsEquality.it.accept(inv)) {
        Assert.assert(inv.ppt.var_infos.length == 2);
        Assert.assert(inv.ppt.var_infos[1].isCanonical() == false);
        if (inv.ppt.var_infos[0].isCanonical()) {
          Global.reported_invariants++;
          continue;
        } else {
          Global.non_canonical_invariants++;
          continue;
        }
      } else {
        boolean all_canonical = true;
        boolean some_nonconstant = false;
        VarInfo[] vis = inv.ppt.var_infos;
        for (int i=0; i<vis.length; i++) {
          if (! vis[i].isCanonical()) {
            if (Global.debugPrintInvariants)
              System.out.println("  [Suppressing " + inv.repr() + " because " + vis[i].name + " is non-canonical]");
            all_canonical = false;
            break;
          }
          if (! vis[i].isConstant()) {
            some_nonconstant = true;
            // Don't break out of the loop; this is computed as a side
            // effect but isn't the main point of the loop.
          }
        }
        if (! all_canonical) {
          if (Global.debugPptTopLevel) {
            System.out.println("  [not all vars canonical:  " + inv.repr() + " ]");
            System.out.print("    [Canonicalness:");
            for (int i=0; i<vis.length; i++)
              System.out.print(" " + vis[i].isCanonical());
            System.out.println("]");
            // I'm not sure this can still happen; reinstate the error to check.
            // // This *does* happen, because we instantiate all invariants
            // // simultaneously. so we don't yet know which of the new
            // // variables is canonical.  I should fix this.
            throw new Error("this shouldn't happen");
          }
          Global.non_canonical_invariants++;
          continue;
        }
        if (! some_nonconstant) {
          Assert.assert((inv instanceof OneOf) || (inv instanceof Comparison)
                        // , "Unexpected invariant with all vars constant: "
                        // + inv + "  " + inv.repr() + "  " + inv.format()
                        );
          if (inv instanceof Comparison) {
            Assert.assert(! IsEquality.it.accept(inv));
            if (Global.debugPrintInvariants) {
              System.out.println("  [over constants:  " + inv.repr() + " ]");
            }
            Global.obvious_invariants++;
            continue;
          }
        }
      }

      if (inv.isObvious()) {
        if (Global.debugPrintInvariants) {
          System.out.println("  [obvious:  " + inv.repr() + " ]");
        }
        Global.obvious_invariants++;
        continue;
      }

      if (!inv.justified()) {
        if (Global.debugPrintInvariants) {
          System.out.println("  [not justified:  " + inv.repr() + " ]");
        }
        Global.unjustified_invariants++;
        continue;
      }

      String inv_rep = inv.format();
      if (inv_rep != null) {
	if (Daikon.suppress_object_invariants_in_public_methods) {
	  Invariant obj_inv = findMatchingObjectInvariant(inv);
	  if (obj_inv != null) { // TODO: && will_print(obj_inv)
	    // TODO: Fix global statistics for this?
	    continue;
	  }
	}
	System.out.println(inv_rep + num_values_samples);
	if (Global.debugPrintInvariants) {
	  System.out.println("  [" + inv.repr() + "]");
	}
	Global.reported_invariants++;
      } else {
        if (Global.debugPrintInvariants) {
          System.out.println("[format returns null: " + inv.repr() + " ]");
        }
        Global.unjustified_invariants++;
      }
    }
  }


  // /** Print invariants for a single program point. */
  // public void print_invariants() {
  //   System.out.println(name + "  "
  //                      + num_samples() + " samples");
  //   System.out.println("    Samples breakdown: "
  //                      + values.tuplemod_samples_summary());
  //   // for (Iterator itor2 = views.keySet().iterator() ; itor2.hasNext() ; ) {
  //   for (Iterator itor2 = views.iterator() ; itor2.hasNext() ; ) {
  //     PptSlice slice = (PptSlice) itor2.next();
  //     if (Global.debugPrintInvariants) {
  //       System.out.println("Slice: " + slice.varNames() + "  "
  //                          + slice.num_samples() + " samples");
  //       System.out.println("    Samples breakdown: "
  //                          + slice.values_cache.tuplemod_samples_summary());
  //     }
  //     Invariants invs = slice.invs;
  //     int num_invs = invs.size();
  //     for (int i=0; i<num_invs; i++) {
  //       Invariant inv = invs.elementAt(i);
  //       String inv_rep = inv.format();
  //       if (inv_rep != null) {
  //         System.out.println(inv_rep);
  //         if (Global.debugPrintInvariants) {
  //           System.out.println("  " + inv.repr());
  //         }
  //       } else {
  //         if (Global.debugPrintInvariants) {
  //           System.out.println("[suppressed: " + inv.repr() + " ]");
  //         }
  //       }
  //     }
  //   }
  // }


  ///////////////////////////////////////////////////////////////////////////
  /// Diffing invariants
  ///

  static Comparator arityNameComparator = new PptSlice.ArityNameComparator();

  public void diff(PptTopLevel other) {
    SortedSet ss1 = new TreeSet(arityNameComparator);
    ss1.addAll(this.views);
    SortedSet ss2 = new TreeSet(arityNameComparator);
    ss2.addAll(other.views);
    for (OrderedPairIterator opi = new OrderedPairIterator(ss1.iterator(), ss2.iterator()); opi.hasNext(); ) {
      OrderedPairIterator.Pair pair = (OrderedPairIterator.Pair) opi.next();
      if (pair.b == null) {
        // Invariants are only on the left-hand side.
        System.out.println("Invariants are only on the left-hand side.");
      } else if (pair.a == null) {
        // Invariants are only on the right-hand side.
        System.out.println("Invariants are only on the right-hand side.");
      } else {
        // Invariants on both sides, must do more diffing.
        System.out.println("Invariants on both sides, must do more diffing.");
      }
    }

  }

}
