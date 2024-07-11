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

import java.util.Arrays;

/**
 * Simple optimization constraints: lower and upper bounds.
 * The valid range of the parameters is an interval that can be infinite
 * (in one or both directions). <br/>
 * Immutable class.
 * 
 * @version $Id: SimpleBounds.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class SimpleBounds implements OptimizationData {
    /** Lower bounds. */
    private final double[] lower;
    /** Upper bounds. */
    private final double[] upper;

    /**
     * @param lB
     *        Lower bounds.
     * @param uB
     *        Upper bounds.
     */
    public SimpleBounds(final double[] lB,
        final double[] uB) {
        this.lower = lB.clone();
        this.upper = uB.clone();
    }

    /**
     * Gets the lower bounds.
     * 
     * @return the lower bounds.
     */
    public double[] getLower() {
        return this.lower.clone();
    }

    /**
     * Gets the upper bounds.
     * 
     * @return the upper bounds.
     */
    public double[] getUpper() {
        return this.upper.clone();
    }

    /**
     * Factory method that creates instance of this class that represents
     * unbounded ranges.
     * 
     * @param dim
     *        Number of parameters.
     * @return a new instance suitable for passing to an optimizer that
     *         requires bounds specification.
     */
    public static SimpleBounds unbounded(final int dim) {
        final double[] lB = new double[dim];
        Arrays.fill(lB, Double.NEGATIVE_INFINITY);
        final double[] uB = new double[dim];
        Arrays.fill(uB, Double.POSITIVE_INFINITY);

        return new SimpleBounds(lB, uB);
    }
}
