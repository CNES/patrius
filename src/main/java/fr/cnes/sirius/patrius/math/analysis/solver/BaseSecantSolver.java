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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.solver;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Base class for all bracketing <em>Secant</em>-based methods for root-finding
 * (approximating a zero of a univariate real function).
 * 
 * <p>
 * Implementation of the {@link RegulaFalsiSolver <em>Regula Falsi</em>} and {@link IllinoisSolver <em>Illinois</em>}
 * methods is based on the following article: M. Dowell and P. Jarratt,
 * <em>A modified regula falsi method for computing the root of an
 * equation</em>, BIT Numerical Mathematics, volume 11, number 2, pages 168-174, Springer, 1971.
 * </p>
 * 
 * <p>
 * Implementation of the {@link PegasusSolver <em>Pegasus</em>} method is based on the following article: M. Dowell and
 * P. Jarratt, <em>The "Pegasus" method for computing the root of an equation</em>, BIT Numerical Mathematics, volume
 * 12, number 4, pages 503-508, Springer, 1972.
 * </p>
 * 
 * <p>
 * The {@link SecantSolver <em>Secant</em>} method is <em>not</em> a bracketing method, so it is not implemented here.
 * It has a separate implementation.
 * </p>
 * 
 * @since 3.0
 * @version $Id: BaseSecantSolver.java 18108 2017-10-04 06:45:27Z bignon $
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseSecantSolver
    extends AbstractUnivariateSolver
    implements BracketedUnivariateSolver<UnivariateFunction> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Default absolute accuracy. */
    protected static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    /** Serializable UID. */
    private static final long serialVersionUID = 3837217855950758878L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** The kinds of solutions that the algorithm may accept. */
    private AllowedSolution allowed;

    /** The <em>Secant</em>-based root-finding method to use. */
    private final Method method;

    /**
     * Construct a solver.
     * 
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param methodIn
     *        <em>Secant</em>-based root-finding method to use.
     */
    protected BaseSecantSolver(final double absoluteAccuracy, final Method methodIn) {
        super(absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = methodIn;
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Relative accuracy.
     * @param absoluteAccuracy
     *        Absolute accuracy.
     * @param methodIn
     *        <em>Secant</em>-based root-finding method to use.
     */
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final Method methodIn) {
        super(relativeAccuracy, absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = methodIn;
    }

    /**
     * Construct a solver.
     * 
     * @param relativeAccuracy
     *        Maximum relative error.
     * @param absoluteAccuracy
     *        Maximum absolute error.
     * @param functionValueAccuracy
     *        Maximum function value error.
     * @param methodIn
     *        <em>Secant</em>-based root-finding method to use
     */
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final double functionValueAccuracy,
                               final Method methodIn) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = methodIn;
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final UnivariateFunction f,
                        final double min, final double max,
                        final AllowedSolution allowedSolution) {
        return this.solve(maxEval, f, min, max, min + HALF * (max - min), allowedSolution);
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final UnivariateFunction f,
                        final double min, final double max, final double startValue,
                        final AllowedSolution allowedSolution) {
        this.allowed = allowedSolution;
        return super.solve(maxEval, f, min, max, startValue);
    }

    /** {@inheritDoc} */
    @Override
    public double solve(final int maxEval, final UnivariateFunction f,
                        final double min, final double max, final double startValue) {
        return this.solve(maxEval, f, min, max, startValue, AllowedSolution.ANY_SIDE);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ConvergenceException
     *         if the algorithm failed due to finite
     *         precision.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    protected final double doSolve() {
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check

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

        // Keep track of inverted intervals, meaning that the left bound is
        // larger than the right bound.
        boolean inverted = false;

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
            if (f1 * fx < 0) {
                // The value of x1 has switched to the other bound, thus inverting
                // the interval.
                x0 = x1;
                f0 = f1;
                inverted = !inverted;
            } else {
                switch (this.method) {
                    case ILLINOIS:
                        f0 *= HALF;
                        break;
                    case PEGASUS:
                        f0 *= f1 / (f1 + fx);
                        break;
                    case REGULA_FALSI:
                        // Detect early that algorithm is stuck, instead of waiting
                        // for the maximum number of iterations to be exceeded.
                        if (x == x1) {
                            throw new ConvergenceException();
                        }
                        break;
                    default:
                        // Should never happen.
                        throw new MathInternalError();
                }
            }

            // Update from [x0, x1] to [x0, x].
            x1 = x;
            f1 = fx;

            // If the function value of the last approximation is too small,
            // given the function value accuracy, then we can't get closer to
            // the root than we already are.
            if (MathLib.abs(f1) <= ftol) {
                switch (this.allowed) {
                    case ANY_SIDE:
                        return x1;
                    case LEFT_SIDE:
                        if (inverted) {
                            return x1;
                        }
                        break;
                    case RIGHT_SIDE:
                        if (!inverted) {
                            return x1;
                        }
                        break;
                    case BELOW_SIDE:
                        if (f1 <= 0) {
                            return x1;
                        }
                        break;
                    case ABOVE_SIDE:
                        if (f1 >= 0) {
                            return x1;
                        }
                        break;
                    default:
                        throw new MathInternalError();
                }
            }

            // If the current interval is within the given accuracies, we
            // are satisfied with the current approximation.
            if (MathLib.abs(x1 - x0) < MathLib.max(rtol * MathLib.abs(x1),
                atol)) {
                return getDoubleFromInterval(inverted, x1, x0, f1);
            }
        }
    }

    /**
     * internal method to get the double value inside the right interval
     * 
     * @param inverted true if it is inverted
     * @param x1 max value
     * @param x0 min value
     * @param f1 objective function value
     * @return double from the interval
     */
    private double getDoubleFromInterval(final boolean inverted, final double x1, final double x0, final double f1) {
        switch (this.allowed) {
            case ANY_SIDE:
                return x1;
            case LEFT_SIDE:
                return inverted ? x1 : x0;
            case RIGHT_SIDE:
                return inverted ? x0 : x1;
            case BELOW_SIDE:
                return (f1 <= 0) ? x1 : x0;
            case ABOVE_SIDE:
                return (f1 >= 0) ? x1 : x0;
            default:
                throw new MathInternalError();
        }
    }

    /** <em>Secant</em>-based root-finding methods. */
    protected enum Method {

        /**
         * The {@link RegulaFalsiSolver <em>Regula Falsi</em>} or <em>False Position</em> method.
         */
        REGULA_FALSI,

        /** The {@link IllinoisSolver <em>Illinois</em>} method. */
        ILLINOIS,

        /** The {@link PegasusSolver <em>Pegasus</em>} method. */
        PEGASUS;
    }
}
