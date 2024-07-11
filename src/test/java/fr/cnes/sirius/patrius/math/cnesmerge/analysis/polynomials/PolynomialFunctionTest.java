/**
 * 
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.cnesmerge.analysis.polynomials;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * @description additional unit tests for PolynomialFunction
 * 
 * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials#PolynomialFunction}
 * 
 * @author cardosop
 * 
 * @version $Id: PolynomialFunctionTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class PolynomialFunctionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle improve the code coverage
         * 
         * @featureDescription Improve the code coverage.
         * 
         * @coveredRequirements DV-CALCUL_10,DV-CALCUL_20
         */
        COVERAGE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#hashCode()}
     * 
     * @description Coverage for the hashcode implementation.
     * 
     * @input An array for the PolynomialFunction constructor
     * 
     * @output The hashcode
     * 
     * @testPassCriteria Hashcode value
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testHashCode() {
        // Some array
        final double[] polyArr = { 2, 5, 7, 9 };
        // The hashcode for PolynomialFunction
        // (NOTE : depends on an Array.hashcode()
        // so it may change across JRE/JDK updates!)
        final int expected = 1319638944;
        final PolynomialFunction pf = new PolynomialFunction(polyArr);
        assertEquals(expected, pf.hashCode());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#PolynomialFunction(double[])}
     * 
     * @description Coverage for the constructor - failure cases.
     * 
     * @input An array for the PolynomialFunction constructor
     * 
     * @output None
     * 
     * @testPassCriteria Expected exceptions
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NoDataException.class)
    public final void testPolynomialFunction() {
        // Empty array
        final double[] empty = {};
        // We build a PolynomialFunction out of it,
        // It should raise a NoDataException
        new PolynomialFunction(empty);
        // We should not arrive here
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#evaluate(double[], double)}
     * 
     * @description Coverage and fail case for the evaluate method.
     * 
     * @input None
     * 
     * @output None
     * 
     * @testPassCriteria Expected exceptions
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NoDataException.class)
    public final void testEvaluate() {
        final double[] useless = { 1 };
        // Class with an empty array call
        final MyPolynomialFunction mpf = new MyPolynomialFunction(useless);
        // This call should raise NoDataException
        mpf.runEvalEmpty();
        // We should not arrive here
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#add(PolynomialFunction)}
     * 
     * @description Coverage test case for lower order minuend polynomial.
     * 
     * @input Arrays for 2 polynomials
     * 
     * @output Added array
     * 
     * @testPassCriteria Expected array
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testAdd() {
        final double[] a = { 2, 3, 4 };
        final double[] b = { 4, 5, 6, 7 };
        final double[] e = { 6, 8, 10, 7 };
        final PolynomialFunction pfa = new PolynomialFunction(a);
        final PolynomialFunction pfb = new PolynomialFunction(b);
        final PolynomialFunction pfe = new PolynomialFunction(e);
        // Note the subtrahend is a polynomial of higher order
        final PolynomialFunction added = pfa.add(pfb);
        assertTrue(pfe.equals(added));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#subtract(PolynomialFunction)}
     * 
     * @description Coverage test case for lower order minuend polynomial.
     * 
     * @input Arrays for 2 polynomials
     * 
     * @output Subtracted array
     * 
     * @testPassCriteria Expected array
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testSubtract() {
        final double[] a = { 2, 3, 4 };
        final double[] b = { 4, 5, 6, 7 };
        final double[] e = { -2, -2, -2, -7 };
        final PolynomialFunction pfa = new PolynomialFunction(a);
        final PolynomialFunction pfb = new PolynomialFunction(b);
        final PolynomialFunction pfe = new PolynomialFunction(e);
        // Note the subtrahend is a polynomial of higher order
        final PolynomialFunction subtracted = pfa.subtract(pfb);
        assertTrue(pfe.equals(subtracted));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#negate()}
     * 
     * @description Test the negate method.
     * 
     * @input Arrays for polynomials
     * 
     * @output Negated array
     * 
     * @testPassCriteria Expected array
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testNegate() {
        final double[] a = { 4, -5, 6, -7 };
        final double[] e = { -4, 5, -6, 7 };
        final PolynomialFunction pfa = new PolynomialFunction(a);
        final PolynomialFunction pfe = new PolynomialFunction(e);
        final PolynomialFunction negated = pfa.negate();
        assertTrue(pfe.equals(negated));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#differentiate(double[])}
     * 
     * @description Coverage and fail case for the differentiate method.
     * 
     * @input None
     * 
     * @output None
     * 
     * @testPassCriteria Expected exceptions
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = NoDataException.class)
    public final void testDifferentiate() {
        final double[] useless = { 1 };
        // Class with an empty array call
        final MyPolynomialFunction mpf = new MyPolynomialFunction(useless);
        // This call should raise NoDataException
        mpf.runDiffEmpty();
        // We should not arrive here
        fail();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#toString()}
     * 
     * @description Test ".0" removal for the toString method.
     * 
     * @input Array for a polynomial
     * 
     * @output A representing String
     * 
     * @testPassCriteria Expected String
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testToString() {
        // This test checks if the toString method
        // does cut the ".0" ending double coefficients
        // that "are" integer values
        final double[] a = { 2.5, 3, -2 };
        final String expected = "2.5 + 3 x - 2 x^2";
        final PolynomialFunction pfa = new PolynomialFunction(a);
        assertEquals(expected, pfa.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction#equals(java.lang.Object)}
     * 
     * @description Additional (quite obvious) coverage test cases for the equals method.
     * 
     * @input Misc
     * 
     * @output Misc
     * 
     * @testPassCriteria Misc
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testEqualsObject() {
        // Does it equal itself? Yes.
        final double[] a = { 6, -2 };
        final PolynomialFunction pfa = new PolynomialFunction(a);
        assertTrue(pfa.equals(pfa));
        // Does it equal some random object? No.
        final Object randomObject = new Object();
        assertFalse(pfa.equals(randomObject));
        // Does it equal some other different polynomial? No.
        final double[] b = { 6, -2, 2.5 };
        final PolynomialFunction pfb = new PolynomialFunction(b);
        assertFalse(pfa.equals(pfb));
    }

    /**
     * Worker class meant to call evaluate.<br>
     * NOTE : no longer needed if merged inside commons-math!
     */
    private class MyPolynomialFunction extends PolynomialFunction {

        /**
         * Generated serialVersionUID.
         */
        private static final long serialVersionUID = 301311427753063823L;

        /**
         * Constructor.
         * 
         * @param c
         *        constructor array
         * @throws NullArgumentException
         *         null argument
         * @throws NoDataException
         *         empty array argument
         */
        public MyPolynomialFunction(final double[] c) {
            super(c);
        }

        /**
         * Has "evaluate" fail.
         */
        public void runEvalEmpty() {
            final double[] empty = {};
            // This should fail with NoDataException
            PolynomialFunction.evaluate(empty, 0.);
        }

        /**
         * Has "differentiate" fail.
         */
        public void runDiffEmpty() {
            final double[] empty = {};
            // This should fail with NoDataException
            PolynomialFunction.differentiate(empty);
        }
    }

}
