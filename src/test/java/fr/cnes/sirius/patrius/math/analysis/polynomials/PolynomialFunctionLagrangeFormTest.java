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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:288:18/09/2014: ephemeris interpolation with variable steps
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test case for Lagrange form of polynomial function.
 * <p>
 * We use n+1 points to interpolate a polynomial of degree n. This should give us the exact same polynomial as result.
 * Thus we can use a very small tolerance to account only for round-off errors.
 * 
 * @version $Id: PolynomialFunctionLagrangeFormTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class PolynomialFunctionLagrangeFormTest {

    /** Numerical precision. */
    private static final double EPSILON = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * Test of polynomial for the linear function.
     */
    @Test
    public void testLinearFunction() {
        PolynomialFunctionLagrangeForm p;
        double c[], z, expected, result;
        final double tolerance = 1E-12;

        // p(x) = 1.5x - 4
        final double x[] = { 0.0, 3.0 };
        final double y[] = { -4.0, 0.5 };
        p = new PolynomialFunctionLagrangeForm(x, y);

        final double[] coeff = p.getCoefficients();
        Assert.assertEquals(2, coeff.length);

        z = 2.0;
        expected = -1.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = 4.5;
        expected = 2.75;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = 6.0;
        expected = 5.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        Assert.assertEquals(1, p.degree());

        c = p.getCoefficients();
        Assert.assertEquals(2, c.length);
        Assert.assertEquals(-4.0, c[0], tolerance);
        Assert.assertEquals(1.5, c[1], tolerance);

        final double[] ip1 = p.getInterpolatingPoints();
        Assert.assertEquals(2, ip1.length);
        Assert.assertEquals(0., ip1[0], tolerance);
        Assert.assertEquals(3., ip1[1], tolerance);

        final double[] iv1 = p.getInterpolatingValues();
        Assert.assertEquals(2, iv1.length);
        Assert.assertEquals(-4., iv1[0], tolerance);
        Assert.assertEquals(.5, iv1[1], tolerance);

        final double v1 = PolynomialFunctionLagrangeForm.evaluate(ip1, iv1, 5.);
        Assert.assertEquals(3.5, v1, tolerance);

        final double[] x2 = new double[] { 0., 3., 1. };
        final double[] y2 = new double[] { -4., .5, -2.5 };
        final double v2 = PolynomialFunctionLagrangeForm.evaluate(x2, y2, 5.);
        Assert.assertEquals(3.5, v2, tolerance);

    }

    /**
     * Test of polynomial for the quadratic function.
     */
    @Test
    public void testQuadraticFunction() {
        PolynomialFunctionLagrangeForm p;
        double c[], z, expected, result;
        final double tolerance = 1E-12;

        // p(x) = 2x^2 + 5x - 3 = (2x - 1)(x + 3)
        final double x[] = { 0.0, -1.0, 0.5 };
        final double y[] = { -3.0, -6.0, 0.0 };
        p = new PolynomialFunctionLagrangeForm(x, y);

        z = 1.0;
        expected = 4.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = 2.5;
        expected = 22.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = -2.0;
        expected = -5.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        Assert.assertEquals(2, p.degree());

        c = p.getCoefficients();
        Assert.assertEquals(3, c.length);
        Assert.assertEquals(-3.0, c[0], tolerance);
        Assert.assertEquals(5.0, c[1], tolerance);
        Assert.assertEquals(2.0, c[2], tolerance);
    }

    /**
     * Test of polynomial for the quintic function.
     */
    @Test
    public void testQuinticFunction() {
        PolynomialFunctionLagrangeForm p;
        double c[], z, expected, result;
        final double tolerance = 1E-12;

        // p(x) = x^5 - x^4 - 7x^3 + x^2 + 6x = x(x^2 - 1)(x + 2)(x - 3)
        final double x[] = { 1.0, -1.0, 2.0, 3.0, -3.0, 0.5 };
        final double y[] = { 0.0, 0.0, -24.0, 0.0, -144.0, 2.34375 };
        p = new PolynomialFunctionLagrangeForm(x, y);

        z = 0.0;
        expected = 0.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = -2.0;
        expected = 0.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        z = 4.0;
        expected = 360.0;
        result = p.value(z);
        Assert.assertEquals(expected, result, tolerance);

        Assert.assertEquals(5, p.degree());

        c = p.getCoefficients();
        Assert.assertEquals(6, c.length);
        Assert.assertEquals(0.0, c[0], tolerance);
        Assert.assertEquals(6.0, c[1], tolerance);
        Assert.assertEquals(1.0, c[2], tolerance);
        Assert.assertEquals(-7.0, c[3], tolerance);
        Assert.assertEquals(-1.0, c[4], tolerance);
        Assert.assertEquals(1.0, c[5], tolerance);
    }

    /**
     * Test of parameters for the polynomial.
     */
    @Test
    public void testParameters() {

        try {
            // bad input array length
            final double x[] = { 1.0 };
            final double y[] = { 2.0 };
            new PolynomialFunctionLagrangeForm(x, y);
            Assert.fail("Expecting MathIllegalArgumentException - bad input array length");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
        try {
            // mismatch input arrays
            final double x[] = { 1.0, 2.0, 3.0, 4.0 };
            final double y[] = { 0.0, -4.0, -24.0 };
            new PolynomialFunctionLagrangeForm(x, y);
            Assert.fail("Expecting MathIllegalArgumentException - mismatch input arrays");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    /**
     * Tests the second constructor, with the tab array.
     */
    @Test(expected = NonMonotonicSequenceException.class)
    public void testConstructor() {

        // nominal case, very easy
        final double[] x = { 0, 1, 2, 3, 4, 5, 6 };
        final double[][] ytab = new double[2][7];
        final double[] y0 = { 7, 8, 9, 10, 11, 12, 13 };
        ytab[0] = y0;
        ytab[1] = x;
        final PolynomialFunctionLagrangeForm pflf = new PolynomialFunctionLagrangeForm(x, ytab);

        double[] x2 = pflf.getInterpolatingPoints();
        double[][] ytab2 = pflf.getInterpolatingTabValues();
        for (int i = 0; i < x.length; i++) {
            Assert.assertEquals(x[i], x2[i], EPSILON);
        }
        Assert.assertArrayEquals(ytab, ytab2);

        // case with non sorted x
        final double[] xbis = { 6, 5, 4, 2, 3, 0, 1 };
        final double[] y0bis = { 13, 12, 11, 9, 10, 7, 8 };
        final double[][] ytabbis = new double[2][7];
        ytabbis[0] = y0bis;
        ytabbis[1] = xbis;
        final PolynomialFunctionLagrangeForm pflf2 = new PolynomialFunctionLagrangeForm(xbis, ytabbis);

        x2 = pflf2.getInterpolatingPoints();
        ytab2 = pflf2.getInterpolatingTabValues();
        for (int i = 0; i < x.length; i++) {
            Assert.assertEquals(x[i], x2[i], EPSILON);
        }
        Assert.assertArrayEquals(ytab, ytab2);

        // case with duplicated values x
        final double[] xter = { 0, 1, 1, 2, 3, 4, 5 };
        final double[] y0ter = { 7, 8, 9, 10, 11, 12, 13 };
        final double[][] ytabter = new double[2][7];
        ytabter[0] = y0ter;
        ytabter[1] = xter;
        new PolynomialFunctionLagrangeForm(xter, ytabter);

    }

    /**
     * Tests the second value fonction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueIndex() {

        // nominal case, very easy
        final double[] x = { 0, 1, 2, 3, 4, 5, 6 };
        final double[][] ytab = new double[2][7];
        final double[] y0 = { 7, 8, 9, 10, 11, 12, 13 };
        ytab[0] = y0;
        ytab[1] = x;
        final PolynomialFunctionLagrangeForm pflf = new PolynomialFunctionLagrangeForm(x, ytab);

        final double z = 1.47;

        // interpolation for ytab[0]
        int index = 0;
        double z0 = pflf.valueIndex(index, z);
        Assert.assertEquals(z0, 8.47, EPSILON);
        index = 1;
        z0 = pflf.valueIndex(index, z);
        Assert.assertEquals(z0, z0, EPSILON);

        // expected behavior : throws exception
        index = 2;
        z0 = pflf.valueIndex(index, z);

    }
}
