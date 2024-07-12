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

import java.util.Arrays;
import java.util.Comparator;

import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Special implementation of the {@link UnivariateOptimizer} interface
 * adding multi-start features to an existing optimizer. <br/>
 * This class wraps an optimizer in order to use it several times in
 * turn with different starting points (trying to avoid being trapped
 * in a local extremum when looking for a global one).
 * 
 * @version $Id: MultiStartUnivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class MultiStartUnivariateOptimizer
    extends UnivariateOptimizer {
    /** Underlying classical optimizer. */
    private final UnivariateOptimizer optimizer;
    /** Number of evaluations already performed for all starts. */
    private int totalEvaluations;
    /** Number of starts to go. */
    private final int starts;
    /** Random generator for multi-start. */
    private final RandomGenerator generator;
    /** Found optima. */
    private UnivariatePointValuePair[] optima;
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
    private int searchIntervalIndex = -1;

    /**
     * Create a multi-start optimizer from a single-start optimizer.
     * 
     * @param optimizerIn
     *        Single-start optimizer to wrap.
     * @param startsIn
     *        Number of starts to perform. If {@code starts == 1},
     *        the {@code optimize} methods will return the same solution as {@code optimizer} would.
     * @param generatorIn
     *        Random generator to use for restarts.
     * @throws NotStrictlyPositiveException
     *         if {@code starts < 1}.
     */
    public MultiStartUnivariateOptimizer(final UnivariateOptimizer optimizerIn,
        final int startsIn,
        final RandomGenerator generatorIn) {
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
     * the best point found across all starts.
     * 
     * @return an array containing the optima.
     * @throws MathIllegalStateException
     *         if {@link #optimize(OptimizationData[])
     *         optimize} has not been called.
     */
    public UnivariatePointValuePair[] getOptima() {
        if (this.optima == null) {
            throw new MathIllegalStateException(PatriusMessages.NO_OPTIMUM_COMPUTED_YET);
        }
        return this.optima.clone();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MathIllegalStateException
     *         if {@code optData} does not contain an
     *         instance of {@link MaxEval} or {@link SearchInterval}.
     */
    @Override
    public UnivariatePointValuePair optimize(final OptimizationData... optData) {
        // Store arguments in order to pass them to the internal optimizer.
        this.optimData = optData;
        // Set up base class and perform computations.
        return super.optimize(optData);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected UnivariatePointValuePair doOptimize() {
        // Remove all instances of "MaxEval" and "SearchInterval" from the
        // array that will be passed to the internal optimizer.
        // The former is to enforce smaller numbers of allowed evaluations
        // (according to how many have been used up already), and the latter
        // to impose a different start value for each start.
        for (int i = 0; i < this.optimData.length; i++) {
            if (this.optimData[i] instanceof MaxEval) {
                this.optimData[i] = null;
                this.maxEvalIndex = i;
                continue;
            }
            if (this.optimData[i] instanceof SearchInterval) {
                this.optimData[i] = null;
                this.searchIntervalIndex = i;
                continue;
            }
        }
        if (this.maxEvalIndex == -1) {
            throw new MathIllegalStateException();
        }
        if (this.searchIntervalIndex == -1) {
            throw new MathIllegalStateException();
        }

        RuntimeException lastException = null;
        this.optima = new UnivariatePointValuePair[this.starts];
        this.totalEvaluations = 0;

        final int maxEval = this.getMaxEvaluations();
        final double min = this.getMin();
        final double max = this.getMax();
        final double startValue = this.getStartValue();

        // Multi-start loop.
        for (int i = 0; i < this.starts; i++) {
            // CHECKSTYLE: stop IllegalCatch
            try {
                // Decrease number of allowed evaluations.
                this.optimData[this.maxEvalIndex] = new MaxEval(maxEval - this.totalEvaluations);
                // New start value.
                final double s = (i == 0) ?
                    startValue :
                    min + this.generator.nextDouble() * (max - min);
                this.optimData[this.searchIntervalIndex] = new SearchInterval(min, max, s);
                // Optimize.
                this.optima[i] = this.optimizer.optimize(this.optimData);
            } catch (final RuntimeException mue) {
                lastException = mue;
                this.optima[i] = null;
            }
            // CHECKSTYLE: resume IllegalCatch

            this.totalEvaluations += this.optimizer.getEvaluations();
        }

        this.sortPairs(this.getGoalType());

        if (this.optima[0] == null) {
            // Cannot be null if starts >= 1.
            throw lastException;
        }

        // Return the point with the best objective function value.
        return this.optima[0];
    }

    /**
     * Sort the optima from best to worst, followed by {@code null} elements.
     * 
     * @param goal
     *        Goal type.
     */
    private void sortPairs(final GoalType goal) {
        Arrays.sort(this.optima, new Comparator<UnivariatePointValuePair>(){
            /** {@inheritDoc} */
            @Override
            public int compare(final UnivariatePointValuePair o1,
                               final UnivariatePointValuePair o2) {
                // initialize result
                final int res;
                if (o1 == null) {
                    // if o1 null but not o2 return positive value
                    // if both o1 and o2 are null, return 0
                    res = (o2 == null) ? 0 : 1;
                } else if (o2 == null) {
                    // o2 is null but not o1, return negative value
                    res = -1;
                } else {
                    // compute comparison result depending on goaltype :
                    // - if the goal is to minimize, compare the value of o1 with the value of o2
                    // - if the goal is to maximize, compare the value of o2 with the value of o1
                    final double v1 = o1.getValue();
                    final double v2 = o2.getValue();
                    res = (goal == GoalType.MINIMIZE) ?
                        Double.compare(v1, v2) : Double.compare(v2, v1);
                }
                return res;
            }
        });
    }
}
