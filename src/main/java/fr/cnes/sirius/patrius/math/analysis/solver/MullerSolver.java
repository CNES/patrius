/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements the <a href="http://mathworld.wolfram.com/MullersMethod.html">
 * Muller's Method</a> for root finding of real univariate functions. For
 * reference, see <b>Elementary Numerical Analysis</b>, ISBN 0070124477,
 * chapter 3.
 * <p>
 * Muller's method applies to both real and complex functions, but here we restrict ourselves to real functions. This
 * class differs from {@link MullerSolver} in the way it avoids complex operations.
 * </p>
 * Muller's original method would have function evaluation at complex point.
 * Since our f(x) is real, we have to find ways to avoid that. Bracketing
 * condition is one way to go: by requiring bracketing in every iteration,
 * the newly computed approximation is guaranteed to be real.</p>
 * <p>
 * Normally Muller's method converges quadratically in the vicinity of a zero, however it may be very slow in regions
 * far away from zeros. For example, f(x) = exp(x) - 1, min = -50, max = 100. In such case we use bisection as a safety
 * backup if it performs very poorly.
 * </p>
 * <p>
 * The formulas here use divided differences directly.
 * </p>
 * 
 * @version $Id: MullerSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 * @see MullerSolver2
 */
public class MullerSolver extends AbstractUnivariateSolver {

    /** Threshold for bissection solver. */
    private static final double THRESHOLD = 0.95;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /**
     * Construct a solver with default accuracy (1e-6).
     */
    public MullerSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public MullerSolver(final double absoluteAccuracy) {
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
    public MullerSolver(final double relativeAccuracy,
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

        // Initialization
        final double min = this.getMin();
        final double max = this.getMax();
        final double initial = this.getStartValue();

        final double functionValueAccuracy = this.getFunctionValueAccuracy();

        this.verifySequence(min, initial, max);

        // check for zeros before verifying bracketing
        final double fMin = this.computeObjectiveValue(min);
        if (MathLib.abs(fMin) < functionValueAccuracy) {
            return min;
        }
        final double fMax = this.computeObjectiveValue(max);
        if (MathLib.abs(fMax) < functionValueAccuracy) {
            return max;
        }
        final double fInitial = this.computeObjectiveValue(initial);
        if (MathLib.abs(fInitial) < functionValueAccuracy) {
            return initial;
        }

        // Check bracketing
        this.verifyBracketing(min, max);

        if (this.isBracketing(min, initial)) {
            return this.solve(min, initial, fMin, fInitial);
        } else {
            return this.solve(initial, max, fInitial, fMax);
        }
    }

    /**
     * Find a real root in the given interval.
     * 
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param fMin
     *        function value at the lower bound.
     * @param fMax
     *        function value at the upper bound.
     * @return the point at which the function value is zero.
     * @throws TooManyEvaluationsException
     *         if the allowed number of calls to
     *         the function to be solved has been exhausted.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private double solve(final double min, final double max,
                         final double fMin, final double fMax) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double relativeAccuracy = this.getRelativeAccuracy();
        final double absoluteAccuracy = this.getAbsoluteAccuracy();
        final double functionValueAccuracy = this.getFunctionValueAccuracy();

        // [x0, x2] is the bracketing interval in each iteration
        // x1 is the last approximation and an interpolation point in (x0, x2)
        // x is the new root approximation and new x1 for next round
        // d01, d12, d012 are divided differences

        double x0 = min;
        double y0 = fMin;
        double x2 = max;
        double y2 = fMax;
        double x1 = HALF * (x0 + x2);
        double y1 = this.computeObjectiveValue(x1);

        double oldx = Double.POSITIVE_INFINITY;
        while (true) {
            // Muller's method employs quadratic interpolation through
            // x0, x1, x2 and x is the zero of the interpolating parabola.
            // Due to bracketing condition, this parabola must have two
            // real roots and we choose one in [x0, x2] to be x.
            final double d01 = (y1 - y0) / (x1 - x0);
            final double d12 = (y2 - y1) / (x2 - x1);
            final double d012 = (d12 - d01) / (x2 - x0);
            final double c1 = d01 + (x1 - x0) * d012;
            final double delta = c1 * c1 - 4 * y1 * d012;
            final double xplus = x1 + (-2.0 * y1) / (c1 + MathLib.sqrt(delta));
            final double xminus = x1 + (-2.0 * y1) / (c1 - MathLib.sqrt(delta));
            // xplus and xminus are two roots of parabola and at least
            // one of them should lie in (x0, x2)
            final double x = this.isSequence(x0, xplus, x2) ? xplus : xminus;
            final double y = this.computeObjectiveValue(x);

            // check for convergence
            final double tolerance = MathLib.max(relativeAccuracy * MathLib.abs(x), absoluteAccuracy);
            if (MathLib.abs(x - oldx) <= tolerance ||
                MathLib.abs(y) <= functionValueAccuracy) {
                return x;
            }

            // Bisect if convergence is too slow. Bisection would waste
            // our calculation of x, hopefully it won't happen often.
            // the real number equality test x == x1 is intentional and
            // completes the proximity tests above it
            final boolean cond1 = x < x1 && (x1 - x0) > THRESHOLD * (x2 - x0);
            final boolean bisect = cond1 ||
                (x > x1 && (x2 - x1) > THRESHOLD * (x2 - x0)) ||
                (x == x1);
            // prepare the new bracketing interval for next iteration
            if (bisect) {
                final double xm = 0.5 * (x0 + x2);
                final double ym = this.computeObjectiveValue(xm);
                if (MathLib.signum(y0) + MathLib.signum(ym) == 0.0) {
                    x2 = xm;
                    y2 = ym;
                } else {
                    x0 = xm;
                    y0 = ym;
                }
                x1 = HALF * (x0 + x2);
                y1 = this.computeObjectiveValue(x1);
                oldx = Double.POSITIVE_INFINITY;
            } else {
                x0 = x < x1 ? x0 : x1;
                y0 = x < x1 ? y0 : y1;
                x2 = x > x1 ? x2 : x1;
                y2 = x > x1 ? y2 : y1;
                x1 = x;
                y1 = y;
                oldx = x;
            }
        }
    }
}
