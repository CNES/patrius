/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.BaseMultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;

/**
 * Base class for a multivariate scalar function optimizer.
 * 
 * @version $Id: MultivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class MultivariateOptimizer
    extends BaseMultivariateOptimizer<PointValuePair> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Objective function. */
    private MultivariateFunction function;
    /** Type of optimization. */
    private GoalType goal;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected MultivariateOptimizer(final ConvergenceChecker<PointValuePair> checker) {
        super(checker);
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxEval}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.InitialGuess}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.SimpleBounds}</li>
     *        <li>{@link ObjectiveFunction}</li>
     *        <li>{@link GoalType}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     */
    @Override
    public PointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link ObjectiveFunction}</li>
     *        <li>{@link GoalType}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof GoalType) {
                this.goal = (GoalType) data;
                continue;
            }
            if (data instanceof ObjectiveFunction) {
                this.function = ((ObjectiveFunction) data).getObjectiveFunction();
                continue;
            }
        }
    }

    /**
     * @return the optimization type.
     */
    public GoalType getGoalType() {
        return this.goal;
    }

    /**
     * Computes the objective function value.
     * This method <em>must</em> be called by subclasses to enforce the
     * evaluation counter limit.
     * 
     * @param params
     *        Point at which the objective function must be evaluated.
     * @return the objective function value at the specified point.
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     */
    protected double computeObjectiveValue(final double[] params) {
        super.incrementEvaluationCount();
        return this.function.value(params);
    }
}
