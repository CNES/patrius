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

/**
 * Maximum number of iterations performed by an (iterative) algorithm.
 * 
 * @version $Id: MaxIter.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class MaxIter implements OptimizationData {

    /** Allowed number of evalutations. */
    private final int maxIterations;

    /**
     * @param max
     *        Allowed number of iterations.
     * @throws NotStrictlyPositiveException
     *         if {@code max <= 0}.
     */
    public MaxIter(final int max) {
        if (max <= 0) {
            throw new NotStrictlyPositiveException(max);
        }

        this.maxIterations = max;
    }

    /**
     * Gets the maximum number of evaluations.
     * 
     * @return the allowed number of evaluations.
     */
    public int getMaxIter() {
        return this.maxIterations;
    }

    /**
     * Factory method that creates instance of this class that represents
     * a virtually unlimited number of iterations.
     * 
     * @return a new instance suitable for allowing {@link Integer#MAX_VALUE} evaluations.
     */
    public static MaxIter unlimited() {
        return new MaxIter(Integer.MAX_VALUE);
    }
}
