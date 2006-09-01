package daikon.inv.filter;

import daikon.inv.*;
import daikon.VarInfo;
import daikon.PrintInvariants;
import java.util.logging.Level;

/**
 * Suppress invariants that merely indicate that a variable was
 * unmodified.  Used only for ESC output.
 **/
public class UnmodifiedVariableEqualityFilter extends InvariantFilter {
  public String getDescription() {
    return "Suppress invariants that merely indicate that a variable was unmodified";
  }

  /**
   * Boolean. If true, UnmodifiedVariableEqualityFilter is initially turned on.
   */
  public static boolean dkconfig_enabled = true;

  public UnmodifiedVariableEqualityFilter () {
    isOn = dkconfig_enabled;
  }

  boolean shouldDiscardInvariant( Invariant invariant ) {
    if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
      PrintInvariants.debugFiltering.fine ("\tEntering UmVEF.shouldDiscard");
    }

    if (!IsEqualityComparison.it.accept(invariant)) {
      if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
        PrintInvariants.debugFiltering.fine ("\tUnmodVarEqF thinks this isn't an equality comparison");
      }
      return false;
    }

    Comparison comp = (Comparison)invariant;
    VarInfo var1 = comp.var1();
    VarInfo var2 = comp.var2();

    if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
      PrintInvariants.debugFiltering.fine ("compared " + var1.prestate_name()
                                           + " to " + var2.name());
    }

    if (var1.prestate_name().equals(var2.name())) {
      if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
        PrintInvariants.debugFiltering.fine ("\t(yes...)");
      }
      return true ;
    }

    if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
      PrintInvariants.debugFiltering.fine ("compared " + var2.prestate_name()
                                           + " to " + var1.name());
    }

    if (var2.prestate_name().equals(var1.name())) {
      if (PrintInvariants.debugFiltering.isLoggable(Level.FINE)) {
        PrintInvariants.debugFiltering.fine ("\t(yes...)");
      }
      return true;
    }

    return false;
  }
}
