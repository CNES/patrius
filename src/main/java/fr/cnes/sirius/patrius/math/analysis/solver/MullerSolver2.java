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

import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
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
 * Except for the initial [min, max], it does not require bracketing
 * condition, e.g. f(x0), f(x1), f(x2) can have the same sign. If complex
 * number arises in the computation, we simply use its modulus as real
 * approximation.</p>
 * <p>
 * Because the interval may not be bracketing, bisection alternative is not applicable here. However in practice our
 * treatment usually works well, especially near real zeroes where the imaginary part of complex approximation is often
 * negligible.
 * </p>
 * <p>
 * The formulas here do not use divided differences directly.
 * </p>
 * 
 * @version $Id: MullerSolver2.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 * @see MullerSolver
 */
public class MullerSolver2 extends AbstractUnivariateSolver {

    /** Serializable UID. */
    private static final long serialVersionUID = -6879280067828575565L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /**
     * Construct a solver with default accuracy (1e-6).
     */
    public MullerSolver2() {
        this(DEFAULT_ABSOLUTE_ACCURACY);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     */
    public MullerSolver2(final double absoluteAccuracy) {
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
    public MullerSolver2(final double relativeAccuracy,
        final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double min = this.getMin();
        final double max = this.getMax();

        // Check interval
        this.verifyInterval(min, max);

        final double functionValueAccuracy = this.getFunctionValueAccuracy();

        // x2 is the last root approximation
        // x is the new approximation and new x2 for next round
        // x0 < x1 < x2 does not hold here

        // Immediate return
        double x0 = min;
        double y0 = this.computeObjectiveValue(x0);
        if (MathLib.abs(y0) < functionValueAccuracy) {
            return x0;
        }
        double x1 = max;
        double y1 = this.computeObjectiveValue(x1);
        if (MathLib.abs(y1) < functionValueAccuracy) {
            return x1;
        }

        // Root not bracketed
        if (y0 * y1 > 0) {
            throw new NoBracketingException(x0, x1, y0, y1);
        }

        final double relativeAccuracy = this.getRelativeAccuracy();
        final double absoluteAccuracy = this.getAbsoluteAccuracy();

        double x2 = HALF * (x0 + x1);
        double y2 = this.computeObjectiveValue(x2);

        double oldx = Double.POSITIVE_INFINITY;
        while (true) {
            // quadratic interpolation through x0, x1, x2
            final double q = (x2 - x1) / (x1 - x0);
            final double a = q * (y2 - (1 + q) * y1 + q * y0);
            final double b = (2 * q + 1) * y2 - (1 + q) * (1 + q) * y1 + q * q * y0;
            final double c = (1 + q) * y2;
            final double delta = b * b - 4 * a * c;
            double x;
            final double denominator;
            if (delta >= 0.0) {
                // choose a denominator larger in magnitude
                final double dplus = b + MathLib.sqrt(delta);
                final double dminus = b - MathLib.sqrt(delta);
                denominator = MathLib.abs(dplus) > MathLib.abs(dminus) ? dplus : dminus;
            } else {
                // take the modulus of (B +/- FastMath.sqrt(delta))
                denominator = MathLib.sqrt(b * b - delta);
            }
            if (denominator == 0) {
                // extremely rare case, get a random number to skip it
                x = min + MathLib.random() * (max - min);
                oldx = Double.POSITIVE_INFINITY;
            } else {
                x = x2 - 2.0 * c * (x2 - x1) / denominator;
                // perturb x if it exactly coincides with x1 or x2
                // the equality tests here are intentional
                while (x == x1 || x == x2) {
                    x += absoluteAccuracy;
                }
            }
            final double y = this.computeObjectiveValue(x);

            // check for convergence
            final double tolerance = MathLib.max(relativeAccuracy * MathLib.abs(x), absoluteAccuracy);
            if (MathLib.abs(x - oldx) <= tolerance ||
                MathLib.abs(y) <= functionValueAccuracy) {
                return x;
            }

            // prepare the next iteration
            x0 = x1;
            y0 = y1;
            x1 = x2;
            y1 = y2;
            x2 = x;
            y2 = y;
            oldx = x;
        }
    }
}
