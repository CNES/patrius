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

import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for the tricubic interpolator.
 * 
 * @version $Id: TricubicSplineInterpolatorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class TricubicSplineInterpolatorTest {
    /**
     * Test preconditions.
     */
    @Test
    public void testPreconditions() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2.5 };
        final double[] zval = new double[] { -12, -8, -5.5, -3, 0, 2.5 };
        final double[][][] fval = new double[xval.length][yval.length][zval.length];

        final TrivariateGridInterpolator interpolator = new TricubicSplineInterpolator();

        interpolator.interpolate(xval, yval, zval, fval);

        final double[] wxval = new double[] { 3, 2, 5, 6.5 };
        try {
            interpolator.interpolate(wxval, yval, zval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        final double[] wyval = new double[] { -4, -3, -1, -1 };
        try {
            interpolator.interpolate(xval, wyval, zval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        final double[] wzval = new double[] { -12, -8, -5.5, -3, -4, 2.5 };
        try {
            interpolator.interpolate(xval, yval, wzval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        double[][][] wfval = new double[xval.length][yval.length + 1][zval.length];
        try {
            interpolator.interpolate(xval, yval, zval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        wfval = new double[xval.length - 1][yval.length][zval.length];
        try {
            interpolator.interpolate(xval, yval, zval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        wfval = new double[xval.length][yval.length][zval.length - 1];
        try {
            interpolator.interpolate(xval, yval, zval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }

        final double[] xvalue1 = new double[0];
        final double[] yvalue1 = new double[] { 1., 2., 3. };
        final double[] zvalue1 = new double[] { 1., 2., 3. };
        final double[][][] fvalue1 = new double[1][1][1];
        try {
            interpolator.interpolate(xvalue1, yvalue1, zvalue1, fvalue1);
            Assert.fail("an exception should have been thrown");
        } catch (final NoDataException e) {
            // Expected
        }

        final double[] xvalue2 = new double[] { 1., 2., 3. };
        final double[] yvalue2 = new double[0];
        final double[] zvalue2 = new double[] { 1., 2., 3. };
        final double[][][] fvalue2 = new double[1][1][1];
        try {
            interpolator.interpolate(xvalue2, yvalue2, zvalue2, fvalue2);
            Assert.fail("an exception should have been thrown");
        } catch (final NoDataException e) {
            // Expected
        }

        final double[] xvalue3 = new double[] { 1., 2., 3. };
        final double[] yvalue3 = new double[] { 1., 2., 3. };
        final double[] zvalue3 = new double[0];
        final double[][][] fvalue3 = new double[1][1][1];
        try {
            interpolator.interpolate(xvalue3, yvalue3, zvalue3, fvalue3);
            Assert.fail("an exception should have been thrown");
        } catch (final NoDataException e) {
            // Expected
        }

        final double[] xvalue4 = new double[] { 1., 2., 3. };
        final double[] yvalue4 = new double[] { 1., 2., 3. };
        final double[] zvalue4 = new double[] { 1., 2., 3. };
        final double[][][] fvalue4 = new double[0][0][0];
        try {
            interpolator.interpolate(xvalue4, yvalue4, zvalue4, fvalue4);
            Assert.fail("an exception should have been thrown");
        } catch (final NoDataException e) {
            // Expected
        }

    }

    /**
     * Test of interpolator for a plane.
     * <p>
     * f(x, y, z) = 2 x - 3 y - z + 5
     */
    @Test
    public void testPlane() {
        final TrivariateFunction f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return 2 * x - 3 * y - z + 5;
            }
        };

        final TrivariateGridInterpolator interpolator = new TricubicSplineInterpolator();

        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        final double[] zval = new double[] { -12, -8, -5.5, -3, 0, 2.5 };
        final double[][][] fval = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    fval[i][j][k] = f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        final TrivariateFunction p = interpolator.interpolate(xval, yval, zval, fval);
        double x, y, z;
        double expected, result;

        x = 4;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("On sample point", expected, result, 1e-15);

        x = 4.5;
        y = -1.5;
        z = -4.25;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("half-way between sample points (middle of the patch)", expected, result, 0.3);

        x = 3.5;
        y = -3.5;
        z = -10;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("half-way between sample points (border of the patch)", expected, result, 0.3);
    }

    /**
     * Test of interpolator for a sine wave.
     * <p>
     * <p>
     * f(x, y, z) = a cos [&omega; z - k<sub>y</sub> x - k<sub>y</sub> y]
     * </p>
     * with A = 0.2, &omega; = 0.5, k<sub>x</sub> = 2, k<sub>y</sub> = 1.
     */
    @Test
    public void testWave() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        final double[] zval = new double[] { -12, -8, -5.5, -3, 0, 4 };

        final double a = 0.2;
        final double omega = 0.5;
        final double kx = 2;
        final double ky = 1;

        // Function values
        final TrivariateFunction f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.cos(omega * z - kx * x - ky * y);
            }
        };

        final double[][][] fval = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    fval[i][j][k] = f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        final TrivariateGridInterpolator interpolator = new TricubicSplineInterpolator();

        final TrivariateFunction p = interpolator.interpolate(xval, yval, zval, fval);
        double x, y, z;
        double expected, result;

        x = 4;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("On sample point",
            expected, result, 1e-12);

        x = 4.5;
        y = -1.5;
        z = -4.25;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (middle of the patch)",
            expected, result, 0.1);

        x = 3.5;
        y = -3.5;
        z = -10;
        expected = f.value(x, y, z);
        result = p.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (border of the patch)",
            expected, result, 0.1);
    }
}
