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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionNewtonForm;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Implements the <a href="
 * http://mathworld.wolfram.com/NewtonsDividedDifferenceInterpolationFormula.html">
 * Divided Difference Algorithm</a> for interpolation of real univariate
 * functions. For reference, see <b>Introduction to Numerical Analysis</b>,
 * ISBN 038795452X, chapter 2.
 * <p>
 * The actual code of Neville's evaluation is in PolynomialFunctionLagrangeForm, this class provides an easy-to-use
 * interface to it.
 * </p>
 * 
 * @version $Id: DividedDifferenceInterpolator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class DividedDifferenceInterpolator
    implements UnivariateInterpolator, Serializable {
     /** Serializable UID. */
    private static final long serialVersionUID = 107049519551235069L;

    /**
     * Compute an interpolating function for the dataset.
     * 
     * @param x
     *        Interpolating points array.
     * @param y
     *        Interpolating values array.
     * @return a function which interpolates the dataset.
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws NonMonotonicSequenceException
     *         if {@code x} is not sorted in
     *         strictly increasing order.
     */
    @Override
    public PolynomialFunctionNewtonForm interpolate(final double[] x, final double[] y) {
        /**
         * a[] and c[] are defined in the general formula of Newton form:
         * p(x) = a[0] + a[1](x-c[0]) + a[2](x-c[0])(x-c[1]) + ... +
         * a[n](x-c[0])(x-c[1])...(x-c[n-1])
         */
        PolynomialFunctionLagrangeForm.verifyInterpolationArray(x, y, true);

        /**
         * When used for interpolation, the Newton form formula becomes
         * p(x) = f[x0] + f[x0,x1](x-x0) + f[x0,x1,x2](x-x0)(x-x1) + ... +
         * f[x0,x1,...,x[n-1]](x-x0)(x-x1)...(x-x[n-2])
         * Therefore, a[k] = f[x0,x1,...,xk], c[k] = x[k].
         * <p>
         * Note x[], y[], a[] have the same length but c[]'s size is one less.
         * </p>
         */
        final double[] c = new double[x.length - 1];
        System.arraycopy(x, 0, c, 0, c.length);

        final double[] a = computeDividedDifference(x, y);
        return new PolynomialFunctionNewtonForm(a, c);
    }

    /**
     * Return a copy of the divided difference array.
     * <p>
     * The divided difference array is defined recursively by
     * 
     * <pre>
     * f[x0] = f(x0)
     * f[x0,x1,...,xk] = (f[x1,...,xk] - f[x0,...,x[k-1]]) / (xk - x0)
     * </pre>
     * 
     * </p>
     * <p>
     * The computational complexity is O(N^2).
     * </p>
     * 
     * @param x
     *        Interpolating points array.
     * @param y
     *        Interpolating values array.
     * @return a fresh copy of the divided difference array.
     * @throws DimensionMismatchException
     *         if the array lengths are different.
     * @throws NumberIsTooSmallException
     *         if the number of points is less than 2.
     * @throws NonMonotonicSequenceException
     *         if {@code x} is not sorted in strictly increasing order.
     */
    protected static double[] computeDividedDifference(final double[] x, final double[] y) {
        
        //Check that the interpolation arrays are valid
        PolynomialFunctionLagrangeForm.verifyInterpolationArray(x, y, true);

        // initialization
        final double[] divdiff = y.clone();
        
        // x length
        final int n = x.length;
        final double[] a = new double[n];
        a[0] = divdiff[0];
        // loop on the x length
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n - i; j++) {
                final double denominator = x[j + i] - x[j];
                divdiff[j] = (divdiff[j + 1] - divdiff[j]) / denominator;
            }
            a[i] = divdiff[0];
        }

        return a;
    }
}
