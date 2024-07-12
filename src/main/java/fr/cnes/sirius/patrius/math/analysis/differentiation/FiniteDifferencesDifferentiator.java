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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Univariate functions differentiator using finite differences.
 * <p>
 * This class creates some wrapper objects around regular {@link UnivariateFunction univariate functions} (or
 * {@link UnivariateVectorFunction univariate vector functions} or {@link UnivariateMatrixFunction univariate matrix
 * functions}). These wrapper objects compute derivatives in addition to function value.
 * </p>
 * <p>
 * The wrapper objects work by calling the underlying function on a sampling grid around the current point and
 * performing polynomial interpolation. A finite differences scheme with n points is theoretically able to compute
 * derivatives up to order n-1, but it is generally better to have a slight margin. The step size must also be small
 * enough in order for the polynomial approximation to be good in the current point neighborhood, but it should not be
 * too small because numerical instability appears quickly (there are several differences of close points). Choosing the
 * number of points and the step size is highly problem dependent.
 * </p>
 * <p>
 * As an example of good and bad settings, lets consider the quintic polynomial function
 * {@code f(x) = (x-1)*(x-0.5)*x*(x+0.5)*(x+1)}. Since it is a polynomial, finite differences with at least 6 points
 * should theoretically recover the exact same polynomial and hence compute accurate derivatives for any order. However,
 * due to numerical errors, we get the following results for a 7 points finite differences for abscissae in the [-10,
 * 10] range:
 * <ul>
 * <li>step size = 0.25, second order derivative error about 9.97e-10</li>
 * <li>step size = 0.25, fourth order derivative error about 5.43e-8</li>
 * <li>step size = 1.0e-6, second order derivative error about 148</li>
 * <li>step size = 1.0e-6, fourth order derivative error about 6.35e+14</li>
 * </ul>
 * This example shows that the small step size is really bad, even simply for second order derivative!
 * </p>
 *
 * @version $Id: FiniteDifferencesDifferentiator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class FiniteDifferencesDifferentiator implements UnivariateFunctionDifferentiator,
    UnivariateVectorFunctionDifferentiator, UnivariateMatrixFunctionDifferentiator {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Serializable UID. */
    private static final long serialVersionUID = 20120917L;

    /** Number of points to use. */
    private final int nbPoints;

    /** Step size. */
    private final double stepSize;

    /** Half sample span. */
    private final double halfSampleSpan;

    /** Lower bound for independent variable. */
    private final double tMin;

    /** Upper bound for independent variable. */
    private final double tMax;

    /**
     * Build a differentiator with number of points and step size when independent variable is unbounded.
     * <p>
     * Beware that wrong settings for the finite differences differentiator can lead to highly unstable and inaccurate
     * results, especially for high derivation orders. Using very small step sizes is often a <em>bad</em> idea.
     * </p>
     *
     * @param nbPointsIn
     *        number of points to use
     * @param stepSizeIn
     *        step size (gap between each point)
     * @exception NotPositiveException
     *            if {@code stepsize <= 0} (note that {@link NotPositiveException} extends
     *            {@link NumberIsTooSmallException})
     * @exception NumberIsTooSmallException
     *            {@code nbPoint <= 1}
     */
    public FiniteDifferencesDifferentiator(final int nbPointsIn, final double stepSizeIn) {
        this(nbPointsIn, stepSizeIn, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Build a differentiator with number of points and step size when independent variable is bounded.
     * <p>
     * When the independent variable is bounded (tLower &lt; t &lt; tUpper), the sampling points used for
     * differentiation will be adapted to ensure the constraint holds even near the boundaries. This means the sample
     * will not be centered anymore in these cases. At an extreme case, computing derivatives exactly at the lower bound
     * will lead the sample to be entirely on the right side of the derivation point.
     * </p>
     * <p>
     * Note that the boundaries are considered to be excluded for function evaluation.
     * </p>
     * <p>
     * Beware that wrong settings for the finite differences differentiator can lead to highly unstable and inaccurate
     * results, especially for high derivation orders. Using very small step sizes is often a <em>bad</em> idea.
     * </p>
     *
     * @param nbPointsIn
     *        number of points to use
     * @param stepSizeIn
     *        step size (gap between each point)
     * @param tLower
     *        lower bound for independent variable (may be {@code Double.NEGATIVE_INFINITY} if there are no lower
     *        bounds)
     * @param tUpper
     *        upper bound for independent variable (may be {@code Double.POSITIVE_INFINITY} if there are no upper
     *        bounds)
     * @exception NotPositiveException
     *            if {@code stepsize <= 0} (note that {@link NotPositiveException} extends
     *            {@link NumberIsTooSmallException})
     * @exception NumberIsTooSmallException
     *            {@code nbPoint <= 1}
     * @exception NumberIsTooLargeException
     *            {@code stepSize * (nbPoints - 1) >= tUpper - tLower}
     */
    public FiniteDifferencesDifferentiator(final int nbPointsIn, final double stepSizeIn,
                                           final double tLower, final double tUpper) {

        if (nbPointsIn <= 1) {
            throw new NumberIsTooSmallException(stepSizeIn, 1, false);
        }
        this.nbPoints = nbPointsIn;

        if (stepSizeIn <= 0) {
            throw new NotPositiveException(stepSizeIn);
        }
        this.stepSize = stepSizeIn;

        this.halfSampleSpan = HALF * stepSizeIn * (nbPointsIn - 1);
        if (2 * this.halfSampleSpan >= tUpper - tLower) {
            throw new NumberIsTooLargeException(2 * this.halfSampleSpan, tUpper - tLower, false);
        }
        final double safety = MathLib.ulp(this.halfSampleSpan);
        this.tMin = tLower + this.halfSampleSpan + safety;
        this.tMax = tUpper - this.halfSampleSpan - safety;

    }

    /**
     * Get the number of points to use.
     *
     * @return number of points to use
     */
    public int getNbPoints() {
        return this.nbPoints;
    }

    /**
     * Get the step size.
     *
     * @return step size
     */
    public double getStepSize() {
        return this.stepSize;
    }

    /**
     * Evaluate derivatives from a sample.
     * <p>
     * Evaluation is done using divided differences.
     * </p>
     *
     * @param t
     *        evaluation abscissa value and derivatives
     * @param t0
     *        first sample point abscissa
     * @param y
     *        function values sample {@code y[i] = f(t[i]) = f(t0 + i * stepSize)}
     * @return value and derivatives at {@code t}
     * @exception NumberIsTooLargeException
     *            if the requested derivation order
     *            is larger or equal to the number of points
     */
    private DerivativeStructure evaluate(final DerivativeStructure t, final double t0,
                                         final double[] y) {

        // create divided differences diagonal arrays
        final double[] top = new double[this.nbPoints];
        final double[] bottom = new double[this.nbPoints];

        for (int i = 0; i < this.nbPoints; ++i) {

            // update the bottom diagonal of the divided differences array
            bottom[i] = y[i];
            for (int j = 1; j <= i; ++j) {
                bottom[i - j] = (bottom[i - j + 1] - bottom[i - j]) / (j * this.stepSize);
            }

            // update the top diagonal of the divided differences array
            top[i] = bottom[0];

        }

        // evaluate interpolation polynomial (represented by top diagonal) at t
        final int order = t.getOrder();
        final int parameters = t.getFreeParameters();
        final double[] derivatives = t.getAllDerivatives();
        final double dt0 = t.getValue() - t0;
        DerivativeStructure interpolation = new DerivativeStructure(parameters, order, 0.0);
        DerivativeStructure monomial = null;
        for (int i = 0; i < this.nbPoints; ++i) {
            if (i == 0) {
                // start with monomial(t) = 1
                monomial = new DerivativeStructure(parameters, order, 1.0);
            } else {
                // monomial(t) = (t - t0) * (t - t1) * ... * (t - t(i-1))
                derivatives[0] = dt0 - (i - 1) * this.stepSize;
                final DerivativeStructure deltaX = new DerivativeStructure(parameters, order, derivatives);
                monomial = monomial.multiply(deltaX);
            }
            interpolation = interpolation.add(monomial.multiply(top[i]));
        }

        return interpolation;

    }

    /**
     * <p>
     * The returned object cannot compute derivatives to arbitrary orders. The value function will throw a
     * {@link NumberIsTooLargeException} if the requested derivation order is larger or equal to the number of points.
     * </p>
     */
    @Override
    public UnivariateDifferentiableFunction differentiate(final UnivariateFunction function) {
        return new UnivariateDifferentiableFunction(){

            /** Serializable UID. */
            private static final long serialVersionUID = 3201501436876014436L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return function.value(x);
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure value(final DerivativeStructure t) {

                // check we can achieve the requested derivation order with the sample
                if (t.getOrder() >= FiniteDifferencesDifferentiator.this.nbPoints) {
                    throw new NumberIsTooLargeException(t.getOrder(), FiniteDifferencesDifferentiator.this.nbPoints,
                        false);
                }

                // compute sample position, trying to be centered if possible
                final double t0 =
                    MathLib.max(MathLib.min(t.getValue(), FiniteDifferencesDifferentiator.this.tMax),
                        FiniteDifferencesDifferentiator.this.tMin)
                            - FiniteDifferencesDifferentiator.this.halfSampleSpan;

                // compute sample points
                final double[] y = new double[FiniteDifferencesDifferentiator.this.nbPoints];
                for (int i = 0; i < FiniteDifferencesDifferentiator.this.nbPoints; ++i) {
                    y[i] = function.value(t0 + i * FiniteDifferencesDifferentiator.this.stepSize);
                }

                // evaluate derivatives
                return FiniteDifferencesDifferentiator.this.evaluate(t, t0, y);
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned object cannot compute derivatives to arbitrary orders. The value function will throw a
     * {@link NumberIsTooLargeException} if the requested derivation order is larger or equal to the number of points.
     * </p>
     */
    @Override
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public UnivariateDifferentiableVectorFunction differentiate(final UnivariateVectorFunction function) {
        return new UnivariateDifferentiableVectorFunction(){

            /** {@inheritDoc} */
            @Override
            public double[] value(final double x) {
                return function.value(x);
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure[] value(final DerivativeStructure t) {

                // check we can achieve the requested derivation order with the sample
                // raise an exception otherwise
                if (t.getOrder() >= FiniteDifferencesDifferentiator.this.nbPoints) {
                    throw new NumberIsTooLargeException(t.getOrder(), FiniteDifferencesDifferentiator.this.nbPoints,
                        false);
                }

                // compute sample position, trying to be centered if possible
                final double t0 =
                    MathLib.max(MathLib.min(t.getValue(), FiniteDifferencesDifferentiator.this.tMax),
                        FiniteDifferencesDifferentiator.this.tMin)
                            - FiniteDifferencesDifferentiator.this.halfSampleSpan;

                // compute sample points
                double[][] y = null;
                // loop on the points numbers
                for (int i = 0; i < FiniteDifferencesDifferentiator.this.nbPoints; ++i) {
                    final double[] v = function.value(t0 + i * FiniteDifferencesDifferentiator.this.stepSize);
                    if (i == 0) {
                        y = new double[v.length][FiniteDifferencesDifferentiator.this.nbPoints];
                    }
                    for (int j = 0; j < v.length; ++j) {
                        y[j][i] = v[j];
                    }
                }

                // evaluate derivatives
                final DerivativeStructure[] value = new DerivativeStructure[y.length];
                for (int j = 0; j < value.length; ++j) {
                    value[j] = FiniteDifferencesDifferentiator.this.evaluate(t, t0, y[j]);
                }

                return value;

            }

            /** {@inheritDoc} */
            @Override
            public int getSize() {
                return function.getSize();
            }

        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned object cannot compute derivatives to arbitrary orders. The value function will throw a
     * {@link NumberIsTooLargeException} if the requested derivation order is larger or equal to the number of points.
     * </p>
     */
    @Override
    public UnivariateDifferentiableMatrixFunction differentiate(final UnivariateMatrixFunction function) {
        return new UnivariateDifferentiableMatrixFunction(){

            /** {@inheritDoc} */
            @Override
            public double[][] value(final double x) {
                return function.value(x);
            }

            /** {@inheritDoc} */
            @Override
            public DerivativeStructure[][] value(final DerivativeStructure t) {

                // check we can achieve the requested derivation order with the sample
                // raise an exception otherwise
                if (t.getOrder() >= FiniteDifferencesDifferentiator.this.nbPoints) {
                    throw new NumberIsTooLargeException(t.getOrder(), FiniteDifferencesDifferentiator.this.nbPoints,
                        false);
                }

                // compute sample position, trying to be centered if possible
                final double t0 =
                    MathLib.max(MathLib.min(t.getValue(), FiniteDifferencesDifferentiator.this.tMax),
                        FiniteDifferencesDifferentiator.this.tMin)
                            - FiniteDifferencesDifferentiator.this.halfSampleSpan;

                // compute sample points
                double[][][] y = null;
                // loop on the points number
                for (int i = 0; i < FiniteDifferencesDifferentiator.this.nbPoints; ++i) {
                    final double[][] v = function.value(t0 + i * FiniteDifferencesDifferentiator.this.stepSize);
                    if (i == 0) {
                        y = new double[v.length][v[0].length][FiniteDifferencesDifferentiator.this.nbPoints];
                    }
                    // loop on the v length
                    for (int j = 0; j < v.length; ++j) {
                        for (int k = 0; k < v[j].length; ++k) {
                            y[j][k][i] = v[j][k];
                        }
                    }
                }

                // evaluate derivatives
                final DerivativeStructure[][] value = new DerivativeStructure[y.length][y[0].length];
                // loop on the value length
                for (int j = 0; j < value.length; ++j) {
                    for (int k = 0; k < y[j].length; ++k) {
                        value[j][k] = FiniteDifferencesDifferentiator.this.evaluate(t, t0, y[j][k]);
                    }
                }

                return value;

            }

        };
    }

}
