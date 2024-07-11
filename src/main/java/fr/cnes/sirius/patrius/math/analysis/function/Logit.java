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
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * <a href="http://en.wikipedia.org/wiki/Logit">
 * Logit</a> function.
 * It is the inverse of the {@link Sigmoid sigmoid} function.
 * 
 * @since 3.0
 * @version $Id: Logit.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Logit implements UnivariateDifferentiableFunction {
    /** Lower bound. */
    private final double lo;
    /** Higher bound. */
    private final double hi;

    /**
     * Usual logit function, where the lower bound is 0 and the higher
     * bound is 1.
     */
    public Logit() {
        this(0, 1);
    }

    /**
     * Logit function.
     * 
     * @param loIn
     *        Lower bound of the function domain.
     * @param hiIn
     *        Higher bound of the function domain.
     */
    public Logit(final double loIn,
        final double hiIn) {
        this.lo = loIn;
        this.hi = hiIn;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        return value(x, this.lo, this.hi);
    }

    /**
     * @param x
     *        Value at which to compute the logit.
     * @param lo
     *        Lower bound.
     * @param hi
     *        Higher bound.
     * @return the value of the logit function at {@code x}.
     * @throws OutOfRangeException
     *         if {@code x < lo} or {@code x > hi}.
     */
    private static double value(final double x,
                                final double lo,
                                final double hi) {
        if (x < lo || x > hi) {
            throw new OutOfRangeException(x, lo, hi);
        }
        return MathLib.log((x - lo) / (hi - x));
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     * @exception OutOfRangeException
     *            if parameter is outside of function domain
     */
    @Override
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public DerivativeStructure value(final DerivativeStructure t) {
        final double x = t.getValue();
        if (x < this.lo || x > this.hi) {
            throw new OutOfRangeException(x, this.lo, this.hi);
        }
        final double[] f = new double[t.getOrder() + 1];

        // function value
        f[0] = MathLib.log((x - this.lo) / (this.hi - x));

        if (Double.isInfinite(f[0])) {

            if (f.length > 1) {
                f[1] = Double.POSITIVE_INFINITY;
            }
            // fill the array with infinities
            // (for x close to lo the signs will flip between -inf and +inf,
            // for x close to hi the signs will always be +inf)
            // this is probably overkill, since the call to compose at the end
            // of the method will transform most infinities into NaN ...
            for (int i = 2; i < f.length; ++i) {
                f[i] = f[i - 2];
            }

        } else {

            // function derivatives
            final double invL = 1.0 / (x - this.lo);
            double xL = invL;
            final double invH = 1.0 / (this.hi - x);
            double xH = invH;
            for (int i = 1; i < f.length; ++i) {
                f[i] = xL + xH;
                xL *= -i * invL;
                xH *= i * invH;
            }
        }

        return t.compose(f);
    }

    /**
     * Parametric function where the input array contains the parameters of
     * the logit function, ordered as follows:
     * <ul>
     * <li>Lower bound</li>
     * <li>Higher bound</li>
     * </ul>
     */
    public static class Parametric implements ParametricUnivariateFunction {
        /**
         * Computes the value of the logit at {@code x}.
         * 
         * @param x
         *        Value for which the function must be computed.
         * @param param
         *        Values of lower bound and higher bounds.
         * @return the value of the function.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 2.
         */
        @Override
        public double value(final double x, final double... param) {
            this.validateParameters(param);
            return Logit.value(x, param[0], param[1]);
        }

        /**
         * Computes the value of the gradient at {@code x}.
         * The components of the gradient vector are the partial
         * derivatives of the function with respect to each of the <em>parameters</em> (lower bound and higher bound).
         * 
         * @param x
         *        Value at which the gradient must be computed.
         * @param param
         *        Values for lower and higher bounds.
         * @return the gradient vector at {@code x}.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 2.
         */
        @Override
        public double[] gradient(final double x, final double... param) {
            this.validateParameters(param);

            final double lo = param[0];
            final double hi = param[1];

            return new double[] { 1 / (lo - x), 1 / (hi - x) };
        }

        /**
         * Validates parameters to ensure they are appropriate for the evaluation of
         * the {@link #value(double,double[])} and {@link #gradient(double,double[])} methods.
         * 
         * @param param
         *        Values for lower and higher bounds.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 2.
         */
        private void validateParameters(final double[] param) {
            if (param == null) {
                throw new NullArgumentException();
            }
            if (param.length != 2) {
                throw new DimensionMismatchException(param.length, 2);
            }
        }
    }
}
