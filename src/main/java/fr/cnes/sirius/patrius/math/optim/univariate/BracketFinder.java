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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.univariate;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.util.Incrementor;

//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math model kept as such
/**
 * Provide an interval that brackets a local optimum of a function.
 * This code is based on a Python implementation (from <em>SciPy</em>,
 * module {@code optimize.py} v0.5).
 * 
 * @version $Id: BracketFinder.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.2
 */
public class BracketFinder implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -3649109901691603491L;

    /** Tolerance to avoid division by zero. */
    private static final double EPS_MIN = 1e-21;
    /** Golden section. */
    private static final double GOLD = 1.618034;
    /** Limit. */
    private static final int LIMIT = 100;
    /** Maximum number of evaluations. */
    private static final int MAX_EVAL = 50;

    /**
     * Factor for expanding the interval.
     */
    private final double growLimit;
    /**
     * Counter for function evaluations.
     */
    private final Incrementor evaluations = new Incrementor();
    /**
     * Lower bound of the bracket.
     */
    private double lo;
    /**
     * Higher bound of the bracket.
     */
    private double hi;
    /**
     * Point inside the bracket.
     */
    private double mid;
    /**
     * Function value at {@link #lo}.
     */
    private double fLo;
    /**
     * Function value at {@link #hi}.
     */
    private double fHi;
    /**
     * Function value at {@link #mid}.
     */
    private double fMid;

    /**
     * Constructor with default values {@code 100, 50} (see the {@link #BracketFinder(double,int) other constructor}).
     */
    public BracketFinder() {
        this(LIMIT, MAX_EVAL);
    }

    /**
     * Create a bracketing interval finder.
     * 
     * @param growLimitIn
     *        Expanding factor.
     * @param maxEvaluations
     *        Maximum number of evaluations allowed for finding
     *        a bracketing interval.
     */
    public BracketFinder(final double growLimitIn,
        final int maxEvaluations) {
        if (growLimitIn <= 0) {
            throw new NotStrictlyPositiveException(growLimitIn);
        }
        if (maxEvaluations <= 0) {
            throw new NotStrictlyPositiveException(maxEvaluations);
        }

        this.growLimit = growLimitIn;
        this.evaluations.setMaximalCount(maxEvaluations);
    }

    /**
     * Search new points that bracket a local optimum of the function.
     * 
     * @param func
     *        Function whose optimum should be bracketed.
     * @param goal
     *        {@link GoalType Goal type}.
     * @param xAIn
     *        Initial point.
     * @param xBIn
     *        Initial point.
     * @throws TooManyEvaluationsException
     *         if the maximum number of evaluations
     *         is exceeded.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public void search(final UnivariateFunction func, final GoalType goal, final double xAIn, final double xBIn) {
        double xA = xAIn;
        double xB = xBIn;
        // CHECKSTYLE: resume CyclomaticComplexity check
        this.evaluations.resetCount();
        final boolean isMinim = goal == GoalType.MINIMIZE;

        double fA = this.eval(func, xA);
        double fB = this.eval(func, xB);
        if (isMinim ? fA < fB : fA > fB) {

            double tmp = xA;
            xA = xB;
            xB = tmp;

            tmp = fA;
            fA = fB;
            fB = tmp;
        }

        double xC = xB + GOLD * (xB - xA);
        double fC = this.eval(func, xC);

        while (isMinim ? fC < fB : fC > fB) {
            final double tmp1 = (xB - xA) * (fB - fC);
            final double tmp2 = (xB - xC) * (fB - fA);

            final double val = tmp2 - tmp1;
            final double denom = Math.abs(val) < EPS_MIN ? 2 * EPS_MIN : 2 * val;

            double w = xB - ((xB - xC) * tmp2 - (xB - xA) * tmp1) / denom;
            final double wLim = xB + this.growLimit * (xC - xB);

            double fW;
            if ((w - xC) * (xB - w) > 0) {
                fW = this.eval(func, w);
                if (isMinim ? fW < fC : fW > fC) {
                    xA = xB;
                    xB = w;
                    fA = fB;
                    fB = fW;
                    break;
                } else if (isMinim ? fW > fB : fW < fB) {
                    xC = w;
                    fC = fW;
                    break;
                }
                w = xC + GOLD * (xC - xB);
                fW = this.eval(func, w);
            } else if ((w - wLim) * (wLim - xC) >= 0) {
                w = wLim;
                fW = this.eval(func, w);
            } else if ((w - wLim) * (xC - w) > 0) {
                fW = this.eval(func, w);
                if (isMinim ? fW < fC : fW > fC) {
                    xB = xC;
                    xC = w;
                    w = xC + GOLD * (xC - xB);
                    fB = fC;
                    fC = fW;
                    fW = this.eval(func, w);
                }
            } else {
                w = xC + GOLD * (xC - xB);
                fW = this.eval(func, w);
            }

            xA = xB;
            fA = fB;
            xB = xC;
            fB = fC;
            xC = w;
            fC = fW;
        }

        this.lo = xA;
        this.fLo = fA;
        this.mid = xB;
        this.fMid = fB;
        this.hi = xC;
        this.fHi = fC;

        if (this.lo > this.hi) {
            double tmp = this.lo;
            this.lo = this.hi;
            this.hi = tmp;

            tmp = this.fLo;
            this.fLo = this.fHi;
            this.fHi = tmp;
        }
    }

    /**
     * @return the number of evalutations.
     */
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /**
     * @return the number of evalutations.
     */
    public int getEvaluations() {
        return this.evaluations.getCount();
    }

    /**
     * @return the lower bound of the bracket.
     * @see #getFLo()
     */
    public double getLo() {
        return this.lo;
    }

    /**
     * Get function value at {@link #getLo()}.
     * 
     * @return function value at {@link #getLo()}
     */
    public double getFLo() {
        return this.fLo;
    }

    /**
     * @return the higher bound of the bracket.
     * @see #getFHi()
     */
    public double getHi() {
        return this.hi;
    }

    /**
     * Get function value at {@link #getHi()}.
     * 
     * @return function value at {@link #getHi()}
     */
    public double getFHi() {
        return this.fHi;
    }

    /**
     * @return a point in the middle of the bracket.
     * @see #getFMid()
     */
    public double getMid() {
        return this.mid;
    }

    /**
     * Get function value at {@link #getMid()}.
     * 
     * @return function value at {@link #getMid()}
     */
    public double getFMid() {
        return this.fMid;
    }

    /**
     * @param f
     *        Function.
     * @param x
     *        Argument.
     * @return {@code f(x)}
     * @throws TooManyEvaluationsException
     *         if the maximal number of evaluations is
     *         exceeded.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private double eval(final UnivariateFunction f, final double x) {
        try {
            this.evaluations.incrementCount();
        } catch (final MaxCountExceededException e) {
            throw new TooManyEvaluationsException(e.getMax());
        }
        return f.value(x);
    }

    // CHECKSTYLE: resume CommentRatio check
}
