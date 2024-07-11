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
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when a number is too small.
 * 
 * @since 2.2
 * @version $Id: NumberIsTooSmallException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NumberIsTooSmallException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -6100997100383932834L;
    /**
     * Higher bound.
     */
    private final Number min;
    /**
     * Whether the maximum is included in the allowed range.
     */
    private final boolean boundIsAllowed;

    /**
     * Construct the exception.
     * 
     * @param wrong
     *        Value that is smaller than the minimum.
     * @param minIn
     *        Minimum.
     * @param boundIsAllowedIn
     *        Whether {@code min} is included in the allowed range.
     */
    public NumberIsTooSmallException(final Number wrong,
        final Number minIn,
        final boolean boundIsAllowedIn) {
        this(boundIsAllowedIn ?
            PatriusMessages.NUMBER_TOO_SMALL :
            PatriusMessages.NUMBER_TOO_SMALL_BOUND_EXCLUDED,
            wrong, minIn, boundIsAllowedIn);
    }

    /**
     * Construct the exception with a specific context.
     * 
     * @param specific
     *        Specific context pattern.
     * @param wrong
     *        Value that is smaller than the minimum.
     * @param minIn
     *        Minimum.
     * @param boundIsAllowedIn
     *        Whether {@code min} is included in the allowed range.
     */
    public NumberIsTooSmallException(final Localizable specific,
        final Number wrong,
        final Number minIn,
        final boolean boundIsAllowedIn) {
        super(specific, wrong, minIn);

        this.min = minIn;
        this.boundIsAllowed = boundIsAllowedIn;
    }

    /**
     * @return {@code true} if the minimum is included in the allowed range.
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getBoundIsAllowed() {
        return this.boundIsAllowed;
    }

    /**
     * @return the minimum.
     */
    public Number getMin() {
        return this.min;
    }
}
