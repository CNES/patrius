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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <a href="http://en.wikipedia.org/wiki/Gaussian_function">
 * Gaussian</a> function.
 * 
 * @since 3.0
 * @version $Id: Gaussian.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Gaussian implements UnivariateDifferentiableFunction {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Mean. */
    private final double mean;
    /** Inverse of the standard deviation. */
    private final double is;
    /** Inverse of twice the square of the standard deviation. */
    private final double i2s2;
    /** Normalization factor. */
    private final double norm;

    /**
     * Gaussian with given normalization factor, mean and standard deviation.
     * 
     * @param normIn
     *        Normalization factor.
     * @param meanIn
     *        Mean.
     * @param sigma
     *        Standard deviation.
     * @throws NotStrictlyPositiveException
     *         if {@code sigma <= 0}.
     */
    public Gaussian(final double normIn,
        final double meanIn,
        final double sigma) {
        if (sigma <= 0) {
            throw new NotStrictlyPositiveException(sigma);
        }

        this.norm = normIn;
        this.mean = meanIn;
        this.is = 1 / sigma;
        this.i2s2 = HALF * this.is * this.is;
    }

    /**
     * Normalized gaussian with given mean and standard deviation.
     * 
     * @param meanIn
     *        Mean.
     * @param sigma
     *        Standard deviation.
     * @throws NotStrictlyPositiveException
     *         if {@code sigma <= 0}.
     */
    public Gaussian(final double meanIn,
        final double sigma) {
        this(1 / (sigma * MathLib.sqrt(2 * Math.PI)), meanIn, sigma);
    }

    /**
     * Normalized gaussian with zero mean and unit standard deviation.
     */
    public Gaussian() {
        this(0, 1);
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        return value(x - this.mean, this.norm, this.i2s2);
    }

    /**
     * @param xMinusMean
     *        {@code x - mean}.
     * @param norm
     *        Normalization factor.
     * @param i2s2
     *        Inverse of twice the square of the standard deviation.
     * @return the value of the Gaussian at {@code x}.
     */
    private static double value(final double xMinusMean,
                                final double norm,
                                final double i2s2) {
        return norm * MathLib.exp(-xMinusMean * xMinusMean * i2s2);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {

        final double u = this.is * (t.getValue() - this.mean);
        final double[] f = new double[t.getOrder() + 1];

        // the nth order derivative of the Gaussian has the form:
        // dn(g(x)/dxn = (norm / s^n) P_n(u) exp(-u^2/2) with u=(x-m)/s
        // where P_n(u) is a degree n polynomial with same parity as n
        // P_0(u) = 1, P_1(u) = -u, P_2(u) = u^2 - 1, P_3(u) = -u^3 + 3 u...
        // the general recurrence relation for P_n is:
        // P_n(u) = P_(n-1)'(u) - u P_(n-1)(u)
        // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
        final double[] p = new double[f.length];
        p[0] = 1;
        final double u2 = u * u;
        double coeff = this.norm * MathLib.exp(-HALF * u2);
        if (coeff <= Precision.SAFE_MIN) {
            Arrays.fill(f, 0.0);
        } else {
            f[0] = coeff;
            for (int n = 1; n < f.length; ++n) {

                // update and evaluate polynomial P_n(x)
                double v = 0;
                p[n] = -p[n - 1];
                for (int k = n; k >= 0; k -= 2) {
                    v = v * u2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] - p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 1) {
                    v *= u;
                }

                coeff *= this.is;
                f[n] = coeff * v;

            }
        }

        return t.compose(f);

    }

    /**
     * Parametric function where the input array contains the parameters of
     * the Gaussian, ordered as follows:
     * <ul>
     * <li>Norm</li>
     * <li>Mean</li>
     * <li>Standard deviation</li>
     * </ul>
     */
    public static class Parametric implements ParametricUnivariateFunction {
        /**
         * Computes the value of the Gaussian at {@code x}.
         * 
         * @param x
         *        Value for which the function must be computed.
         * @param param
         *        Values of norm, mean and standard deviation.
         * @return the value of the function.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         * @throws NotStrictlyPositiveException
         *         if {@code param[2]} is negative.
         */
        @Override
        public double value(final double x, final double... param) {
            this.validateParameters(param);

            final double diff = x - param[1];
            final double i2s2 = 1 / (2 * param[2] * param[2]);
            return Gaussian.value(diff, param[0], i2s2);
        }

        /**
         * Computes the value of the gradient at {@code x}.
         * The components of the gradient vector are the partial
         * derivatives of the function with respect to each of the <em>parameters</em> (norm, mean and standard
         * deviation).
         * 
         * @param x
         *        Value at which the gradient must be computed.
         * @param param
         *        Values of norm, mean and standard deviation.
         * @return the gradient vector at {@code x}.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         * @throws NotStrictlyPositiveException
         *         if {@code param[2]} is negative.
         */
        @Override
        public double[] gradient(final double x, final double... param) {
            this.validateParameters(param);

            final double norm = param[0];
            final double diff = x - param[1];
            final double sigma = param[2];
            final double i2s2 = 1 / (2 * sigma * sigma);

            final double n = Gaussian.value(diff, 1, i2s2);
            final double m = norm * n * 2 * i2s2 * diff;
            final double s = m * diff / sigma;

            return new double[] { n, m, s };
        }

        /**
         * Validates parameters to ensure they are appropriate for the evaluation of
         * the {@link #value(double,double[])} and {@link #gradient(double,double[])} methods.
         * 
         * @param param
         *        Values of norm, mean and standard deviation.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         * @throws NotStrictlyPositiveException
         *         if {@code param[2]} is negative.
         */
        private void validateParameters(final double[] param) {
            if (param == null) {
                throw new NullArgumentException();
            }
            if (param.length != 3) {
                throw new DimensionMismatchException(param.length, 3);
            }
            if (param[2] <= 0) {
                throw new NotStrictlyPositiveException(param[2]);
            }
        }
    }

}
