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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.dfp;

import fr.cnes.sirius.patrius.math.analysis.solver.AllowedSolution;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.Incrementor;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * This class implements a modification of the <a
 * href="http://mathworld.wolfram.com/BrentsMethod.html"> Brent algorithm</a>.
 * <p>
 * The changes with respect to the original Brent algorithm are:
 * <ul>
 * <li>the returned value is chosen in the current interval according to user specified {@link AllowedSolution},</li>
 * <li>the maximal order for the invert polynomial root search is user-specified instead of being
 *  invert quadratic only</li>
 * </ul>
 * </p>
 * The given interval must bracket the root.
 * 
 * @version $Id: BracketingNthOrderBrentSolverDFP.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BracketingNthOrderBrentSolverDFP {

    /** Maximal aging triggering an attempt to balance the bracketing interval. */
    private static final int MAXIMAL_AGING = 2;

    /** Sixteen. */
    private static final int SIXTEEN = 16;

    /** Maximal order. */
    private final int maximalOrder;

    /** Function value accuracy. */
    private final Dfp functionValueAccuracy;

    /** Absolute accuracy. */
    private final Dfp absoluteAccuracy;

    /** Relative accuracy. */
    private final Dfp relativeAccuracy;

    /** Evaluations counter. */
    private final Incrementor evaluations = new Incrementor();

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracyIn
     *        Relative accuracy.
     * @param absoluteAccuracyIn
     *        Absolute accuracy.
     * @param functionValueAccuracyIn
     *        Function value accuracy.
     * @param maximalOrderIn
     *        maximal order.
     * @exception NumberIsTooSmallException
     *            if maximal order is lower than 2
     */
    public BracketingNthOrderBrentSolverDFP(final Dfp relativeAccuracyIn,
                                            final Dfp absoluteAccuracyIn,
                                            final Dfp functionValueAccuracyIn,
                                            final int maximalOrderIn) {
        if (maximalOrderIn < 2) {
            throw new NumberIsTooSmallException(maximalOrderIn, 2, true);
        }
        this.maximalOrder = maximalOrderIn;
        this.absoluteAccuracy = absoluteAccuracyIn;
        this.relativeAccuracy = relativeAccuracyIn;
        this.functionValueAccuracy = functionValueAccuracyIn;
    }

    /**
     * Get the maximal order.
     * 
     * @return maximal order
     */
    public int getMaximalOrder() {
        return this.maximalOrder;
    }

    /**
     * Get the maximal number of function evaluations.
     * 
     * @return the maximal number of function evaluations.
     */
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /**
     * Get the number of evaluations of the objective function.
     * The number of evaluations corresponds to the last call to the {@code optimize} method. It is 0 if the method has
     * not been
     * called yet.
     * 
     * @return the number of evaluations of the objective function.
     */
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /**
     * Get the absolute accuracy.
     * 
     * @return absolute accuracy
     */
    public Dfp getAbsoluteAccuracy() {
        return this.absoluteAccuracy;
    }

    /**
     * Get the relative accuracy.
     * 
     * @return relative accuracy
     */
    public Dfp getRelativeAccuracy() {
        return this.relativeAccuracy;
    }

    /**
     * Get the function accuracy.
     * 
     * @return function accuracy
     */
    public Dfp getFunctionValueAccuracy() {
        return this.functionValueAccuracy;
    }

    /**
     * Solve for a zero in the given interval.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param allowedSolution
     *        The kind of solutions that the root-finding algorithm may
     *        accept as solutions.
     * @return a value where the function is zero.
     * @exception NullArgumentException
     *            if f is null.
     * @exception NoBracketingException
     *            if root cannot be bracketed
     */
    public Dfp solve(final int maxEval, final UnivariateDfpFunction f,
                     final Dfp min, final Dfp max, final AllowedSolution allowedSolution) {
        return this.solve(maxEval, f, min, max, min.add(max).divide(2), allowedSolution);
    }

    /**
     * Solve for a zero in the given interval, start at {@code startValue}.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     * 
     * @param maxEval
     *        Maximum number of evaluations.
     * @param f
     *        Function to solve.
     * @param min
     *        Lower bound for the interval.
     * @param max
     *        Upper bound for the interval.
     * @param startValue
     *        Start value to use.
     * @param allowedSolution
     *        The kind of solutions that the root-finding algorithm may
     *        accept as solutions.
     * @return a value where the function is zero.
     * @exception NullArgumentException
     *            if f is null.
     * @exception NoBracketingException
     *            if root cannot be bracketed
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public Dfp solve(final int maxEval, final UnivariateDfpFunction f,
                     final Dfp min, final Dfp max, final Dfp startValue,
                     final AllowedSolution allowedSolution) {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Checks.
        MathUtils.checkNotNull(f);

        // Reset.
        this.evaluations.setMaximalCount(maxEval);
        this.evaluations.resetCount();

        // prepare arrays with the first points
        final Dfp[] x = new Dfp[this.maximalOrder + 1];
        final Dfp[] y = new Dfp[this.maximalOrder + 1];
        x[0] = min;
        x[1] = startValue;
        x[2] = max;

        // evaluate initial guess
        this.evaluations.incrementCount();
        y[1] = f.value(x[1]);
        if (y[1].isZero()) {
            // return the initial guess if it is a perfect root.
            return x[1];
        }

        // evaluate first endpoint
        this.evaluations.incrementCount();
        y[0] = f.value(x[0]);
        if (y[0].isZero()) {
            // return the first endpoint if it is a perfect root.
            return x[0];
        }

        int nbPoints;
        int signChangeIndex;
        if (y[0].multiply(y[1]).negativeOrNull()) {

            // reduce interval if it brackets the root
            nbPoints = 2;
            signChangeIndex = 1;
        } else {

            // evaluate second endpoint
            this.evaluations.incrementCount();
            y[2] = f.value(x[2]);
            if (y[2].isZero()) {
                // return the second endpoint if it is a perfect root.
                return x[2];
            }

            if (y[1].multiply(y[2]).negativeOrNull()) {
                // use all computed point as a start sampling array for solving
                nbPoints = 3;
                signChangeIndex = 2;
            } else {
                throw new NoBracketingException(x[0].toDouble(), x[2].toDouble(),
                    y[0].toDouble(), y[2].toDouble());
            }
        }

        // prepare a work array for inverse polynomial interpolation
        final Dfp[] tmpX = new Dfp[x.length];

        // current tightest bracketing of the root
        Dfp xA = x[signChangeIndex - 1];
        Dfp yA = y[signChangeIndex - 1];
        final Dfp absXA = xA.abs();
        Dfp absYA = yA.abs();
        int agingA = 0;
        Dfp xB = x[signChangeIndex];
        Dfp yB = y[signChangeIndex];
        final Dfp absXB = xB.abs();
        Dfp absYB = yB.abs();
        int agingB = 0;

        // Constant
        final Dfp zero = startValue.getZero();
        final Dfp nan = zero.newInstance((byte) 1, Dfp.QNAN);

        // search loop
        while (true) {

            // check convergence of bracketing interval
            final Dfp maxX = absXA.lessThan(absXB) ? absXB : absXA;
            final Dfp maxY = absYA.lessThan(absYB) ? absYB : absYA;
            final Dfp xTol = this.absoluteAccuracy.add(this.relativeAccuracy.multiply(maxX));
            if (xB.subtract(xA).subtract(xTol).negativeOrNull() ||
                    maxY.lessThan(this.functionValueAccuracy)) {
                switch (allowedSolution) {
                    case ANY_SIDE:
                        return absYA.lessThan(absYB) ? xA : xB;
                    case LEFT_SIDE:
                        return xA;
                    case RIGHT_SIDE:
                        return xB;
                    case BELOW_SIDE:
                        return yA.lessThan(zero) ? xA : xB;
                    case ABOVE_SIDE:
                        return yA.lessThan(zero) ? xB : xA;
                    default:
                        // this should never happen
                        throw new MathInternalError(null);
                }
            }

            // target for the next evaluation point
            final Dfp targetY;
            if (agingA >= MAXIMAL_AGING) {
                // we keep updating the high bracket, try to compensate this
                targetY = yB.divide(SIXTEEN).negate();
            } else if (agingB >= MAXIMAL_AGING) {
                // we keep updating the low bracket, try to compensate this
                targetY = yA.divide(SIXTEEN).negate();
            } else {
                // bracketing is balanced, try to find the root itself
                targetY = zero;
            }

            // make a few attempts to guess a root,
            Dfp nextX;
            int start = 0;
            int end = nbPoints;
            do {

                // guess a value for current target, using inverse polynomial interpolation
                System.arraycopy(x, start, tmpX, start, end - start);
                nextX = guessX(targetY, tmpX, y, start, end);

                if (!(nextX.greaterThan(xA) && nextX.lessThan(xB))) {
                    // the guessed root is not strictly inside of the tightest bracketing interval

                    // the guessed root is either not strictly inside the interval or it
                    // is a NaN (which occurs when some sampling points share the same y)
                    // we try again with a lower interpolation order
                    if (signChangeIndex - start >= end - signChangeIndex) {
                        // we have more points before the sign change, drop the lowest point
                        ++start;
                    } else {
                        // we have more points after sign change, drop the highest point
                        --end;
                    }

                    // we need to do one more attempt
                    nextX = nan;
                }
            } while (nextX.isNaN() && (end - start > 1));

            if (nextX.isNaN()) {
                // fall back to bisection
                nextX = xA.add(xB.subtract(xA).divide(2));
                start = signChangeIndex - 1;
                end = signChangeIndex;
            }

            // evaluate the function at the guessed root
            this.evaluations.incrementCount();
            final Dfp nextY = f.value(nextX);
            if (nextY.isZero()) {
                // we have found an exact root, since it is not an approximation
                // we don't need to bother about the allowed solutions setting
                return nextX;
            }

            if ((nbPoints > 2) && (end - start != nbPoints)) {

                // we have been forced to ignore some points to keep bracketing,
                // they are probably too far from the root, drop them from now on
                nbPoints = end - start;
                System.arraycopy(x, start, x, 0, nbPoints);
                System.arraycopy(y, start, y, 0, nbPoints);
                signChangeIndex -= start;
            } else if (nbPoints == x.length) {

                // we have to drop one point in order to insert the new one
                nbPoints--;

                // keep the tightest bracketing interval as centered as possible
                if (signChangeIndex >= (x.length + 1) / 2) {
                    // we drop the lowest point, we have to shift the arrays and the index
                    System.arraycopy(x, 1, x, 0, nbPoints);
                    System.arraycopy(y, 1, y, 0, nbPoints);
                    --signChangeIndex;
                }
            }

            // insert the last computed point
            // (by construction, we know it lies inside the tightest bracketing interval)
            System.arraycopy(x, signChangeIndex, x, signChangeIndex + 1, nbPoints - signChangeIndex);
            x[signChangeIndex] = nextX;
            System.arraycopy(y, signChangeIndex, y, signChangeIndex + 1, nbPoints - signChangeIndex);
            y[signChangeIndex] = nextY;
            ++nbPoints;

            // update the bracketing interval
            if (nextY.multiply(yA).negativeOrNull()) {
                // the sign change occurs before the inserted point
                xB = nextX;
                yB = nextY;
                absYB = yB.abs();
                ++agingA;
                agingB = 0;
            } else {
                // the sign change occurs after the inserted point
                xA = nextX;
                yA = nextY;
                absYA = yA.abs();
                agingA = 0;
                ++agingB;

                // update the sign change index
                signChangeIndex++;
            }
        }
    }

    /**
     * Guess an x value by n<sup>th</sup> order inverse polynomial interpolation.
     * <p>
     * The x value is guessed by evaluating polynomial Q(y) at y = targetY, where Q is built such that for all
     * considered points (x<sub>i</sub>, y<sub>i</sub>), Q(y<sub>i</sub>) = x<sub>i</sub>.
     * </p>
     * 
     * @param targetY
     *        target value for y
     * @param x
     *        reference points abscissas for interpolation,
     *        note that this array <em>is</em> modified during computation
     * @param y
     *        reference points ordinates for interpolation
     * @param start
     *        start index of the points to consider (inclusive)
     * @param end
     *        end index of the points to consider (exclusive)
     * @return guessed root (will be a NaN if two points share the same y)
     */
    private static Dfp guessX(final Dfp targetY, final Dfp[] x, final Dfp[] y,
                       final int start, final int end) {

        // compute Q Newton coefficients by divided differences
        for (int i = start; i < end - 1; ++i) {
            // evaluate delta for each step
            final int delta = i + 1 - start;
            for (int j = end - 1; j > i; --j) {
                x[j] = x[j].subtract(x[j - 1]).divide(y[j].subtract(y[j - delta]));
            }
        }

        // evaluate Q(targetY)
        Dfp x0 = targetY.getZero();
        for (int j = end - 1; j >= start; --j) {
            x0 = x[j].add(x0.multiply(targetY.subtract(y[j])));
        }

        // return guessed root
        return x0;
    }
}
