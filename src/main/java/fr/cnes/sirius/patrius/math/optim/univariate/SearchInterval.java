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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;

/**
 * Search interval and (optional) start value. <br/>
 * Immutable class.
 * 
 * @version $Id: SearchInterval.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class SearchInterval implements OptimizationData {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Lower bound. */
    private final double lower;
    /** Upper bound. */
    private final double upper;
    /** Start value. */
    private final double start;

    /**
     * @param lo
     *        Lower bound.
     * @param hi
     *        Upper bound.
     * @param init
     *        Start value.
     * @throws NumberIsTooLargeException
     *         if {@code lo >= hi}.
     * @throws OutOfRangeException
     *         if {@code init < lo} or {@code init > hi}.
     */
    public SearchInterval(final double lo,
        final double hi,
        final double init) {
        if (lo >= hi) {
            throw new NumberIsTooLargeException(lo, hi, false);
        }
        if (init < lo ||
            init > hi) {
            throw new OutOfRangeException(init, lo, hi);
        }

        this.lower = lo;
        this.upper = hi;
        this.start = init;
    }

    /**
     * @param lo
     *        Lower bound.
     * @param hi
     *        Upper bound.
     * @throws NumberIsTooLargeException
     *         if {@code lo >= hi}.
     */
    public SearchInterval(final double lo,
        final double hi) {
        this(lo, hi, HALF * (lo + hi));
    }

    /**
     * Gets the lower bound.
     * 
     * @return the lower bound.
     */
    public double getMin() {
        return this.lower;
    }

    /**
     * Gets the upper bound.
     * 
     * @return the upper bound.
     */
    public double getMax() {
        return this.upper;
    }

    /**
     * Gets the start value.
     * 
     * @return the start value.
     */
    public double getStartValue() {
        return this.start;
    }
}
