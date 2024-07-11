/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;

/**
 * Function that implements the
 * <a href="http://en.wikipedia.org/wiki/Bicubic_interpolation">
 * bicubic spline interpolation</a>.
 * 
 * @since 2.1
 * @version $Id: BicubicSplineInterpolatingFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BicubicSplineInterpolatingFunction
    implements BivariateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = 7322860837281925212L;

    /**
     * Matrix to compute the spline coefficients from the function values
     * and function derivatives values
     */
    private static final double[][] AINV = {
        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { -3, 3, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { 2, -2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, -2, -1, 0, 0 },
        { 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 1, 1, 0, 0 },
        { -3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0 },
        { 9, -9, -9, 9, 6, 3, -6, -3, 6, -6, 3, -3, 4, 2, 2, 1 },
        { -6, 6, 6, -6, -3, -3, 3, 3, -4, 4, -2, 2, -2, -2, -1, -1 },
        { 2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0 },
        { -6, 6, 6, -6, -4, -2, 4, 2, -3, 3, -3, 3, -2, -1, -2, -1 },
        { 4, -4, -4, 4, 2, 2, -2, -2, 2, -2, 2, -2, 1, 1, 1, 1 }
    };

    /** Number of coefficients. */
    private static final int NB_COEFS = 16;

    /** Samples x-coordinates */
    private final double[] xval;
    /** Samples y-coordinates */
    private final double[] yval;
    /** Set of cubic splines patching the whole data grid */
    private final BicubicSplineFunction[][] splines;
    /**
     * Partial derivatives
     * The value of the first index determines the kind of derivatives:
     * 0 = first partial derivatives wrt x
     * 1 = first partial derivatives wrt y
     * 2 = second partial derivatives wrt x
     * 3 = second partial derivatives wrt y
     * 4 = cross partial derivatives
     */
    private BivariateFunction[][][] partialDerivatives = null;

    /**
     * @param x
     *        Sample values of the x-coordinate, in increasing order.
     * @param y
     *        Sample values of the y-coordinate, in increasing order.
     * @param f
     *        Values of the function on every grid point.
     * @param dFdX
     *        Values of the partial derivative of function with respect
     *        to x on every grid point.
     * @param dFdY
     *        Values of the partial derivative of function with respect
     *        to y on every grid point.
     * @param d2FdXdY
     *        Values of the cross partial derivative of function on
     *        every grid point.
     * @throws DimensionMismatchException
     *         if the various arrays do not contain
     *         the expected number of elements.
     * @throws NonMonotonicSequenceException
     *         if {@code x} or {@code y} are
     *         not strictly increasing.
     * @throws NoDataException
     *         if any of the arrays has zero length.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public BicubicSplineInterpolatingFunction(final double[] x,
                                              final double[] y,
                                              final double[][] f,
                                              final double[][] dFdX,
                                              final double[][] dFdY,
                                              final double[][] d2FdXdY) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final int xLen = x.length;
        final int yLen = y.length;

        if (xLen == 0 || yLen == 0 || f.length == 0 || f[0].length == 0) {
            throw new NoDataException();
        }
        if (xLen != f.length) {
            throw new DimensionMismatchException(xLen, f.length);
        }
        if (xLen != dFdX.length) {
            throw new DimensionMismatchException(xLen, dFdX.length);
        }
        if (xLen != dFdY.length) {
            throw new DimensionMismatchException(xLen, dFdY.length);
        }
        if (xLen != d2FdXdY.length) {
            throw new DimensionMismatchException(xLen, d2FdXdY.length);
        }

        MathArrays.checkOrder(x);
        MathArrays.checkOrder(y);

        this.xval = x.clone();
        this.yval = y.clone();

        final int lastI = xLen - 1;
        final int lastJ = yLen - 1;
        this.splines = new BicubicSplineFunction[lastI][lastJ];

        for (int i = 0; i < lastI; i++) {
            if (f[i].length != yLen) {
                throw new DimensionMismatchException(f[i].length, yLen);
            }
            if (dFdX[i].length != yLen) {
                throw new DimensionMismatchException(dFdX[i].length, yLen);
            }
            if (dFdY[i].length != yLen) {
                throw new DimensionMismatchException(dFdY[i].length, yLen);
            }
            if (d2FdXdY[i].length != yLen) {
                throw new DimensionMismatchException(d2FdXdY[i].length, yLen);
            }
            final int ip1 = i + 1;
            for (int j = 0; j < lastJ; j++) {
                final int jp1 = j + 1;
                final double[] beta = new double[] {
                    f[i][j], f[ip1][j], f[i][jp1], f[ip1][jp1],
                    dFdX[i][j], dFdX[ip1][j], dFdX[i][jp1], dFdX[ip1][jp1],
                    dFdY[i][j], dFdY[ip1][j], dFdY[i][jp1], dFdY[ip1][jp1],
                    d2FdXdY[i][j], d2FdXdY[ip1][j], d2FdXdY[i][jp1], d2FdXdY[ip1][jp1]
                };

                this.splines[i][j] = new BicubicSplineFunction(this.computeSplineCoefficients(beta));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double value(final double x, final double y) {
        final int i = this.searchIndex(x, this.xval);
        if (i == -1) {
            throw new OutOfRangeException(x, this.xval[0], this.xval[this.xval.length - 1]);
        }
        final int j = this.searchIndex(y, this.yval);
        if (j == -1) {
            throw new OutOfRangeException(y, this.yval[0], this.yval[this.yval.length - 1]);
        }

        final double xN = (x - this.xval[i]) / (this.xval[i + 1] - this.xval[i]);
        final double yN = (y - this.yval[j]) / (this.yval[j + 1] - this.yval[j]);

        return this.splines[i][j].value(xN, yN);
    }

    /**
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the first partial derivative with
     *         respect to x.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    public double partialDerivativeX(final double x, final double y) {
        return this.partialDerivative(0, x, y);
    }

    /**
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the first partial derivative with
     *         respect to y.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    public double partialDerivativeY(final double x, final double y) {
        return this.partialDerivative(1, x, y);
    }

    /**
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the second partial derivative with
     *         respect to x.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    public double partialDerivativeXX(final double x, final double y) {
        return this.partialDerivative(2, x, y);
    }

    /**
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the second partial derivative with
     *         respect to y.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    public double partialDerivativeYY(final double x, final double y) {
        return this.partialDerivative(3, x, y);
    }

    /**
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the second partial cross-derivative.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    public double partialDerivativeXY(final double x, final double y) {
        return this.partialDerivative(4, x, y);
    }

    /**
     * @param which
     *        First index in {@link #partialDerivatives}.
     * @param x
     *        x-coordinate.
     * @param y
     *        y-coordinate.
     * @return the value at point (x, y) of the selected partial derivative.
     * @throws OutOfRangeException
     *         if {@code x} (resp. {@code y}) is outside
     *         the range defined by the boundary values of {@code xval} (resp. {@code yval}).
     */
    private double partialDerivative(final int which, final double x, final double y) {
        if (this.partialDerivatives == null) {
            this.computePartialDerivatives();
        }
        // search index for xval
        final int i = this.searchIndex(x, this.xval);
        if (i == -1) {
            throw new OutOfRangeException(x, this.xval[0], this.xval[this.xval.length - 1]);
        }
        // search index for yval
        final int j = this.searchIndex(y, this.yval);
        if (j == -1) {
            // raise an exception if the y index does not exist
            throw new OutOfRangeException(y, this.yval[0], this.yval[this.yval.length - 1]);
        }

        final double xN = (x - this.xval[i]) / (this.xval[i + 1] - this.xval[i]);
        final double yN = (y - this.yval[j]) / (this.yval[j + 1] - this.yval[j]);

        return this.partialDerivatives[which][i][j].value(xN, yN);
    }

    /**
     * Compute all partial derivatives.
     */
    private void computePartialDerivatives() {
        // Initialization
        final int lastI = this.xval.length - 1;
        final int lastJ = this.yval.length - 1;
        this.partialDerivatives = new BivariateFunction[5][lastI][lastJ];

        // Loop to retrieve all components partial derivatives
        //
        for (int i = 0; i < lastI; i++) {
            for (int j = 0; j < lastJ; j++) {
                final BicubicSplineFunction f = this.splines[i][j];
                this.partialDerivatives[0][i][j] = f.partialDerivativeX();
                this.partialDerivatives[1][i][j] = f.partialDerivativeY();
                this.partialDerivatives[2][i][j] = f.partialDerivativeXX();
                this.partialDerivatives[3][i][j] = f.partialDerivativeYY();
                this.partialDerivatives[4][i][j] = f.partialDerivativeXY();
            }
        }
    }

    /**
     * @param c
     *        Coordinate.
     * @param val
     *        Coordinate samples.
     * @return the index in {@code val} corresponding to the interval
     *         containing {@code c}, or {@code -1} if {@code c} is out of the
     *         range defined by the boundary values of {@code val}.
     */
    private int searchIndex(final double c, final double[] val) {
        final int res;

        // if c is lower than the first value of val array the index does not exist
        if (c < val[0]) {
            res = -1;
        } else {

            // val length
            final int max = val.length;
            for (int i = 1; i < max; i++) {
                if (c <= val[i]) {
                    // return the index as soon as the val[i] is greater than c
                    return i - 1;
                }
            }

            res = -1;
        }
        return res;
    }

    /**
     * Compute the spline coefficients from the list of function values and
     * function partial derivatives values at the four corners of a grid
     * element. They must be specified in the following order:
     * <ul>
     * <li>f(0,0)</li>
     * <li>f(1,0)</li>
     * <li>f(0,1)</li>
     * <li>f(1,1)</li>
     * <li>f<sub>x</sub>(0,0)</li>
     * <li>f<sub>x</sub>(1,0)</li>
     * <li>f<sub>x</sub>(0,1)</li>
     * <li>f<sub>x</sub>(1,1)</li>
     * <li>f<sub>y</sub>(0,0)</li>
     * <li>f<sub>y</sub>(1,0)</li>
     * <li>f<sub>y</sub>(0,1)</li>
     * <li>f<sub>y</sub>(1,1)</li>
     * <li>f<sub>xy</sub>(0,0)</li>
     * <li>f<sub>xy</sub>(1,0)</li>
     * <li>f<sub>xy</sub>(0,1)</li>
     * <li>f<sub>xy</sub>(1,1)</li>
     * </ul>
     * where the subscripts indicate the partial derivative with respect to
     * the corresponding variable(s).
     * 
     * @param beta
     *        List of function values and function partial derivatives
     *        values.
     * @return the spline coefficients.
     */
    private double[] computeSplineCoefficients(final double[] beta) {
        // initiliaze a array
        final double[] a = new double[NB_COEFS];

        // loop on the coefficients number
        for (int i = 0; i < NB_COEFS; i++) {
            double result = 0;
            final double[] row = AINV[i];
            for (int j = 0; j < NB_COEFS; j++) {
                result += row[j] * beta[j];
            }
            a[i] = result;
        }
        // return the spline coefficients
        return a;
    }
}
