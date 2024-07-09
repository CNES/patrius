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
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialSplineFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements a linear function for interpolation of real univariate functions.
 * 
 * @version $Id: LinearInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class LinearInterpolator implements UnivariateInterpolator {
    /**
     * Computes a linear interpolating function for the data set.
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
     *         than 2.
     */
    @Override
    public PolynomialSplineFunction interpolate(final double[] x, final double[] y) {
        if (x.length != y.length) {
            throw new DimensionMismatchException(x.length, y.length);
        }

        if (x.length < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.NUMBER_OF_POINTS,
                x.length, 2, true);
        }

        // Number of intervals. The number of data points is n + 1.
        final int n = x.length - 1;

        // Check
        MathArrays.checkOrder(x);

        // Slope of the lines between the datapoints.
        final double[] m = new double[n];
        for (int i = 0; i < n; i++) {
            m[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
        }

        final PolynomialFunction[] polynomials = new PolynomialFunction[n];
        final double[] coefficients = new double[2];
        for (int i = 0; i < n; i++) {
            coefficients[0] = y[i];
            coefficients[1] = m[i];
            polynomials[i] = new PolynomialFunction(coefficients);
        }

        // Return PolynomialSplineFunction
        return new PolynomialSplineFunction(x, polynomials);
    }
}
