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
 * @history Created on 02/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathRuntimeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * Test class for {@link TrigonometricPolynomialPrimitive}
 * </p>
 * 
 * @see TrigonometricPolynomialPrimitive
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: TrigonometricPolynomialPrimitiveTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.1
 * 
 */
public class TrigonometricPolynomialPrimitiveTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle {@link TrigonometricPolynomialPrimitive} Constructor
         * 
         * @featureDescription Test creation of a TrigonometricPolynomialPrimitive and getters
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        EPOL_CONSTRUCTOR,

        /**
         * @featureTitle {@link TrigonometricPolynomialPrimitive} Scalar Operations
         * 
         * @featureDescription Add, subtract, multiply, divide TrigonometricPolynomialPrimitive by scalars. Test
         *                     negate()
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        EPOL_SCALAR_OPERATIONS,

        /**
         * @featureTitle {@link TrigonometricPolynomialPrimitive} Operations
         * 
         * @featureDescription Add, subtract TrigonometricPolynomialPrimitive by
         *                     {@link TrigonometricPolynomialPrimitive}
         * 
         * @coveredRequirements DV-CALCUL_30, DV-CALCUL_40
         */
        EPOL_OPERATIONS

    }

    /**
     * Doubles Comparison epsilon
     */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EPOL_CONSTRUCTOR}
     * 
     * @testedMethod {@link TrigonometricPolynomialPrimitive#TrigonometricPolynomialPrimitive(PolynomialFunction, TrigonometricPolynomialFunction)}
     * 
     * @description Test TrigonometricPolynomialPrimitive Constructor. Here we
     *              check the correctness of the constructor. Nominal case as well as degraded cases are
     *              checked.
     * 
     * @input cosine and sine coefficients
     * 
     * @output {@link TrigonometricPolynomialPrimitive}
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
        final double[] b = new double[] { -4, 3, 2, 1 };
        final TrigonometricPolynomialFunction trigoPol = new TrigonometricPolynomialFunction(a0, a, b);

        final PolynomialFunction linPol = new PolynomialFunction(new double[] { 1.1, -2, 3, 4 });

        try {
            new TrigonometricPolynomialPrimitive(linPol, trigoPol);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail();
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EPOL_SCALAR_OPERATIONS}
     * 
     * @testedMethod {@link TrigonometricPolynomialPrimitive#negate()}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#scalarAdd(double)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#scalarDivide(double)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#scalarMultiply(double)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#scalarDivide(double)}
     * 
     * @description Test TrigonometricPolynomialPrimitive scalar operations methods. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input {@link TrigonometricPolynomialFunction} and {@link PolynomialFunction}
     * 
     * @output {@link TrigonometricPolynomialPrimitive}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testScalarOperations() {

        final double a0 = 1;
        final double[] a = new double[] { 1, 2, 3, 4 };
        final double[] b = new double[] { -4, 3, 2, 1 };
        final double[] l = new double[] { 1.1, -2, 3, 4 };

        final TrigonometricPolynomialFunction trigoPol1 = new TrigonometricPolynomialFunction(a0, a, b);
        final PolynomialFunction linPol1 = new PolynomialFunction(l);

        final TrigonometricPolynomialPrimitive myPol1 = new TrigonometricPolynomialPrimitive(linPol1, trigoPol1);

        // Addition
        areExtendedPolynomialsEqual(myPol1.scalarAdd(2),
            new TrigonometricPolynomialPrimitive(linPol1, trigoPol1.scalarAdd(2)));
        areExtendedPolynomialsEqual(
            myPol1.scalarAdd(2),
            new TrigonometricPolynomialPrimitive(linPol1.add(new PolynomialFunction(new double[] { 2 })), trigoPol1));

        // Subtraction
        areExtendedPolynomialsEqual(myPol1.scalarSubtract(2),
            new TrigonometricPolynomialPrimitive(linPol1, trigoPol1.scalarSubtract(2)));
        areExtendedPolynomialsEqual(myPol1.scalarSubtract(2),
            new TrigonometricPolynomialPrimitive(linPol1.subtract(new PolynomialFunction(new double[] { 2 })),
                trigoPol1));

        // Multiplication
        areExtendedPolynomialsEqual(myPol1.scalarMultiply(2),
            new TrigonometricPolynomialPrimitive(linPol1.multiply(new PolynomialFunction(new double[] { 2 })),
                trigoPol1.scalarMultiply(2)));

        // Division
        areExtendedPolynomialsEqual(myPol1.scalarDivide(2),
            new TrigonometricPolynomialPrimitive(linPol1.multiply(new PolynomialFunction(new double[] { .5 })),
                trigoPol1.scalarDivide(2)));
        try {
            myPol1.scalarDivide(0);
            Assert.fail("Division by zero");
        } catch (final MathIllegalArgumentException e) {
            // expected !!
        }

        // negate
        areExtendedPolynomialsEqual(myPol1.negate(), myPol1.scalarMultiply(-1));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EPOL_OPERATIONS}
     * 
     * @testedMethod {@link TrigonometricPolynomialPrimitive#add(TrigonometricPolynomialPrimitive)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#add(PolynomialFunction)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#add(TrigonometricPolynomialFunction)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#subtract(TrigonometricPolynomialPrimitive)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#subtract(PolynomialFunction)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#subtract(TrigonometricPolynomialFunction)}
     * 
     * @description Test TrigonometricPolynomialPrimitive scalar operations methods. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input {@link TrigonometricPolynomialFunction} and {@link PolynomialFunction}
     * 
     * @output {@link TrigonometricPolynomialPrimitive}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testOperations() {
        double a0 = 1;
        double[] a = new double[] { 1, 2, 3, 4 };
        double[] b = new double[] { -4, 3, 2, 1 };
        double[] l = new double[] { 1.1, -2, 3, 4 };

        final TrigonometricPolynomialFunction trigoPol1 = new TrigonometricPolynomialFunction(a0, a, b);
        final PolynomialFunction linPol1 = new PolynomialFunction(l);

        final TrigonometricPolynomialPrimitive myPol1 = new TrigonometricPolynomialPrimitive(linPol1, trigoPol1);

        a0 = 1.3;
        a = new double[] { -6, 6, -12, 3, +3, 11 };
        b = new double[] { 11, .4, -.2, 5.8, -9.55, 3 };
        l = new double[] { -1.1, 2, .2, -4, 3, 0, 0, 1 };

        final TrigonometricPolynomialFunction trigoPol2 = new TrigonometricPolynomialFunction(a0, a, b);
        final PolynomialFunction linPol2 = new PolynomialFunction(l);

        final TrigonometricPolynomialPrimitive myPol2 = new TrigonometricPolynomialPrimitive(linPol2, trigoPol2);

        a0 = 2.3;
        a = new double[] { -5, 8, -9, 7, +3, 11 };
        b = new double[] { 7, 3.4, 1.8, 6.8, -9.55, 3 };
        l = new double[] { 0, 0, 3.2, 0, 3, 0, 0, 1 };

        final TrigonometricPolynomialFunction trigoPolRef = new TrigonometricPolynomialFunction(a0, a, b);
        final PolynomialFunction linPolRef = new PolynomialFunction(l);

        final TrigonometricPolynomialPrimitive myPolRef = new TrigonometricPolynomialPrimitive(linPolRef, trigoPolRef);

        // add and subtract TrigonometricPolynomialPrimitive
        areExtendedPolynomialsEqual(myPolRef, myPol1.add(myPol2));
        areExtendedPolynomialsEqual(myPol1, myPolRef.subtract(myPol2));

        // add and subtract TrigonometricPolynomialFunction
        areExtendedPolynomialsEqual(new TrigonometricPolynomialPrimitive(linPol1, trigoPol1.add(trigoPol2)),
            myPol1.add(trigoPol2));
        areExtendedPolynomialsEqual(new TrigonometricPolynomialPrimitive(linPol1, trigoPol1.subtract(trigoPol2)),
            myPol1.subtract(trigoPol2));

        // add and subtract PolynomialFunction
        areExtendedPolynomialsEqual(new TrigonometricPolynomialPrimitive(linPol1.add(linPol2), trigoPol1),
            myPol1.add(linPol2));
        areExtendedPolynomialsEqual(new TrigonometricPolynomialPrimitive(linPol1.subtract(linPol2), trigoPol1),
            myPol1.subtract(linPol2));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EPOL_CONSTRUCTOR}
     * 
     * @testedMethod {@link TrigonometricPolynomialPrimitive#getLinearPolynomial()}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#getTrigonometricPolynomial()}
     * 
     * @description Test TrigonometricPolynomialPrimitive getters.
     * 
     * @input {@link TrigonometricPolynomialFunction} and {@link PolynomialFunction}
     * 
     * @output {@link TrigonometricPolynomialPrimitive}
     * 
     * @testPassCriteria Results from getters arer as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testGetters() {
        final double a0 = 1;
        final double[] a = new double[] { 1, 2, 3, 4 };
        final double[] b = new double[] { -4, 3, 2, 1 };
        final double[] l = new double[] { 1.1, -2, 3, 4 };

        final TrigonometricPolynomialFunction trigoPol1 = new TrigonometricPolynomialFunction(a0, a, b);
        final PolynomialFunction linPol1 = new PolynomialFunction(l);

        final TrigonometricPolynomialPrimitive myPol1 = new TrigonometricPolynomialPrimitive(linPol1, trigoPol1);

        areLinearPolynomialsEqual(linPol1.add(new PolynomialFunction(new double[] { 1 })), myPol1.getLinearPolynomial());
        areTrigonometricPolynomialsEqual(trigoPol1.scalarSubtract(a0), myPol1.getTrigonometricPolynomial());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EPOL_OPERATIONS}
     * 
     * @testedMethod {@link TrigonometricPolynomialPrimitive#value(double)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#value(int, double)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#polynomialDerivative()}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#polynomialDerivative(int)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#value(DerivativeStructure)}
     * @testedMethod {@link TrigonometricPolynomialPrimitive#toString()}
     * 
     * @description Test TrigonometricPolynomialPrimitive value methods. Here we
     *              check that the returned polynomial is as expected.
     * 
     * @input {@link TrigonometricPolynomialFunction} and {@link PolynomialFunction}
     * 
     * @output {@link TrigonometricPolynomialPrimitive}
     * 
     * @testPassCriteria Returned values are equal to expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testValue() {

        final double a0 = -2;
        final double[] a = new double[] { 1, 0 };
        final double[] b = new double[] { 0, 2 };
        final TrigonometricPolynomialFunction trigoPol = new TrigonometricPolynomialFunction(a0, a, b);

        final PolynomialFunction linPol = new PolynomialFunction(new double[] { 1.1, -2, 3, 4 });

        final TrigonometricPolynomialPrimitive myPol = new TrigonometricPolynomialPrimitive(linPol, trigoPol);

        final double x = 4;

        final double expectedPolVal = -2 + MathLib.cos(x) + 2 * MathLib.sin(2 * x) + 1.1 - 2 * x + 3 * x * x + 4 * x
            * x * x;
        Assert.assertEquals(expectedPolVal, myPol.value(x), EPS);

        final double expectedPolDev = -Math.sin(x) + 4 * MathLib.cos(2 * x) - 2 + 6 * x + 12 * x * x;
        Assert.assertEquals(expectedPolDev, myPol.value(1, x), EPS);
        Assert.assertEquals(expectedPolDev, myPol.derivative().value(x), EPS);
        Assert.assertEquals(expectedPolDev, myPol.derivative(1).value(x), EPS);

        final double expectedPolDev2 = -Math.cos(x) - 8 * MathLib.sin(2 * x) + 6 + 24 * x;
        Assert.assertEquals(expectedPolDev2, myPol.derivative(2).value(x), EPS);

        // New interface
        final DerivativeStructure inDerStr = new DerivativeStructure(1, 2, 0, x);
        final DerivativeStructure outDerStr = myPol.value(inDerStr);
        Assert.assertEquals(expectedPolDev, outDerStr.getPartialDerivative(1), EPS);
        Assert.assertEquals(expectedPolDev2, outDerStr.getPartialDerivative(2), EPS);

        try {
            myPol.derivative(-1);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected!
        }
        try {
            myPol.polynomialDerivative(-1);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected!
        }
        try {
            final DerivativeStructure inDerStr2 = new DerivativeStructure(2, 2, 0, x);
            final DerivativeStructure outDerStr2 = myPol.value(inDerStr2);
            Assert.assertEquals(expectedPolDev, outDerStr2.getPartialDerivative(1), EPS);
            Assert.fail();
        } catch (final MathRuntimeException e) {
            // expected!
        }

        Assert.assertEquals("6 + 24 x - 1 cos(1 x) - 8 sin(2 x)", myPol.polynomialDerivative(2).toString());
    }

    /**
     * Check polynomials against each other
     * 
     * @precondition
     * 
     * @param pol1
     *        first polynomial
     * @param pol2
     *        second polynomial
     * 
     * @since 1.1
     */
    public static void areExtendedPolynomialsEqual(final TrigonometricPolynomialPrimitive pol1,
                                                   final TrigonometricPolynomialPrimitive pol2) {

        // equality of linear parts
        areLinearPolynomialsEqual(pol1.getLinearPolynomial(), pol2.getLinearPolynomial());
        // equality of trigonometric parts
        areTrigonometricPolynomialsEqual(pol1.getTrigonometricPolynomial(), pol2.getTrigonometricPolynomial());

    }

    /**
     * Check polynomials against each other
     * 
     * @precondition
     * 
     * @param pol1
     *        first polynomial
     * @param pol2
     *        second polynomial
     * 
     * @since 1.1
     */
    public static void areTrigonometricPolynomialsEqual(final TrigonometricPolynomialFunction pol1,
                                                        final TrigonometricPolynomialFunction pol2) {

        // same degree
        Assert.assertEquals(pol1.getDegree(), pol2.getDegree(), EPS);
        // same constant
        Assert.assertEquals(pol1.getA0(), pol2.getA0(), EPS);
        // same coefficients
        Assert.assertArrayEquals(pol1.getA(), pol2.getA(), EPS);
        Assert.assertArrayEquals(pol1.getB(), pol2.getB(), EPS);

    }

    /**
     * Check polynomials against each other
     * 
     * @precondition
     * 
     * @param pol1
     *        first polynomial
     * @param pol2
     *        second polynomial
     * 
     * @since 1.1
     */
    protected static void areLinearPolynomialsEqual(final PolynomialFunction pol1, final PolynomialFunction pol2) {

        // same degree
        Assert.assertEquals(pol1.degree(), pol2.degree());
        // same coefficients
        Assert.assertArrayEquals(pol1.getCoefficients(), pol2.getCoefficients(), EPS);

    }

}
