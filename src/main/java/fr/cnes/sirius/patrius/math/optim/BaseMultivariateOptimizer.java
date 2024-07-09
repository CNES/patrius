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
package fr.cnes.sirius.patrius.math.optim;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Base class for implementing optimizers for multivariate functions.
 * It contains the boiler-plate code for initial guess and bounds
 * specifications. <em>It is not a "user" class.</em>
 * 
 * @param <T>
 *        Type of the point/value pair returned by the optimization
 *        algorithm.
 * 
 * @version $Id: BaseMultivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class BaseMultivariateOptimizer<T>
    extends BaseOptimizer<T> {
    // CHECKSTYLE: resume AbstractClassName check

    /** Initial guess. */
    private double[] start;
    /** Lower bounds. */
    private double[] lowerBound;
    /** Upper bounds. */
    private double[] upperBound;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected BaseMultivariateOptimizer(final ConvergenceChecker<T> checker) {
        super(checker);
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data.
     *        The following data will be looked for:
     *        <ul>
     *        <li>{@link MaxEval}</li>
     *        <li>{@link InitialGuess}</li>
     *        <li>{@link SimpleBounds}</li>
     *        </ul>
     * @return {@inheritDoc}
     */
    @Override
    public T optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Check input consistency.
        this.checkParameters();
        // Perform optimization.
        return super.optimize(optData);
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link InitialGuess}</li>
     *        <li>{@link SimpleBounds}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof InitialGuess) {
                this.start = ((InitialGuess) data).getInitialGuess();
                continue;
            }
            if (data instanceof SimpleBounds) {
                final SimpleBounds bounds = (SimpleBounds) data;
                this.lowerBound = bounds.getLower();
                this.upperBound = bounds.getUpper();
                continue;
            }
        }
    }

    /**
     * Gets the initial guess.
     * 
     * @return the initial guess, or {@code null} if not set.
     */
    public double[] getStartPoint() {
        return this.start == null ? null : this.start.clone();
    }

    /**
     * @return the lower bounds, or {@code null} if not set.
     */
    public double[] getLowerBound() {
        return this.lowerBound == null ? null : this.lowerBound.clone();
    }

    /**
     * @return the upper bounds, or {@code null} if not set.
     */
    public double[] getUpperBound() {
        return this.upperBound == null ? null : this.upperBound.clone();
    }

    /**
     * Check parameters consistency.
     */
    private void checkParameters() {
        if (this.start != null) {
            final int dim = this.start.length;
            // if lower bound exists, check its size and content
            if (this.lowerBound != null) {
                if (this.lowerBound.length != dim) {
                    // Exception
                    throw new DimensionMismatchException(this.lowerBound.length, dim);
                }
                for (int i = 0; i < dim; i++) {
                    final double v = this.start[i];
                    final double lo = this.lowerBound[i];
                    if (v < lo) {
                        // Exception
                        throw new NumberIsTooSmallException(v, lo, true);
                    }
                }
            }
            // is upper bound exist, check its size and content
            if (this.upperBound != null) {
                if (this.upperBound.length != dim) {
                    // Exception
                    throw new DimensionMismatchException(this.upperBound.length, dim);
                }
                for (int i = 0; i < dim; i++) {
                    final double v = this.start[i];
                    final double hi = this.upperBound[i];
                    if (v > hi) {
                        // Exception
                        throw new NumberIsTooLargeException(v, hi, true);
                    }
                }
            }
        }
    }
}
