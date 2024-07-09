/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.fitting.PolynomialChebyshevFitter;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.MultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.GaussNewtonOptimizer;

/**
 * Unit test class for the {@link PolynomialChebyshevFunction} class.
 *
 * @author bonitt
* HISTORY
* VERSION:4.8:DM:DM-2997:15/11/2021:[PATRIUS] Disposer de fonctionnalites d'evaluation de l'erreur d'approximation d'une fonction 
* VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation de fonctions 
* END-HISTORY
 */
public final class PolynomialChebyshevFunctionTest {

    /** Error tolerance for tests. */
    private double tolerance;

    /** Basic start range used by tests. */
    private double basicStart;

    /** Basic end range used by tests. */
    private double basicEnd;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#PolynomialChebyshevFunction(double, double, double[])}
     * @testedMethod {@link PolynomialChebyshevFunction#degree()}
     * @testedMethod {@link PolynomialChebyshevFunction#getCoefficients()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     */
    @Test
    public void testConstructor() {

        final double[] coefficientsChebyshev = { -1.5, 3, -2.5 };
        final PolynomialChebyshevFunction f =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficientsChebyshev);

        Assert.assertEquals(2, f.degree());
        Assert.assertTrue(Arrays.equals(f.getCoefficients(), coefficientsChebyshev));

        // Build a second Chebyshev polynomial with last coefficients 0 (shouldn't be considered)
        final double[] coefficientsChebyshev2 = { -1.5, 3, -2.5, 0., 0. };
        final PolynomialChebyshevFunction f2 =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficientsChebyshev2);

        Assert.assertEquals(2, f2.degree());
        Assert.assertTrue(Arrays.equals(f2.getCoefficients(), coefficientsChebyshev));
        Assert.assertFalse(Arrays.equals(f2.getCoefficients(), coefficientsChebyshev2));
    }

