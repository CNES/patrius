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

/**
 * Base class for all convergence checker implementations.
 * 
 * @param <T>
 *        Type of (point, value) pair.
 * 
 * @version $Id: AbstractConvergenceChecker.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public abstract class AbstractConvergenceChecker<T>
    implements ConvergenceChecker<T> {
    /**
     * Relative tolerance threshold.
     */
    private final double relativeThreshold;
    /**
     * Absolute tolerance threshold.
     */
    private final double absoluteThreshold;

    /**
     * Build an instance with a specified thresholds.
     * 
     * @param relativeThresholdIn
     *        relative tolerance threshold
     * @param absoluteThresholdIn
     *        absolute tolerance threshold
     */
    public AbstractConvergenceChecker(final double relativeThresholdIn,
        final double absoluteThresholdIn) {
        this.relativeThreshold = relativeThresholdIn;
        this.absoluteThreshold = absoluteThresholdIn;
    }

    /**
     * @return the relative threshold.
     */
    public double getRelativeThreshold() {
        return this.relativeThreshold;
    }

    /**
     * @return the absolute threshold.
     */
    public double getAbsoluteThreshold() {
        return this.absoluteThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean converged(int iteration,
                                      T previous,
                                      T current);
}
