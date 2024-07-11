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
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements the <em>Secant</em> method for root-finding (approximating a
 * zero of a univariate real function). The solution that is maintained is
 * not bracketed, and as such convergence is not guaranteed.
 * 
 * <p>
 * Implementation based on the following article: M. Dowell and P. Jarratt,
 * <em>A modified regula falsi method for computing the root of an
 * equation</em>, BIT Numerical Mathematics, volume 11, number 2, pages 168-174, Springer, 1971.
 * </p>
 * 
 * <p>
 * Note that since release 3.0 this class implements the actual <em>Secant</em> algorithm, and not a modified one. As
 * such, the 3.0 version is not backwards compatible with previous versions. To use an algorithm similar to the pre-3.0
 * releases, use the {@link IllinoisSolver <em>Illinois</em>} algorithm or the {@link PegasusSolver <em>Pegasus</em>}
 * algorithm.
 * </p>
 * 
 * @version $Id: SecantSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SecantSolver extends AbstractUnivariateSolver {

    /** Default absolute accuracy. */
    protected static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /** Construct a solver with default accuracy (1e-6). */
    public SecantSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        absolute accuracy
     */
    public SecantSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        relative accuracy
     * @param absoluteAccuracy
     *        absolute accuracy
     */
    public SecantSolver(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected final double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        // Get initial solution

        // If one of the bounds is the exact root, return it. Since these are
        // not under-approximations or over-approximations, we can return them
        // regardless of the allowed solutions.
        double x0 = this.getMin();
        double f0 = this.computeObjectiveValue(x0);
        if (f0 == 0.0) {
            return x0;
        }
        double x1 = this.getMax();
        double f1 = this.computeObjectiveValue(x1);
        if (f1 == 0.0) {
            return x1;
        }

        // Verify bracketing of initial solution.
        this.verifyBracketing(x0, x1);

        // Get accuracies.
        final double ftol = this.getFunctionValueAccuracy();
        final double atol = this.getAbsoluteAccuracy();
        final double rtol = this.getRelativeAccuracy();

        // Keep finding better approximations.
        while (true) {
            // Calculate the next approximation.
            final double x = x1 - ((f1 * (x1 - x0)) / (f1 - f0));
            final double fx = this.computeObjectiveValue(x);

            // If the new approximation is the exact root, return it. Since
            // this is not an under-approximation or an over-approximation,
            // we can return it regardless of the allowed solutions.
            if (fx == 0.0) {
                return x;
            }

            // Update the bounds with the new approximation.
            x0 = x1;
            f0 = f1;
            x1 = x;
            f1 = fx;

            // If the function value of the last approximation is too small,
            // given the function value accuracy, then we can't get closer to
            // the root than we already are.
            if (MathLib.abs(f1) <= ftol) {
                return x1;
            }

            // If the current interval is within the given accuracies, we
            // are satisfied with the current approximation.
            if (MathLib.abs(x1 - x0) < MathLib.max(rtol * MathLib.abs(x1), atol)) {
                return x1;
            }
        }
    }

}