    /**
     * @description Try to build a Chebyshev polynomial with null or empty coefficients or with an invalid range.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#PolynomialChebyshevFunction(double, double, double[])}
     *
     * @testPassCriteria The exceptions are returned as expected.
     */
    @Test
    public void testConstructorException() {
        // Try to build a Chebyshev polynomial with null coefficients
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to build a Chebyshev polynomial with empty coefficients
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[0]);
            Assert.fail();
        } catch (final NoDataException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to build a Chebyshev polynomial with an invalid range
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicStart, new double[2]);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to build a Chebyshev polynomial with an invalid range
        try {
            new PolynomialChebyshevFunction(this.basicEnd, this.basicStart, new double[2]);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Check the Chebyshev polynomial evaluation exception cases.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The polynomials returns the expected exceptions.
     */
    @Test
    public void testValueException() {
        // Try to evaluate the polynomial outside its range (should fail)
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[2])
                .value(this.basicStart - 0.001);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[2]).value(this.basicEnd + 0.001);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Evaluate the Chebyshev polynomials addition feature (+ the exceptions cases).
     *
     * @testedMethod {@link PolynomialChebyshevFunction#add(PolynomialChebyshevFunction)}
     *
     * @testPassCriteria The resulting Chebyshev polynomial is built as expected.
     */
    @Test
    public void testAddition() {

        // First polynomial definition: f1(x) = -1.5 + 3x
        final double[] coefficients1 = { -1.5, 3. };
        final UnivariateFunction function1 = new PolynomialFunction(coefficients1);
        final PolynomialChebyshevFunction fctCheb1 =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function1, 50, this.basicStart, this.basicEnd);

        // Second polynomial definition: f2(x) = 0.5 + 2x - x^2
        final double[] coefficients2 = { 0.5, 2., -1. };
        final UnivariateFunction function2 = new PolynomialFunction(coefficients2);
        final PolynomialChebyshevFunction fctCheb2 =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function2, 50, this.basicStart, this.basicEnd);

        // Addition: f3(x) = f4(x) = f1(x) + f2(x) = -1 + 5x - x^2
        final PolynomialChebyshevFunction fctCheb3 = fctCheb1.add(fctCheb2);
        final PolynomialChebyshevFunction fctCheb4 = fctCheb2.add(fctCheb1);

        // Evaluate the values
        final double[] coefficientsExpected = { -1, 5., -1. };
        final UnivariateFunction functionExpected = new PolynomialFunction(coefficientsExpected);

        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(functionExpected.value(i), fctCheb3.value(i), 1e-9);
            Assert.assertEquals(functionExpected.value(i), fctCheb4.value(i), 1e-9);
        }

        // Try to add two Chebyshev polynomials with different ranges
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .add(new PolynomialChebyshevFunction(this.basicStart + 0.1, this.basicEnd,
                            coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .add(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd - 0.1,
                            coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Evaluate the Chebyshev polynomials subtraction feature (+ the exceptions cases).
     *
     * @testedMethod {@link PolynomialChebyshevFunction#subtract(PolynomialChebyshevFunction)}
     *
     * @testPassCriteria The resulting Chebyshev polynomial is built as expected.
     */
    @Test
    public void testSubtraction() {

        // First polynomial definition: f1(x) = -1.5 + 3x
        final double[] coefficients1 = { -1.5, 3. };
        final UnivariateFunction function1 = new PolynomialFunction(coefficients1);
        final PolynomialChebyshevFunction fctCheb1 =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function1, 50, this.basicStart, this.basicEnd);

        // Second polynomial definition: f2(x) = 0.5 + 2x - x^2
        final double[] coefficients2 = { 0.5, 2., -1. };
        final UnivariateFunction function2 = new PolynomialFunction(coefficients2);
        final PolynomialChebyshevFunction fctCheb2 =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function2, 50, this.basicStart, this.basicEnd);

        // Subtraction: f3(x) = f1(x) - f2(x) = -2 + x + x^2
        final PolynomialChebyshevFunction fctCheb3 = fctCheb1.subtract(fctCheb2);

        // Subtraction: f4(x) = f2(x) - f1(x) = -f3(x)
        final PolynomialChebyshevFunction fctCheb4 = fctCheb2.subtract(fctCheb1);

        // Evaluate the values
        final double[] coefficientsExpected = { -2, 1., 1. };
        final UnivariateFunction functionExpected = new PolynomialFunction(coefficientsExpected);

        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(functionExpected.value(i), fctCheb3.value(i), 1e-9);
            Assert.assertEquals(-functionExpected.value(i), fctCheb4.value(i), 1e-9);
        }

        // Try to add two Chebyshev polynomials with different ranges
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .subtract(new PolynomialChebyshevFunction(this.basicStart + 0.1, this.basicEnd,
                            coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .subtract(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd - 0.1,
                            coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Evaluate the Chebyshev polynomials negate feature.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#negate()}
     *
     * @testPassCriteria The resulting Chebyshev polynomial is built as expected.
     */
    @Test
    public void testNegate() {

        // First polynomial definition: f1(x) = -1.5 + 3x
        final double[] coefficients1 = { -1.5, 3. };
        final UnivariateFunction function1 = new PolynomialFunction(coefficients1);
        final PolynomialChebyshevFunction fctCheb1 =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function1, 50, this.basicStart, this.basicEnd);

        // Negate: f2(x) = -f1(x) = 1.5 - 3x
        final PolynomialChebyshevFunction fctCheb2 = fctCheb1.negate();

        // Evaluate the values
        final double[] coefficientsExpected = { 1.5, -3. };
        final UnivariateFunction functionExpected = new PolynomialFunction(coefficientsExpected);

        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(functionExpected.value(i), fctCheb2.value(i), 1e-9);
        }
    }

    /**
     * @description Evaluate the derivatives of a 0 degree Chebyshev polynomial (should be 0).
     *
     * @testedMethod {@link PolynomialChebyshevFunction#polynomialDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The polynomials return the expected derivatives.
     */
    @Test
    public void test0DegreeDerivative() {
        final double[] coefficients = { -1.5 };
        final PolynomialChebyshevFunction f =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients);

        final PolynomialChebyshevFunction polyDerivative1 = f.polynomialDerivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(0, polyDerivative1.degree());

        final UnivariateFunction polyDerivative2 = f.derivative(); // Generic type

        // Check a several places (should be equal to the f'(x) = 0 function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(0., polyDerivative1.value(i), this.tolerance);
            Assert.assertEquals(0., polyDerivative2.value(i), this.tolerance);
        }
    }

    /**
     * @description Evaluate the derivatives of a 1st degree Chebyshev polynomial against a reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = -1.5 + 3x</math> -> <math>f'(x) = 3</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev polynomial and evaluate it
     *              against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#polynomialDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test1stDegreeDerivative() {
        final double[] coefficients = { -1.5, 3. };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function, 50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.polynomialDerivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(0, polyDerivative1.degree());

        final UnivariateFunction polyDerivative2 = fctCheb.derivative(); // Generic type

        final double[] coefficientsPolynomial = { 3. };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), this.tolerance);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), this.tolerance);
        }
    }

    /**
     * @description Evaluate the derivatives of a 2nd degree Chebyshev polynomial against a reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = -1.5 + 3x - 2.5x^2</math> -> <math>f'(x) = 3 -
     *              5x</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev polynomial and evaluate it
     *              against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#polynomialDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test2ndDegreeDerivative() {
        final double[] coefficients = { -1.5, 3., -2.5 };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function, 50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.polynomialDerivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(fctCheb.degree() - 1, polyDerivative1.degree());

        final UnivariateFunction polyDerivative2 = fctCheb.derivative(); // Generic type

        final double[] coefficientsPolynomial = { 3., -5. };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), 1e-9);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), 1e-9);
        }
    }

    /**
     * @description Evaluate the derivatives of a 3rd degree Chebyshev polynomial against a reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = 8 + 7x - 2.5x^2 + 1.5x^3</math> -> <math>f'(x) = 7 -
     *              5x + 4.5x^2</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev polynomial and evaluate it
     *              against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#polynomialDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test3rdDegreeDerivative() {
        final double[] coefficients = { 8., 7., -2.5, 1.5 };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb =
            ChebyshevDecompositionEngine.approximateChebyshevFunction(function, 50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.polynomialDerivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(2, polyDerivative1.degree());

        final UnivariateFunction polyDerivative2 = fctCheb.derivative(); // Generic type

        final double[] coefficientsPolynomial = { 7., -5., 4.5 };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), 1e-9);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), 1e-9);
        }
    }

    /**
     * @description Check the String representation method behavior.
     *
     * @testedMethod {@link AngularActors#toString()}
     *
     * @testPassCriteria The polynomials String representation contains the expected information.
     */
    @Test
    public void testString() {

        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { -5., 3., 1. }).toString(), "-5 + 3 T1 + T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 0., -2., 3. }).toString(), "-2 T1 + 3 T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 1., -2., 3. }).toString(), "1 - 2 T1 + 3 T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 0., 2., 3. }).toString(), "2 T1 + 3 T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 1., 2., 3. }).toString(), "1 + 2 T1 + 3 T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 1., 0., 3. }).toString(), "1 + 3 T2");
        Assert.assertEquals(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 0. }).toString(), "0");
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#equals(Object)}
     * @testedMethod {@link PolynomialChebyshevFunction#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {

        final double[] coefficients = new double[] { -0.1, 0.2, 0.3 };

        // New instance
        final PolynomialChebyshevFunction instance =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients);

        // Check the hashCode consistency between calls
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());

        // Compared object is null
        Assert.assertFalse(instance.equals(null));

        // Compared object is a different class
        Assert.assertFalse(instance.equals(new Object()));

        // Same instance
        Assert.assertTrue(instance.equals(instance));

        // Same data, but different instances
        PolynomialChebyshevFunction other =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients);

        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());

        // Different start range
        other = new PolynomialChebyshevFunction(-1.1, this.basicEnd, coefficients);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different end range
        other = new PolynomialChebyshevFunction(this.basicStart, 1.1, coefficients);

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different coefficients
        other = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { -0.1, 0.2, 0.4 });

        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    /**
     * @description Test class serialization.
     */
    @Test
    public void testSerial() {
        final PolynomialChebyshevFunction p =
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { 3, 2, 1 });
        Assert.assertEquals(p, TestUtils.serializeAndRecover(p));
    }

    /**
     * @description Evaluate the Chebyshev polynomial fitter feature.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#gradient(double, double...)}
     * @testedMethod {@link PolynomialChebyshevFitter#fit(int, double[])}
     * @testedMethod {@link PolynomialChebyshevFitter#fit(double[])}
     *
     * @testPassCriteria the optimized Chebyshev polynomial as expected (reference: math, threshold:
     *                   0).
     */
    @Test
    public void testFitter() {

        // Initialization
        final double eps = 1E-14;
        final double[] coefficientsChebyshev = { -1.5, 3, -2.5 };
        final PolynomialChebyshevFunction f = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficientsChebyshev);

        final ConvergenceChecker<PointVectorValuePair> checker = new ConvergenceChecker<PointVectorValuePair>() {
            @Override
            public boolean converged(final int iteration,
                    final PointVectorValuePair previous,
                    final PointVectorValuePair current) {
                boolean res = true;
                for (int i = 0; i < coefficientsChebyshev.length; i++) {
                    res &= (previous.getSecond()[i] - current.getSecond()[i]) <= eps;
                }
                return res;
            }
        }; 
        final MultivariateVectorOptimizer optimizer = new GaussNewtonOptimizer(checker);
        final PolynomialChebyshevFitter fitter = new PolynomialChebyshevFitter(this.basicStart, this.basicEnd, optimizer);
        
        // Add observations
        for (int i = 0; i < 100; i++) {
            final double x = this.basicStart + (i / 100.) * (this.basicEnd - this.basicStart);
            fitter.addObservedPoint(x, f.value(x));
        }

        // Fit
        final double[] initialGuess = {0, 0, 0};
        final double[] actual = fitter.fit(initialGuess);
        final double[] actual2 = fitter.fit(1000000, initialGuess);
        // Check
        Assert.assertEquals(coefficientsChebyshev[0], actual[0], eps);
        Assert.assertEquals(coefficientsChebyshev[1], actual[1], eps);
        Assert.assertEquals(coefficientsChebyshev[2], actual[2], eps);
        Assert.assertEquals(coefficientsChebyshev[0], actual2[0], eps);
        Assert.assertEquals(coefficientsChebyshev[1], actual2[1], eps);
        Assert.assertEquals(coefficientsChebyshev[2], actual2[2], eps);
        
        // Other functional check
        try {
            new PolynomialChebyshevFitter(this.basicEnd, this.basicStart, optimizer);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Initialize the global parameters.
     */
    @Before
    public void setUp() {
        this.tolerance = 1e-10;
        this.basicStart = -1;
        this.basicEnd = 1.5;
    }
}
