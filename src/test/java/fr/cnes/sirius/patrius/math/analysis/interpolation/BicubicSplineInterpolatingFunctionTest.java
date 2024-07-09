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

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Test case for the bicubic function.
 * 
 * @version $Id: BicubicSplineInterpolatingFunctionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BicubicSplineInterpolatingFunctionTest {
    /**
     * Test preconditions.
     */
    @Test
    public void testPreconditions() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2.5 };
        final double[][] zval = new double[xval.length][yval.length];

        new BicubicSplineInterpolatingFunction(xval, yval, zval,
            zval, zval, zval);

        final double[] wxval = new double[] { 3, 2, 5, 6.5 };
        try {
            new BicubicSplineInterpolatingFunction(wxval, yval, zval, zval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
        final double[] wyval = new double[] { -4, -1, -1, 2.5 };
        try {
            new BicubicSplineInterpolatingFunction(xval, wyval, zval, zval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // Expected
        }
        double[][] wzval = new double[xval.length][yval.length - 1];
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, wzval, zval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, wzval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, zval, wzval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, zval, zval, wzval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }

        wzval = new double[xval.length - 1][yval.length];
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, wzval, zval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, wzval, zval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, zval, wzval, zval);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException e) {
            // Expected
        }
        try {
            new BicubicSplineInterpolatingFunction(xval, yval, zval, zval, zval, wzval);
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
            final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(new double[0], new double[1],
                new double[0][0], new double[0][0], new double[0][0], new double[0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(new double[1], new double[0],
                new double[0][0], new double[0][0], new double[0][0], new double[0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(new double[1], new double[1],
                new double[0][0], new double[0][0], new double[0][0], new double[0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
        try {
            final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(new double[1], new double[1],
                new double[1][0], new double[0][0], new double[0][0], new double[0][0]);
            Assert.fail("expecting NoDataException but got : " + bcf);
        } catch (final NoDataException ex) {
            // expected
        }
    }

    /**
     * Test for a plane.
     * <p>
     * z = 2 x - 3 y + 5
     */
    @Test
    public void testPlane() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        // Function values
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x - 3 * y + 5;
            }
        };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to x
        final double[][] dZdX = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdX[i][j] = 2;
            }
        }
        // Partial derivatives with respect to y
        final double[][] dZdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdY[i][j] = -3;
            }
        }
        // Partial cross-derivatives
        final double[][] dZdXdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdXdY[i][j] = 0;
            }
        }

        final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(xval, yval, zval,
            dZdX, dZdY, dZdXdY);
        double x, y;
        double expected, result;

        x = 4;
        y = -3;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("On sample point",
            expected, result, 1e-15);

        x = 4.5;
        y = -1.5;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("Half-way between sample points (middle of the patch)",
            expected, result, 0.3);

        x = 3.5;
        y = -3.5;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("Half-way between sample points (border of the patch)",
            expected, result, 0.3);
    }

    /**
     * Added TU : test the limit cases of {@link BicubicSplineInterpolatingFunction} when the function is a paraboloid.
     */
    @Test
    public void testPlaneLimitCases() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        // Function values
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x - 3 * y + 5;
            }
        };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to x
        final double[][] dZdX = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdX[i][j] = 2;
            }
        }
        // Partial derivatives with respect to y
        final double[][] dZdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdY[i][j] = -3;
            }
        }
        // Partial cross-derivatives
        final double[][] dZdXdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdXdY[i][j] = 0;
            }
        }

        final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(xval, yval, zval,
            dZdX, dZdY, dZdXdY);
        double x, y;
        double result;

        x = 2.5;
        y = -1.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 7;
        y = -1.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3;
        y = -4.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3;
        y = 3;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /**
     * Test for a paraboloid.
     * <p>
     * z = 2 x<sup>2</sup> - 3 y<sup>2</sup> + 4 x y - 5
     */
    @Test
    public void testParaboloid() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        // Function values
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x * x - 3 * y * y + 4 * x * y - 5;
            }
        };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to x
        final double[][] dZdX = new double[xval.length][yval.length];
        final BivariateFunction dfdX = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 4 * (x + y);
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdX[i][j] = dfdX.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to y
        final double[][] dZdY = new double[xval.length][yval.length];
        final BivariateFunction dfdY = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 4 * x - 6 * y;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdY[i][j] = dfdY.value(xval[i], yval[j]);
            }
        }
        // Partial cross-derivatives
        final double[][] dZdXdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdXdY[i][j] = 4;
            }
        }

        final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(xval, yval, zval,
            dZdX, dZdY, dZdXdY);

        double x, y;
        double expected, result;

        x = 4;
        y = -3;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("On sample point",
            expected, result, 1e-15);

        x = 4.5;
        y = -1.5;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("Half-way between sample points (middle of the patch)",
            expected, result, 2);

        x = 3.5;
        y = -3.5;
        expected = f.value(x, y);
        result = bcf.value(x, y);
        Assert.assertEquals("Half-way between sample points (border of the patch)",
            expected, result, 2);
    }

    /**
     * Added TU : test the limit cases of {@link BicubicSplineInterpolatingFunction} when the function is a paraboloid.
     */
    @Test
    public void testParaboloidLimitCases() {
        final double[] xval = new double[] { 3, 4, 5, 6.5 };
        final double[] yval = new double[] { -4, -3, -1, 2, 2.5 };
        // Function values
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 2 * x * x - 3 * y * y + 4 * x * y - 5;
            }
        };
        final double[][] zval = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                zval[i][j] = f.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to x
        final double[][] dZdX = new double[xval.length][yval.length];
        final BivariateFunction dfdX = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 4 * (x + y);
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdX[i][j] = dfdX.value(xval[i], yval[j]);
            }
        }
        // Partial derivatives with respect to y
        final double[][] dZdY = new double[xval.length][yval.length];
        final BivariateFunction dfdY = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return 4 * x - 6 * y;
            }
        };
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdY[i][j] = dfdY.value(xval[i], yval[j]);
            }
        }
        // Partial cross-derivatives
        final double[][] dZdXdY = new double[xval.length][yval.length];
        for (int i = 0; i < xval.length; i++) {
            for (int j = 0; j < yval.length; j++) {
                dZdXdY[i][j] = 4;
            }
        }

        final BivariateFunction bcf = new BicubicSplineInterpolatingFunction(xval, yval, zval,
            dZdX, dZdY, dZdXdY);
        double x, y;
        double result;

        x = 2.5;
        y = -1.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 7;
        y = -1.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3;
        y = -4.5;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

        x = 3;
        y = 3;
        f.value(x, y);
        try {
            result = bcf.value(x, y);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }
    }

    /**
     * Test for partial derivatives of {@link BicubicSplineFunction}.
     * <p>
     * f(x, y) = &Sigma;<sub>i</sub>&Sigma;<sub>j</sub> (i+1) (j+2) x<sup>i</sup> y<sup>j</sup>
     */
    @Test
    public void testSplinePartialDerivatives() {
        final int N = 4;
        final double[] coeff = new double[16];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                coeff[i + N * j] = (i + 1) * (j + 2);
            }
        }

        // create 5 BicubicSplineFunction for coverage purpuses (when method partialDerivative? is called,
        // computePartialDerivatives() must be called to insure full coverage)
        final BicubicSplineFunction f = new BicubicSplineFunction(coeff);
        final BicubicSplineFunction f2 = new BicubicSplineFunction(coeff);
        final BicubicSplineFunction f3 = new BicubicSplineFunction(coeff);
        final BicubicSplineFunction f4 = new BicubicSplineFunction(coeff);
        final BicubicSplineFunction f5 = new BicubicSplineFunction(coeff);

        BivariateFunction derivative;
        final double x = 0.435;
        final double y = 0.776;
        final double tol = 1e-13;

        derivative = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double y2 = y * y;
                final double y3 = y2 * y;
                final double yFactor = 2 + 3 * y + 4 * y2 + 5 * y3;
                return yFactor * (2 + 6 * x + 12 * x2);
            }
        };
        Assert.assertEquals("dFdX", derivative.value(x, y),
            f.partialDerivativeX().value(x, y), tol);

        derivative = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double x3 = x2 * x;
                final double y2 = y * y;
                final double xFactor = 1 + 2 * x + 3 * x2 + 4 * x3;
                return xFactor * (3 + 8 * y + 15 * y2);
            }
        };
        Assert.assertEquals("dFdY", derivative.value(x, y),
            f2.partialDerivativeY().value(x, y), tol);

        derivative = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double y2 = y * y;
                final double y3 = y2 * y;
                final double yFactor = 2 + 3 * y + 4 * y2 + 5 * y3;
                return yFactor * (6 + 24 * x);
            }
        };
        Assert.assertEquals("d2FdX2", derivative.value(x, y),
            f3.partialDerivativeXX().value(x, y), tol);

        derivative = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double x3 = x2 * x;
                final double xFactor = 1 + 2 * x + 3 * x2 + 4 * x3;
                return xFactor * (8 + 30 * y);
            }
        };
        Assert.assertEquals("d2FdY2", derivative.value(x, y),
            f4.partialDerivativeYY().value(x, y), tol);

        derivative = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double y2 = y * y;
                final double yFactor = 3 + 8 * y + 15 * y2;
                return yFactor * (2 + 6 * x + 12 * x2);
            }
        };
        Assert.assertEquals("d2FdXdY", derivative.value(x, y),
            f5.partialDerivativeXY().value(x, y), tol);
    }

    /**
     * Added TU : this cases should never happen because the bicubic function is
     * created normally through the bicubic spline interpolating function and the
     * method that computes the value of the bicubic function should be called through
     * the method that computes the value of the bicubic spline interpolating function
     * that ensures 0<x<1 and 0<y<1.
     * This test is a UT, not a TVF.
     */
    @Test
    public void testSplineFunctionLimitCases() {
        final int N = 4;
        final double[] coeff = new double[16];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                coeff[i + N * j] = (i + 1) * (j + 2);
            }
        }

        final BicubicSplineFunction f = new BicubicSplineFunction(coeff);

        try {
            f.value(5, 0.2);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(-1.5, 0.2);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, 5);
        } catch (final OutOfRangeException ex) {

        }
        try {
            f.value(0.2, -1.5);
        } catch (final OutOfRangeException ex) {

        }
    }

    /**
     * Test that the partial derivatives computed from a {@link BicubicSplineInterpolatingFunction} match the input
     * data.
     * <p>
     * f(x, y) = 5 - 3 x + 2 y - x y + 2 x<sup>2</sup> - 3 y<sup>2</sup> + 4 x<sup>2</sup> y - x y<sup>2</sup> - 3
     * x<sup>3</sup> + y<sup>3</sup>
     */
    @Test
    public void testMatchingPartialDerivatives() {
        final int sz = 2;
        final double[] val = new double[sz];
        // Coordinate values
        final double delta = 1d / (sz - 1);
        for (int i = 0; i < sz; i++) {
            val[i] = i * delta;
        }
        // Function values
        final BivariateFunction f = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double x3 = x2 * x;
                final double y2 = y * y;
                final double y3 = y2 * y;

                return 5
                    - 3 * x + 2 * y
                    - x * y + 2 * x2 - 3 * y2
                    + 4 * x2 * y - x * y2 - 3 * x3 + y3;
            }
        };
        final double[][] fval = new double[sz][sz];
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                fval[i][j] = f.value(val[i], val[j]);
            }
        }
        // Partial derivatives with respect to x
        final double[][] dFdX = new double[sz][sz];
        final BivariateFunction dfdX = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double y2 = y * y;
                return -3 - y + 4 * x + 8 * x * y - y2 - 9 * x2;
            }
        };
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                dFdX[i][j] = dfdX.value(val[i], val[j]);
            }
        }
        // Partial derivatives with respect to y
        final double[][] dFdY = new double[sz][sz];
        final BivariateFunction dfdY = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double y2 = y * y;
                return 2 - x - 6 * y + 4 * x2 - 2 * x * y + 3 * y2;
            }
        };
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                dFdY[i][j] = dfdY.value(val[i], val[j]);
            }
        }
        // Partial cross-derivatives
        final double[][] d2FdXdY = new double[sz][sz];
        final BivariateFunction d2fdXdY = new BivariateFunction(){
            @Override
            public double value(final double x, final double y) {
                return -1 + 8 * x - 2 * y;
            }
        };
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                d2FdXdY[i][j] = d2fdXdY.value(val[i], val[j]);
            }
        }

        final BicubicSplineInterpolatingFunction bcf =
            new BicubicSplineInterpolatingFunction(val, val, fval, dFdX, dFdY,
                d2FdXdY);

        double x, y;
        double expected, result;
        final double[][] dXXExp = { { 4, 12 }, { -14, -6 } };
        final double[][] dYYExp = { { -6, 0 }, { -8, -2 } };

        final double tol = 1e-12;
        for (int i = 0; i < sz; i++) {
            x = val[i];
            for (int j = 0; j < sz; j++) {
                y = val[j];

                expected = dfdX.value(x, y);
                result = bcf.partialDerivativeX(x, y);
                Assert.assertEquals(x + " " + y + " dFdX", expected, result, tol);
                Assert.assertEquals(dXXExp[i][j], bcf.partialDerivativeXX(x, y), tol);

                expected = dfdY.value(x, y);
                result = bcf.partialDerivativeY(x, y);
                Assert.assertEquals(x + " " + y + " dFdY", expected, result, tol);
                Assert.assertEquals(dYYExp[i][j], bcf.partialDerivativeYY(x, y), tol);

                expected = d2fdXdY.value(x, y);
                result = bcf.partialDerivativeXY(x, y);
                Assert.assertEquals(x + " " + y + " d2FdXdY", expected, result, tol);
            }
        }

        // Added TU for the limit cases

        try {
            result = bcf.partialDerivativeX(-1, 0);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }
        try {
            result = bcf.partialDerivativeX(0, 2);
            Assert.fail("expecting OutOfRangeException but got : " + result);
        } catch (final OutOfRangeException ex) {
            // expected
        }

    }
}
