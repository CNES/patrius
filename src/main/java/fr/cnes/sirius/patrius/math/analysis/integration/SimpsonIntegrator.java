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
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements <a href="http://mathworld.wolfram.com/SimpsonsRule.html">
 * Simpson's Rule</a> for integration of real univariate functions. For
 * reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X,
 * chapter 3.
 * <p>
 * This implementation employs the basic trapezoid rule to calculate Simpson's rule.
 * </p>
 * 
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class SimpsonIntegrator extends BaseAbstractUnivariateIntegrator {

    /** Maximal number of iterations for Simpson. */
    public static final int SIMPSON_MAX_ITERATIONS_COUNT = 64;

     /** Serializable UID. */
    private static final long serialVersionUID = 5198829425863931979L;

    /**
     * Build a Simpson integrator with given accuracies and iterations counts.
     * 
     * @param relativeAccuracy
     *        relative accuracy of the result
     * @param absoluteAccuracy
     *        absolute accuracy of the result
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #SIMPSON_MAX_ITERATIONS_COUNT})
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #SIMPSON_MAX_ITERATIONS_COUNT}
     */
    public SimpsonIntegrator(final double relativeAccuracy,
        final double absoluteAccuracy,
        final int minimalIterationCount,
        final int maximalIterationCount) {
        super(relativeAccuracy, absoluteAccuracy, minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > SIMPSON_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                SIMPSON_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Build a Simpson integrator with given iteration counts.
     * 
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #SIMPSON_MAX_ITERATIONS_COUNT})
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #SIMPSON_MAX_ITERATIONS_COUNT}
     */
    public SimpsonIntegrator(final int minimalIterationCount,
        final int maximalIterationCount) {
        super(minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > SIMPSON_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                SIMPSON_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Construct an integrator with default settings.
     * (max iteration count set to {@link #SIMPSON_MAX_ITERATIONS_COUNT})
     */
    public SimpsonIntegrator() {
        super(DEFAULT_MIN_ITERATIONS_COUNT, SIMPSON_MAX_ITERATIONS_COUNT);
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() {

        // Use trapezoid integrator
        final TrapezoidIntegrator qtrap = new TrapezoidIntegrator();
        if (this.getMinimalIterationCount() == 1) {
            return (4 * qtrap.stage(this, 1) - qtrap.stage(this, 0)) / 3.0;
        }

        // Simpson's rule requires at least two trapezoid stages.
        double olds = 0;
        double oldt = qtrap.stage(this, 0);
        while (true) {
            final double t = qtrap.stage(this, this.iterations.getCount());
            this.iterations.incrementCount();
            final double s = (4 * t - oldt) / 3.0;
            if (this.iterations.getCount() >= this.getMinimalIterationCount()) {
                final double delta = MathLib.abs(s - olds);
                final double rLimit =
                    this.getRelativeAccuracy() * (MathLib.abs(olds) + MathLib.abs(s)) * 0.5;
                if ((delta <= rLimit) || (delta <= this.getAbsoluteAccuracy())) {
                    // OK, solution reached
                    //
                    return s;
                }
            }
            olds = s;
            oldt = t;
        }

    }

}
