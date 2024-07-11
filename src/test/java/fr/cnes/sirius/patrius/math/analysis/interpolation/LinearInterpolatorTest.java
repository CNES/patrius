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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialSplineFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Test the LinearInterpolator.
 */
public class LinearInterpolatorTest {

    /** error tolerance for spline interpolator value at knot points */
    protected double knotTolerance = 1E-12;

    /** error tolerance for interpolating polynomial coefficients */
    protected double coefficientTolerance = 1E-6;

    /** error tolerance for interpolated values */
    protected double interpolationTolerance = 1E-12;

    @Test
    public void testInterpolateLinearDegenerateTwoSegment()
    {
        final double x[] = { 0.0, 0.5, 1.0 };
        final double y[] = { 0.0, 0.5, 1.0 };
        final UnivariateInterpolator i = new LinearInterpolator();
        final UnivariateFunction f = i.interpolate(x, y);
        this.verifyInterpolation(f, x, y);

        // Verify coefficients using analytical values
        final PolynomialFunction polynomials[] = ((PolynomialSplineFunction) f).getPolynomials();
        double target[] = { y[0], 1d };
        TestUtils.assertEquals(polynomials[0].getCoefficients(), target, this.coefficientTolerance);
        target = new double[] { y[1], 1d };
        TestUtils.assertEquals(polynomials[1].getCoefficients(), target, this.coefficientTolerance);

        // Check interpolation
        Assert.assertEquals(0.0, f.value(0.0), this.interpolationTolerance);
        Assert.assertEquals(0.4, f.value(0.4), this.interpolationTolerance);
        Assert.assertEquals(1.0, f.value(1.0), this.interpolationTolerance);
    }

    @Test
    public void testInterpolateLinearDegenerateThreeSegment()
    {
        final double x[] = { 0.0, 0.5, 1.0, 1.5 };
        final double y[] = { 0.0, 0.5, 1.0, 1.5 };
        final UnivariateInterpolator i = new LinearInterpolator();
        final UnivariateFunction f = i.interpolate(x, y);
        this.verifyInterpolation(f, x, y);

        // Verify coefficients using analytical values
        final PolynomialFunction polynomials[] = ((PolynomialSplineFunction) f).getPolynomials();
        double target[] = { y[0], 1d };
        TestUtils.assertEquals(polynomials[0].getCoefficients(), target, this.coefficientTolerance);
        target = new double[] { y[1], 1d };
        TestUtils.assertEquals(polynomials[1].getCoefficients(), target, this.coefficientTolerance);
        target = new double[] { y[2], 1d };
        TestUtils.assertEquals(polynomials[2].getCoefficients(), target, this.coefficientTolerance);

        // Check interpolation
        Assert.assertEquals(0, f.value(0), this.interpolationTolerance);
        Assert.assertEquals(1.4, f.value(1.4), this.interpolationTolerance);
        Assert.assertEquals(1.5, f.value(1.5), this.interpolationTolerance);
    }

    @Test
    public void testInterpolateLinear() {
        final double x[] = { 0.0, 0.5, 1.0 };
        final double y[] = { 0.0, 0.5, 0.0 };
        final UnivariateInterpolator i = new LinearInterpolator();
        final UnivariateFunction f = i.interpolate(x, y);
        this.verifyInterpolation(f, x, y);

        // Verify coefficients using analytical values
        final PolynomialFunction polynomials[] = ((PolynomialSplineFunction) f).getPolynomials();
        double target[] = { y[0], 1d };
        TestUtils.assertEquals(polynomials[0].getCoefficients(), target, this.coefficientTolerance);
        target = new double[] { y[1], -1d };
        TestUtils.assertEquals(polynomials[1].getCoefficients(), target, this.coefficientTolerance);
    }

    @Test
    public void testIllegalArguments() {
        // Data set arrays of different size.
        final UnivariateInterpolator i = new LinearInterpolator();
        try {
            final double xval[] = { 0.0, 1.0 };
            final double yval[] = { 0.0, 1.0, 2.0 };
            i.interpolate(xval, yval);
            Assert.fail("Failed to detect data set array with different sizes.");
        } catch (final DimensionMismatchException iae) {
            // Expected.
        }
        // X values not sorted.
        try {
            final double xval[] = { 0.0, 1.0, 0.5 };
            final double yval[] = { 0.0, 1.0, 2.0 };
            i.interpolate(xval, yval);
            Assert.fail("Failed to detect unsorted arguments.");
        } catch (final NonMonotonicSequenceException iae) {
            // Expected.
        }
        // Not enough data to interpolate.
        try {
            final double xval[] = { 0.0 };
            final double yval[] = { 0.0 };
            i.interpolate(xval, yval);
            Assert.fail("Failed to detect unsorted arguments.");
        } catch (final NumberIsTooSmallException iae) {
            // Expected.
        }
    }

    /**
     * verifies that f(x[i]) = y[i] for i = 0..n-1 where n is common length.
     */
    protected void verifyInterpolation(final UnivariateFunction f, final double x[], final double y[])
    {
        for (int i = 0; i < x.length; i++) {
            Assert.assertEquals(f.value(x[i]), y[i], this.knotTolerance);
        }
    }

}
