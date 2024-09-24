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
package fr.cnes.sirius.patrius.math.fraction;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Error thrown when a double value cannot be converted to a fraction
 * in the allowed number of iterations.
 * 
 * @version $Id: FractionConversionException.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class FractionConversionException extends ConvergenceException {

     /** Serializable UID. */
    private static final long serialVersionUID = -4661812640132576263L;

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * 
     * @param value
     *        double value to convert
     * @param maxIterations
     *        maximal number of iterations allowed
     */
    public FractionConversionException(final double value, final int maxIterations) {
        super(PatriusMessages.FAILED_FRACTION_CONVERSION, value, maxIterations);
    }

    /**
     * Constructs an exception with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * 
     * @param value
     *        double value to convert
     * @param p
     *        current numerator
     * @param q
     *        current denominator
     */
    public FractionConversionException(final double value, final long p, final long q) {
        super(PatriusMessages.FRACTION_CONVERSION_OVERFLOW, value, p, q);
    }

}
