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

import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when the a sequence of values is not monotonically
 * increasing or decreasing.
 * 
 * @since 2.2 (name changed to "NonMonotonicSequenceException" in 3.0)
 * @version $Id: NonMonotonicSequenceException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NonMonotonicSequenceException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 3596849179428944575L;
    /**
     * Direction (positive for increasing, negative for decreasing).
     */
    private final MathArrays.OrderDirection direction;
    /**
     * Whether the sequence must be strictly increasing or decreasing.
     */
    private final boolean strict;
    /**
     * Index of the wrong value.
     */
    private final int index;
    /**
     * Previous value.
     */
    private final Number previous;

    /**
     * Construct the exception.
     * This constructor uses default values assuming that the sequence should
     * have been strictly increasing.
     * 
     * @param wrong
     *        Value that did not match the requirements.
     * @param previousIn
     *        Previous value in the sequence.
     * @param indexIn
     *        Index of the value that did not match the requirements.
     */
    public NonMonotonicSequenceException(final Number wrong,
        final Number previousIn,
        final int indexIn) {
        this(wrong, previousIn, indexIn, MathArrays.OrderDirection.INCREASING, true);
    }

    /**
     * Construct the exception.
     * 
     * @param wrong
     *        Value that did not match the requirements.
     * @param previousIn
     *        Previous value in the sequence.
     * @param indexIn
     *        Index of the value that did not match the requirements.
     * @param directionIn
     *        Strictly positive for a sequence required to be
     *        increasing, negative (or zero) for a decreasing sequence.
     * @param strictIn
     *        Whether the sequence must be strictly increasing or
     *        decreasing.
     */
    public NonMonotonicSequenceException(final Number wrong,
        final Number previousIn,
        final int indexIn,
        final MathArrays.OrderDirection directionIn,
        final boolean strictIn) {
        super(directionIn == MathArrays.OrderDirection.INCREASING ?
            (strictIn ?
                PatriusMessages.NOT_STRICTLY_INCREASING_SEQUENCE :
                PatriusMessages.NOT_INCREASING_SEQUENCE) :
            (strictIn ?
                PatriusMessages.NOT_STRICTLY_DECREASING_SEQUENCE :
                PatriusMessages.NOT_DECREASING_SEQUENCE),
            wrong, previousIn, indexIn, indexIn - 1);

        this.direction = directionIn;
        this.strict = strictIn;
        this.index = indexIn;
        this.previous = previousIn;
    }

    /**
     * @return the order direction.
     **/
    public MathArrays.OrderDirection getDirection() {
        return this.direction;
    }

    /**
     * @return {@code true} is the sequence should be strictly monotonic.
     **/
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getStrict() {
        return this.strict;
    }

    /**
     * Get the index of the wrong value.
     * 
     * @return the current index.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @return the previous value.
     */
    public Number getPrevious() {
        return this.previous;
    }
}
