/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;

/**
 * Unit test class for the {@link ChebyshevDecompositionEngine} class.
 *
 * @author bonitt
* HISTORY
* VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation de fonctions 
* END-HISTORY
 */
public class ChebyshevDecompositionEngineTest {

    /** Cosinus function. */
    private UnivariateFunction function1;

    /** Sinus function. */
    private UnivariateFunction function2;

    /** Composed function. */
    private UnivariateFunction function3;

    /** Polynomial function. */
    private UnivariateFunction function4;

    /** Doubles comparison. */
    private static final double MAXEPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /**
     * @description Evaluate the Chebyshev polynomial approximation against others functions.
     *              <p>
     *              Note: the method
     *              {@link ChebyshevDecompositionEngine#approximateChebyshevFunction(double, double, double[])}
     *              is validated at the same time as it is called by
     *              {@link ChebyshevDecompositionEngine#approximateChebyshevFunction(UnivariateFunction, int, double, double)}
     *              </p>
     *
     * @testedMethod {@link ChebyshevDecompositionEngine#approximateChebyshevFunction(UnivariateFunction, int, double, double)}
     * @testedMethod {@link ChebyshevDecompositionEngine#approximateChebyshevFunction(double, double, double[])}
     * @testedMethod {@link PolynomialChebyshevFunction#value(double)}
     *
     * @testPassCriteria The approximated Chebyshev polynomials should be equal (same value) to the
     *                   origin functions.
     */
    @Test
    public void testApproximateChebyshevFunction() {
        final double start = -0.5;
        final double end = 1.2;
        final int maxDegree = 30;

        PolynomialChebyshevFunction fctCheb = ChebyshevDecompositionEngine
                .approximateChebyshevFunction(this.function1, maxDegree, start, end);
        checkApproximation(fctCheb, this.function1, MAXEPS);

        fctCheb = ChebyshevDecompositionEngine.approximateChebyshevFunction(this.function2,
                maxDegree, start, end);
        checkApproximation(fctCheb, this.function2, MAXEPS);

        fctCheb = ChebyshevDecompositionEngine.approximateChebyshevFunction(this.function3,
                maxDegree, start, end);
        checkApproximation(fctCheb, this.function3, MAXEPS);

        fctCheb = ChebyshevDecompositionEngine.approximateChebyshevFunction(this.function4,
                maxDegree, start, end);
        checkApproximation(fctCheb, this.function4, MAXEPS);
    }

/**
     * @description Check the Chebyshev polynomial approximation exception cases.
     *
     * @testedMethod {@link ChebyshevDecompositionEngine#approximateChebyshevFunction(UnivariateFunction, int, double, double)}
     * @testedMethod {@link ChebyshevDecompositionEngine#approximateChebyshevFunc
     *
     * @testPassCriteria The polynomials approximation returns the expected exceptions.
     */
    @Test
    public void testApproximateChebyshevFunctionException() {
        // Try to give a null input (should fail)
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(null, -1, -0.5, 1.2);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to use a negative degree (should fail)
        try {
            ChebyshevDecompositionEngine
                    .approximateChebyshevFunction(this.function1, -1, -0.5, 1.2);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to use an invalid range (should fail)
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(this.function1, 30, -0.5,
                    -0.5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(this.function1, 30, -0.5,
                    -0.6);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to give a null input (should fail)
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(-0.5, 1.2, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to give an empty value array (should fail)
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(-0.5, 1.2, new double[0]);
            Assert.fail();
        } catch (final NoDataException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to use an invalid range (should fail)
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(-0.5, -0.5, new double[3]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            ChebyshevDecompositionEngine.approximateChebyshevFunction(-0.5, -0.6, new double[3]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Check if the approximated Chebyshev polynomial and the reference function are equal (same
     * value on all the
     * Chebyshev range - 100 points evaluated).
     * 
     * @param approx
     *        Approximated Chebyshev polynomial
     * @param fctRef
     *        Reference function
     * @param threshold
     *        Validity threshold
     */
    private static void checkApproximation(final PolynomialChebyshevFunction approx,
            final UnivariateFunction fctRef, final double threshold) {
        final int numPoints = 100;
        final double start = approx.getStart();
        final double end = approx.getEnd();
        final double step = (end - start) / numPoints;

        for (double i = start; i <= end; i += step) {
            Assert.assertEquals(fctRef.value(i), approx.value(i), threshold);
        }
    }

    /**
     * Initialize the global functions.
     */
    @Before
    public void setUp() {

        // cos(x) function
        this.function1 = new UnivariateFunction() {

            /** Serialization identifier. */
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return MathLib.cos(x);
            }
        };

        // sin(2x) function
        this.function2 = new UnivariateFunction() {

            /** Serialization identifier. */
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return MathLib.sin(2. * x);
            }
        };

        // Composed function
        this.function3 = new UnivariateFunction() {

            /** Serialization identifier. */
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return 1. + MathLib.cos(4. * x) + MathLib.sin(3. * x);
            }
        };

        // Polynomial function
        this.function4 = new UnivariateFunction() {

            /** Serialization identifier. */
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return 2. + 3 * x - 4 * x * x * x;
            }
        };
    }
}