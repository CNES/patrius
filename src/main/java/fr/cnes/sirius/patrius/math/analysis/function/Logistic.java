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

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * <a href="http://en.wikipedia.org/wiki/Generalised_logistic_function">
 * Generalised logistic</a> function.
 * 
 * @since 3.0
 * @version $Id: Logistic.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Logistic implements UnivariateDifferentiableFunction {
    /** Lower asymptote. */
    private final double a;
    /** Upper asymptote. */
    private final double k;
    /** Growth rate. */
    private final double b;
    /** Parameter that affects near which asymptote maximum growth occurs. */
    private final double oneOverN;
    /** Parameter that affects the position of the curve along the ordinate axis. */
    private final double q;
    /** Abscissa of maximum growth. */
    private final double m;

    /**
     * @param kIn
     *        If {@code b > 0}, value of the function for x going towards +&infin;.
     *        If {@code b < 0}, value of the function for x going towards -&infin;.
     * @param mIn
     *        Abscissa of maximum growth.
     * @param bIn
     *        Growth rate.
     * @param qIn
     *        Parameter that affects the position of the curve along the
     *        ordinate axis.
     * @param aIn
     *        If {@code b > 0}, value of the function for x going towards -&infin;.
     *        If {@code b < 0}, value of the function for x going towards +&infin;.
     * @param n
     *        Parameter that affects near which asymptote the maximum
     *        growth occurs.
     * @throws NotStrictlyPositiveException
     *         if {@code n <= 0}.
     */
    public Logistic(final double kIn,
        final double mIn,
        final double bIn,
        final double qIn,
        final double aIn,
        final double n) {
        if (n <= 0) {
            throw new NotStrictlyPositiveException(n);
        }

        this.k = kIn;
        this.m = mIn;
        this.b = bIn;
        this.q = qIn;
        this.a = aIn;
        this.oneOverN = 1 / n;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        return value(this.m - x, this.k, this.b, this.q, this.a, this.oneOverN);
    }

    /**
     * @param mMinusX
     *        {@code m - x}.
     * @param k
     *        {@code k}.
     * @param b
     *        {@code b}.
     * @param q
     *        {@code q}.
     * @param a
     *        {@code a}.
     * @param oneOverN
     *        {@code 1 / n}.
     * @return the value of the function.
     */
    private static double value(final double mMinusX,
                                final double k,
                                final double b,
                                final double q,
                                final double a,
                                final double oneOverN) {
        return a + (k - a) / MathLib.pow(1 + q * MathLib.exp(b * mMinusX), oneOverN);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        return t.negate().add(this.m).multiply(this.b).exp().multiply(this.q).add(1).pow(this.oneOverN).reciprocal()
            .multiply(this.k - this.a).add(this.a);
    }

    /**
     * Parametric function where the input array contains the parameters of
     * the logit function, ordered as follows:
     * <ul>
     * <li>Lower asymptote</li>
     * <li>Higher asymptote</li>
     * </ul>
     */
    public static class Parametric implements ParametricUnivariateFunction {
        /**
         * Computes the value of the sigmoid at {@code x}.
         * 
         * @param x
         *        Value for which the function must be computed.
         * @param param
         *        Values for {@code k}, {@code m}, {@code b}, {@code q}, {@code a} and {@code n}.
         * @return the value of the function.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 6.
         * @throws NotStrictlyPositiveException
         *         if {@code param[5] <= 0}.
         */
        @Override
        public double value(final double x, final double... param) {
            this.validateParameters(param);
            return Logistic.value(param[1] - x, param[0],
                param[2], param[3],
                param[4], 1 / param[5]);
        }

        /**
         * Computes the value of the gradient at {@code x}.
         * The components of the gradient vector are the partial
         * derivatives of the function with respect to each of the <em>parameters</em>.
         * 
         * @param x
         *        Value at which the gradient must be computed.
         * @param param
         *        Values for {@code k}, {@code m}, {@code b}, {@code q}, {@code a} and {@code n}.
         * @return the gradient vector at {@code x}.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 6.
         * @throws NotStrictlyPositiveException
         *         if {@code param[5] <= 0}.
         */
        @Override
        public double[] gradient(final double x, final double... param) {
            // Check
            this.validateParameters(param);

            final double b = param[2];
            final double q = param[3];

            // Intermediate variables
            final double mMinusX = param[1] - x;
            final double oneOverN = 1 / param[5];
            final double exp = MathLib.exp(b * mMinusX);
            final double qExp = q * exp;
            final double qExp1 = qExp + 1;
            final double factor1 = (param[0] - param[4]) * oneOverN / MathLib.pow(qExp1, oneOverN);
            final double factor2 = -factor1 / qExp1;

            // Components of the gradient.
            final double gk = Logistic.value(mMinusX, 1, b, q, 0, oneOverN);
            final double gm = factor2 * b * qExp;
            final double gb = factor2 * mMinusX * qExp;
            final double gq = factor2 * exp;
            final double ga = Logistic.value(mMinusX, 0, b, q, 1, oneOverN);
            final double gn = factor1 * Math.log(qExp1) * oneOverN;

            // Return result
            return new double[] { gk, gm, gb, gq, ga, gn };
        }

        /**
         * Validates parameters to ensure they are appropriate for the evaluation of
         * the {@link #value(double,double[])} and {@link #gradient(double,double[])} methods.
         * 
         * @param param
         *        Values for {@code k}, {@code m}, {@code b}, {@code q}, {@code a} and {@code n}.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 6.
         * @throws NotStrictlyPositiveException
         *         if {@code param[5] <= 0}.
         */
        private void validateParameters(final double[] param) {
            if (param == null) {
                throw new NullArgumentException();
            }
            if (param.length != 6) {
                throw new DimensionMismatchException(param.length, 6);
            }
            if (param[5] <= 0) {
                throw new NotStrictlyPositiveException(param[5]);
            }
        }
    }
}
