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
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when a number is too large.
 * 
 * @since 2.2
 * @version $Id: NumberIsTooLargeException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NumberIsTooLargeException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 4330003017885151975L;
    /**
     * Higher bound.
     */
    private final Number max;
    /**
     * Whether the maximum is included in the allowed range.
     */
    private final boolean boundIsAllowed;

    /**
     * Construct the exception.
     * 
     * @param wrong
     *        Value that is larger than the maximum.
     * @param maxIn
     *        Maximum.
     * @param boundIsAllowedIn
     *        if true the maximum is included in the allowed range.
     */
    public NumberIsTooLargeException(final Number wrong,
        final Number maxIn,
        final boolean boundIsAllowedIn) {
        this(boundIsAllowedIn ?
            PatriusMessages.NUMBER_TOO_LARGE :
            PatriusMessages.NUMBER_TOO_LARGE_BOUND_EXCLUDED,
            wrong, maxIn, boundIsAllowedIn);
    }

    /**
     * Construct the exception with a specific context.
     * 
     * @param specific
     *        Specific context pattern.
     * @param wrong
     *        Value that is larger than the maximum.
     * @param maxIn
     *        Maximum.
     * @param boundIsAllowedIn
     *        if true the maximum is included in the allowed range.
     */
    public NumberIsTooLargeException(final Localizable specific,
        final Number wrong,
        final Number maxIn,
        final boolean boundIsAllowedIn) {
        super(specific, wrong, maxIn);

        this.max = maxIn;
        this.boundIsAllowed = boundIsAllowedIn;
    }

    /**
     * @return {@code true} if the maximum is included in the allowed range.
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getBoundIsAllowed() {
        return this.boundIsAllowed;
    }

    /**
     * @return the maximum.
     */
    public Number getMax() {
        return this.max;
    }
}