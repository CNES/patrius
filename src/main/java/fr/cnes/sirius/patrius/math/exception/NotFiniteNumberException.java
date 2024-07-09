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
 * Exception to be thrown when a number is not finite.
 * 
 * @since 3.0
 * @version $Id: NotFiniteNumberException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NotFiniteNumberException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -6100997100383932834L;

    /**
     * Construct the exception.
     * 
     * @param wrong
     *        Value that is infinite or NaN.
     * @param args
     *        Optional arguments.
     */
    public NotFiniteNumberException(final Number wrong,
        final Object... args) {
        this(PatriusMessages.NOT_FINITE_NUMBER, wrong, args);
    }

    /**
     * Construct the exception with a specific context.
     * 
     * @param specific
     *        Specific context pattern.
     * @param wrong
     *        Value that is infinite or NaN.
     * @param args
     *        Optional arguments.
     */
    public NotFiniteNumberException(final Localizable specific,
        final Number wrong,
        final Object... args) {
        super(specific, wrong, args);
    }
}
