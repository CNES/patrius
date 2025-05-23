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
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import java.util.Comparator;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;

/**
 * This class implements simplex-based direct search optimization.
 * 
 * <p>
 * Direct search methods only use objective function values, they do not need derivatives and don't either try to
 * compute approximation of the derivatives. According to a 1996 paper by Margaret H. Wright (<a
 * href="http://cm.bell-labs.com/cm/cs/doc/96/4-02.ps.gz">Direct Search Methods: Once Scorned, Now Respectable</a>),
 * they are used when either the computation of the derivative is impossible (noisy functions, unpredictable
 * discontinuities) or difficult (complexity, computation cost). In the first cases, rather than an optimum, a
 * <em>not too bad</em> point is desired. In the latter cases, an optimum is desired but cannot be reasonably found. In
 * all cases direct search methods can be useful.
 * </p>
 * <p>
 * Simplex-based direct search methods are based on comparison of the objective function values at the vertices of a
 * simplex (which is a set of n+1 points in dimension n) that is updated by the algorithms steps.
 * </p>
 * <p>
 * The simplex update procedure ({@link NelderMeadSimplex} or {@link MultiDirectionalSimplex}) must be passed to the
 * {@code optimize} method.
 * </p>
 * <p>
 * Each call to {@code optimize} will re-use the start configuration of the current simplex and move it such that its
 * first vertex is at the provided start point of the optimization. If the {@code optimize} method is called to solve a
 * different problem and the number of parameters change, the simplex must be re-initialized to one with the appropriate
 * dimensions.
 * </p>
 * <p>
 * Convergence is checked by providing the <em>worst</em> points of previous and current simplex to the convergence
 * checker, not the best ones.
 * </p>
 * <p>
 * This simplex optimizer implementation does not directly support constrained optimization with simple bounds; so, for
 * such optimizations, either a more dedicated algorithm must be used like {@link CMAESOptimizer} or
 * {@link BOBYQAOptimizer}, or the objective function must be wrapped in an adapter like
 * {@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter
 * MultivariateFunctionMappingAdapter} or
 * {@link fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter
 * MultivariateFunctionPenaltyAdapter}.
 * </p>
 * 
 * @version $Id: SimplexOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class SimplexOptimizer extends MultivariateOptimizer {
    /** Simplex update rule. */
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
    private AbstractSimplex simplex;

    // CHECKSTYLE: resume IllegalType check

    /**
     * @param checker
     *        Convergence checker.
     */
    public SimplexOptimizer(final ConvergenceChecker<PointValuePair> checker) {
        super(checker);
    }

    /**
     * @param rel
     *        Relative threshold.
     * @param abs
     *        Absolute threshold.
     */
    public SimplexOptimizer(final double rel, final double abs) {
        this(new SimpleValueChecker(rel, abs));
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
     *        <li>{@link AbstractSimplex}</li>
     *        </ul>
     * @return {@inheritDoc}
     */
    @Override
    public PointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CommentRatio check
    @Override
    protected PointValuePair doOptimize() {
        // CHECKSTYLE: resume CommentRatio check

        // Check simplex
        if (this.simplex == null) {
            // Exception, simplex not initialized
            throw new NullArgumentException();
        }

        // Indirect call to "computeObjectiveValue" in order to update the
        // evaluations counter.
        final MultivariateFunction evalFunc = new MultivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double[] point) {
                return SimplexOptimizer.this.computeObjectiveValue(point);
            }
        };

        // Get boolean for goal type
        final boolean isMinim = this.getGoalType() == GoalType.MINIMIZE;
        final Comparator<PointValuePair> comparator = new Comparator<PointValuePair>(){
            /** {@inheritDoc} */
            @Override
            public int compare(final PointValuePair o1,
                               final PointValuePair o2) {
                final double v1 = o1.getValue();
                final double v2 = o2.getValue();
                return isMinim ? Double.compare(v1, v2) : Double.compare(v2, v1);
            }
        };

        // Initialize search.
        this.simplex.build(this.getStartPoint());
        this.simplex.evaluate(evalFunc, comparator);

        PointValuePair[] previous = null;
        int iteration = 0;
        final ConvergenceChecker<PointValuePair> checker = this.getConvergenceChecker();
        while (true) {
            if (iteration > 0) {
                boolean converged = true;
                for (int i = 0; i < this.simplex.getSize(); i++) {
                    final PointValuePair prev = previous[i];
                    converged = converged &&
                        checker.converged(iteration, prev, this.simplex.getPoint(i));
                }
                if (converged) {
                    // We have found an optimum.
                    return this.simplex.getPoint(0);
                }
            }

            // We still need to search.
            previous = this.simplex.getPoints();
            this.simplex.iterate(evalFunc, comparator);
            ++iteration;
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
     *        <li>{@link AbstractSimplex}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof AbstractSimplex) {
                this.simplex = (AbstractSimplex) data;
                // If more data must be parsed, this statement _must_ be
                // changed to "continue".
                break;
            }
        }
    }
}
