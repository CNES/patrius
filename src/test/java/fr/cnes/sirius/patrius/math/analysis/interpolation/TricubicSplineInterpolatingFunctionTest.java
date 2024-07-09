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
* VERSION:4.7:DM:DM-2687:18/05/2021:Traitement de modèles de gravité, autres que les harmoniques sphériques
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
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for the bicubic function.
 * 
 * @version $Id: TricubicSplineInterpolatingFunctionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class TricubicSplineInterpolatingFunctionTest {
    /**
     * Test preconditions.
     */
    @Test
    public void testPreconditions() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2.5 };
        final double[] zval = new double[] { -12, -8, -5.5, -3, 0, 2.5 };
        final double[][][] fval = new double[xval.length][yval.length][zval.length];

        new TricubicSplineInterpolatingFunction(xval, yval, zval,
            fval, fval, fval, fval,
            fval, fval, fval, fval);

        final double[] wxval = new double[] { 3, 2, 5, 6.5 };
        try {
            new TricubicSplineInterpolatingFunction(wxval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
        final double[] wyval = new double[] { -4, -1, -1, 2.5 };
        try {
            new TricubicSplineInterpolatingFunction(xval, wyval, zval,
                fval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
        final double[] wzval = new double[] { -12, -8, -9, -3, 0, 2.5 };
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, wzval,
                fval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
        double[][][] wfval = new double[xval.length - 1][yval.length - 1][zval.length];
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                wfval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, wfval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, wfval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, wfval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                wfval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, wfval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, wfval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, fval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        wfval = new double[xval.length][yval.length - 1][zval.length];
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                wfval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, wfval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, wfval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, wfval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                wfval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, wfval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, wfval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, fval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        wfval = new double[xval.length][yval.length][zval.length - 1];
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                wfval, fval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, wfval, fval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, wfval, fval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, wfval,
                fval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                wfval, fval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, wfval, fval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, wfval, fval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new TricubicSplineInterpolatingFunction(xval, yval, zval,
                fval, fval, fval, fval,
                fval, fval, fval, wfval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
    }

    /**
     * Added TU : test the no data case for the constructor of {@link BicubicSplineInterpolatingFunction}.
     */
    @Test
    public void testPreconditionsNoDataCase() {
        try {
            final TrivariateFunction bcf = new TricubicSplineInterpolatingFunction(new double[0], new double[1],
                new double[1],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final TrivariateFunction bcf = new TricubicSplineInterpolatingFunction(new double[1], new double[0],
                new double[1],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final TrivariateFunction bcf = new TricubicSplineInterpolatingFunction(new double[1], new double[1],
                new double[0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final TrivariateFunction bcf = new TricubicSplineInterpolatingFunction(new double[1], new double[1],
                new double[1],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final TrivariateFunction bcf = new TricubicSplineInterpolatingFunction(new double[1], new double[1],
                new double[1],
                new double[1][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0],
                new double[0][0][0], new double[0][0][0], new double[0][0][0], new double[0][0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
    }

    /**
     * Test for a plane.
     * <p>
     * f(x, y, z) = 2 x - 3 y - 4 z + 5
     * </p>
     */
    @Test
    public void testPlane() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        final double[] zval = new double[] { -12, -8, -5.5, -3, 0, 2.5 };

        // Function values
        final TrivariateFunction f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return 2 * x - 3 * y - 4 * z + 5;
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
        // Partial derivatives with respect to x
        final double[][][] dFdX = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdX[i][j][k] = 2;
                }
            }
        }
        // Partial derivatives with respect to y
        final double[][][] dFdY = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdY[i][j][k] = -3;
                }
            }
        }

        // Partial derivatives with respect to z
        final double[][][] dFdZ = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdZ[i][j][k] = -4;
                }
            }
        }
        // Partial cross-derivatives
        final double[][][] d2FdXdY = new double[xval.length][yval.length][zval.length];
        final double[][][] d2FdXdZ = new double[xval.length][yval.length][zval.length];
        final double[][][] d2FdYdZ = new double[xval.length][yval.length][zval.length];
        final double[][][] d3FdXdYdZ = new double[xval.length][yval.length][zval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    d2FdXdY[i][j][k] = 0;
                    d2FdXdZ[i][j][k] = 0;
                    d2FdYdZ[i][j][k] = 0;
                    d3FdXdYdZ[i][j][k] = 0;
                }
            }
        }

        final TrivariateFunction tcf = new TricubicSplineInterpolatingFunction(xval, yval, zval,
            fval, dFdX, dFdY, dFdZ,
            d2FdXdY, d2FdXdZ, d2FdYdZ,
            d3FdXdYdZ);
        double x, y, z;
        double expected, result;

        x = 4;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("On sample point",
            expected, result, 1e-15);

        x = 4.5;
        y = -1.5;
        z = -4.25;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (middle of the patch)",
            expected, result, 0.3);

        x = 3.5;
        y = -3.5;
        z = -10;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (border of the patch)",
            expected, result, 0.3);

        // Added limit cases : when the point is out of the range of definition

        x = 2;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 4.5;
        y = 3;
        z = -4.25;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3.5;
        y = -3.5;
        z = 3;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /**
     * Sine wave.
     * <p>
     * f(x, y, z) = a cos [&omega; z - k<sub>x</sub> x - k<sub>y</sub> y]
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

        // Partial derivatives with respect to x
        final double[][][] dFdX = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction dFdX_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.sin(omega * z - kx * x - ky * y) * kx;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdX[i][j][k] = dFdX_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial derivatives with respect to y
        final double[][][] dFdY = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction dFdY_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.sin(omega * z - kx * x - ky * y) * ky;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdY[i][j][k] = dFdY_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial derivatives with respect to z
        final double[][][] dFdZ = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction dFdZ_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return -a * MathLib.sin(omega * z - kx * x - ky * y) * omega;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    dFdZ[i][j][k] = dFdZ_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial second derivatives w.r.t. (x, y)
        final double[][][] d2FdXdY = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction d2FdXdY_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return -a * MathLib.cos(omega * z - kx * x - ky * y) * kx * ky;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    d2FdXdY[i][j][k] = d2FdXdY_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial second derivatives w.r.t. (x, z)
        final double[][][] d2FdXdZ = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction d2FdXdZ_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.cos(omega * z - kx * x - ky * y) * kx * omega;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    d2FdXdZ[i][j][k] = d2FdXdZ_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial second derivatives w.r.t. (y, z)
        final double[][][] d2FdYdZ = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction d2FdYdZ_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.cos(omega * z - kx * x - ky * y) * ky * omega;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    d2FdYdZ[i][j][k] = d2FdYdZ_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        // Partial third derivatives
        final double[][][] d3FdXdYdZ = new double[xval.length][yval.length][zval.length];
        final TrivariateFunction d3FdXdYdZ_f = new TrivariateFunction(){
            @Override
            public double value(final double x, final double y, final double z) {
                return a * MathLib.sin(omega * z - kx * x - ky * y) * kx * ky * omega;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                for (int k = 0; k < zval.length; k++) {
                    d3FdXdYdZ[i][j][k] = d3FdXdYdZ_f.value(xval[i], yval[j], zval[k]);
                }
            }
        }

        final TrivariateFunction tcf = new TricubicSplineInterpolatingFunction(xval, yval, zval,
            fval, dFdX, dFdY, dFdZ,
            d2FdXdY, d2FdXdZ, d2FdYdZ,
            d3FdXdYdZ);
        double x, y, z;
        double expected, result;

        x = 4;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("On sample point",
            expected, result, 1e-13);

        x = 4.5;
        y = -1.5;
        z = -4.25;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (middle of the patch)",
            expected, result, 0.1);

        x = 3.5;
        y = -3.5;
        z = -10;
        expected = f.value(x, y, z);
        result = tcf.value(x, y, z);
        Assert.assertEquals("Half-way between sample points (border of the patch)",
            expected, result, 0.1);

        // Added limit cases : when the point is out of the range of definition

        x = 2;
        y = -3;
        z = 0;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 4.5;
        y = 3;
        z = -4.25;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3.5;
        y = -3.5;
        z = 5;
        expected = f.value(x, y, z);
        try {
            result = tcf.value(x, y, z);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /**
     * Added TU : this cases should never happen because the tricubic function is
     * created normally through the tricubic spline interpolating function and the
     * method that computes the value of the tricubic function should be called through
     * the method that computes the value of the tricubic spline interpolating function
     * that ensures 0<x<1 and 0<y<1.
     * This test is a UT, not a TVF.
     */
    @Test
    public void testSplineFunctionLimitCases() {
        final int N = 4;
        final double[] coeff = new double[64];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    coeff[i + N * j + N * k] = (i + 1) * (j + 2) * (k + 3);
                }
            }
        }

        final TricubicSplineFunction f = new TricubicSplineFunction(coeff);

        try {
            f.value(5, 0.2, 0.1);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(-1.5, 0.2, 0.1);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, 5, 0.1);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, -1.5, 0.1);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, 0.1, -1.2);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, 0.1, 2);
        } catch (final OutOfRangeException ex) {

        }
    }
}
