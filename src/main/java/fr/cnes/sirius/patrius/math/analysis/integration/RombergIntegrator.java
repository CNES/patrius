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
package fr.cnes.sirius.patrius.math.analysis.integration;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements the <a href="http://mathworld.wolfram.com/RombergIntegration.html">
 * Romberg Algorithm</a> for integration of real univariate functions. For
 * reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X,
 * chapter 3.
 * <p>
 * Romberg integration employs k successive refinements of the trapezoid rule to remove error terms less than order
 * O(N^(-2k)). Simpson's rule is a special case of k = 2.
 * </p>
 * 
 * @version $Id: RombergIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class RombergIntegrator extends BaseAbstractUnivariateIntegrator {

    /** Maximal number of iterations for Romberg. */
    public static final int ROMBERG_MAX_ITERATIONS_COUNT = 32;

    /** Serial UID. */
    private static final long serialVersionUID = -5196995194861376560L;

    /**
     * Build a Romberg integrator with given accuracies and iterations counts.
     * 
     * @param relativeAccuracy
     *        relative accuracy of the result
     * @param absoluteAccuracy
     *        absolute accuracy of the result
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #ROMBERG_MAX_ITERATIONS_COUNT})
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #ROMBERG_MAX_ITERATIONS_COUNT}
     */
    public RombergIntegrator(final double relativeAccuracy,
        final double absoluteAccuracy,
        final int minimalIterationCount,
        final int maximalIterationCount) {
        super(relativeAccuracy, absoluteAccuracy, minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > ROMBERG_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                ROMBERG_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Build a Romberg integrator with given iteration counts.
     * 
     * @param minimalIterationCount
     *        minimum number of iterations
     * @param maximalIterationCount
     *        maximum number of iterations
     *        (must be less than or equal to {@link #ROMBERG_MAX_ITERATIONS_COUNT})
     * @exception NotStrictlyPositiveException
     *            if minimal number of iterations
     *            is not strictly positive
     * @exception NumberIsTooSmallException
     *            if maximal number of iterations
     *            is lesser than or equal to the minimal number of iterations
     * @exception NumberIsTooLargeException
     *            if maximal number of iterations
     *            is greater than {@link #ROMBERG_MAX_ITERATIONS_COUNT}
     */
    public RombergIntegrator(final int minimalIterationCount,
        final int maximalIterationCount) {
        super(minimalIterationCount, maximalIterationCount);
        if (maximalIterationCount > ROMBERG_MAX_ITERATIONS_COUNT) {
            throw new NumberIsTooLargeException(maximalIterationCount,
                ROMBERG_MAX_ITERATIONS_COUNT, false);
        }
    }

    /**
     * Construct a Romberg integrator with default settings
     * (max iteration count set to {@link #ROMBERG_MAX_ITERATIONS_COUNT})
     */
    public RombergIntegrator() {
        super(DEFAULT_MIN_ITERATIONS_COUNT, ROMBERG_MAX_ITERATIONS_COUNT);
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() {

        // Initialization
        final int m = this.iterations.getMaximalCount() + 1;
        double[] previousRow = new double[m];
        double[] currentRow = new double[m];

        // Use trapezoid integrator
        final TrapezoidIntegrator qtrap = new TrapezoidIntegrator();
        currentRow[0] = qtrap.stage(this, 0);
        this.iterations.incrementCount();
        double olds = currentRow[0];
        while (true) {
            // Loop until accuracy is reached
            final int i = this.iterations.getCount();

            // switch rows
            final double[] tmpRow = previousRow;
            previousRow = currentRow;
            currentRow = tmpRow;

            currentRow[0] = qtrap.stage(this, i);
            this.iterations.incrementCount();
            for (int j = 1; j <= i; j++) {
                // Richardson extrapolation coefficient
                final double r = (1L << (2 * j)) - 1;
                final double tIJm1 = currentRow[j - 1];
                currentRow[j] = tIJm1 + (tIJm1 - previousRow[j - 1]) / r;
            }
            final double s = currentRow[i];
            if (i >= this.getMinimalIterationCount()) {
                final double delta = MathLib.abs(s - olds);
                final double rLimit = this.getRelativeAccuracy() * (MathLib.abs(olds) + MathLib.abs(s)) * 0.5;
                if ((delta <= rLimit) || (delta <= this.getAbsoluteAccuracy())) {
                    // OK, solution found
                    //
                    return s;
                }
            }
            olds = s;
        }

    }

}
