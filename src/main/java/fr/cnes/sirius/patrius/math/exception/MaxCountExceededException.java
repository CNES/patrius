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
 * Exception to be thrown when some counter maximum value is exceeded.
 * 
 * @since 3.0
 * @version $Id: MaxCountExceededException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MaxCountExceededException extends MathIllegalStateException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 4330003017885151975L;
    /**
     * Maximum number of evaluations.
     */
    private final Number max;

    /**
     * Construct the exception.
     * 
     * @param maxIn
     *        Maximum.
     */
    public MaxCountExceededException(final Number maxIn) {
        this(PatriusMessages.MAX_COUNT_EXCEEDED, maxIn);
    }

    /**
     * Construct the exception with a specific context.
     * 
     * @param specific
     *        Specific context pattern.
     * @param maxIn
     *        Maximum.
     * @param args
     *        Additional arguments.
     */
    public MaxCountExceededException(final Localizable specific,
        final Number maxIn,
        final Object... args) {
        super();
        this.getContext().addMessage(specific, maxIn, args);
        this.max = maxIn;
    }

    /**
     * @return the maximum number of evaluations.
     */
    public Number getMax() {
        return this.max;
    }
}
