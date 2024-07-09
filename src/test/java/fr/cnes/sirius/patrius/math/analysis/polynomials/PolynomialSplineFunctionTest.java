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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Tests the PolynomialSplineFunction implementation.
 * 
 * @version $Id: PolynomialSplineFunctionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PolynomialSplineFunctionTest {

    /** Error tolerance for tests */
    protected double tolerance = 1.0e-12;

    /**
     * Quadratic polynomials used in tests:
     * 
     * x^2 + x [-1, 0)
     * x^2 + x + 2 [0, 1)
     * x^2 + x + 4 [1, 2)
     * 
     * Defined so that evaluation using PolynomialSplineFunction evaluation
     * algorithm agrees at knot point boundaries.
     */
    protected PolynomialFunction[] polynomials = {
        new PolynomialFunction(new double[] { 0d, 1d, 1d }),
        new PolynomialFunction(new double[] { 2d, 1d, 1d }),
        new PolynomialFunction(new double[] { 4d, 1d, 1d })
    };

    /** Knot points */
    protected double[] knots = { -1, 0, 1, 2 };

    /** Derivative of test polynomials -- 2x + 1 */
    protected PolynomialFunction dp =
        new PolynomialFunction(new double[] { 1d, 2d });

    @Test
    public void testConstructor() {
        final PolynomialSplineFunction spline =
            new PolynomialSplineFunction(this.knots, this.polynomials);
        Assert.assertTrue(Arrays.equals(this.knots, spline.getKnots()));
        Assert.assertEquals(1d, spline.getPolynomials()[0].getCoefficients()[2], 0);
        Assert.assertEquals(3, spline.getN());

        try { // too few knots
            new PolynomialSplineFunction(new double[] { 0 }, this.polynomials);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        try { // too many knots
            new PolynomialSplineFunction(new double[] { 0, 1, 2, 3, 4 }, this.polynomials);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        try { // knots not increasing
            new PolynomialSplineFunction(new double[] { 0, 1, 3, 2 }, this.polynomials);
            Assert.fail("Expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        // Added case

        try { // knots not increasing
            new PolynomialSplineFunction(null, this.polynomials);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }

        try { // knots not increasing
            new PolynomialSplineFunction(this.knots, null);
            Assert.fail("Expecting NullArgumentException");
        } catch (final NullArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testValues() {
        final PolynomialSplineFunction spline =
            new PolynomialSplineFunction(this.knots, this.polynomials);
        final UnivariateFunction dSpline = spline.derivative();

        /**
         * interior points -- spline value at x should equal p(x - knot)
         * where knot is the largest knot point less than or equal to x and p
         * is the polynomial defined over the knot segment to which x belongs.
         */
        double x = -1;
        int index = 0;
        for (int i = 0; i < 10; i++) {
            x += 0.25;
            index = this.findKnot(this.knots, x);
            Assert.assertEquals("spline function evaluation failed for x=" + x,
                this.polynomials[index].value(x - this.knots[index]), spline.value(x), this.tolerance);
            Assert.assertEquals("spline derivative evaluation failed for x=" + x,
                this.dp.value(x - this.knots[index]), dSpline.value(x), this.tolerance);
        }

        // knot points -- centering should zero arguments
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals("spline function evaluation failed for knot=" + this.knots[i],
                this.polynomials[i].value(0), spline.value(this.knots[i]), this.tolerance);
            Assert.assertEquals("spline function evaluation failed for knot=" + this.knots[i],
                this.dp.value(0), dSpline.value(this.knots[i]), this.tolerance);
        }

        try { // outside of domain -- under min
            x = spline.value(-1.5);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }

        try { // outside of domain -- over max
            x = spline.value(2.5);
            Assert.fail("Expecting OutOfRangeException");
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /**
     * Do linear search to find largest knot point less than or equal to x.
     * Implementation does binary search.
     */
    protected int findKnot(final double[] knots, final double x) {
        if (x < knots[0] || x >= knots[knots.length - 1]) {
            throw new OutOfRangeException(x, knots[0], knots[knots.length - 1]);
        }
        for (int i = 0; i < knots.length; i++) {
            if (knots[i] > x) {
                return i - 1;
            }
        }
        throw new MathIllegalStateException();
    }
}
