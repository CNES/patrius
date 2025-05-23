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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * Tests the PolynomialFunction implementation of a UnivariateFunction.
 *
 * @version $Id: PolynomialFunctionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class PolynomialFunctionTest {
    /** Error tolerance for tests */
    protected double tolerance = 1e-12;

    /**
     * tests the value of a constant polynomial.
     *
     * <p>
     * value of this is 2.5 everywhere.
     * </p>
     */
    @Test
    public void testConstants() {
        final double[] c = { 2.5 };
        final PolynomialFunction f = new PolynomialFunction(c);

        // verify that we are equal to c[0] at several (nonsymmetric) places
        Assert.assertEquals(f.value(0), c[0], this.tolerance);
        Assert.assertEquals(f.value(-1), c[0], this.tolerance);
        Assert.assertEquals(f.value(-123.5), c[0], this.tolerance);
        Assert.assertEquals(f.value(3), c[0], this.tolerance);
        Assert.assertEquals(f.value(456.89), c[0], this.tolerance);

        Assert.assertEquals(f.getDegree(), 0);
        Assert.assertEquals(f.univariateDerivative().value(0), 0, this.tolerance);

        Assert.assertEquals(f.derivative().univariateDerivative().value(0), 0, this.tolerance);
        Assert.assertEquals(PolynomialType.CLASSICAL, f.getPolynomialType());
    }

    /**
     * tests the value of a linear polynomial.
     *
     * <p>
     * This will test the function f(x) = 3*x - 1.5
     * </p>
     * <p>
     * This will have the values <tt>f(0) = -1.5, f(-1) = -4.5, f(-2.5) = -9,
     *      f(0.5) = 0, f(1.5) = 3</tt> and <tt>f(3) = 7.5</tt>
     * </p>
     */
    @Test
    public void testLinear() {
        final double[] c = { -1.5, 3 };
        final PolynomialFunction f = new PolynomialFunction(c);

        // verify that we are equal to c[0] when x=0
        Assert.assertEquals(f.value(0), c[0], this.tolerance);

        // now check a few other places
        Assert.assertEquals(-4.5, f.value(-1), this.tolerance);
        Assert.assertEquals(-9, f.value(-2.5), this.tolerance);
        Assert.assertEquals(0, f.value(0.5), this.tolerance);
        Assert.assertEquals(3, f.value(1.5), this.tolerance);
        Assert.assertEquals(7.5, f.value(3), this.tolerance);

        Assert.assertEquals(f.getDegree(), 1);

        Assert.assertEquals(f.derivative().univariateDerivative().value(0), 0, this.tolerance);
    }

    /**
     * Tests a second order polynomial.
     * <p>
     * This will test the function f(x) = 2x^2 - 3x -2 = (2x+1)(x-2)
     * </p>
     */
    @Test
    public void testQuadratic() {
        final double[] c = { -2, -3, 2 };
        final PolynomialFunction f = new PolynomialFunction(c);

        // verify that we are equal to c[0] when x=0
        Assert.assertEquals(f.value(0), c[0], this.tolerance);

        // now check a few other places
        Assert.assertEquals(0, f.value(-0.5), this.tolerance);
        Assert.assertEquals(0, f.value(2), this.tolerance);
        Assert.assertEquals(-2, f.value(1.5), this.tolerance);
        Assert.assertEquals(7, f.value(-1.5), this.tolerance);
        Assert.assertEquals(265.5312, f.value(12.34), this.tolerance);
    }

    /**
     * This will test the quintic function
     * f(x) = x^2(x-5)(x+3)(x-1) = x^5 - 3x^4 -13x^3 + 15x^2</p>
     */
    @Test
    public void testQuintic() {
        final double[] c = { 0, 0, 15, -13, -3, 1 };
        final PolynomialFunction f = new PolynomialFunction(c);

        // verify that we are equal to c[0] when x=0
        Assert.assertEquals(f.value(0), c[0], this.tolerance);

        // now check a few other places
        Assert.assertEquals(0, f.value(5), this.tolerance);
        Assert.assertEquals(0, f.value(1), this.tolerance);
        Assert.assertEquals(0, f.value(-3), this.tolerance);
        Assert.assertEquals(54.84375, f.value(-1.5), this.tolerance);
        Assert.assertEquals(-8.06637, f.value(1.3), this.tolerance);

        Assert.assertEquals(f.getDegree(), 5);
    }

    /**
     * tests the firstDerivative function by comparison
     *
     * <p>
     * This will test the functions <tt>f(x) = x^3 - 2x^2 + 6x + 3, g(x) = 3x^2 - 4x + 6</tt> and
     * <tt>h(x) = 6x - 4</tt>
     */
    @Test
    public void testfirstDerivativeComparison() {
        final double[] f_coeff = { 3, 6, -2, 1 };
        final double[] g_coeff = { 6, -4, 3 };
        final double[] h_coeff = { -4, 6 };

        final PolynomialFunction f = new PolynomialFunction(f_coeff);
        final PolynomialFunction g = new PolynomialFunction(g_coeff);
        final PolynomialFunction h = new PolynomialFunction(h_coeff);

        // compare f' = g
        Assert.assertEquals(f.univariateDerivative().value(0), g.value(0), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(1), g.value(1), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(100), g.value(100), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(4.1), g.value(4.1), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(-3.25), g.value(-3.25), this.tolerance);

        // compare g' = h
        Assert.assertEquals(g.univariateDerivative().value(FastMath.PI), h.value(FastMath.PI), this.tolerance);
        Assert.assertEquals(g.univariateDerivative().value(FastMath.E), h.value(FastMath.E), this.tolerance);
    }

    /**
     * tests the primitive method by comparison
     *
     * <p>
     * This will test the functions <tt>f(x) = x^3 - 2x^2 + 6x + 3, g(x) = 3x^2 - 4x + 6</tt> and
     * <tt>h(x) = 6x - 4</tt>
     */
    @Test
    public void testPrimitive() {
        final double[] f_coeff = { 3, 6, -2, 1 };
        final double[] g_coeff = { 6, -4, 3 };
        final double[] h_coeff = { -4, 6 };

        final PolynomialFunction f = new PolynomialFunction(f_coeff);
        final PolynomialFunction g = new PolynomialFunction(g_coeff);
        final PolynomialFunction h = new PolynomialFunction(h_coeff);

        // check that the primitive of h is g with the appropriate reference (x0,y0) points
        Assert.assertEquals(g.value(0), h.primitive(0, 6).value(0), this.tolerance);
        Assert.assertEquals(g.value(1), h.primitive(0, 6).value(1), this.tolerance);
        Assert.assertEquals(g.value(100), h.primitive(0, 6).value(100), this.tolerance);
        Assert.assertEquals(g.value(4.1), h.primitive(0, 6).value(4.1), this.tolerance);
        Assert.assertEquals(g.value(-3.25), h.primitive(0, 6).value(-3.25), this.tolerance);

        // check that the primitive of g is f with the appropriate reference (x0,y0) points
        Assert.assertEquals(f.value(FastMath.PI), g.primitive(2, 15).value(FastMath.PI), this.tolerance);
        Assert.assertEquals(f.value(FastMath.E), g.primitive(2, 15).value(FastMath.E), this.tolerance);

        // check that the derivative of the primitive is identity for all polynomials
        Assert.assertEquals(f.value(194.2), f.primitive(1.255, -7.218).univariateDerivative().value(194.2), this.tolerance);
        Assert.assertEquals(g.value(FastMath.PI), g.primitive(-1.2785, -778).univariateDerivative().value(FastMath.PI),
                this.tolerance);
        Assert.assertEquals(h.value(-78.6), h.primitive(1849856, 6198561).univariateDerivative().value(-78.6), this.tolerance);
    }

    @Test
    public void testString() {
        final PolynomialFunction p = new PolynomialFunction(new double[] { -5, 3, 1 });
        checkPolynomial(p, "-5 + 3 x + x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 0, -2, 3 }), "-2 x + 3 x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 1, -2, 3 }), "1 - 2 x + 3 x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 0, 2, 3 }), "2 x + 3 x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 1, 2, 3 }), "1 + 2 x + 3 x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 1, 0, 3 }), "1 + 3 x^2");
        checkPolynomial(new PolynomialFunction(new double[] { 0 }), "0");
    }

    @Test
    public void testAddition() {
        PolynomialFunction p1 = new PolynomialFunction(new double[] { -2, 1 });
        PolynomialFunction p2 = new PolynomialFunction(new double[] { 2, -1, 0 });
        checkNullPolynomial(p1.add(p2));

        p2 = p1.add(p1);
        checkPolynomial(p2, "-4 + 2 x");

        p1 = new PolynomialFunction(new double[] { 1, -4, 2 });
        p2 = new PolynomialFunction(new double[] { -1, 3, -2 });
        p1 = p1.add(p2);
        Assert.assertEquals(1, p1.getDegree());
        checkPolynomial(p1, "-x");
    }

    @Test
    public void testSubtraction() {
        PolynomialFunction p1 = new PolynomialFunction(new double[] { -2, 1 });
        checkNullPolynomial(p1.subtract(p1));

        PolynomialFunction p2 = new PolynomialFunction(new double[] { -2, 6 });
        p2 = p2.subtract(p1);
        checkPolynomial(p2, "5 x");

        p1 = new PolynomialFunction(new double[] { 1, -4, 2 });
        p2 = new PolynomialFunction(new double[] { -1, 3, 2 });
        p1 = p1.subtract(p2);
        Assert.assertEquals(1, p1.getDegree());
        checkPolynomial(p1, "2 - 7 x");
    }

    @Test
    public void testMultiplication() {
        PolynomialFunction p1 = new PolynomialFunction(new double[] { -3, 2 });
        PolynomialFunction p2 = new PolynomialFunction(new double[] { 3, 2, 1 });
        checkPolynomial(p1.multiply(p2), "-9 + x^2 + 2 x^3");

        p1 = new PolynomialFunction(new double[] { 0, 1 });
        p2 = p1;
        for (int i = 2; i < 10; ++i) {
            p2 = p2.multiply(p1);
            checkPolynomial(p2, "x^" + i);
        }
    }

    /**
     * @description Evaluate the polynomial function serialization / deserialization process.
     *
     * @testPassCriteria The polynomial function can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {
        final PolynomialFunction p2 = new PolynomialFunction(new double[] { 3, 2, 1 });
        Assert.assertEquals(p2, TestUtils.serializeAndRecover(p2));
    }

    /**
     * tests the firstDerivative function by comparison
     *
     * <p>
     * This will test the functions <tt>f(x) = x^3 - 2x^2 + 6x + 3, g(x) = 3x^2 - 4x + 6</tt> and
     * <tt>h(x) = 6x - 4</tt>
     */
    @Test
    public void testMath341() {
        final double[] f_coeff = { 3, 6, -2, 1 };
        final double[] g_coeff = { 6, -4, 3 };
        final double[] h_coeff = { -4, 6 };

        final PolynomialFunction f = new PolynomialFunction(f_coeff);
        final PolynomialFunction g = new PolynomialFunction(g_coeff);
        final PolynomialFunction h = new PolynomialFunction(h_coeff);

        // compare f' = g
        Assert.assertEquals(f.univariateDerivative().value(0), g.value(0), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(1), g.value(1), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(100), g.value(100), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(4.1), g.value(4.1), this.tolerance);
        Assert.assertEquals(f.univariateDerivative().value(-3.25), g.value(-3.25), this.tolerance);

        // compare g' = h
        Assert.assertEquals(g.univariateDerivative().value(FastMath.PI), h.value(FastMath.PI), this.tolerance);
        Assert.assertEquals(g.univariateDerivative().value(FastMath.E), h.value(FastMath.E), this.tolerance);
    }

    public static void checkPolynomial(final PolynomialFunction p, final String reference) {
        Assert.assertEquals(reference, p.toString());
    }

    private static void checkNullPolynomial(final PolynomialFunction p) {
        for (final double coefficient : p.getCoefficients()) {
            Assert.assertEquals(0, coefficient, 1e-15);
        }
    }
}
