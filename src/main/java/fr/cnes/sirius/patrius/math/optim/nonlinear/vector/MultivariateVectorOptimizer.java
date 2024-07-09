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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector;

import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.BaseMultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;

/**
 * Base class for a multivariate vector function optimizer.
 * 
 * @version $Id: MultivariateVectorOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class MultivariateVectorOptimizer
    extends BaseMultivariateOptimizer<PointVectorValuePair> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Target values for the model function at optimum. */
    private double[] target;
    /** Weight matrix. */
    private RealMatrix weightMatrix;
    /** Model function. */
    private MultivariateVectorFunction model;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected MultivariateVectorOptimizer(final ConvergenceChecker<PointVectorValuePair> checker) {
        super(checker);
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
     *         if the maximal number of evaluations
     *         (of the model vector function) is exceeded.
     */
    protected double[] computeObjectiveValue(final double[] params) {
        super.incrementEvaluationCount();
        return this.model.value(params);
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
     *        <li>{@link Target}</li>
     *        <li>{@link Weight}</li>
     *        <li>{@link ModelFunction}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     * @throws DimensionMismatchException
     *         if the initial guess, target, and weight
     *         arguments have inconsistent dimensions.
     */
    @Override
    public PointVectorValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Check input consistency.
        this.checkParameters();
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /**
     * Gets the weight matrix of the observations.
     * 
     * @return the weight matrix.
     */
    public RealMatrix getWeight() {
        return this.weightMatrix.copy();
    }

    /**
     * Gets the observed values to be matched by the objective vector
     * function.
     * 
     * @return the target values.
     */
    public double[] getTarget() {
        return this.target.clone();
    }

    /**
     * Gets the number of observed values.
     * 
     * @return the length of the target vector.
     */
    public int getTargetSize() {
        return this.target.length;
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link Target}</li>
     *        <li>{@link Weight}</li>
     *        <li>{@link ModelFunction}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof ModelFunction) {
                this.model = ((ModelFunction) data).getModelFunction();
                continue;
            }
            if (data instanceof Target) {
                this.target = ((Target) data).getTarget();
                continue;
            }
            if (data instanceof Weight) {
                this.weightMatrix = ((Weight) data).getWeight();
                continue;
            }
        }
    }

    /**
     * Check parameters consistency.
     * 
     * @throws DimensionMismatchException
     *         if {@link #target} and {@link #weightMatrix} have inconsistent dimensions.
     */
    private void checkParameters() {
        if (this.target.length != this.weightMatrix.getColumnDimension()) {
            throw new DimensionMismatchException(this.target.length,
                this.weightMatrix.getColumnDimension());
        }
    }
}
