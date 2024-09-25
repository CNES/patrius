/**
 * 
 * Copyright 2011-2022 CNES
 *
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
 * VERSION:4.13:DM:DM-119:08/12/2023:[PATRIUS] Ajout d'une methode copy(AbsoluteDate)
 * à  l'interface DatePolynomialFunctionInterface
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre la classe QuaternionPolynomialSegment plus generique et ajouter de la coherence dans le package polynomials
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class test {@link DatePolynomialFunction} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DatePolynomialFunctionTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class DatePolynomialFunctionTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Polynomial functions
         * 
         * @featureDescription Validate polynomial functions
         * 
         * @coveredRequirements
         */
        POLYNOMIAL_FUNCTION
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#value(AbsoluteDate)}
     * 
     * @description test value function
     * 
     * @input DatePolynomialFunction
     * 
     * @output value
     * 
     * @testPassCriteria values are exactly as expected (mathematical cases)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testValue() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;

        // Normal case (check before, and after reference date)
        final double[] coefs = { 2., 3., 4., 5. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);

        Assert.assertEquals(-4628., f.value(date0.shiftedBy(-10)), 0.);
        Assert.assertEquals(5432., f.value(date0.shiftedBy(10)), 0.);

        // Test cache
        final double[] coefs2 = { 2., 3., 4., 5. };
        final PolynomialFunction poly2 = new PolynomialFunction(coefs2);
        final DatePolynomialFunction f2 = new DatePolynomialFunction(date0, poly2);
        Assert.assertEquals(5432., f2.value(date0.shiftedBy(10)), 0.);

        final double[] coefs3 = { 2., 3., 4., 5., 6. };
        final PolynomialFunction poly3 = new PolynomialFunction(coefs3);
        final DatePolynomialFunction f3 = new DatePolynomialFunction(date0, poly3);
        Assert.assertEquals(65432., f3.value(date0.shiftedBy(10)), 0.);
        Assert.assertEquals(5432., f2.value(date0.shiftedBy(10)), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#getX0()}
     * @testedMethod {@link DatePolynomialFunction#getCoefficients()}
     * @testedMethod {@link DatePolynomialFunction#getDegree()}
     * @testedMethod {@link DatePolynomialFunction#getT0()}
     * @testedMethod {@link DatePolynomialFunction#getDateIntervals()}
     * @testedMethod {@link DatePolynomialFunction#getPolynomialType()}
     * 
     * @description test getters and copy
     * 
     * @input DatePolynomialFunction
     * 
     * @output getters
     * 
     * @testPassCriteria values are exactly as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testGetters() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 2., 3., 4, 5 };
        final PolynomialFunction poly = new PolynomialFunction(coefs);
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);

        // Test getters
        Assert.assertEquals(3., f.getDegree(), 0.);
        Assert.assertEquals(0., f.getT0().durationFrom(date0), 0.);
        Assert.assertEquals(2., f.getCoefficients()[0], 0.);
        Assert.assertEquals(3., f.getCoefficients()[1], 0.);
        Assert.assertEquals(4., f.getCoefficients()[2], 0.);
        Assert.assertEquals(5., f.getCoefficients()[3], 0.);
        Assert.assertNull(f.getTimeFactor());
        Assert.assertEquals(PolynomialType.CLASSICAL, f.getPolynomialType());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#DatePolynomialFunction(AbsoluteDate, double[])}
     * 
     * @description test exception at initialization
     * 
     * @input DatePolynomialFunction
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are raised as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() {
        // Not enough coefficients
        try {
            final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
            final double[] coefs = {};
            final PolynomialFunction poly = new PolynomialFunction(coefs);
            new DatePolynomialFunction(date0, poly);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#derivative()}
     * 
     * @description test derivative function
     * 
     * @input DatePolynomialFunction
     * 
     * @output derivative
     * 
     * @testPassCriteria derivative is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDerivative() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 2., 3., 4., 5. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);
        final DatePolynomialFunction der = f.derivative();
        final double[] actualDerCoefs = der.getCoefficients();
        final double[] expectedDerCoefs = { 3., 8., 15. };
        Assert.assertArrayEquals(expectedDerCoefs, actualDerCoefs, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#primitive()}
     * 
     * @description test primitive function
     * 
     * @input DatePolynomialFunction
     * 
     * @output primitive
     * 
     * @testPassCriteria primitive is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testPrimitive() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double[] coefs = { 3., 8., 15. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);
        final DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);
        final DatePolynomialFunction prim = f.primitive(date0, 2.);
        final double[] actualPrimCoefs = prim.getCoefficients();
        final double[] expectedPrimCoefs = { 2., 3., 4., 5. };
        Assert.assertArrayEquals(expectedPrimCoefs, actualPrimCoefs, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#copy(AbsoluteDate)}
     * 
     * @description test copy function
     * 
     * @input DatePolynomialFunction
     * 
     * @output copied function
     * 
     * @testPassCriteria copied function is exactly as expected
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testCopy() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = date0.shiftedBy(-10.);
        final AbsoluteDate date2 = date0.shiftedBy(10.);
        final double[] coefs = { 2., 3., 4., 5. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);

        // #1 With time factor equal to 1 (null)
        DatePolynomialFunction fct = new DatePolynomialFunction(date0, poly);

        // Evaluate same origin date
        DatePolynomialFunction fctCopied = fct.copy(date0);
        Assert.assertEquals(date0, fctCopied.getT0());
        Assert.assertNull(fctCopied.getTimeFactor());
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // Evaluate backward origin date
        fctCopied = fct.copy(date1);
        Assert.assertEquals(date1, fctCopied.getT0());
        Assert.assertNull(fctCopied.getTimeFactor());
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // Evaluate forward origin date
        fctCopied = fct.copy(date2);
        Assert.assertEquals(date2, fctCopied.getT0());
        Assert.assertNull(fctCopied.getTimeFactor());
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // #2 With time factor equal to 2
        final double timeFactor = 2.;
        final AbsoluteDate date3 = date0.shiftedBy(timeFactor - 1e-7);
        fct = new DatePolynomialFunction(date0, timeFactor, poly);

        // Evaluate same origin date
        fctCopied = fct.copy(date0);
        Assert.assertEquals(date0, fctCopied.getT0());
        Assert.assertEquals(timeFactor, fctCopied.getTimeFactor(), 0.);
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // Evaluate backward origin date
        fctCopied = fct.copy(date1);
        Assert.assertEquals(date1, fctCopied.getT0());
        Assert.assertEquals(timeFactor - date1.durationFrom(date0), fctCopied.getTimeFactor(), 0.);
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // Evaluate forward origin date (newOriginDate < originDate + timeFactor) (shouldn't fail)
        fctCopied = fct.copy(date3);
        Assert.assertEquals(date3, fctCopied.getT0());
        Assert.assertEquals(timeFactor - date3.durationFrom(date0), fctCopied.getTimeFactor(), 0.);
        Assert.assertTrue(evaluateFunction(fct, fctCopied));

        // Try to evaluate forward origin date (newOriginDate >= originDate + timeFactor) (should fail)
        final AbsoluteDate date4 = date0.shiftedBy(timeFactor);
        try {
            fct.copy(date2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
        try {
            fct.copy(date4);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Evaluate two {@link DatePolynomialFunction} on several dates.
     * 
     * @param fct1
     *        First function (reference, on the interval [T0-20 ; T0+timeFactor])
     * @param fct2
     *        Second function
     * @return {@code true} if the two functions generate the same values
     */
    private static boolean evaluateFunction(final DatePolynomialFunction fct1, final DatePolynomialFunction fct2) {
        boolean isEqual = true;
        final AbsoluteDate startDate = fct1.getT0().shiftedBy(-20.);
        final AbsoluteDate endDate = fct1.getTimeFactor() == null ? fct1.getT0() : fct1.getT0().shiftedBy(
            fct1.getTimeFactor());
        final int duration = (int) endDate.durationFrom(startDate);

        for (int i = 0; i <= duration; i++) {
            final AbsoluteDate date = startDate.shiftedBy(i);
            if (MathLib.abs(fct1.value(date) - fct2.value(date)) > 1e-8) {
//                System.out.println(fct1.value(date) + "\t" + fct2.value(date)); // TODO
                isEqual = false;
            }

        }
        return isEqual;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#dateToDouble(AbsoluteDate)}
     * 
     * @description test date to double function
     * 
     * @input DatePolynomialFunction
     * 
     * @output double
     * 
     * @testPassCriteria double is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDateToDouble() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        double expectedDouble = 60.;
        final AbsoluteDate date1 = date0.shiftedBy(expectedDouble);
        final double[] coefs = { 2., 3., 4., 5. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);

        // With time factor null
        DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);
        double actualDouble = f.dateToDouble(date1);
        Assert.assertEquals(expectedDouble, actualDouble, Precision.DOUBLE_COMPARISON_EPSILON);

        // With time factor equal to 2
        final double timeFactor = 2.;
        f = new DatePolynomialFunction(date0, timeFactor, poly);
        actualDouble = f.dateToDouble(date1);
        expectedDouble = expectedDouble / timeFactor;
        Assert.assertEquals(expectedDouble, actualDouble, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#POLYNOMIAL_FUNCTION}
     * 
     * @testedMethod {@link DatePolynomialFunction#DoubleToDate(double)}
     * 
     * @description test double to date function
     * 
     * @input DatePolynomialFunction
     * 
     * @output Absolute date
     * 
     * @testPassCriteria Absolute date is exactly as expected
     * 
     * @referenceVersion 4.11
     * 
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDoubleToDate() {
        // Initialization
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final double duration = 60.;
        final double[] coefs = { 2., 3., 4., 5. };
        final PolynomialFunction poly = new PolynomialFunction(coefs);

        // With time factor null
        DatePolynomialFunction f = new DatePolynomialFunction(date0, poly);
        AbsoluteDate actualDate = f.doubleToDate(duration);
        AbsoluteDate expectedDate = date0.shiftedBy(duration);
        Assert.assertEquals(expectedDate, actualDate);

        // With time factor equal to 2
        final double timeFactor = 2.;
        f = new DatePolynomialFunction(date0, timeFactor, poly);
        actualDate = f.doubleToDate(duration);
        expectedDate = date0.shiftedBy(duration * timeFactor);
        Assert.assertEquals(expectedDate, actualDate);
    }
}
