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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements the <a href="http://mathworld.wolfram.com/TrapezoidalRule.html">
 * Trapezoid Rule</a> for integration of real univariate functions. For
 * reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X,
 * chapter 3.
 * <p>
 * The function should be integrable.
 * </p>
 * 
 * @version $Id: TrapezoidIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class TrapezoidIntegrator extends BaseAbstractUnivariateIntegrator {

    /** Maximum number of iterations for trapezoid. */
    public static final int TRAPEZOID_MAX_ITERATIONS_COUNT = 64;
    
     /** Serializable UID. */
    private static final long serialVersionUID = -3070310804345891361L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Intermediate result. */
    private double s;

    /**
     * Build a trapezoid integrator with given accuracies and iterations counts.
     * 
     * @param relativeAccuracy
     *        relative accuracy of the result
     * @param absoluteAccuracy
     *        absolute accuracy of the result
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #TRAPEZOID_MAX_ITERATIONS_COUNT}
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #TRAPEZOID_MAX_ITERATIONS_COUNT}
     */
    public TrapezoidIntegrator(final double relativeAccuracy,
        final double absoluteAccuracy,
        final int minimalIterationCount,
        final int maximalIterationCount) {
        super(relativeAccuracy, absoluteAccuracy, minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > TRAPEZOID_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                TRAPEZOID_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Build a trapezoid integrator with given iteration counts.
     * 
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #TRAPEZOID_MAX_ITERATIONS_COUNT}
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #TRAPEZOID_MAX_ITERATIONS_COUNT}
     */
    public TrapezoidIntegrator(final int minimalIterationCount,
        final int maximalIterationCount) {
        super(minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > TRAPEZOID_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                TRAPEZOID_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Construct a trapezoid integrator with default settings.
     * (max iteration count set to {@link #TRAPEZOID_MAX_ITERATIONS_COUNT})
     */
    public TrapezoidIntegrator() {
        super(DEFAULT_MIN_ITERATIONS_COUNT, TRAPEZOID_MAX_ITERATIONS_COUNT);
    }

    /**
     * Compute the n-th stage integral of trapezoid rule. This function
     * should only be called by API <code>integrate()</code> in the package.
     * To save time it does not verify arguments - caller does.
     * <p>
     * The interval is divided equally into 2^n sections rather than an arbitrary m sections because this configuration
     * can best utilize the already computed values.
     * </p>
     * 
     * @param baseIntegrator
     *        integrator holding integration parameters
     * @param n
     *        the stage of 1/2 refinement, n = 0 is no refinement
     * @return the value of n-th stage integral
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations
     *         is exceeded.
     */
    public double stage(final BaseAbstractUnivariateIntegrator baseIntegrator, final int n) {

        if (n == 0) {
            final double max = baseIntegrator.getMax();
            final double min = baseIntegrator.getMin();
            this.s = HALF * (max - min) *
                (baseIntegrator.computeObjectiveValue(min) +
                baseIntegrator.computeObjectiveValue(max));
            return this.s;
        } else {
            // number of new points in this stage
            final long np = 1L << (n - 1);
            double sum = 0;
            final double max = baseIntegrator.getMax();
            final double min = baseIntegrator.getMin();
            // spacing between adjacent new points
            final double spacing = (max - min) / np;
            // the first new point
            double x = min + HALF * spacing;
            for (long i = 0; i < np; i++) {
                sum += baseIntegrator.computeObjectiveValue(x);
                x += spacing;
            }
            // add the new sum to previously calculated result
            this.s = HALF * (this.s + sum * spacing);
            return this.s;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() {

        double oldt = this.stage(this, 0);
        this.iterations.incrementCount();
        while (true) {
            // Loop until accuracy is reached
            final int i = this.iterations.getCount();
            final double t = this.stage(this, i);
            if (i >= this.getMinimalIterationCount()) {
                final double delta = MathLib.abs(t - oldt);
                final double rLimit =
                    this.getRelativeAccuracy() * (MathLib.abs(oldt) + MathLib.abs(t)) * 0.5;
                if ((delta <= rLimit) || (delta <= this.getAbsoluteAccuracy())) {
                    // OK, solution found
                    //
                    return t;
                }
            }
            oldt = t;
            this.iterations.incrementCount();
        }

    }

}
