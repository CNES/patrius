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
 * HISTORY
 * VERSION:4.11.1:FA:FA-86:30/06/2023:[PATRIUS] Retours JE Alice
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Exp;
import fr.cnes.sirius.patrius.math.analysis.function.Power;
import fr.cnes.sirius.patrius.math.analysis.function.Tan;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * <p>
 * Unit tests for {@link RiddersDifferentiator}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: RiddersDifferentiatorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.2
 * 
 */
public class RiddersDifferentiatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Ridders differentiator
         * 
         * @featureDescription Validate the Ridders differentiator
         * 
         * @coveredRequirements DV-ATT_120, DV-ATT_130
         */
        VALIDATE_RIDDERS_DIFFERENTIATOR
    }

    /** The Ridders differentiator. */
    static RiddersDifferentiator differentiator1;

    /** The Ridders differentiator. */
    static RiddersDifferentiator differentiator2;

    /**
     * Setup for all unit tests in the class.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        differentiator1 = new RiddersDifferentiator(0.1);
        differentiator2 = new RiddersDifferentiator(0.01);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_RIDDERS_DIFFERENTIATOR}
     * 
     * @testedMethod {@link RiddersDifferentiator#differentiate(double, UnivariateFunction)}
     * 
     * @description test the Ridders differentiation on various derivable functions
     * 
     * @input some functions.
     * 
     * @output the derivative of the functions
     * 
     * @testPassCriteria the numerical derivative is equal to the analytical derivative
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDifferentiateFunctions() {
        // TEST - exception when the step is zero:
        try {
            new RiddersDifferentiator(0);
            fail();
        } catch (final RuntimeException e) {
            // does nothing
        }
        // TEST - Cosine function:
        final UnivariateDifferentiableFunction cos = new Cos();
        this.test(cos);
        // TEST - Exponential function:
        final UnivariateDifferentiableFunction exp = new Exp();
        this.test(exp);
        // TEST - Tangent function.
        final UnivariateDifferentiableFunction tan = new Tan();
        this.test(tan);
        // TEST - Power function.
        final UnivariateDifferentiableFunction pow = new Power(0.5);
        this.test(pow);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_RIDDERS_DIFFERENTIATOR}
     * 
     * @testedMethod {@link RiddersDifferentiator#differentiate(double, UnivariateFunction)}
     * @testedMethod {@link RiddersDifferentiator#differentiate(UnivariateFunction)}
     * 
     * @description test the Ridders differentiator on a Fourier series
     * 
     * @input a Fourier series
     * 
     * @output the derivative of the Fourier series
     * 
     * @testPassCriteria the numerical derivative is equal to the analytical derivative
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testFourierSeries() {
        final double a0 = 1;
        final double[] a = new double[] { 1, 2, 3, 4 };
        final double[] b = new double[] { 4, 3, 2, 1 };

        final FourierSeries fourier = new FourierSeries(1, a0, a, b);

        // Ridders differentiator:
        double expected = fourier.derivative().value(4.23);
        double actual = differentiator1.differentiate(4.23, fourier);
        Assert.assertEquals(expected, actual, 1E-10);
        expected = fourier.derivative().value(0.25);
        actual = differentiator2.differentiate(0.25, fourier);
        Assert.assertEquals(expected, actual, 1E-10);
        expected = fourier.derivative().value(-10.3);
        actual = differentiator2.differentiate(-10.3, fourier);
        Assert.assertEquals(expected, actual, 1E-10);
        // New interface
        final double expectedVal = fourier.value(4.23);
        final double actualVal = differentiator1.differentiate(fourier).value(4.23);
        Assert.assertEquals(expectedVal, actualVal, 1E-10);
        expected = fourier.derivative().value(4.23);
        final DerivativeStructure devVal = new DerivativeStructure(1, 1, 0, 4.23);
        actual = differentiator1.differentiate(fourier).value(devVal).getPartialDerivative(1);
        Assert.assertEquals(expected, actual, 1E-10);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_RIDDERS_DIFFERENTIATOR}
     * 
     * @testedMethod {@link RiddersDifferentiator#differentiate(double, UnivariateFunction)}
     * @testedMethod {@link RiddersDifferentiator#differentiate(UnivariateFunction)}
     * 
     * @description test the Ridders differentiator on a polynomial function
     * 
     * @input a polynomial function
     * 
     * @output the derivative of the polynomial function
     * 
     * @testPassCriteria the numerical derivative is equal to the analytical derivative
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testPolynomialFunction() {
        final double[] c = new double[] { 1, 2, 3, 4 };
        final PolynomialFunction function = new PolynomialFunction(c);

        // Ridders differentiator:
        double expected = function.univariateDerivative().value(8.568);
        double actual = differentiator1.differentiate(8.568, function);
        Assert.assertEquals(0, MathLib.abs((expected - actual) / expected), 1E-10);
        expected = function.univariateDerivative().value(0.25);
        actual = differentiator1.differentiate(0.25, function);
        Assert.assertEquals(0, MathLib.abs((expected - actual) / expected), 1E-10);
        expected = function.univariateDerivative().value(-156.3);
        actual = differentiator1.differentiate(-156.3, function);
        Assert.assertEquals(0, MathLib.abs((expected - actual) / expected), 1E-10);
        // New interface
        expected = function.univariateDerivative().value(8.568);
        final DerivativeStructure valDev = new DerivativeStructure(1, 1, 0, 8.568);
        actual = differentiator1.differentiate(function).value(valDev).getPartialDerivative(1);
        Assert.assertEquals(0, MathLib.abs((expected - actual) / expected), 1E-10);
        expected = function.univariateDerivative().value(0.25);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_RIDDERS_DIFFERENTIATOR}
     * 
     * @testedMethod {@link RiddersDifferentiator#differentiate(UnivariateFunction)}
     * 
     * @description tests the exception raised for an incorrect DerivativeStructure
     * 
     * @input an incorrect DerivativeStructure
     * 
     * @output result of call to differentiate()
     * 
     * @testPassCriteria MathRuntimeException
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test(expected = MathRuntimeException.class)
    public void testError() {
        final double a0 = 1;
        final double[] a = new double[] { 1, 2, 3, 4 };
        final double[] b = new double[] { 4, 3, 2, 1 };

        final FourierSeries fourier = new FourierSeries(1, a0, a, b);

        // New interface
        final DerivativeStructure devVal = new DerivativeStructure(2, 2, 0, 4.23);
        final double actual = differentiator1.differentiate(fourier).value(devVal).getPartialDerivative(1);
        // Should not reach here
        Assert.fail(Double.toString(actual));
    }

    /**
     * Compute the derivative of a function f and compare the result with the reference value.
     * 
     * @param f
     *        the function to differentiate
     */
    private void test(final UnivariateDifferentiableFunction f) {
        final double val1 = 0.1;
        final double val2 = FastMath.PI / 3;
        final double val3 = 5.;
        // Ridders differentiation:
        double actual1 = differentiator2.differentiate(val1, f);
        final double actual2 = differentiator2.differentiate(val2, f);
        final double actual3 = differentiator1.differentiate(val3, f);
        // compute the expected values (err --> O(h))
        final DerivativeStructure dval1 = new DerivativeStructure(1, 1, 0, val1);
        final DerivativeStructure dval2 = new DerivativeStructure(1, 1, 0, val2);
        final DerivativeStructure dval3 = new DerivativeStructure(1, 1, 0, val3);
        final double expected1 = f.value(dval1).getPartialDerivative(1);
        final double expected2 = f.value(dval2).getPartialDerivative(1);
        final double expected3 = f.value(dval3).getPartialDerivative(1);
        Assert.assertEquals(expected1, actual1, 1E-10);
        Assert.assertEquals(expected2, actual2, 1E-10);
        Assert.assertEquals(expected3, actual3, 1E-10);
        // New interface
        final DerivativeStructure valDev = new DerivativeStructure(1, 1, 0, val1);
        actual1 = differentiator2.differentiate(f).value(valDev).getPartialDerivative(1);
        Assert.assertEquals(expected1, actual1, 1E-10);
    }
}
