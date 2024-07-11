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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.gauss.GaussIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.gauss.GaussIntegratorFactory;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This algorithm divides the integration interval into equally-sized
 * sub-interval and on each of them performs a
 * <a href="http://mathworld.wolfram.com/Legendre-GaussQuadrature.html">
 * Legendre-Gauss</a> quadrature.
 * 
 * @version $Id: IterativeLegendreGaussIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */

public class IterativeLegendreGaussIntegrator
    extends BaseAbstractUnivariateIntegrator {

     /** Serializable UID. */
    private static final long serialVersionUID = 5062867174492005861L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Factory that computes the points and weights. */
    private static final GaussIntegratorFactory FACTORY = new GaussIntegratorFactory();

    /** Number of integration points (per interval). */
    private final int numberOfPoints;

    /**
     * Builds an integrator with given accuracies and iterations counts.
     * 
     * @param n
     *        Number of integration points.
     * @param relativeAccuracy
     *        Relative accuracy of the result.
     * @param absoluteAccuracy
     *        Absolute accuracy of the result.
     * @param minimalIterationCount
     *        Minimum number of iterations.
     * @param maximalIterationCount
     *        Maximum number of iterations.
     * @throws NotStrictlyPositiveException
     *         if minimal number of iterations
     *         is not strictly positive.
     * @throws NumberIsTooSmallException
     *         if maximal number of iterations
     *         is smaller than or equal to the minimal number of iterations.
     */
    public IterativeLegendreGaussIntegrator(final int n,
                                            final double relativeAccuracy,
                                            final double absoluteAccuracy,
                                            final int minimalIterationCount,
                                            final int maximalIterationCount) {
        super(relativeAccuracy, absoluteAccuracy, minimalIterationCount, maximalIterationCount);
        this.numberOfPoints = n;
    }

    /**
     * Builds an integrator with given accuracies.
     * 
     * @param n
     *        Number of integration points.
     * @param relativeAccuracy
     *        Relative accuracy of the result.
     * @param absoluteAccuracy
     *        Absolute accuracy of the result.
     */
    public IterativeLegendreGaussIntegrator(final int n,
                                            final double relativeAccuracy,
                                            final double absoluteAccuracy) {
        this(n, relativeAccuracy, absoluteAccuracy,
                DEFAULT_MIN_ITERATIONS_COUNT, DEFAULT_MAX_ITERATIONS_COUNT);
    }

    /**
     * Builds an integrator with given iteration counts.
     * 
     * @param n
     *        Number of integration points.
     * @param minimalIterationCount
     *        Minimum number of iterations.
     * @param maximalIterationCount
     *        Maximum number of iterations.
     * @throws NotStrictlyPositiveException
     *         if minimal number of iterations
     *         is not strictly positive.
     * @throws NumberIsTooSmallException
     *         if maximal number of iterations
     *         is smaller than or equal to the minimal number of iterations.
     */
    public IterativeLegendreGaussIntegrator(final int n,
                                            final int minimalIterationCount,
                                            final int maximalIterationCount) {
        this(n, DEFAULT_RELATIVE_ACCURACY, DEFAULT_ABSOLUTE_ACCURACY,
                minimalIterationCount, maximalIterationCount);
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() {
        // Compute first estimate with a single step.
        double oldt = this.stage(1);

        int n = 2;
        while (true) {
            // Improve integral with a larger number of steps.
            final double t = this.stage(n);

            // Estimate the error.
            final double delta = MathLib.abs(t - oldt);
            final double limit =
                MathLib.max(this.getAbsoluteAccuracy(),
                    this.getRelativeAccuracy() * (MathLib.abs(oldt) + MathLib.abs(t)) * HALF);

            // check convergence
            if (this.iterations.getCount() + 1 >= this.getMinimalIterationCount() &&
                    delta <= limit) {
                return t;
            }

            // Prepare next iteration.
            final double ratio = MathLib.min(4, MathLib.pow(delta / limit, HALF / this.numberOfPoints));
            n = MathLib.max((int) (ratio * n), n + 1);
            oldt = t;
            this.iterations.incrementCount();
        }
    }

    /**
     * Compute the n-th stage integral.
     * 
     * @param n
     *        Number of steps.
     * @return the value of n-th stage integral.
     * @throws TooManyEvaluationsException
     *         if the maximum number of evaluations
     *         is exceeded.
     */
    private double stage(final int n) {
        // Function to be integrated is stored in the base class.
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7892461132642925625L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return IterativeLegendreGaussIntegrator.this.computeObjectiveValue(x);
            }
        };
        // min value
        final double min = this.getMin();
        // max value
        final double max = this.getMax();
        // step value
        final double step = (max - min) / n;

        double sum = 0;
        for (int i = 0; i < n; i++) {
            // Integrate over each sub-interval [a, b].
            final double a = min + i * step;
            final double b = a + step;
            final GaussIntegrator g = FACTORY.legendreHighPrecision(this.numberOfPoints, a, b);
            sum += g.integrate(f);
        }

        return sum;
    }
}
