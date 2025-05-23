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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for {@link UnivariatePeriodicInterpolator}.
 */
public class UnivariatePeriodicInterpolatorTest {
    private final Random rng = new Random(1224465L);

    @Test
    public void testSine() {
        final int n = 30;
        final double[] xval = new double[n];
        final double[] yval = new double[n];
        final double period = 12.3;
        final double offset = 45.67;

        double delta = 0;
        for (int i = 0; i < n; i++) {
            delta += this.rng.nextDouble() * period / n;
            xval[i] = offset + delta;
            yval[i] = MathLib.sin(xval[i]);
        }

        final UnivariateInterpolator inter = new LinearInterpolator();
        final UnivariateFunction f = inter.interpolate(xval, yval);

        final UnivariateInterpolator interP = new UnivariatePeriodicInterpolator(new LinearInterpolator(),
            period, 1);
        final UnivariateFunction fP = interP.interpolate(xval, yval);

        // Comparing with original interpolation algorithm.
        final double xMin = xval[0];
        final double xMax = xval[n - 1];
        for (int i = 0; i < n; i++) {
            final double x = xMin + (xMax - xMin) * this.rng.nextDouble();
            final double y = f.value(x);
            final double yP = fP.value(x);

            Assert.assertEquals("x=" + x, y, yP, Math.ulp(1d));
        }

        // Test interpolation outside the primary interval.
        for (int i = 0; i < n; i++) {
            final double xIn = offset + this.rng.nextDouble() * period;
            final double xOut = xIn + this.rng.nextInt(123456789) * period;
            final double yIn = fP.value(xIn);
            final double yOut = fP.value(xOut);

            Assert.assertEquals(yIn, yOut, 1e-7);
        }
    }

    @Test
    public void testLessThanOnePeriodCoverage() {
        final int n = 30;
        final double[] xval = new double[n];
        final double[] yval = new double[n];
        final double period = 12.3;
        final double offset = 45.67;

        double delta = period / 2;
        for (int i = 0; i < n; i++) {
            delta += period / (2 * n) * this.rng.nextDouble();
            xval[i] = offset + delta;
            yval[i] = MathLib.sin(xval[i]);
        }

        final UnivariateInterpolator interP = new UnivariatePeriodicInterpolator(new LinearInterpolator(),
            period, 1);
        final UnivariateFunction fP = interP.interpolate(xval, yval);

        // Test interpolation outside the sample data interval.
        for (int i = 0; i < n; i++) {
            final double xIn = offset + this.rng.nextDouble() * period;
            final double xOut = xIn + this.rng.nextInt(123456789) * period;
            final double yIn = fP.value(xIn);
            final double yOut = fP.value(xOut);

            Assert.assertEquals(yIn, yOut, 1e-7);
        }
    }

    @Test
    public void testMoreThanOnePeriodCoverage() {
        final int n = 30;
        final double[] xval = new double[n];
        final double[] yval = new double[n];
        final double period = 12.3;
        final double offset = 45.67;

        double delta = period / 2;
        for (int i = 0; i < n; i++) {
            delta += 10 * period / n * this.rng.nextDouble();
            xval[i] = offset + delta;
            yval[i] = MathLib.sin(xval[i]);
        }

        final UnivariateInterpolator interP = new UnivariatePeriodicInterpolator(new LinearInterpolator(),
            period, 1);
        final UnivariateFunction fP = interP.interpolate(xval, yval);

        // Test interpolation outside the sample data interval.
        for (int i = 0; i < n; i++) {
            final double xIn = offset + this.rng.nextDouble() * period;
            final double xOut = xIn + this.rng.nextInt(123456789) * period;
            final double yIn = fP.value(xIn);
            final double yOut = fP.value(xOut);

            Assert.assertEquals(yIn, yOut, 1e-6);
        }
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testTooFewSamples() {
        final double[] xval = { 2, 3, 7 };
        final double[] yval = { 1, 6, 5 };
        final double period = 10;

        final UnivariateInterpolator interpolator =
            new UnivariatePeriodicInterpolator(new LinearInterpolator(), period);
        interpolator.interpolate(xval, yval);
    }

    @Test(expected = NonMonotonicSequenceException.class)
    public void testUnsortedSamples() {
        final double[] xval = { 2, 3, 7, 4, 6 };
        final double[] yval = { 1, 6, 5, -1, -2 };
        final double period = 10;

        final UnivariateInterpolator interpolator =
            new UnivariatePeriodicInterpolator(new LinearInterpolator(), period);
        interpolator.interpolate(xval, yval);
    }
}
