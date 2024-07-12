/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3310:22/05/2023:[PATRIUS] Ajout de methode pour integrer les fonctions polynomiales
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation de fonctions 
 * END-HISTORY
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
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
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
     * @testedMethod {@link PolynomialChebyshevFunction#getDegree()}
     * @testedMethod {@link PolynomialChebyshevFunction#getCoefficients()}
     * @testedMethod {@link PolynomialChebyshevFunction#getPolynomialType()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testConstructor() {

        final double[] coefficientsChebyshev = { -1.5, 3, -2.5 };
        final PolynomialChebyshevFunction f = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficientsChebyshev);

        Assert.assertEquals(2, f.getDegree());
        Assert.assertTrue(Arrays.equals(f.getCoefficients(), coefficientsChebyshev));

        // Build a second Chebyshev polynomial with last coefficients 0
        final double[] coefficientsChebyshev2 = { -1.5, 3, -2.5, 0., 0. };
        final PolynomialChebyshevFunction f2 = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficientsChebyshev2);

        Assert.assertEquals(4, f2.getDegree());
        Assert.assertFalse(Arrays.equals(f2.getCoefficients(), coefficientsChebyshev));
        Assert.assertTrue(Arrays.equals(f2.getCoefficients(), coefficientsChebyshev2));
        Assert.assertEquals(PolynomialType.CHEBYSHEV, f2.getPolynomialType());
    }

    /**
     * @description Try to build a Chebyshev polynomial with null or empty coefficients or with an
     *              invalid range.
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
     * @description Check the Chebyshev polynomial evaluation particular cases.
     *
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The polynomials work nominally.
     */
    @Test
    public void testValueException() {
        // Evaluate the polynomial outside its range (should work nominally)
        final double polyChebFunct1 = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[2])
                .value(this.basicStart - 0.001);
        Assert.assertNotNull(polyChebFunct1);
        final double polyChebFunct2 = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[2])
                .value(this.basicEnd + 0.001);
        Assert.assertNotNull(polyChebFunct2);
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
        final PolynomialChebyshevFunction fctCheb1 = ChebyshevDecompositionEngine.interpolateChebyshevFunction(
                function1, 50, this.basicStart, this.basicEnd);

        // Second polynomial definition: f2(x) = 0.5 + 2x - x^2
        final double[] coefficients2 = { 0.5, 2., -1. };
        final UnivariateFunction function2 = new PolynomialFunction(coefficients2);
        final PolynomialChebyshevFunction fctCheb2 = ChebyshevDecompositionEngine.interpolateChebyshevFunction(
                function2, 50, this.basicStart, this.basicEnd);

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
                    .add(new PolynomialChebyshevFunction(this.basicStart + 0.1, this.basicEnd, coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .add(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd - 0.1, coefficients1));
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
        final PolynomialChebyshevFunction fctCheb1 = ChebyshevDecompositionEngine.interpolateChebyshevFunction(
                function1, 50, this.basicStart, this.basicEnd);

        // Second polynomial definition: f2(x) = 0.5 + 2x - x^2
        final double[] coefficients2 = { 0.5, 2., -1. };
        final UnivariateFunction function2 = new PolynomialFunction(coefficients2);
        final PolynomialChebyshevFunction fctCheb2 = ChebyshevDecompositionEngine.interpolateChebyshevFunction(
                function2, 50, this.basicStart, this.basicEnd);

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
                    .subtract(new PolynomialChebyshevFunction(this.basicStart + 0.1, this.basicEnd, coefficients1));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, coefficients1)
                    .subtract(new PolynomialChebyshevFunction(this.basicStart, this.basicEnd - 0.1, coefficients1));
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
        final PolynomialChebyshevFunction fctCheb1 = ChebyshevDecompositionEngine.interpolateChebyshevFunction(
                function1, 50, this.basicStart, this.basicEnd);

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
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#univariateDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The polynomials return the expected derivatives.
     */
    @Test
    public void test0DegreeDerivative() {
        final double[] coefficients = { -1.5 };
        final PolynomialChebyshevFunction f = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficients);

        final PolynomialChebyshevFunction polyDerivative1 = f.derivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(0, polyDerivative1.getDegree());

        final UnivariateFunction polyDerivative2 = f.univariateDerivative(); // Generic type

        // Check a several places (should be equal to the f'(x) = 0 function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(0., polyDerivative1.value(i), this.tolerance);
            Assert.assertEquals(0., polyDerivative2.value(i), this.tolerance);
        }
    }

    /**
     * @description Evaluate the derivatives of a 1st degree Chebyshev polynomial against a
     *              reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = -1.5 + 3x</math> -> <math>f'(x) =
     *              3</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev
     *              polynomial and evaluate it against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#univariateDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test1stDegreeDerivative() {
        final double[] coefficients = { -1.5, 3. };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
                50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.derivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(fctCheb.getDegree() - 1, polyDerivative1.getDegree());

        final UnivariateFunction polyDerivative2 = fctCheb.univariateDerivative(); // Generic type

        final double[] coefficientsPolynomial = { 3. };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), this.tolerance);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), this.tolerance);
        }
    }

    /**
     * @description Evaluate the derivatives of a 2nd degree Chebyshev polynomial against a
     *              reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = -1.5 + 3x - 2.5x^2</math> ->
     *              <math>f'(x) = 3 - 5x</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev
     *              polynomial and evaluate it against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#univariateDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test2ndDegreeDerivative() {
        final double[] coefficients = { -1.5, 3., -2.5 };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
                50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.derivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(fctCheb.getDegree() - 1, polyDerivative1.getDegree());

        final UnivariateFunction polyDerivative2 = fctCheb.univariateDerivative(); // Generic type

        final double[] coefficientsPolynomial = { 3., -5. };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), 1e-9);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), 1e-9);
        }
    }

    /**
     * @description Evaluate the derivatives of a 3rd degree Chebyshev polynomial against a
     *              reference polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = 8 + 7x - 2.5x^2 + 1.5x^3</math>
     *              -> <math>f'(x) = 7 - 5x + 4.5x^2</math>
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we derive the Chebyshev
     *              polynomial and evaluate it against f'(x).
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#univariateDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The Chebyshev polynomial derivative is equal (same value) to f'(x).
     */
    @Test
    public void test3rdDegreeDerivative() {
        final double[] coefficients = { 8., 7., -2.5, 1.5 };
        final UnivariateFunction function = new PolynomialFunction(coefficients);
        final PolynomialChebyshevFunction fctCheb = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
                50, this.basicStart, this.basicEnd);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.derivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(fctCheb.getDegree() - 1, polyDerivative1.getDegree());

        final UnivariateFunction polyDerivative2 = fctCheb.univariateDerivative(); // Generic type

        final double[] coefficientsPolynomial = { 7., -5., 4.5 };
        final UnivariateFunction fDerivativeRef = new PolynomialFunction(coefficientsPolynomial); // f'(x)

        // Check a several places (should be equal to the f'(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative1.value(i), 1e-9);
            Assert.assertEquals(fDerivativeRef.value(i), polyDerivative2.value(i), 1e-9);
        }
    }

    /**
     * @description Check the coefficients of a 9th degree Chebyshev polynomial derivative.
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#derivative()}
     *
     * @testPassCriteria The Chebyshev polynomial derivative has the expected coefficients.
     */
    @Test
    public void test9thDegreeDerivativeCoefficients() {
        final double[] coefficients = { 1., 2., 3., 4., 5., 6., 7., 8., 9., 10. };
        final PolynomialChebyshevFunction fctCheb = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficients);

        final PolynomialChebyshevFunction polyDerivative1 = fctCheb.derivative();
        Assert.assertEquals(this.basicStart, polyDerivative1.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyDerivative1.getEnd(), 0.);
        Assert.assertEquals(fctCheb.getDegree() - 1, polyDerivative1.getDegree());

        final double[] refPolyDerCoeff = new double[] { 152.0, 224.0, 300.8, 214.4, 281.6, 182.4, 233.60000000000002,
                115.2, 144.0 };
        final double[] resPolyDerCoeff = polyDerivative1.getCoefficients();
        Assert.assertArrayEquals(refPolyDerCoeff, resPolyDerCoeff, 1E-14);
    }

    /**
     * @description Evaluate the primitive of a 3rd degree Chebyshev polynomial against a reference
     *              polynomial.
     *              <p>
     *              This will test the function:</br> <math>f(x) = 7 - 5x + 4.5x^2</math> ->
     *              <math>F(x) = 8 + 7x - 2.5x^2 + 1.5x^3</math> with a given (0,8) point.
     *              </p>
     *              <p>
     *              We build a Chebyshev polynomial from f(x). Then we integrate the Chebyshev
     *              polynomial and evaluate it against F(x).
     *              </p>
     *              <p>
     *              We also check that the derivative of the primitive is identity for a Chebyshev
     *              polynomial.
     *              </p>
     * 
     * @testedMethod {@link PolynomialChebyshevFunction#primitive(double, double)}
     * @testedMethod {@link PolynomialChebyshevFunction#univariateDerivative()}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria
     *                   The Chebyshev polynomial primitive value for x is equal to F(x) and
     *                   the derivate of the Chebyshev polynomial primitive is the initial Chebyshev
     *                   polynomial.
     */
    @Test
    public void testPrimitive() {

        final double[] initialCoefficients = { 7., -5., 4.5 };
        // The reference polynomial f(x)
        final UnivariateFunction function = new PolynomialFunction(initialCoefficients);
        // The Chebyshev polynomial approximating the reference polynomial
        final PolynomialChebyshevFunction fctCheb = ChebyshevDecompositionEngine.interpolateChebyshevFunction(function,
                50, this.basicStart, this.basicEnd);

        // Reference primitive coefficients built with (0,8) point
        final double[] primitiveCoefficients = { 8., 7., -2.5, 1.5 };
        // Reference primitive polynomial F(x)
        final PolynomialFunction fPrimitiveRef = new PolynomialFunction(primitiveCoefficients);

        // Actual Chebyshev polynomial primitive with the (0,8) point
        final PolynomialChebyshevFunction polyPrimitive = fctCheb.primitive(0, 8);
        // Check that the degree and the range are correct
        Assert.assertEquals(this.basicStart, polyPrimitive.getStart(), 0.);
        Assert.assertEquals(this.basicEnd, polyPrimitive.getEnd(), 0.);
        Assert.assertEquals(fctCheb.getDegree() + 1, polyPrimitive.getDegree());

        // Check at several places (should be close to the F(x) function values)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fPrimitiveRef.value(i), polyPrimitive.value(i), this.tolerance);
        }

        // Check that the derivative of the primitive is identity (check at several places)
        for (double i = this.basicStart; i <= this.basicEnd; i += 0.1) {
            Assert.assertEquals(fctCheb.value(i), polyPrimitive.univariateDerivative().value(i), this.tolerance);
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
        Assert.assertEquals(
                new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { 0., 2., 3. }).toString(),
                "2 T1 + 3 T2");
        Assert.assertEquals(
                new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { 1., 2., 3. }).toString(),
                "1 + 2 T1 + 3 T2");
        Assert.assertEquals(
                new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { 1., 0., 3. }).toString(),
                "1 + 3 T2");
        Assert.assertEquals(
                new PolynomialChebyshevFunction(this.basicStart, this.basicEnd, new double[] { 0. }).toString(), "0");
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
        final PolynomialChebyshevFunction instance = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficients);

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
        PolynomialChebyshevFunction other = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficients);

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
     * @description Evaluate the linear function serialization / deserialization process.
     *
     * @testPassCriteria The linear function can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {
        final PolynomialChebyshevFunction p = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                new double[] { 3, 2, 1 });
        final PolynomialChebyshevFunction deserializedP = TestUtils.serializeAndRecover(p);

        Assert.assertEquals(p, deserializedP);
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
        final PolynomialChebyshevFunction f = new PolynomialChebyshevFunction(this.basicStart, this.basicEnd,
                coefficientsChebyshev);

        final ConvergenceChecker<PointVectorValuePair> checker = new ConvergenceChecker<PointVectorValuePair>() {
            @Override
            public boolean converged(final int iteration, final PointVectorValuePair previous,
                    final PointVectorValuePair current) {
                boolean res = true;
                for (int i = 0; i < coefficientsChebyshev.length; i++) {
                    res &= (previous.getSecond()[i] - current.getSecond()[i]) <= eps;
                }
                return res;
            }
        };
        final MultivariateVectorOptimizer optimizer = new GaussNewtonOptimizer(checker);
        final PolynomialChebyshevFitter fitter = new PolynomialChebyshevFitter(this.basicStart, this.basicEnd,
                optimizer);

        // Add observations
        for (int i = 0; i < 100; i++) {
            final double x = this.basicStart + (i / 100.) * (this.basicEnd - this.basicStart);
            fitter.addObservedPoint(x, f.value(x));
        }

        // Fit
        final double[] initialGuess = { 0, 0, 0 };
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
     * @description Evaluate the Chebyshev abscissas determination feature on a specified range with
     *              a specified degree.
     *              Also evaluate the exceptions cases.
     *
     * @testedMethod {@link PolynomialsChebyshevFunction#getChebyshevAbscissas(int)}
     *
     * @testPassCriteria The Chebyshev abscissas values are the ones expected (bases on external
     *                   contexts) and the
     *                   exceptions are returned as expected with the errors cases.
     */
    @Test
    public void testChebyshevAbscissas() {

        PolynomialChebyshevFunction fct = new PolynomialChebyshevFunction(-1., 1., new double[] { 1, 1, 1 });

        double[] abscissas = fct.getChebyshevAbscissas(1);
        Assert.assertEquals(1, abscissas.length);
        Assert.assertEquals(0., abscissas[0], 1e-14);

        // Context #1 extracted from an external example (10 points)
        abscissas = fct.getChebyshevAbscissas(10);

        Assert.assertEquals(10, abscissas.length);
        Assert.assertEquals(-0.987688341, abscissas[0], 1e-9);
        Assert.assertEquals(-0.891006524, abscissas[1], 1e-9);
        Assert.assertEquals(-0.707106781, abscissas[2], 1e-9);
        Assert.assertEquals(-0.453990499, abscissas[3], 1e-9);
        Assert.assertEquals(-0.156434465, abscissas[4], 1e-9);
        Assert.assertEquals(0.156434465, abscissas[5], 1e-9);
        Assert.assertEquals(0.453990499, abscissas[6], 1e-9);
        Assert.assertEquals(0.707106781, abscissas[7], 1e-9);
        Assert.assertEquals(0.891006524, abscissas[8], 1e-9);
        Assert.assertEquals(0.987688341, abscissas[9], 1e-9);

        // Context #2 extracted from an external example (5 points)
        fct = new PolynomialChebyshevFunction(-1., 3., new double[] { 1, 1, 1 });
        abscissas = fct.getChebyshevAbscissas(5);

        Assert.assertEquals(5, abscissas.length);
        Assert.assertEquals(-0.902113, abscissas[0], 1e-6);
        Assert.assertEquals(-0.175571, abscissas[1], 1e-6);
        Assert.assertEquals(1., abscissas[2], 1e-6);
        Assert.assertEquals(2.175571, abscissas[3], 1e-6);
        Assert.assertEquals(2.902113, abscissas[4], 1e-6);

        // Try to use call a 0 points Chebyshev polynomial (should fail)
        try {
            fct.getChebyshevAbscissas(0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            // expected
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
