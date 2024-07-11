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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialSplineFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computes a natural (also known as "free", "unclamped") cubic spline interpolation for the data set.
 * <p>
 * The {@link #interpolate(double[], double[])} method returns a {@link PolynomialSplineFunction} consisting of n cubic
 * polynomials, defined over the subintervals determined by the x values, x[0] < x[i] ... < x[n]. The x values are
 * referred to as "knot points."
 * </p>
 * <p>
 * The value of the PolynomialSplineFunction at a point x that is greater than or equal to the smallest knot point and
 * strictly less than the largest knot point is computed by finding the subinterval to which x belongs and computing the
 * value of the corresponding polynomial at <code>x - x[i] </code> where <code>i</code> is the index of the subinterval.
 * See {@link PolynomialSplineFunction} for more details.
 * </p>
 * <p>
 * The interpolating polynomials satisfy:
 * <ol>
 * <li>The value of the PolynomialSplineFunction at each of the input x values equals the corresponding y value.</li>
 * <li>Adjacent polynomials are equal through two derivatives at the knot points (i.e., adjacent polynomials "match up"
 * at the knot points, as do their first and second derivatives).</li>
 * </ol>
 * </p>
 * <p>
 * The cubic spline interpolation algorithm implemented is as described in R.L. Burden, J.D. Faires, <u>Numerical
 * Analysis</u>, 4th Ed., 1989, PWS-Kent, ISBN 0-53491-585-X, pp 126-131.
 * </p>
 * 
 * @version $Id: SplineInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SplineInterpolator implements UnivariateInterpolator {
    /**
     * Computes an interpolating function for the data set.
     * 
     * @param x
     *        the arguments for the interpolation points
     * @param y
     *        the values for the interpolation points
     * @return a function which interpolates the data set
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} have different sizes.
     * @throws NonMonotonicSequenceException
     *         if {@code x} is not sorted in
     *         strict increasing order.
     * @throws NumberIsTooSmallException
     *         if the size of {@code x} is smaller
     *         than 3.
     */
    @Override
    public PolynomialSplineFunction interpolate(final double[] x, final double[] y) {
        if (x.length != y.length) {
            throw new DimensionMismatchException(x.length, y.length);
        }

        if (x.length < 3) {
            throw new NumberIsTooSmallException(PatriusMessages.NUMBER_OF_POINTS,
                x.length, 3, true);
        }

        // Number of intervals. The number of data points is n + 1.
        final int n = x.length - 1;

        // Check
        MathArrays.checkOrder(x);

        // Differences between knot points
        final double[] h = new double[n];
        for (int i = 0; i < n; i++) {
            h[i] = x[i + 1] - x[i];
        }
        // initialize mu
        final double[] mu = new double[n];
        // initialize z
        final double[] z = new double[n + 1];
        mu[0] = 0d;
        z[0] = 0d;
        double g = 0;
        for (int i = 1; i < n; i++) {
            g = 2d * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1];
            mu[i] = h[i] / g;
            z[i] = (3d * (y[i + 1] * h[i - 1] - y[i] * (x[i + 1] - x[i - 1]) + y[i - 1] * h[i]) /
                    (h[i - 1] * h[i]) - h[i - 1] * z[i - 1]) / g;
        }

        // cubic spline coefficients -- b is linear, c quadratic, d is cubic (original y's are constants)
        // initialize b
        final double[] b = new double[n];
        // initialize c
        final double[] c = new double[n + 1];
        // initialize d
        final double[] d = new double[n];

        z[n] = 0d;
        c[n] = 0d;

        for (int j = n - 1; j >= 0; j--) {
            c[j] = z[j] - mu[j] * c[j + 1];
            b[j] = (y[j + 1] - y[j]) / h[j] - h[j] * (c[j + 1] + 2d * c[j]) / 3d;
            d[j] = (c[j + 1] - c[j]) / (3d * h[j]);
        }

        // Build polynomials
        final PolynomialFunction[] polynomials = new PolynomialFunction[n];
        final double[] coefficients = new double[4];
        for (int i = 0; i < n; i++) {
            coefficients[0] = y[i];
            coefficients[1] = b[i];
            coefficients[2] = c[i];
            coefficients[3] = d[i];
            polynomials[i] = new PolynomialFunction(coefficients);
        }

        // Return PolynomialSplineFunction
        //
        return new PolynomialSplineFunction(x, polynomials);
    }
}
