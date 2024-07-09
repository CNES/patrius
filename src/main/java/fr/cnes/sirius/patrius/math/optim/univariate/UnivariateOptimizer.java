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
package fr.cnes.sirius.patrius.math.optim.univariate;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.BaseOptimizer;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;

/**
 * Base class for a univariate scalar function optimizer.
 * 
 * @version $Id: UnivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class UnivariateOptimizer
    extends BaseOptimizer<UnivariatePointValuePair> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Objective function. */
    private UnivariateFunction function;
    /** Type of optimization. */
    private GoalType goal;
    /** Initial guess. */
    private double start;
    /** Lower bound. */
    private double min;
    /** Upper bound. */
    private double max;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected UnivariateOptimizer(final ConvergenceChecker<UnivariatePointValuePair> checker) {
        super(checker);
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link GoalType}</li>
     *        <li>{@link SearchInterval}</li>
     *        <li>{@link UnivariateObjectiveFunction}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     */
    @Override
    public UnivariatePointValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Perform computation.
        return super.optimize(optData);
    }

    /**
     * @return the optimization type.
     */
    public GoalType getGoalType() {
        return this.goal;
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link GoalType}</li>
     *        <li>{@link SearchInterval}</li>
     *        <li>{@link UnivariateObjectiveFunction}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            // Loop on optimisation data to parse
            if (data instanceof SearchInterval) {
                final SearchInterval interval = (SearchInterval) data;
                this.min = interval.getMin();
                this.max = interval.getMax();
                this.start = interval.getStartValue();
                continue;
            }
            if (data instanceof UnivariateObjectiveFunction) {
                this.function = ((UnivariateObjectiveFunction) data).getObjectiveFunction();
                continue;
            }
            if (data instanceof GoalType) {
                this.goal = (GoalType) data;
                continue;
            }
        }
    }

    /**
     * @return the initial guess.
     */
    public double getStartValue() {
        return this.start;
    }

    /**
     * @return the lower bounds.
     */
    public double getMin() {
        return this.min;
    }

    /**
     * @return the upper bounds.
     */
    public double getMax() {
        return this.max;
    }

    /**
     * Computes the objective function value.
     * This method <em>must</em> be called by subclasses to enforce the
     * evaluation counter limit.
     * 
     * @param x
     *        Point at which the objective function must be evaluated.
     * @return the objective function value at the specified point.
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     */
    protected double computeObjectiveValue(final double x) {
        super.incrementEvaluationCount();
        return this.function.value(x);
    }
}
