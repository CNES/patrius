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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * 2D-spline function.
 * 
 * @version $Id: BicubicSplineInterpolatingFunction.java 18108 2017-10-04 06:45:27Z bignon $
 */
class BicubicSplineFunction implements BivariateFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 4282875069317418851L;

    /** Number of points. */
    private static final int N = 4;

    /** Coefficients */
    private final double[][] a;

    /** First partial derivative along x. */
    private BivariateFunction partialDerX;

    /** First partial derivative along y. */
    private BivariateFunction partialDerY;

    /** Second partial derivative along x. */
    private BivariateFunction partialDerXX;

    /** Second partial derivative along y. */
    private BivariateFunction partialDerYY;

    /** Second crossed partial derivative. */
    private BivariateFunction partialDerXY;

    /**
     * Simple constructor.
     * 
     * @param aIn
     *        Spline coefficients
     */
    public BicubicSplineFunction(final double[] aIn) {
        this.a = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                this.a[i][j] = aIn[i + N * j];
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double value(final double x, final double y) {
        if (x < 0 || x > 1) {
            throw new OutOfRangeException(x, 0, 1);
        }
        if (y < 0 || y > 1) {
            throw new OutOfRangeException(y, 0, 1);
        }
        // x square
        final double x2 = x * x;
        // x cube
        final double x3 = x2 * x;
        final double[] pX = { 1, x, x2, x3 };

        // y square
        final double y2 = y * y;
        // y cube
        final double y3 = y2 * y;
        final double[] pY = { 1, y, y2, y3 };

        return apply(pX, pY, this.a);
    }

    /**
     * Compute the value of the bicubic polynomial.
     * 
     * @param pX
     *        Powers of the x-coordinate.
     * @param pY
     *        Powers of the y-coordinate.
     * @param coeff
     *        Spline coefficients.
     * @return the interpolated value.
     */
    private static double apply(final double[] pX, final double[] pY, final double[][] coeff) {
        double result = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                result += coeff[i][j] * pX[i] * pY[j];
            }
        }

        return result;
    }

    /**
     * @return the partial derivative wrt {@code x}.
     */
    public BivariateFunction partialDerivativeX() {
        if (this.partialDerX == null) {
            this.computePartialDerivatives();
        }

        return this.partialDerX;
    }

    /**
     * @return the partial derivative wrt {@code y}.
     */
    public BivariateFunction partialDerivativeY() {
        if (this.partialDerY == null) {
            this.computePartialDerivatives();
        }

        return this.partialDerY;
    }

    /**
     * @return the second partial derivative wrt {@code x}.
     */
    public BivariateFunction partialDerivativeXX() {
        if (this.partialDerXX == null) {
            this.computePartialDerivatives();
        }

        return this.partialDerXX;
    }

    /**
     * @return the second partial derivative wrt {@code y}.
     */
    public BivariateFunction partialDerivativeYY() {
        if (this.partialDerYY == null) {
            this.computePartialDerivatives();
        }

        return this.partialDerYY;
    }

    /**
     * @return the second partial cross-derivative.
     */
    public BivariateFunction partialDerivativeXY() {
        if (this.partialDerXY == null) {
            this.computePartialDerivatives();
        }

        return this.partialDerXY;
    }

    /**
     * Compute all partial derivatives functions.
     */
    private void computePartialDerivatives() {
        // Initialization
        final double[][] aX = new double[N][N];
        final double[][] aY = new double[N][N];
        final double[][] aXX = new double[N][N];
        final double[][] aYY = new double[N][N];
        final double[][] aXY = new double[N][N];

        // Defined "a" coefficients
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                final double c = this.a[i][j];
                aX[i][j] = i * c;
                aY[i][j] = j * c;
                aXX[i][j] = (i - 1) * aX[i][j];
                aYY[i][j] = (j - 1) * aY[i][j];
                aXY[i][j] = j * aX[i][j];
            }
        }

        // Set the partial derivatives
        // Ax
        setPartialDerX(aX);
        // Ay
        setPartialDerY(aY);
        // Axx
        setPartialDerXX(aXX);
        // Ayy
        setPartialDerYY(aYY);
        // Azz
        setPartialDerXY(aXY);
    }

    /**
     * Setter for the X partial derivatives.
     * 
     * @param aX
     *        aX coefficients
     */
    private void setPartialDerX(final double[][] aX) {
        // X partial derivatives
        this.partialDerX = new BivariateFunction() {
            /** Serializable UID. */
            private static final long serialVersionUID = -7699992209267286840L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double y) {
                // Creation of x squared
                final double x2 = x * x;
                // Creation of pX
                final double[] pX = { 0, 1, x, x2 };
                // Creation of y squared
                final double y2 = y * y;
                // Creation of y to the third
                final double y3 = y2 * y;
                // Creation of pY
                final double[] pY = { 1, y, y2, y3 };

                // Return the value of the bicubic polynomial
                return BicubicSplineFunction.apply(pX, pY, aX);
            }
        };
    }

    /**
     * Setter for the Y partial derivatives.
     * 
     * @param aY
     *        aY coefficients
     */
    private void setPartialDerY(final double[][] aY) {
        // Y partial derivatives
        this.partialDerY = new BivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 91100278478684200L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double y) {
                // Creation of x squared
                final double x2 = x * x;
                // Creation of x to the third
                final double x3 = x2 * x;
                // Creation of pX
                final double[] pX = { 1, x, x2, x3 };
                // Creation of y squared
                final double y2 = y * y;
                // Creation of pY
                final double[] pY = { 0, 1, y, y2 };

                // Return the value of the bicubic polynomial
                return BicubicSplineFunction.apply(pX, pY, aY);
            }
        };
    }

    /**
     * Setter for the XX partial derivatives.
     * 
     * @param aXX
     *        aXX coefficients
     */
    private void setPartialDerXX(final double[][] aXX) {
        // XX partial derivatives
        this.partialDerXX = new BivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 9212997118230193155L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double y) {
                final double[] pX = { 0, 0, 1, x };

                final double y2 = y * y;
                final double y3 = y2 * y;
                final double[] pY = { 1, y, y2, y3 };

                return BicubicSplineFunction.apply(pX, pY, aXX);
            }
        };
    }

    /**
     * Setter for the YY partial derivatives.
     * 
     * @param aYY
     *        aYY coefficients
     */
    private void setPartialDerYY(final double[][] aYY) {
        // YY partial derivatives
        this.partialDerYY = new BivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3665672110311447420L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double x3 = x2 * x;
                final double[] pX = { 1, x, x2, x3 };

                final double[] pY = { 0, 0, 1, y };

                return BicubicSplineFunction.apply(pX, pY, aYY);
            }
        };
    }

    /**
     * Setter for the XY partial derivatives.
     * 
     * @param aXY
     *        aXY coefficients
     */
    private void setPartialDerXY(final double[][] aXY) {
        // XY partial derivatives
        this.partialDerXY = new BivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2019321671542157271L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x, final double y) {
                final double x2 = x * x;
                final double[] pX = { 0, 1, x, x2 };

                final double y2 = y * y;
                final double[] pY = { 0, 1, y, y2 };

                return BicubicSplineFunction.apply(pX, pY, aXY);
            }
        };
    }
}
