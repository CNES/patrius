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
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implements the <a href="http://mathworld.wolfram.com/RiddersMethod.html">
 * Ridders' Method</a> for root finding of real univariate functions. For
 * reference, see C. Ridders, <i>A new algorithm for computing a single root
 * of a real continuous function </i>, IEEE Transactions on Circuits and
 * Systems, 26 (1979), 979 - 980.
 * <p>
 * The function should be continuous but not necessarily smooth.
 * </p>
 * 
 * @version $Id: RiddersSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class RiddersSolver extends AbstractUnivariateSolver {
    /** Serializable UID. */
    private static final long serialVersionUID = -7887739698512125058L;
    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /**
     * Construct a solver with default accuracy (1e-6).
     */
    public RiddersSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public RiddersSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public RiddersSolver(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected double doSolve() {
        // CHECKSTYLE: resume ReturnCount check

        // [x1, x2] is the bracketing interval in each iteration
        // x3 is the midpoint of [x1, x2]
        // x is the new root approximation and an endpoint of the new interval

        // check for zeros before verifying bracketing
        final double min = this.getMin();
        double x1 = min;
        double y1 = this.computeObjectiveValue(x1);
        if (y1 == 0) {
            return min;
        }
        final double max = this.getMax();
        double x2 = max;
        double y2 = this.computeObjectiveValue(x2);
        if (y2 == 0) {
            return max;
        }
        this.verifyBracketing(min, max);

        final double absoluteAccuracy = this.getAbsoluteAccuracy();
        final double functionValueAccuracy = this.getFunctionValueAccuracy();
        final double relativeAccuracy = this.getRelativeAccuracy();

        double oldx = Double.POSITIVE_INFINITY;
        while (true) {
            // calculate the new root approximation
            final double x3 = 0.5 * (x1 + x2);
            final double y3 = this.computeObjectiveValue(x3);
            if (MathLib.abs(y3) <= functionValueAccuracy) {
                return x3;
            }
            // delta > 1 due to bracketing
            final double delta = 1 - (y1 * y2) / (y3 * y3);
            final double correction = (MathLib.signum(y2) * MathLib.signum(y3)) *
                (x3 - x1) / MathLib.sqrt(delta);
            // correction != 0
            final double x = x3 - correction;

            // check for convergence
            final double tolerance = MathLib.max(relativeAccuracy * MathLib.abs(x), absoluteAccuracy);
            if (MathLib.abs(x - oldx) <= tolerance) {
                return x;
            }
            final double y = this.computeObjectiveValue(x);
            if (MathLib.abs(y) <= functionValueAccuracy) {
                return x;
            }

            // prepare the new interval for next iteration
            // Ridders' method guarantees x1 < x < x2
            if (correction > 0.0) {
                // x1 < x < x3
                if (MathLib.signum(y1) + MathLib.signum(y) == 0.0) {
                    x2 = x;
                    y2 = y;
                } else {
                    x1 = x;
                    x2 = x3;
                    y1 = y;
                    y2 = y3;
                }
            } else {
                // x3 < x < x2
                if (MathLib.signum(y2) + MathLib.signum(y) == 0.0) {
                    x1 = x;
                    y1 = y;
                } else {
                    x1 = x3;
                    x2 = x;
                    y1 = y3;
                    y2 = y;
                }
            }
            oldx = x;
        }
    }
}
