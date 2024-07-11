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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.util.ExceptionContext;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when a positive definite matrix is expected.
 * 
 * @since 3.0
 * @version $Id: NonPositiveDefiniteMatrixException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NonPositiveDefiniteMatrixException extends NumberIsTooSmallException {
     /** Serializable UID. */
    private static final long serialVersionUID = 1641613838113738061L;
    /** Index (diagonal element). */
    private final int index;
    /** Threshold. */
    private final double threshold;

    /**
     * Construct an exception.
     * 
     * @param wrong
     *        Value that fails the positivity check.
     * @param indexIn
     *        Row (and column) index.
     * @param thresholdIn
     *        Absolute positivity threshold.
     */
    public NonPositiveDefiniteMatrixException(final double wrong,
        final int indexIn,
        final double thresholdIn) {
        super(wrong, thresholdIn, false);
        this.index = indexIn;
        this.threshold = thresholdIn;

        final ExceptionContext context = this.getContext();
        context.addMessage(PatriusMessages.NOT_POSITIVE_DEFINITE_MATRIX);
        context.addMessage(PatriusMessages.ARRAY_ELEMENT, wrong, indexIn);
    }

    /**
     * @return the row index.
     */
    public int getRow() {
        return this.index;
    }

    /**
     * @return the column index.
     */
    public int getColumn() {
        return this.index;
    }

    /**
     * @return the absolute positivity threshold.
     */
    public double getThreshold() {
        return this.threshold;
    }
}
