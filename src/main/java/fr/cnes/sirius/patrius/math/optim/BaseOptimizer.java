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
package fr.cnes.sirius.patrius.math.optim;

import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.exception.TooManyIterationsException;
import fr.cnes.sirius.patrius.math.util.Incrementor;

/**
 * Base class for implementing optimizers.
 * It contains the boiler-plate code for counting the number of evaluations
 * of the objective function and the number of iterations of the algorithm,
 * and storing the convergence checker. <em>It is not a "user" class.</em>
 * 
 * @param <T>
 *        Type of the point/value pair returned by the optimization
 *        algorithm.
 * 
 * @version $Id: BaseOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseOptimizer<T> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Evaluations counter. */
    protected final Incrementor evaluations;
    /** Iterations counter. */
    protected final Incrementor iterations;
    /** Convergence checker. */
    private final ConvergenceChecker<T> checker;

    /**
     * @param checkerIn
     *        Convergence checker.
     */
    protected BaseOptimizer(final ConvergenceChecker<T> checkerIn) {
        this.checker = checkerIn;

        this.evaluations = new Incrementor(0, new MaxEvalCallback());
        this.iterations = new Incrementor(0, new MaxIterCallback());
    }

    /**
     * Gets the maximal number of function evaluations.
     * 
     * @return the maximal number of function evaluations.
     */
    public int getMaxEvaluations() {
        return this.evaluations.getMaximalCount();
    }

    /**
     * Gets the number of evaluations of the objective function.
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
     * Gets the maximal number of iterations.
     * 
     * @return the maximal number of iterations.
     */
    public int getMaxIterations() {
        return this.iterations.getMaximalCount();
    }

    /**
     * Gets the number of iterations performed by the algorithm.
     * The number iterations corresponds to the last call to the {@code optimize} method. It is 0 if the method has not
     * been
     * called yet.
     * 
     * @return the number of evaluations of the objective function.
     */
    public int getIterations() {
        return this.iterations.getCount();
    }

    /**
     * Gets the convergence checker.
     * 
     * @return the object used to check for convergence.
     */
    public ConvergenceChecker<T> getConvergenceChecker() {
        return this.checker;
    }

    /**
     * Stores data and performs the optimization. <br/>
     * The list of parameters is open-ended so that sub-classes can extend it
     * with arguments specific to their concrete implementations. <br/>
     * When the method is called multiple times, instance data is overwritten
     * only when actually present in the list of arguments: when not specified,
     * data set in a previous call is retained (and thus is optional in
     * subsequent calls).
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link MaxEval}</li>
     *        <li>{@link MaxIter}</li>
     *        </ul>
     * @return a point/value pair that satifies the convergence criteria.
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     * @throws TooManyIterationsException
     *         if the maximal number of
     *         iterations is exceeded.
     */
    public T optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Reset counters.
        this.evaluations.resetCount();
        this.iterations.resetCount();
        // Perform optimization.
        return this.doOptimize();
    }

    /**
     * Performs the bulk of the optimization algorithm.
     * 
     * @return the point/value pair giving the optimal value of the
     *         objective function.
     */
    protected abstract T doOptimize();

    /**
     * Increment the evaluation count.
     * 
     * @throws TooManyEvaluationsException
     *         if the allowed evaluations
     *         have been exhausted.
     */
    protected void incrementEvaluationCount() {
        this.evaluations.incrementCount();
    }

    /**
     * Increment the iteration count.
     * 
     * @throws TooManyIterationsException
     *         if the allowed iterations
     *         have been exhausted.
     */
    protected void incrementIterationCount() {
        this.iterations.incrementCount();
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link MaxEval}</li>
     *        <li>{@link MaxIter}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof MaxEval) {
                this.evaluations.setMaximalCount(((MaxEval) data).getMaxEval());
                continue;
            }
            if (data instanceof MaxIter) {
                this.iterations.setMaximalCount(((MaxIter) data).getMaxIter());
                continue;
            }
        }
    }

    /**
     * Defines the action to perform when reaching the maximum number
     * of evaluations.
     */
    private static class MaxEvalCallback
        implements Incrementor.MaxCountExceededCallback {
        /**
         * {@inheritDoc}
         * 
         * @throws TooManyEvaluationsException.
         */
        @Override
        public void trigger(final int max) {
            throw new TooManyEvaluationsException(max);
        }
    }

    /**
     * Defines the action to perform when reaching the maximum number
     * of evaluations.
     */
    private static class MaxIterCallback
        implements Incrementor.MaxCountExceededCallback {
        /**
         * {@inheritDoc}
         * 
         * @throws TooManyIterationsException.
         */
        @Override
        public void trigger(final int max) {
            throw new TooManyIterationsException(max);
        }
    }
}
