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

import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomVectorGenerator;

/**
 * Base class multi-start optimizer for a multivariate function. <br/>
 * This class wraps an optimizer in order to use it several times in
 * turn with different starting points (trying to avoid being trapped
 * in a local extremum when looking for a global one). <em>It is not a "user" class.</em>
 * 
 * @param <T>
 *        Type of the point/value pair returned by the optimization
 *        algorithm.
 * 
 * @version $Id: BaseMultiStartMultivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.NullAssignment"})
public abstract class BaseMultiStartMultivariateOptimizer<T>
    extends BaseMultivariateOptimizer<T> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Underlying classical optimizer. */
    private final BaseMultivariateOptimizer<T> optimizer;
    /** Number of evaluations already performed for all starts. */
    private int totalEvaluations;
    /** Number of starts to go. */
    private final int starts;
    /** Random generator for multi-start. */
    private final RandomVectorGenerator generator;
    /** Optimization data. */
    private OptimizationData[] optimData;
    /**
     * Location in {@link #optimData} where the updated maximum
     * number of evaluations will be stored.
     */
    private int maxEvalIndex = -1;
    /**
     * Location in {@link #optimData} where the updated start value
     * will be stored.
     */
    private int initialGuessIndex = -1;

    /**
     * Create a multi-start optimizer from a single-start optimizer.
     * 
     * @param optimizerIn
     *        Single-start optimizer to wrap.
     * @param startsIn
     *        Number of starts to perform. If {@code starts == 1},
     *        the {@link #optimize(OptimizationData[]) optimize} will return the
     *        same solution as the given {@code optimizer} would return.
     * @param generatorIn
     *        Random vector generator to use for restarts.
     * @throws NotStrictlyPositiveException
     *         if {@code starts < 1}.
     */
    public BaseMultiStartMultivariateOptimizer(final BaseMultivariateOptimizer<T> optimizerIn,
        final int startsIn,
        final RandomVectorGenerator generatorIn) {
        super(optimizerIn.getConvergenceChecker());

        if (startsIn < 1) {
            throw new NotStrictlyPositiveException(startsIn);
        }

        this.optimizer = optimizerIn;
        this.starts = startsIn;
        this.generator = generatorIn;
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluations() {
        return this.totalEvaluations;
    }

    /**
     * Gets all the optima found during the last call to {@code optimize}.
     * The optimizer stores all the optima found during a set of
     * restarts. The {@code optimize} method returns the best point only.
     * This method returns all the points found at the end of each starts,
     * including the best one already returned by the {@code optimize} method. <br/>
     * The returned array as one element for each start as specified
     * in the constructor. It is ordered with the results from the
     * runs that did converge first, sorted from best to worst
     * objective value (i.e in ascending order if minimizing and in
     * descending order if maximizing), followed by {@code null} elements
     * corresponding to the runs that did not converge. This means all
     * elements will be {@code null} if the {@code optimize} method did throw
     * an exception.
     * This also means that if the first element is not {@code null}, it is
     * the best point found across all starts. <br/>
     * The behaviour is undefined if this method is called before {@code optimize}; it will likely throw
     * {@code NullPointerException}.
     * 
     * @return an array containing the optima sorted from best to worst.
     */
    public abstract T[] getOptima();

    /**
     * {@inheritDoc}
     * 
     * @throws MathIllegalStateException
     *         if {@code optData} does not contain an
     *         instance of {@link MaxEval} or {@link InitialGuess}.
     */
    @Override
    public T optimize(final OptimizationData... optData) {
        // Store arguments in order to pass them to the internal optimizer.
        this.optimData = optData;
        // Set up base class and perform computations.
        return super.optimize(optData);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected T doOptimize() {
        // Remove all instances of "MaxEval" and "InitialGuess" from the
        // array that will be passed to the internal optimizer.
        // The former is to enforce smaller numbers of allowed evaluations
        // (according to how many have been used up already), and the latter
        // to impose a different start value for each start.
        for (int i = 0; i < this.optimData.length; i++) {
            if (this.optimData[i] instanceof MaxEval) {
                this.optimData[i] = null;
                this.maxEvalIndex = i;
            }
            if (this.optimData[i] instanceof InitialGuess) {
                this.optimData[i] = null;
                this.initialGuessIndex = i;
                continue;
            }
        }
        if (this.maxEvalIndex == -1) {
            throw new MathIllegalStateException();
        }
        if (this.initialGuessIndex == -1) {
            throw new MathIllegalStateException();
        }

        RuntimeException lastException = null;
        this.totalEvaluations = 0;
        this.clear();

        final int maxEval = this.getMaxEvaluations();
        final double[] startPoint = this.getStartPoint();

        // Multi-start loop.
        for (int i = 0; i < this.starts; i++) {
            // CHECKSTYLE: stop IllegalCatch
            try {
                // Decrease number of allowed evaluations.
                this.optimData[this.maxEvalIndex] = new MaxEval(maxEval - this.totalEvaluations);
                // New start value.
                // This does not enforce bounds!
                final double[] s = (i == 0) ?
                    startPoint :
                    this.generator.nextVector();
                this.optimData[this.initialGuessIndex] = new InitialGuess(s);
                // Optimize.
                final T result = this.optimizer.optimize(this.optimData);
                this.store(result);
            } catch (final RuntimeException mue) {
                lastException = mue;
            }
            // CHECKSTYLE: resume IllegalCatch

            this.totalEvaluations += this.optimizer.getEvaluations();
        }

        final T[] optima = this.getOptima();
        if (optima.length == 0) {
            // All runs failed.
            // Cannot be null if starts >= 1.
            throw lastException;
        }

        // Return the best optimum.
        return optima[0];
    }

    /**
     * Method that will be called in order to store each found optimum.
     * 
     * @param optimum
     *        Result of an optimization run.
     */
    protected abstract void store(T optimum);

    /**
     * Method that will called in order to clear all stored optima.
     */
    protected abstract void clear();
}
