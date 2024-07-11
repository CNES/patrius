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

import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * This class implements the <a href="http://mathworld.wolfram.com/BrentsMethod.html">
 * Brent algorithm</a> for finding zeros of real univariate functions.
 * The function should be continuous but not necessarily smooth.
 * The {@code solve} method returns a zero {@code x} of the function {@code f} in the given interval {@code [a, b]} to
 * within a tolerance {@code 6 eps abs(x) + t} where {@code eps} is the relative accuracy and {@code t} is the absolute
 * accuracy.
 * The given interval must bracket the root.
 * 
 * @version $Id: BrentSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BrentSolver extends AbstractUnivariateSolver {

    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /** 0.5. */
    private static final double HALF = 0.5;

    /**
     * Construct a solver with default accuracy (1e-6).
     */
    public BrentSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public BrentSolver(final double absoluteAccuracy) {
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
    public BrentSolver(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param functionValueAccuracy
     *        Function value accuracy.
     */
    public BrentSolver(final double relativeAccuracy,
        final double absoluteAccuracy,
        final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        final double min = this.getMin();
        final double max = this.getMax();
        final double initial = this.getStartValue();
        final double functionValueAccuracy = this.getFunctionValueAccuracy();

        this.verifySequence(min, initial, max);

        // Return the initial guess if it is good enough.
        final double yInitial = this.computeObjectiveValue(initial);
        if (MathLib.abs(yInitial) <= functionValueAccuracy) {
            return initial;
        }

        // Return the first endpoint if it is good enough.
        final double yMin = this.computeObjectiveValue(min);
        if (MathLib.abs(yMin) <= functionValueAccuracy) {
            return min;
        }

        // Reduce interval if min and initial bracket the root.
        if (yInitial * yMin < 0) {
            return this.brent(min, initial, yMin, yInitial);
        }

        // Return the second endpoint if it is good enough.
        final double yMax = this.computeObjectiveValue(max);
        if (MathLib.abs(yMax) <= functionValueAccuracy) {
            return max;
        }

        // Reduce interval if initial and max bracket the root.
        if (yInitial * yMax < 0) {
            return this.brent(initial, max, yInitial, yMax);
        }

        throw new NoBracketingException(min, max, yMin, yMax);
    }

    /**
     * Search for a zero inside the provided interval.
     * This implementation is based on the algorithm described at page 58 of
     * the book
     * <quote>
     * <b>Algorithms for Minimization Without Derivatives</b>
     * <it>Richard P. Brent</it>
     * Dover 0-486-41998-3
     * </quote>
     * 
     * @param lo
     *        Lower bound of the search interval.
     * @param hi
     *        Higher bound of the search interval.
     * @param fLo
     *        Function value at the lower bound of the search interval.
     * @param fHi
     *        Function value at the higher bound of the search interval.
     * @return the value where the function is zero.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private double brent(final double lo, final double hi,
                         final double fLo, final double fHi) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        double a = lo;
        double fa = fLo;
        double b = hi;
        double fb = fHi;
        double c = a;
        double fc = fa;
        double d = b - a;
        double e = d;

        final double t = this.getAbsoluteAccuracy();
        final double eps = this.getRelativeAccuracy();

        while (true) {
            // Loop until convergence
            if (MathLib.abs(fc) < MathLib.abs(fb)) {
                a = b;
                b = c;
                c = a;
                fa = fb;
                fb = fc;
                fc = fa;
            }

            final double tol = 2 * eps * MathLib.abs(b) + t;
            final double m = 0.5 * (c - b);

            if (MathLib.abs(m) <= tol ||
                Precision.equals(fb, 0)) {
                return b;
            }
            if (MathLib.abs(e) < tol ||
                MathLib.abs(fa) <= MathLib.abs(fb)) {
                // Force bisection.
                d = m;
                e = d;
            } else {
                double s = fb / fa;
                double p;
                double q;
                // The equality test (a == c) is intentional,
                // it is part of the original Brent's method and
                // it should NOT be replaced by proximity test.
                if (a == c) {
                    // Linear interpolation
                    //
                    p = 2 * m * s;
                    q = 1 - s;
                } else {
                    // Inverse quadratic interpolation
                    //
                    q = fa / fc;
                    final double r = fb / fc;
                    p = s * (2 * m * q * (q - r) - (b - a) * (r - 1));
                    q = (q - 1) * (r - 1) * (s - 1);
                }
                if (p > 0) {
                    q = -q;
                } else {
                    p = -p;
                }
                s = e;
                e = d;
                if (p >= 3 * HALF * m * q - MathLib.abs(tol * q) ||
                    p >= MathLib.abs(HALF * s * q)) {
                    // Inverse quadratic interpolation gives a value
                    // in the wrong direction, or progress is slow.
                    // Fall back to bisection.
                    d = m;
                    e = d;
                } else {
                    d = p / q;
                }
            }
            a = b;
            fa = fb;

            if (MathLib.abs(d) > tol) {
                b += d;
            } else if (m > 0) {
                b += tol;
            } else {
                b -= tol;
            }
            fb = this.computeObjectiveValue(b);
            if ((fb > 0 && fc > 0) ||
                (fb <= 0 && fc <= 0)) {
                c = a;
                fc = fa;
                d = b - a;
                e = d;
            }
        }
    }
}
