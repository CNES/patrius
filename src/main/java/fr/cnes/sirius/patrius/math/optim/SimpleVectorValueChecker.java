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

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Simple implementation of the {@link ConvergenceChecker} interface using
 * only objective function values.
 * 
 * Convergence is considered to have been reached if either the relative
 * difference between the objective function values is smaller than a
 * threshold or if either the absolute difference between the objective
 * function values is smaller than another threshold for all vectors elements. <br/>
 * The {@link #converged(int,PointVectorValuePair,PointVectorValuePair) converged} method will also return {@code true}
 * if the number of iterations has been set
 * (see {@link #SimpleVectorValueChecker(double,double,int) this constructor}).
 * 
 * @version $Id: SimpleVectorValueChecker.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class SimpleVectorValueChecker
    extends AbstractConvergenceChecker<PointVectorValuePair> {
    /**
     * If {@link #maxIterationCount} is set to this value, the number of
     * iterations will never cause {@link #converged(int,PointVectorValuePair,PointVectorValuePair)} to return
     * {@code true}.
     */
    private static final int ITERATION_CHECK_DISABLED = -1;
    /**
     * Number of iterations after which the {@link #converged(int,PointVectorValuePair,PointVectorValuePair)} method
     * will return true (unless the check is disabled).
     */
    private final int maxIterationCount;

    /**
     * Build an instance with specified thresholds.
     * 
     * In order to perform only relative checks, the absolute tolerance
     * must be set to a negative value. In order to perform only absolute
     * checks, the relative tolerance must be set to a negative value.
     * 
     * @param relativeThreshold
     *        relative tolerance threshold
     * @param absoluteThreshold
     *        absolute tolerance threshold
     */
    public SimpleVectorValueChecker(final double relativeThreshold,
        final double absoluteThreshold) {
        super(relativeThreshold, absoluteThreshold);
        this.maxIterationCount = ITERATION_CHECK_DISABLED;
    }

    /**
     * Builds an instance with specified tolerance thresholds and
     * iteration count.
     * 
     * In order to perform only relative checks, the absolute tolerance
     * must be set to a negative value. In order to perform only absolute
     * checks, the relative tolerance must be set to a negative value.
     * 
     * @param relativeThreshold
     *        Relative tolerance threshold.
     * @param absoluteThreshold
     *        Absolute tolerance threshold.
     * @param maxIter
     *        Maximum iteration count.
     * @throws NotStrictlyPositiveException
     *         if {@code maxIter <= 0}.
     * 
     * @since 3.1
     */
    public SimpleVectorValueChecker(final double relativeThreshold,
        final double absoluteThreshold,
        final int maxIter) {
        super(relativeThreshold, absoluteThreshold);

        if (maxIter <= 0) {
            throw new NotStrictlyPositiveException(maxIter);
        }
        this.maxIterationCount = maxIter;
    }

    /**
     * Check if the optimization algorithm has converged considering the
     * last two points.
     * This method may be called several times from the same algorithm
     * iteration with different points. This can be detected by checking the
     * iteration number at each call if needed. Each time this method is
     * called, the previous and current point correspond to points with the
     * same role at each iteration, so they can be compared. As an example,
     * simplex-based algorithms call this method for all points of the simplex,
     * not only for the best or worst ones.
     * 
     * @param iteration
     *        Index of current iteration
     * @param previous
     *        Best point in the previous iteration.
     * @param current
     *        Best point in the current iteration.
     * @return {@code true} if the arguments satify the convergence criterion.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public boolean converged(final int iteration,
                             final PointVectorValuePair previous,
                             final PointVectorValuePair current) {
        // CHECKSTYLE: resume ReturnCount check
        if (this.maxIterationCount != ITERATION_CHECK_DISABLED) {
            if (iteration >= this.maxIterationCount) {
                return true;
            }
        }

        final double[] p = previous.getValueRef();
        final double[] c = current.getValueRef();
        for (int i = 0; i < p.length; ++i) {
            final double pi = p[i];
            final double ci = c[i];
            final double difference = MathLib.abs(pi - ci);
            final double size = MathLib.max(MathLib.abs(pi), MathLib.abs(ci));
            if (difference > size * this.getRelativeThreshold() &&
                difference > this.getAbsoluteThreshold()) {
                return false;
            }
        }

        // Convergence has been reached
        //
        return true;
    }
}
