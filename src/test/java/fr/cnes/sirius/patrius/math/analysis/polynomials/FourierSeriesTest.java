/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * @history Created 02/01/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * Test class for {@link FourierSeries}
 * </p>
 * 
 * @see FourierSeries
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: FourierSeriesTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.1
 * 
 */
public class FourierSeriesTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle {@link FourierSeries} Constructor
         * 
         * @featureDescription Test creation of a FourierSeries and getters
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        FOURIERSERIES_CONSTRUCTOR,

        /**
         * @featureTitle {@link FourierSeries} Scalar Operations
         * 
         * @featureDescription Add, subtract, multiply, divide FourierSeries by scalars. Test negate()
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        FOURIERSERIES_SCALAR_OPERATIONS,

        /**
         * @featureTitle {@link FourierSeries} Operations
         * 
         * @featureDescription Add, subtract, multiply, FourierSeries by {@link FourierSeries}
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        FOURIERSERIES_OPERATIONS

    }

    /**
     * Doubles comparison epsilon
     */
    static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_CONSTRUCTOR}
     * 
     * @testedMethod {@link FourierSeries#FourierSeries(double, double, double[], double[])}
     * 
     * @description Test FourierSeries Constructor. Here we
     *              check the correctness of the constructor. Nominal case as well as degraded cases are
     *              checked.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria No exception is raised for nominal cases, a MathIllegalArgumentException is raised for degraded
     *                   cases.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testConstructor() {
        final double a0 = 1;
        final double[] a = new double[] { 1, 2, 3, 4 };
        final double[] b = new double[] { 4, 3, 2, 1 };

        try {
            new FourierSeries(1, a0, a, b);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }

        try {
            final double[] incorrectA = new double[] { 1, 2, 3 };
            new FourierSeries(1, a0, incorrectA, b);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // Expected!!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_CONSTRUCTOR}
     * 
     * @testedMethod {@link FourierSeries#getCosArray()}
     * @testedMethod {@link FourierSeries#getSinArray()}
     * @testedMethod {@link FourierSeries#getConstant()}
     * @testedMethod {@link FourierSeries#getOrder()}
     * 
     * @description Test FourierSeries getters. Here we
     *              check that the coefficients are stored as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetters() {
        double a0 = 1;
        double[] a = new double[] { 1, 2, 3, 4, 1 };
        double[] b = new double[] { 4, 3, 2, 1, 0 };

        FourierSeries myPol = new FourierSeries(1, a0, a, b);

        Assert.assertArrayEquals(a, myPol.getCosArray(), EPS);
        Assert.assertArrayEquals(b, myPol.getSinArray(), EPS);
        Assert.assertEquals(a0, myPol.getConstant(), EPS);
        Assert.assertEquals(6, myPol.getOrder(), EPS);

        a0 = 1;
        a = new double[] { 1, 2, 3, 4, 0 };
        b = new double[] { 4, 3, 2, 1, 0 };

        myPol = new FourierSeries(1, a0, a, b);

        Assert.assertArrayEquals(new double[] { a[0], a[1], a[2], a[3] }, myPol.getCosArray(), EPS);
        Assert.assertArrayEquals(new double[] { b[0], b[1], b[2], b[3] }, myPol.getSinArray(), EPS);
        Assert.assertEquals(a0, myPol.getConstant(), EPS);
        Assert.assertEquals(5, myPol.getOrder(), EPS);

        a0 = 1;
        a = new double[] { 0, 0, 0, 0, 0 };
        b = new double[] { 0, 0, 0, 0, 0 };

        myPol = new FourierSeries(1, a0, a, b);

        Assert.assertArrayEquals(new double[] {}, myPol.getCosArray(), EPS);
        Assert.assertArrayEquals(new double[] {}, myPol.getSinArray(), EPS);
        Assert.assertEquals(a0, myPol.getConstant(), EPS);
        Assert.assertEquals(1, myPol.getOrder(), EPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#negate()}
     * 
     * @description Test FourierSeries negate() method. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testNegate() {
        double a0 = 1;
        double[] a = new double[] { 1, 2, 3, 4, 1 };
        double[] b = new double[] { 4, 3, 2, 1, 0 };

        final FourierSeries myPol = new FourierSeries(1, a0, a, b);

        a0 = -1;
        a = new double[] { -1, -2, -3, -4, -1 };
        b = new double[] { -4, -3, -2, -1, 0 };

        final FourierSeries myPolNeg = new FourierSeries(1, a0, a, b);

        areFourierSeriesEqual(myPol.negate(), myPolNeg);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#scalarAdd(double)}
     * 
     * @description Test FourierSeries scalar operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testScalarAdd() {
        // first polynomial
        final double a0 = 1.333;
        final double[] a = new double[] { 1, 0, 2, 8, -3 };
        final double[] b = new double[] { 0, 1, 4, -5.2, .1 };
        final FourierSeries myPol = new FourierSeries(3, a0, a, b);
        final FourierSeries myPolNew = new FourierSeries(3, a0 + 1.1001, a, b);
        areFourierSeriesEqual(myPolNew, myPol.scalarAdd(1.1001));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#scalarSubtract(double)}
     * 
     * @description Test FourierSeries scalar operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testScalarSubtract() {
        // first polynomial
        final double a0 = 1.333;
        final double[] a = new double[] { 1, 0, 2, 8, -3 };
        final double[] b = new double[] { 0, 1, 4, -5.2, .1 };
        final FourierSeries myPol = new FourierSeries(22.5, a0, a, b);
        final FourierSeries myPolNew = new FourierSeries(22.5, a0 - 2, a, b);
        areFourierSeriesEqual(myPolNew, myPol.scalarSubtract(2));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#scalarMultiply(double)}
     * 
     * @description Test FourierSeries scalar operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testScalarMultiply() {
        // polynomial
        final double a0 = 1.333;
        final double[] a = new double[] { 1, 0, 2, 8, -3 };
        final double[] b = new double[] { 0, 1, 4, -5.2, .1 };
        final FourierSeries myPol = new FourierSeries(3, a0, a, b);

        // multiplication by a scalar
        final FourierSeries newPol = myPol.scalarMultiply(2);

        // checks
        Assert.assertArrayEquals(new double[] { 2, 0, 4, 16, -6 }, newPol.getCosArray(), EPS);
        Assert.assertArrayEquals(new double[] { 0, 2, 8, -10.4, .2 }, newPol.getSinArray(), EPS);
        Assert.assertEquals(2.666, newPol.getConstant(), EPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#scalarDivide(double)}
     * 
     * @description Test FourierSeries scalar operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values. MathIllegalArgumentException
     *                   should be raised when attempting to divide polynomial by 0.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testScalarDivide() {
        // polynomial
        final double a0 = 1.333;
        final double[] a = new double[] { 1, 0, 2, 8, -3 };
        final double[] b = new double[] { 0, 1, 4, -5.2, .1 };
        final FourierSeries myPol = new FourierSeries(1, a0, a, b);

        // multiplication by a scalar
        final FourierSeries newPol = myPol.scalarDivide(2);

        // checks
        Assert.assertArrayEquals(new double[] { .5, 0, 1, 4, -1.5 }, newPol.getCosArray(), EPS);
        Assert.assertArrayEquals(new double[] { 0, .5, 2, -2.6, .05 }, newPol.getSinArray(), EPS);
        Assert.assertEquals(1.333 / 2, newPol.getConstant(), EPS);

        try {
            myPol.scalarDivide(0);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected!!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#value(double)}
     * 
     * @description Test FourierSeries operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testValue() {
        double a0 = 0;
        double[] a = new double[] { 1 };
        double[] b = new double[] { 0 };

        FourierSeries myPol = new FourierSeries(3, a0, a, b);
        final double[] x = new double[] { -10, -8, -7, -5, .25563, 1, 25., 633, 1, -1 };
        for (final double element : x) {
            Assert.assertEquals(MathLib.cos(3 * element), myPol.value(element), EPS);
        }

        myPol = new FourierSeries(1, a0, b, a);
        for (final double element : x) {
            Assert.assertEquals(MathLib.sin(element), myPol.value(element), EPS);
        }

        myPol = new FourierSeries(1, a0, a, a);
        for (final double element : x) {
            Assert.assertEquals(MathLib.sin(element) + MathLib.cos(element), myPol.value(element), EPS);
        }

        a = new double[] { 1, 0 };
        b = new double[] { 0, 1 };
        myPol = new FourierSeries(1, a0, a, b);
        for (final double element : x) {
            Assert.assertEquals(MathLib.cos(element) + MathLib.sin(2 * element), myPol.value(element), EPS);
        }

        a0 = 1.333;
        a = new double[] { 1, 0 };
        b = new double[] { 0, 2 };
        myPol = new FourierSeries(2, a0, a, b);
        for (final double element : x) {
            Assert
                .assertEquals(a0 + MathLib.cos(2 * element) + 2 * MathLib.sin(4 * element), myPol.value(element), EPS);
        }

        a0 = 1;
        a = new double[] { 0, 0, 0, 0, 0 };
        b = new double[] { 0, 0, 0, 0, 0 };
        myPol = new FourierSeries(1, a0, a, b);
        for (final double element : x) {
            Assert.assertEquals(a0, myPol.value(element), EPS);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#polynomialDerivative()}
     * @testedMethod {@link FourierSeries#polynomialDerivative(int)}
     * @testedMethod {@link FourierSeries#derivative()}
     * @testedMethod {@link FourierSeries#derivative(int)}
     * @testedMethod {@link FourierSeries#value(DerivativeStructure)}
     * @testedMethod {@link FourierSeries#derivativeValue(int, double)}
     * 
     * @description Test FourierSeries operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testDerivative() {
        // first order derivative - simple case
        double a0 = 1.333;
        double[] a = new double[] { 1, 0 };
        double[] b = new double[] { 0, 1 };
        FourierSeries myPol = new FourierSeries(2, a0, a, b);
        FourierSeries myPolDV = myPol.polynomialDerivative(1);
        FourierSeries myPolDVR = new FourierSeries(2, 0, new double[] { 0, 4 }, new double[] { -2, 0 });
        areFourierSeriesEqual(myPolDV, myPolDVR);

        try {
            myPol.polynomialDerivative(-1);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected!!
        }
        try {
            myPol.polynomialDerivative();
            myPol.derivative();
            myPol.derivative(1);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }

        // first order derivative - complex case
        a0 = 1.333;
        a = new double[] { 1, 0, -3, -5, 9 };
        b = new double[] { 0, 1, 2, -2, -7 };
        myPol = new FourierSeries(3, a0, a, b);
        myPolDV = myPol.polynomialDerivative(1);
        myPolDVR = new FourierSeries(3, 0, new double[] { 0, 2 * 3, 6 * 3, -8 * 3, -35 * 3 }, new double[] { -1 * 3, 0,
            9 * 3, 20 * 3, -45 * 3 });
        areFourierSeriesEqual(myPolDV, myPolDVR);
        areFourierSeriesEqual(myPol.polynomialDerivative(), myPolDVR);

        // second order derivative of complex case
        myPolDV = myPol.polynomialDerivative(2);
        myPolDVR = new FourierSeries(3, 0, new double[] { -1 * 9, 0, 27 * 9, 80 * 9, -225 * 9 }, new double[] { 0,
            -4 * 9, -18 * 9, 32 * 9, 175 * 9 });
        areFourierSeriesEqual(myPolDV, myPolDVR);

        // polynomial derivative value method
        Assert.assertEquals(myPolDVR.value(5), myPol.derivativeValue(2, 5), EPS);

        // New interface
        final DerivativeStructure inDerStr = new DerivativeStructure(1, 2, 0, 5);
        final DerivativeStructure outDerStr = myPol.value(inDerStr);
        Assert.assertEquals(myPolDVR.value(5), outDerStr.getPartialDerivative(2), EPS);
        try {
            final DerivativeStructure inDerStr2 = new DerivativeStructure(2, 2, 0, 5);
            final DerivativeStructure outDerStr2 = myPol.value(inDerStr2);
            Assert.fail(outDerStr2.toString());
        } catch (final MathRuntimeException e) {
            // expected!!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_OPERATIONS}
     * 
     * @testedMethod {@link FourierSeries#polynomialPrimitive()}
     * @testedMethod {@link FourierSeries#primitive()}
     * 
     * @description Test FourierSeries operations. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned values are equal to expected values.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testPrimitive() {
        // first order derivative - simple case
        final double a0 = 1.333;
        final double[] a = new double[] { 1, 0 };
        final double[] b = new double[] { 0, 1 };
        final FourierSeries myPol = new FourierSeries(2, a0, a, b);
        final FourierSeries myPolPr = myPol.polynomialPrimitive();
        final FourierSeries myPolPrEx = new FourierSeries(2, 0, new double[] { 0, -.25 }, new double[] { .5, 0 });

        areFourierSeriesEqual(myPolPrEx, myPolPr);

        final UnivariateFunction prim = myPol.primitive();

        final Random gen = new Random();
        for (double x = -20; x < 20; x += gen.nextDouble()) {
            Assert.assertEquals(myPolPrEx.value(x), prim.value(x), EPS);
            Assert.assertEquals(myPol.primitiveValue(x), prim.value(x), EPS);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#FOURIERSERIES_CONSTRUCTOR}
     * 
     * @testedMethod {@link FourierSeries#toString()}
     * 
     * @description Test FourierSeries operation toString
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link FourierSeries}
     * 
     * @testPassCriteria Returned string are equal to expected string.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testToString() {
        final double a0 = +2;
        final double[] a = new double[] { -1, 0 };
        final double[] b = new double[] { 0, 1.1 };
        final FourierSeries myPol = new FourierSeries(1, a0, a, b);

        final String expected = " + 2 - 1 cos( + 1 x ) + 1.1 sin( + 2 x )";
        final String actual = myPol.toString();

        Assert.assertEquals(expected, actual);
    }

    /**
     * 
     * Compare two {@link FourierSeries}
     * 
     * @param f1
     *        expected result
     * @param f2
     *        actual result
     * 
     */
    public static void areFourierSeriesEqual(final FourierSeries f1, final FourierSeries f2) {
        Assert.assertEquals(f1.getOrder(), f2.getOrder(), EPS);
        // same constant
        Assert.assertEquals(f1.getConstant(), f2.getConstant(), EPS);
        // same angular frequency
        Assert.assertEquals(f1.getAngularFrequency(), f2.getAngularFrequency(), EPS);
        // same coefficients
        Assert.assertArrayEquals(f1.getCosArray(), f2.getCosArray(), EPS);
        Assert.assertArrayEquals(f1.getSinArray(), f2.getSinArray(), EPS);
    }

}
