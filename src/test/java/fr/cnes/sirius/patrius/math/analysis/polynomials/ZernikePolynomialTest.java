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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Unit test class for the {@link ZernikePolynomial} class.
 *
 * @author bonitt
 */
public class ZernikePolynomialTest {

    /** Epsilon for double comparison. */
    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * @description Evaluate the Zernike monomials computation by cross validation.<br>
     *              The external reference values are computed by the Python library zernpy:0.0.11.
     * 
     * @testedMethod {@link ZernikePolynomial#computeZernikeMonomials(int, double, double)}
     * @testedMethod {@link ZernikePolynomial#azimuthalDegreeToArrayIndex(int, int)}
     * 
     * @testPassCriteria The computed values are the same as the references.
     */
    @Test
    public void testCrossValidation() {

        double[][] monomials;
        int arrayIndex;

        /*
         * Evaluate the following modes (from the Zernike pyramid):
         * #1: Zr0az0 _with rho = 0.1 & azimuth = 0.2 expected result: 1.0
         * #2: Zr1az-1 with rho = 0.2 & azimuth = 0.4 expected result: 0.15576733692346023
         * #3: Zr1az1 _with rho = 0.3 & azimuth = 0.6 expected result: 0.49520136894580696
         * #4: Zr2az-2 with rho = 0.4 & azimuth = 0.8 expected result: 0.3917512460491186
         * #5: Zr2az0 _with rho = 0.5 & azimuth = 1.0 expected result: -0.8660254037844386
         * #6: Zr2az2 _with rho = 0.6 & azimuth = 1.2 expected result: -0.6502458033399805
         * #7: Zr3az-3 with rho = 0.7 & azimuth = 1.4 expected result: -0.8455596746964078
         * #8: Zr3az-1 with rho = 0.8 & azimuth = 1.6 expected result: -0.18094214988946283
         * #9: Zr3az1 _with rho = 0.9 & azimuth = 1.8 expected result: -0.24869570759505413
         * #10:Zr3az3 _with rho = 1.0 & azimuth = 2.0 expected result: 2.71577168313722
         * _
         * + some extra random modes:
         * #11: Zr5az-5 with rho = 0.00 & azimuth = 5.72 expected result: 0.0
         * #12: Zr6az-4 with rho = 0.35 & azimuth = 4.28 expected result: 0.23646070084964874
         * #13: Zr5az1 _with rho = 0.46 & azimuth = 0.00 expected result: 1.4477553689776979
         * #14: Zr6az-6 with rho = 0.58 & azimuth = 0.27 expected result: 0.14226761571638272
         */

        // #1
        monomials = ZernikePolynomial.computeZernikeMonomials(0, 0.1, 0.2);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(0, 0);
        Assert.assertEquals(1.0, monomials[0][arrayIndex], this.eps);

        // #2
        monomials = ZernikePolynomial.computeZernikeMonomials(1, 0.2, 0.4);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(1, -1);
        Assert.assertEquals(0.15576733692346023, monomials[1][arrayIndex], this.eps);

        // #3
        monomials = ZernikePolynomial.computeZernikeMonomials(1, 0.3, 0.6);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(1, 1);
        Assert.assertEquals(0.49520136894580696, monomials[1][arrayIndex], this.eps);

        // #4
        monomials = ZernikePolynomial.computeZernikeMonomials(2, 0.4, 0.8);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(2, -2);
        Assert.assertEquals(0.3917512460491186, monomials[2][arrayIndex], this.eps);

        // #5
        monomials = ZernikePolynomial.computeZernikeMonomials(2, 0.5, 1.);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(2, 0);
        Assert.assertEquals(-0.8660254037844386, monomials[2][arrayIndex], this.eps);

        // #6
        monomials = ZernikePolynomial.computeZernikeMonomials(2, 0.6, 1.2);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(2, 2);
        Assert.assertEquals(-0.6502458033399805, monomials[2][arrayIndex], this.eps);

        // #7
        monomials = ZernikePolynomial.computeZernikeMonomials(3, 0.7, 1.4);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(3, -3);
        Assert.assertEquals(-0.8455596746964078, monomials[3][arrayIndex], this.eps);

        // #8
        monomials = ZernikePolynomial.computeZernikeMonomials(3, 0.8, 1.6);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(3, -1);
        Assert.assertEquals(-0.18094214988946283, monomials[3][arrayIndex], this.eps);

        // #9
        monomials = ZernikePolynomial.computeZernikeMonomials(3, 0.9, 1.8);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(3, 1);
        Assert.assertEquals(-0.24869570759505413, monomials[3][arrayIndex], this.eps);

        // #10
        monomials = ZernikePolynomial.computeZernikeMonomials(3, 1., 2.);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(3, 3);
        Assert.assertEquals(2.71577168313722, monomials[3][arrayIndex], this.eps);

        // #11
        monomials = ZernikePolynomial.computeZernikeMonomials(5, 0., 5.72);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(5, -5);
        Assert.assertEquals(0., monomials[5][arrayIndex], this.eps);

        // #12
        monomials = ZernikePolynomial.computeZernikeMonomials(6, 0.35, 4.28);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(6, -4);
        Assert.assertEquals(0.23646070084964874, monomials[6][arrayIndex], this.eps);

        // #13
        monomials = ZernikePolynomial.computeZernikeMonomials(5, 0.46, 0.);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(5, 1);
        Assert.assertEquals(1.4477553689776979, monomials[5][arrayIndex], this.eps);

        // #14
        monomials = ZernikePolynomial.computeZernikeMonomials(6, 0.58, 0.27);
        arrayIndex = ZernikePolynomial.azimuthalDegreeToArrayIndex(6, -6);
        Assert.assertEquals(0.14226761571638272, monomials[6][arrayIndex], this.eps);
    }

    /**
     * @testType UT
     * @description Evaluate the constructors and getters.
     * 
     * @testedMethod {@link ZernikePolynomial#ZernikePolynomial(int)}
     * @testedMethod {@link ZernikePolynomial#ZernikePolynomial(int, Parameter[][])}
     * @testedMethod {@link ZernikePolynomial#getParameters()}
     * @testedMethod {@link ZernikePolynomial#getCoefficient(int, int)}
     * @testedMethod {@link ZernikePolynomial#supportsParameter(Parameter)}
     * @testedMethod {@link ZernikePolynomial#computeValue(double, double)}
     * @testedMethod {@link ZernikePolynomial#computeDerivatives(double, double, Collection)}
     * @testedMethod {@link ZernikePolynomial#computeValueAndDerivatives(double, double, Collection)}
     * @testedMethod {@link ZernikePolynomial#arrayIndexToAzimuthalDegree(int, int)}
     * @testedMethod {@link ZernikePolynomial#azimuthalDegreeToArrayIndex(int, int)}
     */
    @Test
    public void testConstructor() {

        // --- Polynomial parameters evaluation ---
        ZernikePolynomial polynomial = new ZernikePolynomial(3);

        // Evaluate the getParameters method (check the parameters are built in the right order with 0 value)
        Collection<Parameter> params = polynomial.getParameters();
        final List<String> expectedParamNames = Arrays.asList("Zr0az0", "Zr1az-1", "Zr1az1",
            "Zr2az-2", "Zr2az0", "Zr2az2", "Zr3az-3", "Zr3az-1", "Zr3az1", "Zr3az3");

        Assert.assertEquals(expectedParamNames.size(), params.size());
        int i = 0;
        for (final Parameter param : params) {
            Assert.assertEquals(expectedParamNames.get(i), param.getName());
            Assert.assertEquals(0., param.getValue(), 0.);
            i++;
        }

        // Evaluate the getCoefficient method
        Assert.assertEquals("Zr0az0", polynomial.getCoefficient(0, 0).getName());
        Assert.assertEquals("Zr2az-2", polynomial.getCoefficient(2, -2).getName());
        Assert.assertEquals("Zr3az1", polynomial.getCoefficient(3, 1).getName());

        // Evaluate the supportsParameter method
        for (final Parameter param : params) {
            Assert.assertTrue(polynomial.supportsParameter(param));
        }
        Assert.assertFalse(polynomial.supportsParameter(new Parameter("", 1.)));

        // --- Polynomial values and derivatives evaluation ---
        polynomial = new ZernikePolynomial(2);
        params = polynomial.getParameters();
        params.add(new Parameter("", 1.));

        final double rho = 0.1;
        final double azimuth = 0.2;
        double[][] monomials;

        // Evaluate the computeValue method
        double value = polynomial.computeValue(rho, azimuth);
        Assert.assertEquals(0., value, 0.); // The standard polynomial has 0 values parameters, so should return 0 value

        final Parameter[][] coefficients = new Parameter[][] { { new Parameter("Zr0az0", 0.5) },
            { new Parameter("Zr1az-1", 0.2), new Parameter("Zr1az1", 0.8) } };
        final ZernikePolynomial polynomialBis = new ZernikePolynomial(1, coefficients);
        value = polynomialBis.computeValue(rho, azimuth);

        // Compute the monomials (computeZernikeMonomials method already validated in the testCrossValidation)
        monomials = ZernikePolynomial.computeZernikeMonomials(1, rho, azimuth);
        // Then, compute the expected value according to the parameters values and the monomials
        final double expectedValue = coefficients[0][0].getValue() * monomials[0][0]
                + coefficients[1][0].getValue() * monomials[1][0] + coefficients[1][1].getValue() * monomials[1][1];

        Assert.assertEquals(expectedValue, value, 0.);

        // Evaluate the computeDerivatives method
        final double[] derivatives = polynomial.computeDerivatives(rho, azimuth, params);

        // Compute the monomials (computeZernikeMonomials method already validated in the testCrossValidation)
        monomials = ZernikePolynomial.computeZernikeMonomials(2, rho, azimuth);
        // Build the expected derivatives array from the monomials (last should be 0 for the non-supported param)
        final double[] expectedDerivatives = new double[7];
        expectedDerivatives[0] = monomials[0][0];
        expectedDerivatives[1] = monomials[1][0];
        expectedDerivatives[2] = monomials[1][1];
        expectedDerivatives[3] = monomials[2][0];
        expectedDerivatives[4] = monomials[2][1];
        expectedDerivatives[5] = monomials[2][2];
        expectedDerivatives[6] = 0.; // Non-supported parameter

        Assert.assertTrue(Arrays.equals(derivatives, expectedDerivatives));

        // Evaluate the computeValueAndDerivatives method
        final Pair<Double, double[]> valueAndDerivatives = polynomial.computeValueAndDerivatives(rho, azimuth, params);

        Assert.assertEquals(0., valueAndDerivatives.getFirst(), 0.);
        Assert.assertTrue(Arrays.equals(valueAndDerivatives.getSecond(), expectedDerivatives));

        // --- Static features evaluation ---

        // Evaluate the arrayIndexToAzimuthalDegree method standard behavior
        Assert.assertEquals(0, ZernikePolynomial.arrayIndexToAzimuthalDegree(0, 0));
        Assert.assertEquals(2, ZernikePolynomial.arrayIndexToAzimuthalDegree(0, 1));
        Assert.assertEquals(1, ZernikePolynomial.arrayIndexToAzimuthalDegree(1, 1));
        Assert.assertEquals(4, ZernikePolynomial.arrayIndexToAzimuthalDegree(2, 3));

        // Evaluate the azimuthalDegreeToArrayIndex method standard behavior
        Assert.assertEquals(0, ZernikePolynomial.azimuthalDegreeToArrayIndex(0, 0));
        Assert.assertEquals(1, ZernikePolynomial.azimuthalDegreeToArrayIndex(2, 0));
        Assert.assertEquals(0, ZernikePolynomial.azimuthalDegreeToArrayIndex(1, -1));
        Assert.assertEquals(1, ZernikePolynomial.azimuthalDegreeToArrayIndex(2, 0));
        Assert.assertEquals(0, ZernikePolynomial.azimuthalDegreeToArrayIndex(2, -2));
        Assert.assertEquals(1, ZernikePolynomial.azimuthalDegreeToArrayIndex(4, -2));
        Assert.assertEquals(3, ZernikePolynomial.azimuthalDegreeToArrayIndex(4, 2));
    }

    /**
     * @description This test evaluates all the exceptions cases (negative values, out of bound values, incompatible
     *              values, null elements, etc).<br>
     *              Each test has its own description.
     *
     * @testPassCriteria The expected exceptions are thrown.
     */
    @Test
    public void testExceptions() {

        // Try the constructors with a strictly negative radial degree (should fail)
        try {
            new ZernikePolynomial(-1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new ZernikePolynomial(-1, new Parameter[1][1]);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the constructor with a coefficients array not compatible with the radial degree
        // (coefficients.length != radialDegree + 1) (should fail)
        try {
            new ZernikePolynomial(1, new Parameter[1][1]);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the constructor with a coefficients array not compatible with the radial degree
        // (coefficientsN.length != n + 1) (should fail)
        final Parameter param = new Parameter("", 1.);
        try {
            // Expect 1-2-3 elements by row, by define 1-3-3 elements by row
            new ZernikePolynomial(2, new Parameter[][] { { param }, { param, param, param }, { param, param, param } });
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the constructor with a coefficients array describing a null element (should fail)
        try {
            new ZernikePolynomial(1, new Parameter[][] { { param }, { param, null } });
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try to use a rho value outside [0, 1] (should fail)
        final ZernikePolynomial polynomial = new ZernikePolynomial(2);
        try {
            polynomial.computeValueAndDerivatives(-1e-9, 0.9, new ArrayList<Parameter>());
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            polynomial.computeValueAndDerivatives(1. + 1e-9, 0.9, new ArrayList<Parameter>());
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }

        try {
            polynomial.computeValue(-1e-9, 0.9);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            polynomial.computeDerivatives(-1e-9, 0.9, new ArrayList<Parameter>());
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the arrayIndexToAzimuthalDegree method with a strictly negative radial degree or array index (should
        // fail)
        try {
            ZernikePolynomial.arrayIndexToAzimuthalDegree(-1, 1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            ZernikePolynomial.arrayIndexToAzimuthalDegree(1, -1);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the arrayIndexToAzimuthalDegree method with an array index not compatible with the radial degree
        // (arrayIndex > radialDegree + 1) (should fail)
        try {
            ZernikePolynomial.arrayIndexToAzimuthalDegree(1, 3);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the azimuthalDegreeToArrayIndex method with a strictly negative radial degree (should fail)
        try {
            ZernikePolynomial.azimuthalDegreeToArrayIndex(-1, 3);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the azimuthalDegreeToArrayIndex method with an azimuthal degree greater in absolute value than the radial
        // degree (should fail)
        try {
            ZernikePolynomial.azimuthalDegreeToArrayIndex(1, -2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            ZernikePolynomial.azimuthalDegreeToArrayIndex(2, 4);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the azimuthalDegreeToArrayIndex method with an odd difference of the two degrees (should fail)
        try {
            ZernikePolynomial.azimuthalDegreeToArrayIndex(4, 1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the computeZernikeMonomials method with a strictly negative radial degree (should fail)
        try {
            ZernikePolynomial.computeZernikeMonomials(-1, 0.5, 0.9);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the computeZernikeMonomials method with a rho value outside [0, 1] (should fail)
        try {
            ZernikePolynomial.computeZernikeMonomials(1, -1e-9, 0.9);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the computeRadialZernikeMonomials method with a strictly negative radial degree (should fail)
        try {
            ZernikePolynomial.computeRadialZernikeMonomials(-1, 0.5);
            Assert.fail();
        } catch (final NotPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try the computeRadialZernikeMonomials method with a rho value outside [0, 1] (should fail)
        try {
            ZernikePolynomial.computeRadialZernikeMonomials(1, -1e-9);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
