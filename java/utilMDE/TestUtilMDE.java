package utilMDE;

import java.util.*;

// run like this:
//   java utilMDE.TestUtilMDE

// Files to test:
// ArraysMDE.java
// Assert.java
// Digest.java
// EqHashMap.java
// Hasher.java
// MathMDE.java
// TestUtilMDE.java
// UtilMDE.java
// WeakHasherMap.java

/** Test code for the utilMDE package. */
public class TestUtilMDE {

  public static void main(String[] args) {
    testTestUtilMDE();
    testArraysMDE();
    testEqHashMap();
    testHasher();
    testMathMDE();
    testUtilMDE();
    testWeakHasherMap();
    System.out.println("All utilMDE tests succeeded.");
  }

// I don't use Assert.assert() because that can be turned off by setting
// Assert.enabled = false, and I want these tests to always be performed.
  private static final void assert(boolean b) { if (!b) throw new Error(); }
  private static final void assert(boolean b, String s) { if (!b) throw new Error(s); }
  private static final void assert_arrays_equals(int[] a1, int[] a2) {
    assert(Arrays.equals(a1, a2),
	   "Arrays differ: " + ArraysMDE.toString(a1) + ", " + ArraysMDE.toString(a2));
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Utility functions
  ///

  private static Iterator int_array_iterator(int[] nums) {
    Object[] o = new Object[nums.length];
    for (int i=0; i<nums.length; i++)
      o[i] = new Integer(nums[i]);
    return Arrays.asList(o).iterator();
  }

  private static int[] int_iterator_array(Iterator itor) {
    Vector v = new Vector();
    while (itor.hasNext())
      v.add(itor.next());
    int[] a = new int[v.size()];
    for (int i=0; i<a.length; i++)
      a[i] = ((Integer)v.elementAt(i)).intValue();
    return a;
  }

  public static void testTestUtilMDE() {
    int[] a = new int[] { 3,4,5 };
    assert_arrays_equals(int_iterator_array(int_array_iterator(a)), a);
  }

  private static Vector toVector(Iterator itor) {
    Vector v = new Vector();
    for ( ; itor.hasNext() ; ) {
      v.add(itor.next());
    }
    return v;
  }

  private static Vector toVector(Enumeration e) {
    Vector v = new Vector();
    for ( ; e.hasMoreElements() ; ) {
      v.add(e.nextElement());
    }
    return v;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Now the actual testing
  ///

  public static void testArraysMDE() {
    // public static int min(int[] a)
    assert(ArraysMDE.min(new int[] { 1,2,3 }) == 1);
    assert(ArraysMDE.min(new int[] { 2,33,1 }) == 1);
    assert(ArraysMDE.min(new int[] { 3,-2,1 }) == -2);
    assert(ArraysMDE.min(new int[] { 3 }) == 3);

    // public static int max(int[] a)
    assert(ArraysMDE.max(new int[] { 1,2,3 }) == 3);
    assert(ArraysMDE.max(new int[] { 2,33,1 }) == 33);
    assert(ArraysMDE.max(new int[] { 3,-2,1 }) == 3);
    assert(ArraysMDE.max(new int[] { 3 }) == 3);

    // public static int[] min_max(int[] a)
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 1,2,3 }),
			 new int[] { 1,3 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 2,33,1 }),
			 new int[] { 1,33 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 3,-2,1 }),
			 new int[] { -2,3 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 3 }),
			 new int[] { 3,3 });

    // public static int element_range(int[] a)
    assert(ArraysMDE.element_range(new int[] { 1,2,3 }) == 2);
    assert(ArraysMDE.element_range(new int[] { 2,33,1 }) == 32);
    assert(ArraysMDE.element_range(new int[] { 3,-2,1 }) == 5);
    assert(ArraysMDE.element_range(new int[] { 3 }) == 0);

    // public static int indexOf(Object[] a, Object elt)
    // public static int indexOfEq(Object[] a, Object elt)
    {
      Integer[] a = new Integer[10];
      for (int i=0; i<a.length; i++)
	a[i] = new Integer(i);
      assert(ArraysMDE.indexOf(a, new Integer(-1)) == -1);
      assert(ArraysMDE.indexOf(a, new Integer(0)) == 0);
      assert(ArraysMDE.indexOf(a, new Integer(7)) == 7);
      assert(ArraysMDE.indexOf(a, new Integer(9)) == 9);
      assert(ArraysMDE.indexOf(a, new Integer(10)) == -1);
      assert(ArraysMDE.indexOf(a, new Integer(20)) == -1);

      assert(ArraysMDE.indexOfEq(a, new Integer(-1)) == -1);
      assert(ArraysMDE.indexOfEq(a, new Integer(0)) == -1);
      assert(ArraysMDE.indexOfEq(a, new Integer(7)) == -1);
      assert(ArraysMDE.indexOfEq(a, new Integer(9)) == -1);
      assert(ArraysMDE.indexOfEq(a, new Integer(10)) == -1);
      assert(ArraysMDE.indexOfEq(a, new Integer(20)) == -1);
      assert(ArraysMDE.indexOfEq(a, a[0]) == 0);
      assert(ArraysMDE.indexOfEq(a, a[7]) == 7);
      assert(ArraysMDE.indexOfEq(a, a[9]) == 9);
    }

    // public static int indexOf(int[] a, int elt)
    {
      int[] a = new int[10];
      for (int i=0; i<a.length; i++)
	a[i] = i;
      assert(ArraysMDE.indexOf(a, -1) == -1);
      assert(ArraysMDE.indexOf(a, 0) == 0);
      assert(ArraysMDE.indexOf(a, 7) == 7);
      assert(ArraysMDE.indexOf(a, 9) == 9);
      assert(ArraysMDE.indexOf(a, 10) == -1);
      assert(ArraysMDE.indexOf(a, 20) == -1);
    }

    // public static int indexOf(boolean[] a, boolean elt)
    {
      boolean[] a = new boolean[10];
      for (int i=0; i<a.length; i++)
	a[i] = false;
      assert(ArraysMDE.indexOf(a, true) == -1);
      assert(ArraysMDE.indexOf(a, false) == 0);
      a[9] = true;
      assert(ArraysMDE.indexOf(a, true) == 9);
      assert(ArraysMDE.indexOf(a, false) == 0);
      a[7] = true;
      assert(ArraysMDE.indexOf(a, true) == 7);
      assert(ArraysMDE.indexOf(a, false) == 0);
      a[0] = true;
      assert(ArraysMDE.indexOf(a, true) == 0);
      assert(ArraysMDE.indexOf(a, false) == 1);
      for (int i=0; i<a.length; i++)
	a[i] = true;
      assert(ArraysMDE.indexOf(a, true) == 0);
      assert(ArraysMDE.indexOf(a, false) == -1);
    }

    // public static int indexOf(Object[] a, Object[] sub)
    // public static int indexOfEq(Object[] a, Object[] sub)
    {
      Integer[] a = new Integer[10];
      for (int i=0; i<a.length; i++)
	a[i] = new Integer(i);
      Integer[] b = new Integer[] { };
      Integer[] c = new Integer[] { a[0], a[1], a[2] };
      Integer[] d = new Integer[] { a[1], a[2] };
      Integer[] e = new Integer[] { a[2], a[3], a[4], a[5] };
      Integer[] f = new Integer[] { a[7], a[8], a[9] };
      Integer[] g = new Integer[] { a[7], new Integer(8), a[9] };
      Integer[] h = new Integer[] { a[7], a[8], a[9], new Integer(10) };
      Integer[] c2 = new Integer[] { new Integer(0), new Integer(1), new Integer(2) };
      Integer[] d2 = new Integer[] { new Integer(1), new Integer(2) };
      Integer[] e2 = new Integer[] { new Integer(2), new Integer(3), new Integer(4), new Integer(5) };
      Integer[] f2 = new Integer[] { new Integer(7), new Integer(8), new Integer(9) };

      assert(ArraysMDE.indexOf(a, b) == 0);
      assert(ArraysMDE.indexOfEq(a, b) == 0);
      assert(ArraysMDE.indexOf(a, c) == 0);
      assert(ArraysMDE.indexOfEq(a, c) == 0);
      assert(ArraysMDE.indexOf(a, c2) == 0);
      assert(ArraysMDE.indexOfEq(a, c2) == -1);
      assert(ArraysMDE.indexOf(a, d) == 1);
      assert(ArraysMDE.indexOfEq(a, d) == 1);
      assert(ArraysMDE.indexOf(a, d2) == 1);
      assert(ArraysMDE.indexOfEq(a, d2) == -1);
      assert(ArraysMDE.indexOf(a, e) == 2);
      assert(ArraysMDE.indexOfEq(a, e) == 2);
      assert(ArraysMDE.indexOf(a, e2) == 2);
      assert(ArraysMDE.indexOfEq(a, e2) == -1);
      assert(ArraysMDE.indexOf(a, f) == 7);
      assert(ArraysMDE.indexOfEq(a, f) == 7);
      assert(ArraysMDE.indexOf(a, f2) == 7);
      assert(ArraysMDE.indexOfEq(a, f2) == -1);
      assert(ArraysMDE.indexOf(a, g) == 7);
      assert(ArraysMDE.indexOfEq(a, g) == -1);
      assert(ArraysMDE.indexOf(a, h) == -1);
      assert(ArraysMDE.indexOfEq(a, h) == -1);
    }


    // public static int indexOf(int[] a, int[] sub)
    {
      int[] a = new int[10];
      for (int i=0; i<a.length; i++)
	a[i] = i;
      int[] b = new int[] { };
      int[] c = new int[] { a[0], a[1], a[2] };
      int[] d = new int[] { a[1], a[2] };
      int[] e = new int[] { a[2], a[3], a[4], a[5] };
      int[] f = new int[] { a[7], a[8], a[9] };
      int[] g = new int[] { a[7], 22, a[9] };
      int[] h = new int[] { a[7], a[8], a[9], 10 };

      assert(ArraysMDE.indexOf(a, b) == 0);
      assert(ArraysMDE.indexOf(a, c) == 0);
      assert(ArraysMDE.indexOf(a, d) == 1);
      assert(ArraysMDE.indexOf(a, e) == 2);
      assert(ArraysMDE.indexOf(a, f) == 7);
      assert(ArraysMDE.indexOf(a, g) == -1);
      assert(ArraysMDE.indexOf(a, h) == -1);
    }

    // public static int indexOf(boolean[] a, boolean[] sub)
    // [I'm punting on this for now; deal with it later...]

    // public static Object[] subarray(Object[] a, int startindex, int length)
    // public static byte[] subarray(byte[] a, int startindex, int length)
    // public static boolean[] subarray(boolean[] a, int startindex, int length)
    // public static char[] subarray(char[] a, int startindex, int length)
    // public static double[] subarray(double[] a, int startindex, int length)
    // public static float[] subarray(float[] a, int startindex, int length)
    // public static int[] subarray(int[] a, int startindex, int length)
    // public static long[] subarray(long[] a, int startindex, int length)
    // public static short[] subarray(short[] a, int startindex, int length)

    // public static boolean isSubarray(Object[] a, Object[] sub, int a_offset)
    // public static boolean isSubarrayEq(Object[] a, Object[] sub, int a_offset)
    // public static boolean isSubarray(int[] a, int[] sub, int a_offset)
    // public static boolean isSubarray(boolean[] a, boolean[] sub, int a_offset)
    // [I'm punting on these for now, in the hopes that the array indexOf
    // operations above test them sufficiently.


    // static String toString(int[] a)
    assert(ArraysMDE.toString(new int[] { }).equals("[]"));
    assert(ArraysMDE.toString(new int[] { 0 }).equals("[0]"));
    assert(ArraysMDE.toString(new int[] { 0,1,2 }).equals("[0, 1, 2]"));

    // public static boolean sorted(int[] a)
    assert(ArraysMDE.sorted(new int[] { 0,1,2 }));
    assert(ArraysMDE.sorted(new int[] { 0,1,2,2,3,3 }));
    assert(ArraysMDE.sorted(new int[] { }));
    assert(ArraysMDE.sorted(new int[] { 0 }));
    assert(ArraysMDE.sorted(new int[] { 0,1 }));
    assert(!ArraysMDE.sorted(new int[] { 1,0 }));
    assert(!ArraysMDE.sorted(new int[] { 0,1,2,1,2,3 }));

    // public static class IntArrayComparatorLexical implements Comparator
    // public static class IntArrayComparatorLengthFirst implements Comparator
    {
      Comparator iacl = new ArraysMDE.IntArrayComparatorLexical();
      Comparator iaclf = new ArraysMDE.IntArrayComparatorLexical();
      int[] a0 = new int[] { };
      int[] a1 = new int[] { };
      int[] a2 = new int[] { 0,1,2,3 };
      int[] a3 = new int[] { 0,1,2,3,0 };
      int[] a4 = new int[] { 0,1,2,3,4 };
      int[] a5 = new int[] { 0,1,2,3,4 };
      int[] a6 = new int[] { 0,1,5,3,4 };
      int[] a7 = new int[] { 1,2,3,4 };

      assert(iacl.compare(a0, a1) == 0);
      assert(iaclf.compare(a0, a1) == 0);
      assert(iacl.compare(a1, a0) == 0);
      assert(iaclf.compare(a1, a0) == 0);
      assert(iacl.compare(a1, a2) < 0);
      assert(iaclf.compare(a1, a2) < 0);
      assert(iacl.compare(a2, a1) > 0);
      assert(iaclf.compare(a2, a1) > 0);
      assert(iacl.compare(a2, a3) < 0);
      assert(iaclf.compare(a2, a3) < 0);
      assert(iacl.compare(a3, a2) > 0);
      assert(iaclf.compare(a3, a2) > 0);
      assert(iacl.compare(a3, a4) < 0);
      assert(iaclf.compare(a3, a4) < 0);
      assert(iacl.compare(a4, a3) > 0);
      assert(iaclf.compare(a4, a3) > 0);
      assert(iacl.compare(a4, a5) == 0);
      assert(iaclf.compare(a4, a5) == 0);
      assert(iacl.compare(a5, a4) == 0);
      assert(iaclf.compare(a5, a4) == 0);
      assert(iacl.compare(a5, a6) < 0);
      assert(iaclf.compare(a5, a6) < 0);
      assert(iacl.compare(a6, a5) > 0);
      assert(iaclf.compare(a6, a5) > 0);
      assert(iacl.compare(a6, a7) < 0);
      assert(iaclf.compare(a6, a7) < 0);
      assert(iacl.compare(a7, a6) > 0);
      assert(iaclf.compare(a7, a6) > 0);
      assert(iacl.compare(a1, a4) < 0);
      assert(iaclf.compare(a1, a4) < 0);
      assert(iacl.compare(a4, a1) > 0);
      assert(iaclf.compare(a4, a1) > 0);
      assert(iacl.compare(a2, a4) < 0);
      assert(iaclf.compare(a2, a4) < 0);
      assert(iacl.compare(a4, a2) > 0);
      assert(iaclf.compare(a4, a2) > 0);
      assert(iacl.compare(a6, a4) > 0);
      assert(iaclf.compare(a6, a4) > 0);
      assert(iacl.compare(a4, a6) < 0);
      assert(iaclf.compare(a4, a6) < 0);
      assert(iacl.compare(a7, a4) > 0);
      assert(iaclf.compare(a7, a4) > 0);
      assert(iacl.compare(a4, a7) < 0);
      assert(iaclf.compare(a4, a7) < 0);
    }

    // Here I can only sensibly compare for equal vs. nonequal.
    // public static class ObjectArrayComparatorLexical implements Comparator
    // public static class ObjectArrayComparatorLengthFirst implements Comparator

    // private static class IntArrayHasher implements Hasher
    // private static class ObjectArrayHasher implements Hasher
    // public static int[] intern(int[] a)
    // public static Object[] intern(Object[] a)

    class InternTest {
      // javadoc won't let this be static.
      void test(boolean random) {
	int size1 = (random ? 100 : 1);
	int size2 = (random ? 10 : 1);

	Random random_gen = new Random();

	int[][] arrays = new int[100][];
	for (int i=0; i<arrays.length; i++) {
	  int[] a = new int[10];
	  for (int j=0; j<a.length; j++) {
	    if (random)
	      a[j] = random_gen.nextInt(1000);
	    else
	      a[j] = j;
	  }
	  arrays[i] = a;
	  // System.out.println(ArraysMDE.toString(a));
	  // Sadly, this is required to get the last array to be garbage-collected
	  // with Jikes 1.03 and JDK 1.2.2.
	  a = null;
	}
	System.gc();
	if (Intern.numIntArrays() != 0)
	  throw new Error();
	for (int i=0; i<arrays.length; i++)
	  Intern.intern(arrays[i]);
	if (Intern.numIntArrays() != size1)
	  throw new Error();
	System.gc();
	if (Intern.numIntArrays() != size1)
	  throw new Error();
	for (int i=10; i<arrays.length; i++)
	  arrays[i] = null;
	System.gc();
	if (Intern.numIntArrays() != size2) {
	  if (Intern.numIntArrays() < size2 + 10) {
	    System.out.println("Is JIT disabled?  Size should have been " + size2 + ", actually was "
			       + Intern.numIntArrays());
	  } else {
	    System.out.println("================");
	    for (int i=0; i<arrays.length; i++)
	      System.out.println(ArraysMDE.toString(arrays[i]));
	    System.out.println("================");
	    for (Iterator itor = Intern.intArrays(); itor.hasNext(); ) {
	      System.out.println(ArraysMDE.toString((int[])itor.next()));
	    }
	    String message = ("Size should have been " + size2 + ", actually was "
			      + Intern.numIntArrays());
	    System.out.println(message);
	    throw new Error(message);
	  }
	}
      }
    }

    InternTest intern = new InternTest();
    intern.test(true);
    intern.test(false);

  }

  public static void testEqHashMap() {
  }

  public static void testHasher() {
  }

  public static void testMathMDE() {

    // int negate(int a)
    assert(MathMDE.negate(3) == -3);
    assert(MathMDE.negate(-22) == 22);
    assert(MathMDE.negate(0) == 0);

    // int bitwiseComplement(int a)
    assert(MathMDE.bitwiseComplement(3) == -4);
    assert(MathMDE.bitwiseComplement(-22) == 21);
    assert(MathMDE.bitwiseComplement(0) == -1);

    // int sign(int a)
    assert(MathMDE.sign(3) == 1);
    assert(MathMDE.sign(-22) == -1);
    assert(MathMDE.sign(0) == 0);

    // int pow(int base, int expt)
    try {
      assert(MathMDE.pow(3, 3) == 27);
      assert(MathMDE.pow(-5, 5) == -3125);
      assert(MathMDE.pow(22, 0) == 1);
      assert(MathMDE.pow(4, 6) == 4096);
      assert(MathMDE.pow(1, 222222) == 1);
      assert(MathMDE.pow(-2, 25) == -33554432);
      // This is beyond the precision.  Maybe return a long instead of an int?
      // assert(MathMDE.pow(-3, 25) == ...);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e.toString());
    }
    {
      boolean exception = false;
      try {
	MathMDE.pow(3, -3);
      } catch (Exception e) {
	exception = true;
      }
      assert(exception);
    }


    // int gcd(int a, int b)
    assert(MathMDE.gcd(2, 50) == 2);
    assert(MathMDE.gcd(50, 2) == 2);
    assert(MathMDE.gcd(12, 144) == 12);
    assert(MathMDE.gcd(144, 12) == 12);
    assert(MathMDE.gcd(96, 144) == 48);
    assert(MathMDE.gcd(144, 96) == 48);
    assert(MathMDE.gcd(10, 25) == 5);
    assert(MathMDE.gcd(25, 10) == 5);
    assert(MathMDE.gcd(17, 25) == 1);
    assert(MathMDE.gcd(25, 17) == 1);

    // int gcd(int[] a)
    assert(MathMDE.gcd(new int[] {2, 50}) == 2);
    assert(MathMDE.gcd(new int[] {12, 144}) == 12);
    assert(MathMDE.gcd(new int[] {96, 144}) == 48);
    assert(MathMDE.gcd(new int[] {10, 25}) == 5);
    assert(MathMDE.gcd(new int[] {100, 10, 25}) == 5);
    assert(MathMDE.gcd(new int[] {768, 324}) == 12);
    assert(MathMDE.gcd(new int[] {2400, 48, 36}) == 12);
    assert(MathMDE.gcd(new int[] {2400, 72, 36}) == 12);

    // int gcd_differences(int[] a)
    // Weak set of tests, derived directly from those of "int gcd(int[] a)".
    assert(MathMDE.gcd_differences(new int[] {0, 2, 52}) == 2);
    assert(MathMDE.gcd_differences(new int[] {0, 12, 156}) == 12);
    assert(MathMDE.gcd_differences(new int[] {0, 96, 240}) == 48);
    assert(MathMDE.gcd_differences(new int[] {0, 10, 35}) == 5);
    assert(MathMDE.gcd_differences(new int[] {0, 100, 110, 135}) == 5);
    assert(MathMDE.gcd_differences(new int[] {0, 768, 1092}) == 12);
    assert(MathMDE.gcd_differences(new int[] {0, 2400, 2448, 2484}) == 12);
    assert(MathMDE.gcd_differences(new int[] {0, 2400, 2472, 2508}) == 12);

    // int mod_positive(int x, int y)
    assert(MathMDE.mod_positive(33, 5) == 3);
    assert(MathMDE.mod_positive(-33, 5) == 2);
    assert(MathMDE.mod_positive(33, -5) == 3);
    assert(MathMDE.mod_positive(-33, -5) == 2);

    // int[] missing_numbers(int[] nums)
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,5,6,7,8 }),
			 new int[] {});
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,6,7,8 }),
			 new int[] { 5 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,8 }),
			 new int[] { 5,6,7 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,5,6,8 }),
			 new int[] { 4,7 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,6,8 }),
			 new int[] { 4,5,7 });


    // class MissingNumbersIterator
    class TestMissingNumbersIterator {
      // javadoc won't let this be static
      void test(int[] orig, int[] goal_missing) {
	Iterator orig_iterator = int_array_iterator(orig);
	Iterator missing_iterator = new MathMDE.MissingNumbersIterator(orig_iterator);
	int[] missing = TestUtilMDE.int_iterator_array(missing_iterator);
	assert_arrays_equals(missing, goal_missing);
      }
    }

    TestMissingNumbersIterator tmni = new TestMissingNumbersIterator();
    tmni.test(new int[] { 3,4,5,6,7,8 }, new int[] {});
    tmni.test(new int[] { 3,4,6,7,8 }, new int[] { 5 });
    tmni.test(new int[] { 3,4,8 }, new int[] { 5,6,7 });
    tmni.test(new int[] { 3,5,6,8 }, new int[] { 4,7 });
    tmni.test(new int[] { 3,6,8 }, new int[] { 4,5,7 });

    // int[] modulus(int[] nums)
    // int[] modulus(Iterator itor)

    class TestModulus {
      // javadoc won't let this be static
      void check(int[] nums, int[] goal_rm) {
	int[] rm = MathMDE.modulus(nums);
	if (!Arrays.equals(rm, goal_rm))
	  throw new Error("Expected (r,m)=" + ArraysMDE.toString(goal_rm)
			  + ", saw (r,m)=" + ArraysMDE.toString(rm));
	if (rm == null)
	  return;
	int goal_r = rm[0];
	int m = rm[1];
	for (int i=0; i<nums.length; i++) {
	  int r = nums[i] % m;
	  if (r < 0) r += m;
	  if (r != goal_r)
	    throw new Error("Expected " + nums[i] + " % " + m + " = " + goal_r
			    + ", got " + r);
	}
      }

      // javadoc won't let this be static
      void check(Iterator itor, int[] goal_rm) {
	// There would be no point to this:  it's testing
	// int_iterator_array, not the iterator version!
	// return check(int_iterator_array(itor), goal_rm);
	assert_arrays_equals(MathMDE.modulus(itor), goal_rm);
      }

      // javadoc won't let this be static
      void check_iterator(int[] nums, int[] goal_rm) {
	check(int_array_iterator(nums), goal_rm);
      }
    }

    TestModulus testModulus = new TestModulus();

    testModulus.check(new int[] {3,7,47,51}, new int[] { 3,4 });
    testModulus.check(new int[] {3,11,43,51}, new int[] { 3,8 });
    testModulus.check(new int[] {3,11,47,55}, new int[] { 3,4 });
    testModulus.check(new int[] {2383,4015,-81,463,-689}, new int[] { 15,32 });

    testModulus.check_iterator(new int[] {3,7,47,51}, new int[] { 3,4 });
    testModulus.check_iterator(new int[] {3,11,43,51}, new int[] { 3,8 });
    testModulus.check_iterator(new int[] {3,11,47,55}, new int[] { 3,4 });
    testModulus.check_iterator(new int[] {2383,4015,-81,463,-689}, new int[] { 15,32 });


    // int[] nonmodulus_strict(int[] nums)
    // int[] nonmodulus_nonstrict(int[] nums)
    // int[] nonmodulus_strict(Iterator nums)

    class TestNonModulus {
      // javadoc won't let this be static
      void check_strict(int[] nums, int[] goal_rm) {
	check(nums, goal_rm, true);
	Iterator itor = int_array_iterator(nums);
	assert_arrays_equals(MathMDE.nonmodulus_strict(itor), goal_rm);
      }

      // javadoc won't let this be static
      void check_nonstrict(int[] nums, int[] goal_rm) {
	check(nums, goal_rm, false);
      }

      // javadoc won't let this be static
      void check(int[] nums, int[] goal_rm, boolean strict) {
	int[] rm;
	if (strict)
	  rm = MathMDE.nonmodulus_strict(nums);
	else
	  rm = MathMDE.nonmodulus_nonstrict(nums);
	if (!Arrays.equals(rm, goal_rm))
	  throw new Error("Expected (r,m)=" + ArraysMDE.toString(goal_rm)
			  + ", saw (r,m)=" + ArraysMDE.toString(rm));
	if (rm == null)
	  return;
	int goal_r = rm[0];
	int m = rm[1];
	for (int i=0; i<nums.length; i++) {
	  int r = nums[i] % m;
	  if (r < 0) r += m;
	  if (r == goal_r)
	    throw new Error("Expected inequality, saw " + nums[i] + " % " + m + " = " + r);
	}
      }
    }

    TestNonModulus testNonModulus = new TestNonModulus();

    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,9}, null);
    testNonModulus.check_strict(new int[] {-1,1,2,3,5,6,7,9}, new int[] {0,4});
    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,9,11}, null);
    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,11}, null);

    // null because only 7 elements, so don't try modulus = 4
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9}, null);
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,10}, new int[] {0,4});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11}, new int[] {0,4});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13}, null);
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13,14,15}, new int[] {4,6});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13,14,15,22}, null);

  }

  public static void testUtilMDE() {

    // public static BufferedReader BufferedFileReader(String filename)
    // public static void addToClasspath(String dir)

    {
      // These names are taken from APL notation, where iota creates an
      // array of all the numbers up to its argument.
      Vector iota0 = new Vector();
      Vector iota10 = new Vector();
      for (int i=0; i<10; i++)
        iota10.add(new Integer(i));
      Vector iota10_twice = new Vector();
      iota10_twice.addAll(iota10);
      iota10_twice.addAll(iota10);
      Vector iota10_thrice = new Vector();
      iota10_thrice.addAll(iota10);
      iota10_thrice.addAll(iota10);
      iota10_thrice.addAll(iota10);

      // public static class EnumerationIterator implements Iterator
      // public static class IteratorEnumeration implements Enumeration

      Assert.assert(iota0.equals(toVector(iota0.iterator())));
      Assert.assert(iota0.equals(toVector(new UtilMDE.IteratorEnumeration(iota0.iterator()))));
      Assert.assert(iota0.equals(toVector(iota0.elements())));
      Assert.assert(iota0.equals(toVector(new UtilMDE.EnumerationIterator(iota0.elements()))));
      Assert.assert(iota10.equals(toVector(iota10.iterator())));
      Assert.assert(iota10.equals(toVector(new UtilMDE.IteratorEnumeration(iota10.iterator()))));
      Assert.assert(iota10.equals(toVector(iota10.elements())));
      Assert.assert(iota10.equals(toVector(new UtilMDE.EnumerationIterator(iota10.elements()))));

      // public static class MergedIterator2 implements Iterator {
      Assert.assert(iota10_twice.equals(toVector(new UtilMDE.MergedIterator2(iota10.iterator(), iota10.iterator()))));
      Assert.assert(iota10.equals(toVector(new UtilMDE.MergedIterator2(iota0.iterator(), iota10.iterator()))));
      Assert.assert(iota10.equals(toVector(new UtilMDE.MergedIterator2(iota10.iterator(), iota0.iterator()))));

      // public static class MergedIterator implements Iterator {
      Vector iota10_iterator_thrice = new Vector();
      iota10_iterator_thrice.add(iota10.iterator());
      iota10_iterator_thrice.add(iota10.iterator());
      iota10_iterator_thrice.add(iota10.iterator());
      Assert.assert(iota10_thrice.equals(toVector(new UtilMDE.MergedIterator(iota10_iterator_thrice.iterator()))));
      Vector iota10_iterator_twice_1 = new Vector();
      iota10_iterator_twice_1.add(iota0.iterator());
      iota10_iterator_twice_1.add(iota10.iterator());
      iota10_iterator_twice_1.add(iota10.iterator());
      Vector iota10_iterator_twice_2 = new Vector();
      iota10_iterator_twice_2.add(iota10.iterator());
      iota10_iterator_twice_2.add(iota0.iterator());
      iota10_iterator_twice_2.add(iota10.iterator());
      Vector iota10_iterator_twice_3 = new Vector();
      iota10_iterator_twice_3.add(iota10.iterator());
      iota10_iterator_twice_3.add(iota10.iterator());
      iota10_iterator_twice_3.add(iota0.iterator());
      Assert.assert(iota10_twice.equals(toVector(new UtilMDE.MergedIterator(iota10_iterator_twice_1.iterator()))));
      Assert.assert(iota10_twice.equals(toVector(new UtilMDE.MergedIterator(iota10_iterator_twice_2.iterator()))));
      Assert.assert(iota10_twice.equals(toVector(new UtilMDE.MergedIterator(iota10_iterator_twice_3.iterator()))));


      class OddFilter implements Filter {
        public OddFilter() { }
        public boolean accept(Object o) {
          if (!(o instanceof Integer))
            return false;
          return ((Integer) o).intValue() % 2 == 1;
        }
      }

      Vector iota10_odd = new Vector();
      for (int i=0; i<iota10.size(); i++)
        if (i%2 == 1)
          iota10_odd.add(new Integer(i));
      Assert.assert(iota10_odd.equals(toVector(new UtilMDE.FilteredIterator(iota10.iterator(), new OddFilter()))));

    }

    // public static boolean propertyIsTrue(Properties p, String key)
    // public static String appendProperty(Properties p, String key, String value)
    // public static String setDefault(Properties p, String key, String value)
    // public static void streamCopy(java.io.InputStream from, java.io.OutputStream to)

    assert(UtilMDE.replaceString("hello dolly well hello dolly", " ", "  ").equals("hello  dolly  well  hello  dolly"));
    assert(UtilMDE.replaceString("  hello  dolly well hello dolly  ", " ", "  ").equals("    hello    dolly  well  hello  dolly    "));
    assert(UtilMDE.replaceString("hello dolly well hello dolly", "ll", "y").equals("heyo doyy wey heyo doyy"));
    assert(UtilMDE.replaceString("hello dolly well hello dolly", "q", "xxx").equals("hello dolly well hello dolly"));

    // public static void internStrings(String[] a)

    assert(UtilMDE.join(new String[] { "foo", "bar", "baz" }, ", ").equals("foo, bar, baz"));
    assert(UtilMDE.join(new String[] { "foo" }, ", ").equals("foo"));
    assert(UtilMDE.join(new String[] { }, ", ").equals(""));
    assert(UtilMDE.join(new Integer[] { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4) }, "").equals("01234"));
    Vector potpourri = new Vector();
    potpourri.add("day"); potpourri.add(new Integer(2)); potpourri.add("day");
    assert(UtilMDE.join(potpourri, " ").equals("day 2 day"));

    // This will be easy to write tests for, when I get around to it.
    // public static Vector tokens(String str, String delim, boolean returnTokens)
    // public static Vector tokens(String str, String delim)
    // public static Vector tokens(String str)

    // This is tested by the tokens methods.
    // public static Vector makeVector(Enumeration e)

  }

  public static void testWeakHasherMap() {
  }

}
