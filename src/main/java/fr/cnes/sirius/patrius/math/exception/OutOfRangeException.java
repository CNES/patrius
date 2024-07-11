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
 * Exception to be thrown when some argument is out of range.
 * 
 * @since 2.2
 * @version $Id: OutOfRangeException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class OutOfRangeException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 111601815794403609L;
    /** Lower bound. */
    private final Number lo;
    /** Higher bound. */
    private final Number hi;

    /**
     * Construct an exception from the mismatched dimensions.
     * 
     * @param wrong
     *        Requested value.
     * @param loIn
     *        Lower bound.
     * @param hiIn
     *        Higher bound.
     */
    public OutOfRangeException(final Number wrong,
        final Number loIn,
        final Number hiIn) {
        this(PatriusMessages.OUT_OF_RANGE_SIMPLE, wrong, loIn, hiIn);
    }

    /**
     * Construct an exception from the mismatched dimensions with a
     * specific context information.
     * 
     * @param specific
     *        Context information.
     * @param wrong
     *        Requested value.
     * @param loIn
     *        Lower bound.
     * @param hiIn
     *        Higher bound.
     */
    public OutOfRangeException(final Localizable specific,
        final Number wrong,
        final Number loIn,
        final Number hiIn) {
        super(specific, wrong, loIn, hiIn);
        this.lo = loIn;
        this.hi = hiIn;
    }

    /**
     * @return the lower bound.
     */
    public Number getLo() {
        return this.lo;
    }

    /**
     * @return the higher bound.
     */
    public Number getHi() {
        return this.hi;
    }
}
