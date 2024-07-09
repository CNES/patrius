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
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NotFiniteNumberException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test of the LoessInterpolator class.
 */
public class LoessInterpolatorTest {

    @Test
    public void testOnOnePoint() {
        final double[] xval = { 0.5 };
        final double[] yval = { 0.7 };
        final double[] res = new LoessInterpolator().smooth(xval, yval);
        Assert.assertEquals(1, res.length);
        Assert.assertEquals(0.7, res[0], 0.0);
    }

    @Test
    public void testOnTwoPoints() {
        final double[] xval = { 0.5, 0.6 };
        final double[] yval = { 0.7, 0.8 };
        final double[] res = new LoessInterpolator().smooth(xval, yval);
        Assert.assertEquals(2, res.length);
        Assert.assertEquals(0.7, res[0], 0.0);
        Assert.assertEquals(0.8, res[1], 0.0);
    }

    @Test
    public void testOnStraightLine() {
        final double[] xval = { 1, 2, 3, 4, 5 };
        final double[] yval = { 2, 4, 6, 8, 10 };
        final LoessInterpolator li = new LoessInterpolator(0.6, 2, 1e-12);
        final double[] res = li.smooth(xval, yval);
        Assert.assertEquals(5, res.length);
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(yval[i], res[i], 1e-8);
        }
    }

    @Test
    public void testOnDistortedSine() {
        final int numPoints = 100;
        final double[] xval = new double[numPoints];
        final double[] yval = new double[numPoints];
        final double xnoise = 0.1;
        final double ynoise = 0.2;

        this.generateSineData(xval, yval, xnoise, ynoise);

        final LoessInterpolator li = new LoessInterpolator(0.3, 4, 1e-12);

        final double[] res = li.smooth(xval, yval);

        // Check that the resulting curve differs from
        // the "real" sine less than the jittered one

        double noisyResidualSum = 0;
        double fitResidualSum = 0;

        for (int i = 0; i < numPoints; ++i) {
            final double expected = MathLib.sin(xval[i]);
            final double noisy = yval[i];
            final double fit = res[i];

            noisyResidualSum += MathLib.pow(noisy - expected, 2);
            fitResidualSum += MathLib.pow(fit - expected, 2);
        }

        Assert.assertTrue(fitResidualSum < noisyResidualSum);
    }

    @Test
    public void testIncreasingBandwidthIncreasesSmoothness() {
        final int numPoints = 100;
        final double[] xval = new double[numPoints];
        final double[] yval = new double[numPoints];
        final double xnoise = 0.1;
        final double ynoise = 0.1;

        this.generateSineData(xval, yval, xnoise, ynoise);

        // Check that variance decreases as bandwidth increases

        final double[] bandwidths = { 0.1, 0.5, 1.0 };
        final double[] variances = new double[bandwidths.length];
        for (int i = 0; i < bandwidths.length; i++) {
            final double bw = bandwidths[i];

            final LoessInterpolator li = new LoessInterpolator(bw, 4, 1e-12);

            final double[] res = li.smooth(xval, yval);

            for (int j = 1; j < res.length; ++j) {
                variances[i] += MathLib.pow(res[j] - res[j - 1], 2);
            }
        }

        for (int i = 1; i < variances.length; ++i) {
            Assert.assertTrue(variances[i] < variances[i - 1]);
        }
    }

    @Test
    public void testIncreasingRobustnessItersIncreasesSmoothnessWithOutliers() {
        final int numPoints = 100;
        final double[] xval = new double[numPoints];
        final double[] yval = new double[numPoints];
        final double xnoise = 0.1;
        final double ynoise = 0.1;

        this.generateSineData(xval, yval, xnoise, ynoise);

        // Introduce a couple of outliers
        yval[numPoints / 3] *= 100;
        yval[2 * numPoints / 3] *= -100;

        // Check that variance decreases as the number of robustness
        // iterations increases

        final double[] variances = new double[4];
        for (int i = 0; i < 4; i++) {
            final LoessInterpolator li = new LoessInterpolator(0.3, i, 1e-12);

            final double[] res = li.smooth(xval, yval);

            for (int j = 1; j < res.length; ++j) {
                variances[i] += MathLib.abs(res[j] - res[j - 1]);
            }
        }

        for (int i = 1; i < variances.length; ++i) {
            Assert.assertTrue(variances[i] < variances[i - 1]);
        }
    }

    @Test(expected = DimensionMismatchException.class)
    public void testUnequalSizeArguments() {
        new LoessInterpolator().smooth(new double[] { 1, 2, 3 }, new double[] { 1, 2, 3, 4 });
    }

    @Test(expected = NoDataException.class)
    public void testEmptyData() {
        new LoessInterpolator().smooth(new double[] {}, new double[] {});
    }

    @Test(expected = NonMonotonicSequenceException.class)
    public void testNonStrictlyIncreasing1() {
        new LoessInterpolator().smooth(new double[] { 4, 3, 1, 2 }, new double[] { 3, 4, 5, 6 });
    }

    @Test(expected = NonMonotonicSequenceException.class)
    public void testNonStrictlyIncreasing2() {
        new LoessInterpolator().smooth(new double[] { 1, 2, 2, 3 }, new double[] { 3, 4, 5, 6 });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal1() {
        new LoessInterpolator().smooth(new double[] { 1, 2, Double.NaN }, new double[] { 3, 4, 5 });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal2() {
        new LoessInterpolator().smooth(new double[] { 1, 2, Double.POSITIVE_INFINITY }, new double[] { 3, 4, 5 });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal3() {
        new LoessInterpolator().smooth(new double[] { 1, 2, Double.NEGATIVE_INFINITY }, new double[] { 3, 4, 5 });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal4() {
        new LoessInterpolator().smooth(new double[] { 3, 4, 5 }, new double[] { 1, 2, Double.NaN });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal5() {
        new LoessInterpolator().smooth(new double[] { 3, 4, 5 }, new double[] { 1, 2, Double.POSITIVE_INFINITY });
    }

    @Test(expected = NotFiniteNumberException.class)
    public void testNotAllFiniteReal6() {
        new LoessInterpolator().smooth(new double[] { 3, 4, 5 }, new double[] { 1, 2, Double.NEGATIVE_INFINITY });
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testInsufficientBandwidth() {
        final LoessInterpolator li = new LoessInterpolator(0.1, 3, 1e-12);
        li.smooth(new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12 });
    }

    @Test(expected = OutOfRangeException.class)
    public void testCompletelyIncorrectBandwidth1() {
        new LoessInterpolator(-0.2, 3, 1e-12);
    }

    @Test(expected = OutOfRangeException.class)
    public void testCompletelyIncorrectBandwidth2() {
        new LoessInterpolator(1.1, 3, 1e-12);
    }

    @Test
    public void testMath296withoutWeights() {
        final double[] xval = {
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
            1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0 };
        final double[] yval = {
            0.47, 0.48, 0.55, 0.56, -0.08, -0.04, -0.07, -0.07,
            -0.56, -0.46, -0.56, -0.52, -3.03, -3.08, -3.09,
            -3.04, 3.54, 3.46, 3.36, 3.35 };
        // Output from R, rounded to .001
        final double[] yref = {
            0.461, 0.499, 0.541, 0.308, 0.175, -0.042, -0.072,
            -0.196, -0.311, -0.446, -0.557, -1.497, -2.133,
            -3.08, -3.09, -0.621, 0.982, 3.449, 3.389, 3.336
        };
        final LoessInterpolator li = new LoessInterpolator(0.3, 4, 1e-12);
        final double[] res = li.smooth(xval, yval);
        Assert.assertEquals(xval.length, res.length);
        for (int i = 0; i < res.length; ++i) {
            Assert.assertEquals(yref[i], res[i], 0.02);
        }
    }

    private void generateSineData(final double[] xval, final double[] yval, final double xnoise, final double ynoise) {
        final double dx = 2 * FastMath.PI / xval.length;
        double x = 0;
        for (int i = 0; i < xval.length; ++i) {
            xval[i] = x;
            yval[i] = MathLib.sin(x) + (2 * MathLib.random() - 1) * ynoise;
            x += dx * (1 + (2 * MathLib.random() - 1) * xnoise);
        }
    }
}
