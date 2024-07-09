/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.fitting.PolynomialFitter;
import fr.cnes.sirius.patrius.math.optim.SimpleVectorValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.GaussNewtonOptimizer;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Generates a bicubic interpolation function.
 * Prior to generating the interpolating function, the input is smoothed using
 * polynomial fitting.
 * 
 * @version $Id: SmoothingPolynomialBicubicSplineInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class SmoothingPolynomialBicubicSplineInterpolator
    extends BicubicSplineInterpolator {
    /** Fitter for x. */
    private final PolynomialFitter xFitter;
    /** Degree of the fitting polynomial. */
    private final int xDegree;
    /** Fitter for y. */
    private final PolynomialFitter yFitter;
    /** Degree of the fitting polynomial. */
    private final int yDegree;

    /**
     * Default constructor. The degree of the fitting polynomials is set to 3.
     */
    public SmoothingPolynomialBicubicSplineInterpolator() {
        this(3);
    }

    /**
     * @param degree
     *        Degree of the polynomial fitting functions.
     */
    public SmoothingPolynomialBicubicSplineInterpolator(final int degree) {
        this(degree, degree);
    }

    /**
     * @param xDegreeIn
     *        Degree of the polynomial fitting functions along the
     *        x-dimension.
     * @param yDegreeIn
     *        Degree of the polynomial fitting functions along the
     *        y-dimension.
     */
    public SmoothingPolynomialBicubicSplineInterpolator(final int xDegreeIn,
        final int yDegreeIn) {
        super();
        if (xDegreeIn < 0) {
            throw new NotPositiveException(xDegreeIn);
        }
        if (yDegreeIn < 0) {
            throw new NotPositiveException(yDegreeIn);
        }
        this.xDegree = xDegreeIn;
        this.yDegree = yDegreeIn;

        final double safeFactor = 1e2;
        final SimpleVectorValueChecker checker = new SimpleVectorValueChecker(safeFactor * Precision.EPSILON,
            safeFactor * Precision.SAFE_MIN);
        this.xFitter = new PolynomialFitter(new GaussNewtonOptimizer(false, checker));
        this.yFitter = new PolynomialFitter(new GaussNewtonOptimizer(false, checker));
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public BicubicSplineInterpolatingFunction interpolate(final double[] xval,
                                                          final double[] yval,
                                                          final double[][] fval) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (xval.length == 0 || yval.length == 0 || fval.length == 0) {
            throw new NoDataException();
        }
        if (xval.length != fval.length) {
            throw new DimensionMismatchException(xval.length, fval.length);
        }

        final int xLen = xval.length;
        final int yLen = yval.length;

        for (int i = 0; i < xLen; i++) {
            if (fval[i].length != yLen) {
                throw new DimensionMismatchException(fval[i].length, yLen);
            }
        }

        MathArrays.checkOrder(xval);
        MathArrays.checkOrder(yval);

        // For each line y[j] (0 <= j < yLen), construct a polynomial, with
        // respect to variable x, fitting array fval[][j]
        final PolynomialFunction[] yPolyX = new PolynomialFunction[yLen];
        for (int j = 0; j < yLen; j++) {
            this.xFitter.clearObservations();
            for (int i = 0; i < xLen; i++) {
                this.xFitter.addObservedPoint(1, xval[i], fval[i][j]);
            }

            // Initial guess for the fit is zero for each coefficients (of which
            // there are "xDegree" + 1).
            yPolyX[j] = new PolynomialFunction(this.xFitter.fit(new double[this.xDegree + 1]));
        }

        // For every knot (xval[i], yval[j]) of the grid, calculate corrected
        // values fval_1
        final double[][] fval1 = new double[xLen][yLen];
        for (int j = 0; j < yLen; j++) {
            final PolynomialFunction f = yPolyX[j];
            for (int i = 0; i < xLen; i++) {
                fval1[i][j] = f.value(xval[i]);
            }
        }

        // For each line x[i] (0 <= i < xLen), construct a polynomial, with
        // respect to variable y, fitting array fval_1[i][]
        final PolynomialFunction[] xPolyY = new PolynomialFunction[xLen];
        for (int i = 0; i < xLen; i++) {
            this.yFitter.clearObservations();
            for (int j = 0; j < yLen; j++) {
                this.yFitter.addObservedPoint(1, yval[j], fval1[i][j]);
            }

            // Initial guess for the fit is zero for each coefficients (of which
            // there are "yDegree" + 1).
            xPolyY[i] = new PolynomialFunction(this.yFitter.fit(new double[this.yDegree + 1]));
        }

        // For every knot (xval[i], yval[j]) of the grid, calculate corrected
        // values fval_2
        final double[][] fval2 = new double[xLen][yLen];
        for (int i = 0; i < xLen; i++) {
            final PolynomialFunction f = xPolyY[i];
            for (int j = 0; j < yLen; j++) {
                fval2[i][j] = f.value(yval[j]);
            }
        }

        return super.interpolate(xval, yval, fval2);
    }
}
