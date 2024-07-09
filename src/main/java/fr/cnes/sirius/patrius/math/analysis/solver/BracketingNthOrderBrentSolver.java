/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 * @history created 16/11/17
 * 
 *          HISTORY
* VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 *          VERSION:4.3.1:FA:FA-2136:11/07/2019:[PATRIUS] Exception NumberIsTooLarge lors de la propagation
 *          VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *          VERSION::DM:1305:16/11/2017: Serializable interface implementation
 *          VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *          VERSION::FA:1868:31/10/2018: handle proper end of integration
 *          END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.util.MathLib;

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
 * @version $Id: BracketingNthOrderBrentSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BracketingNthOrderBrentSolver
    extends AbstractUnivariateSolver
    implements BracketedUnivariateSolver<UnivariateFunction>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 2456670890219413087L;

    /** 16. */
    private static final double SIXTEEN = 16.;

    /** Default absolute accuracy. */
    private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /** Default maximal order. */
    private static final int DEFAULT_MAXIMAL_ORDER = 5;

    /** Maximal aging triggering an attempt to balance the bracketing interval. */
    private static final int MAXIMAL_AGING = 2;

    /** Reduction factor for attempts to balance the bracketing interval. */
    private static final double REDUCTION_FACTOR = 1.0 / SIXTEEN;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Maximal order. */
    private final int maximalOrder;

    /** The kinds of solutions that the algorithm may accept. */
    private AllowedSolution allowed;

    /**
     * Construct a solver with default accuracy and maximal order (1e-6 and 5 respectively)
     */
    public BracketingNthOrderBrentSolver() {
        this(DEFAULT_ABSOLUTE_ACCURACY, DEFAULT_MAXIMAL_ORDER);
    }

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param maximalOrderIn
     *        maximal order.
     * @exception NumberIsTooSmallException
     *            if maximal order is lower than 2
     */
    public BracketingNthOrderBrentSolver(final double absoluteAccuracy,
                                         final int maximalOrderIn) {
        super(absoluteAccuracy);
        if (maximalOrderIn < 2) {
            throw new NumberIsTooSmallException(maximalOrderIn, 2, true);
        }
        this.maximalOrder = maximalOrderIn;
        this.allowed = AllowedSolution.ANY_SIDE;
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param maximalOrderIn
     *        maximal order.
     * @exception NumberIsTooSmallException
     *            if maximal order is lower than 2
     */
    public BracketingNthOrderBrentSolver(final double relativeAccuracy,
                                         final double absoluteAccuracy,
                                         final int maximalOrderIn) {
        super(relativeAccuracy, absoluteAccuracy);
        if (maximalOrderIn < 2) {
            throw new NumberIsTooSmallException(maximalOrderIn, 2, true);
        }
        this.maximalOrder = maximalOrderIn;
        this.allowed = AllowedSolution.ANY_SIDE;
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
     * @param maximalOrderIn
     *        maximal order.
     * @exception NumberIsTooSmallException
     *            if maximal order is lower than 2
     */
    public BracketingNthOrderBrentSolver(final double relativeAccuracy,
                                         final double absoluteAccuracy,
                                         final double functionValueAccuracy,
                                         final int maximalOrderIn) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        if (maximalOrderIn < 2) {
            throw new NumberIsTooSmallException(maximalOrderIn, 2, true);
        }
        this.maximalOrder = maximalOrderIn;
        this.allowed = AllowedSolution.ANY_SIDE;
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
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // prepare arrays with the first points
        final double[] x = new double[this.maximalOrder + 1];
        final double[] y = new double[this.maximalOrder + 1];
        x[0] = this.getMin();
        x[1] = this.getStartValue();
        x[2] = this.getMax();
        verifySequenceStrict(x[0], x[1], x[2]);

        // evaluate initial guess
        y[1] = this.computeObjectiveValue(x[1]);
        if (y[1] == 0.0) {
            // return the initial guess if it is a perfect root.
            return x[1];
        }

        // evaluate first endpoint
        y[0] = this.computeObjectiveValue(x[0]);
        if (y[0] == 0.0) {
            // return the first endpoint if it is a perfect root.
            return x[0];
        }

        int nbPoints;
        int signChangeIndex;
        if (y[0] * y[1] < 0) {

            // reduce interval if it brackets the root
            nbPoints = 2;
            signChangeIndex = 1;

        } else {

            // evaluate second endpoint
            y[2] = this.computeObjectiveValue(x[2]);
            if (y[2] == 0.0) {
                // return the second endpoint if it is a perfect root.
                return x[2];
            }

            if (y[1] * y[2] < 0) {
                // use all computed point as a start sampling array for solving
                nbPoints = 3;
                signChangeIndex = 2;
            } else {
                throw new NoBracketingException(x[0], x[2], y[0], y[2]);
            }

        }

        // prepare a work array for inverse polynomial interpolation
        final double[] tmpX = new double[x.length];

        // current tightest bracketing of the root
        double xA = x[signChangeIndex - 1];
        double yA = y[signChangeIndex - 1];
        double absYA = MathLib.abs(yA);
        int agingA = 0;
        double xB = x[signChangeIndex];
        double yB = y[signChangeIndex];
        double absYB = MathLib.abs(yB);
        int agingB = 0;

        // search loop
        while (true) {

            // check convergence of bracketing interval
            final double xTol = this.getAbsoluteAccuracy() +
                    this.getRelativeAccuracy() * MathLib.max(MathLib.abs(xA), MathLib.abs(xB));
            if (((xB - xA) <= xTol) || (MathLib.max(absYA, absYB) < this.getFunctionValueAccuracy())) {
                switch (this.allowed) {
                    case ANY_SIDE:
                        return absYA < absYB ? xA : xB;
                    case LEFT_SIDE:
                        return xA;
                    case RIGHT_SIDE:
                        return xB;
                    case BELOW_SIDE:
                        return (yA <= 0) ? xA : xB;
                    case ABOVE_SIDE:
                        return (yA < 0) ? xB : xA;
                    default:
                        // this should never happen
                        throw new MathInternalError();
                }
            }

            // target for the next evaluation point
            final double targetY;
            if (agingA >= MAXIMAL_AGING) {
                // we keep updating the high bracket, try to compensate this
                final int p = agingA - MAXIMAL_AGING;
                final double weightA = (1 << p) - 1;
                final double weightB = p + 1;
                targetY = (weightA * yA - weightB * REDUCTION_FACTOR * yB) / (weightA + weightB);
            } else if (agingB >= MAXIMAL_AGING) {
                // we keep updating the low bracket, try to compensate this
                final int p = agingB - MAXIMAL_AGING;
                final double weightA = p + 1;
                final double weightB = (1 << p) - 1;
                targetY = (weightB * yB - weightA * REDUCTION_FACTOR * yA) / (weightA + weightB);
            } else {
                // bracketing is balanced, try to find the root itself
                targetY = 0;
            }

            // make a few attempts to guess a root,
            double nextX;
            int start = 0;
            int end = nbPoints;
            do {

                // guess a value for current target, using inverse polynomial interpolation
                System.arraycopy(x, start, tmpX, start, end - start);
                nextX = this.guessX(targetY, tmpX, y, start, end);

                if (!((nextX > xA) && (nextX < xB))) {
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
                    nextX = Double.NaN;

                }

            } while (Double.isNaN(nextX) && (end - start > 1));

            if (Double.isNaN(nextX)) {
                // fall back to bisection
                nextX = xA + HALF * (xB - xA);
                start = signChangeIndex - 1;
                end = signChangeIndex;
            }

            // evaluate the function at the guessed root
            final double nextY = this.computeObjectiveValue(nextX);
            if (nextY == 0) {
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
            if (nextY * yA <= 0) {
                // the sign change occurs before the inserted point
                xB = nextX;
                yB = nextY;
                absYB = MathLib.abs(yB);
                ++agingA;
                agingB = 0;
            } else {
                // the sign change occurs after the inserted point
                xA = nextX;
                yA = nextY;
                absYA = MathLib.abs(yA);
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
    private double guessX(final double targetY, final double[] x, final double[] y,
                          final int start, final int end) {

        // compute Q Newton coefficients by divided differences
        for (int i = start; i < end - 1; ++i) {
            // evaluate delta for each step
            final int delta = i + 1 - start;
            for (int j = end - 1; j > i; --j) {
                x[j] = (x[j] - x[j - 1]) / (y[j] - y[j - delta]);
            }
        }

        // evaluate Q(targetY)
        double x0 = 0;
        for (int j = end - 1; j >= start; --j) {
            x0 = x[j] + x0 * (targetY - y[j]);
        }

        // return guessed root
        return x0;

    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final UnivariateFunction f, final double min,
                        final double max, final AllowedSolution allowedSolution) {
        this.allowed = allowedSolution;
        return super.solve(maxEval, f, min, max);
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final UnivariateFunction f, final double min,
                        final double max, final double startValue,
                        final AllowedSolution allowedSolution) {
        this.allowed = allowedSolution;
        return super.solve(maxEval, f, min, max, startValue);
    }

}
