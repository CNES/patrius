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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GradientMultivariateOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Non-linear conjugate gradient optimizer.
 * <p>
 * This class supports both the Fletcher-Reeves and the Polak-Ribière update formulas for the conjugate search
 * directions. It also supports optional preconditioning.
 * </p>
 * 
 * @version $Id: NonLinearConjugateGradientOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class NonLinearConjugateGradientOptimizer
    extends GradientMultivariateOptimizer {
    /** Update formula for the beta parameter. */
    private final Formula updateFormula;
    /** Preconditioner (may be null). */
    private final Preconditioner preconditioner;
    /** solver to use in the line search (may be null). */
    private final UnivariateSolver solver;
    /** Initial step used to bracket the optimum in line search. */
    private double initialStep = 1;

    /**
     * Constructor with default {@link BrentSolver line search solver} and {@link IdentityPreconditioner preconditioner}
     * .
     * 
     * @param updateFormulaIn
     *        formula to use for updating the &beta; parameter,
     *        must be one of {@link Formula#FLETCHER_REEVES} or {@link Formula#POLAK_RIBIERE}.
     * @param checker
     *        Convergence checker.
     */
    public NonLinearConjugateGradientOptimizer(final Formula updateFormulaIn,
        final ConvergenceChecker<PointValuePair> checker) {
        this(updateFormulaIn,
            checker,
            new BrentSolver(),
            new IdentityPreconditioner());
    }

    /**
     * Available choices of update formulas for the updating the parameter
     * that is used to compute the successive conjugate search directions.
     * For non-linear conjugate gradients, there are
     * two formulas:
     * <ul>
     * <li>Fletcher-Reeves formula</li>
     * <li>Polak-Ribière formula</li>
     * </ul>
     * 
     * On the one hand, the Fletcher-Reeves formula is guaranteed to converge
     * if the start point is close enough of the optimum whether the
     * Polak-Ribière formula may not converge in rare cases. On the
     * other hand, the Polak-Ribière formula is often faster when it
     * does converge. Polak-Ribière is often used.
     * 
     * @since 2.0
     */
    public static enum Formula {
        /** Fletcher-Reeves formula. */
        FLETCHER_REEVES,
        /** Polak-Ribière formula. */
        POLAK_RIBIERE
    }

    /**
     * Constructor with default {@link IdentityPreconditioner preconditioner}.
     * 
     * @param updateFormulaIn
     *        formula to use for updating the &beta; parameter,
     *        must be one of {@link Formula#FLETCHER_REEVES} or {@link Formula#POLAK_RIBIERE}.
     * @param checker
     *        Convergence checker.
     * @param lineSearchSolver
     *        Solver to use during line search.
     */
    public NonLinearConjugateGradientOptimizer(final Formula updateFormulaIn,
        final ConvergenceChecker<PointValuePair> checker,
        final UnivariateSolver lineSearchSolver) {
        this(updateFormulaIn,
            checker,
            lineSearchSolver,
            new IdentityPreconditioner());
    }

    /**
     * @param updateFormulaIn
     *        formula to use for updating the &beta; parameter,
     *        must be one of {@link Formula#FLETCHER_REEVES} or {@link Formula#POLAK_RIBIERE}.
     * @param checker
     *        Convergence checker.
     * @param lineSearchSolver
     *        Solver to use during line search.
     * @param preconditionerIn
     *        Preconditioner.
     */
    public NonLinearConjugateGradientOptimizer(final Formula updateFormulaIn,
        final ConvergenceChecker<PointValuePair> checker,
        final UnivariateSolver lineSearchSolver,
        final Preconditioner preconditionerIn) {
        super(checker);

        this.updateFormula = updateFormulaIn;
        this.solver = lineSearchSolver;
        this.preconditioner = preconditionerIn;
        this.initialStep = 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxEval}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.InitialGuess}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.SimpleBounds}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunctionGradient}</li>
     *        <li>{@link BracketingStep}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations (of the objective function) is exceeded.
     */
    @Override
    public PointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    protected PointValuePair doOptimize() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final ConvergenceChecker<PointValuePair> checker = this.getConvergenceChecker();
        final double[] point = this.getStartPoint();
        final GoalType goal = this.getGoalType();
        final int n = point.length;
        double[] r = this.computeObjectiveGradient(point);
        if (goal == GoalType.MINIMIZE) {
            for (int i = 0; i < n; i++) {
                r[i] = -r[i];
            }
        }

        // Initial search direction.
        double[] steepestDescent = this.preconditioner.precondition(point, r);
        double[] searchDirection = steepestDescent.clone();

        double delta = 0;
        for (int i = 0; i < n; ++i) {
            delta += r[i] * searchDirection[i];
        }

        PointValuePair current = null;
        int iter = 0;
        int maxEval = this.getMaxEvaluations();
        while (true) {
            ++iter;

            final double objective = this.computeObjectiveValue(point);
            final PointValuePair previous = current;
            current = new PointValuePair(point, objective);
            if (previous != null) {
                if (checker.converged(iter, previous, current)) {
                    // We have found an optimum.
                    return current;
                }
            }

            // Find the optimal step in the search direction.
            final UnivariateFunction lsf = new LineSearchFunction(point, searchDirection);
            final double uB = this.findUpperBound(lsf, 0, this.initialStep);
            // XXX Last parameters is set to a value close to zero in order to
            // work around the divergence problem in the "testCircleFitting"
            // unit test (see MATH-439).
            final double step = this.solver.solve(maxEval, lsf, 0, uB, 1e-15);
            // Subtract used up evaluations.
            maxEval -= this.solver.getEvaluations();

            // Validate new point.
            for (int i = 0; i < point.length; ++i) {
                point[i] += step * searchDirection[i];
            }

            r = this.computeObjectiveGradient(point);
            if (goal == GoalType.MINIMIZE) {
                for (int i = 0; i < n; ++i) {
                    r[i] = -r[i];
                }
            }

            // Compute beta.
            final double deltaOld = delta;
            final double[] newSteepestDescent = this.preconditioner.precondition(point, r);
            delta = 0;
            for (int i = 0; i < n; ++i) {
                delta += r[i] * newSteepestDescent[i];
            }

            final double beta;
            switch (this.updateFormula) {
                case FLETCHER_REEVES:
                    beta = delta / deltaOld;
                    break;
                case POLAK_RIBIERE:
                    double deltaMid = 0;
                    for (int i = 0; i < r.length; ++i) {
                        deltaMid += r[i] * steepestDescent[i];
                    }
                    beta = (delta - deltaMid) / deltaOld;
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
            }
            steepestDescent = newSteepestDescent;

            // Compute conjugate search direction.
            if (iter % n == 0 ||
                beta < 0) {
                // Break conjugation: reset search direction.
                searchDirection = steepestDescent.clone();
            } else {
                // Compute new conjugate search direction.
                for (int i = 0; i < n; ++i) {
                    searchDirection[i] = steepestDescent[i] + beta * searchDirection[i];
                }
            }
        }
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link InitialStep}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof BracketingStep) {
                this.initialStep = ((BracketingStep) data).getBracketingStep();
                // If more data must be parsed, this statement _must_ be
                // changed to "continue".
                break;
            }
        }
    }

    /**
     * Finds the upper bound b ensuring bracketing of a root between a and b.
     * 
     * @param f
     *        function whose root must be bracketed.
     * @param a
     *        lower bound of the interval.
     * @param h
     *        initial step to try.
     * @return b such that f(a) and f(b) have opposite signs.
     * @throws MathIllegalStateException
     *         if no bracket can be found.
     */
    private double findUpperBound(final UnivariateFunction f,
                                  final double a, final double h) {
        final double yA = f.value(a);
        double yB = yA;
        for (double step = h; step < Double.MAX_VALUE; step *= MathLib.max(2, yA / yB)) {
            final double b = a + step;
            yB = f.value(b);
            if (yA * yB <= 0) {
                return b;
            }
        }
        throw new MathIllegalStateException(PatriusMessages.UNABLE_TO_BRACKET_OPTIMUM_IN_LINE_SEARCH);
    }

    /** Default identity preconditioner. */
    public static class IdentityPreconditioner implements Preconditioner {
        /** {@inheritDoc} */
        @Override
        public double[] precondition(final double[] variables, final double[] r) {
            return r.clone();
        }
    }

    /**
     * Internal class for line search.
     * <p>
     * The function represented by this class is the dot product of the objective function gradient and the search
     * direction. Its value is zero when the gradient is orthogonal to the search direction, i.e. when the objective
     * function value is a local extremum along the search direction.
     * </p>
     */
    private class LineSearchFunction implements UnivariateFunction {
        /** Current point. */
        private final double[] currentPoint;
        /** Search direction. */
        private final double[] searchDirection;

        /**
         * @param point
         *        Current point.
         * @param direction
         *        Search direction.
         */
        public LineSearchFunction(final double[] point,
            final double[] direction) {
            this.currentPoint = point.clone();
            this.searchDirection = direction.clone();
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double x) {
            // current point in the search direction
            final double[] shiftedPoint = this.currentPoint.clone();
            for (int i = 0; i < shiftedPoint.length; ++i) {
                shiftedPoint[i] += x * this.searchDirection[i];
            }

            // gradient of the objective function
            final double[] gradient = NonLinearConjugateGradientOptimizer.this.computeObjectiveGradient(shiftedPoint);

            // dot product with the search direction
            double dotProduct = 0;
            for (int i = 0; i < gradient.length; ++i) {
                dotProduct += gradient[i] * this.searchDirection[i];
            }

            return dotProduct;
        }
    }

    /**
     * The initial step is a factor with respect to the search direction
     * (which itself is roughly related to the gradient of the function). <br/>
     * It is used to find an interval that brackets the optimum in line
     * search.
     * 
     * @since 3.1
     */
    public static class BracketingStep implements OptimizationData {
        /** Initial step. */
        private final double initialStep;

        /**
         * @param step
         *        Initial step for the bracket search.
         */
        public BracketingStep(final double step) {
            this.initialStep = step;
        }

        /**
         * Gets the initial step.
         * 
         * @return the initial step.
         */
        public double getBracketingStep() {
            return this.initialStep;
        }
    }
}
