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

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * Test case for the bicubic interpolator.
 * 
 * @version $Id: BicubicSplineInterpolatorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BicubicSplineInterpolatorTest {
    /**
     * Test preconditions.
     */
    @Test
    public void testPreconditions() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2.5 };
        final double[][] zval = new double[xval.length][yval.length];

        final BivariateGridInterpolator interpolator = new BicubicSplineInterpolator();

        interpolator.interpolate(xval, yval, zval);

        final double[] wxval = new double[] { 3, 2, 5, 6.5 };
        try {
            interpolator.interpolate(wxval, yval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        final double[] wyval = new double[] { -4, -3, -1, -1 };
        try {
            interpolator.interpolate(xval, wyval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        double[][] wzval = new double[xval.length][yval.length + 1];
        try {
            interpolator.interpolate(xval, yval, wzval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        wzval = new double[xval.length - 1][yval.length];
        try {
            interpolator.interpolate(xval, yval, wzval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }

        final double[] xvalue1 = new double[0];
        final double[] yvalue1 = new double[] { 1., 2., 3. };
        final double[][] fvalue1 = new double[1][2];
        try {
            interpolator.interpolate(xvalue1, yvalue1, fvalue1);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        final double[] xvalue2 = new double[] { 1., 2., 3. };
        final double[] yvalue2 = new double[0];
        final double[][] fvalue2 = new double[3][1];
        try {
            interpolator.interpolate(xvalue2, yvalue2, fvalue2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }

        final double[] xvalue3 = new double[] { 1., 2., 3. };
        final double[] yvalue3 = new double[] { 1., 2., 3. };
        final double[][] fvalue3 = new double[0][0];
        try {
            interpolator.interpolate(xvalue3, yvalue3, fvalue3);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * Test of interpolator for a plane.
     * <p>
     * z = 2 x - 3 y + 5
     */
    @Test
    public void testPlane() {
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x - 3 * y + 5;
            }
        };

        final BivariateGridInterpolator interpolator = new BicubicSplineInterpolator();

        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }

        final BivariateFunction p = interpolator.interpolate(xval, yval, zval);
        double x, y;
        double expected, result;

        x = 4;
        y = -3;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("On sample point", expected, result, 1e-15);

        x = 4.5;
        y = -1.5;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("half-way between sample points (middle of the patch)", expected, result, 0.3);

        x = 3.5;
        y = -3.5;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("half-way between sample points (border of the patch)", expected, result, 0.3);
    }

    /**
     * Test of interpolator for a paraboloid.
     * <p>
     * z = 2 x<sup>2</sup> - 3 y<sup>2</sup> + 4 x y - 5
     */
    @Test
    public void testParaboloid() {
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x * x - 3 * y * y + 4 * x * y - 5;
            }
        };

        final BivariateGridInterpolator interpolator = new BicubicSplineInterpolator();

        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -2, -1, 0.5, 2.5 };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }

        final BivariateFunction p = interpolator.interpolate(xval, yval, zval);
        double x, y;
        double expected, result;

        x = 5;
        y = 0.5;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("On sample point", expected, result, 1e-13);

        x = 4.5;
        y = -1.5;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("half-way between sample points (middle of the patch)", expected, result, 0.2);

        x = 3.5;
        y = -3.5;
        expected = f.value(x, y);
        result = p.value(x, y);
        Assert.assertEquals("half-way between sample points (border of the patch)", expected, result, 0.2);
    }
}
