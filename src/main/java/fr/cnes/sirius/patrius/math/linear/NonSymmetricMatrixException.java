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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when a symmetric matrix is expected.
 * 
 * @since 3.0
 * @version $Id: NonSymmetricMatrixException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NonSymmetricMatrixException extends MathIllegalArgumentException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -7518495577824189882L;
    /** Row. */
    private final int row;
    /** Column. */
    private final int column;
    /** Threshold. */
    private final double threshold;

    /**
     * Construct an exception.
     * 
     * @param rowIn
     *        Row index.
     * @param columnIn
     *        Column index.
     * @param thresholdIn
     *        Relative symmetry threshold.
     */
    public NonSymmetricMatrixException(final int rowIn,
        final int columnIn,
        final double thresholdIn) {
        super(PatriusMessages.NON_SYMMETRIC_MATRIX, rowIn, columnIn, thresholdIn);
        this.row = rowIn;
        this.column = columnIn;
        this.threshold = thresholdIn;
    }

    /**
     * @return the row index of the entry.
     */
    public int getRow() {
        return this.row;
    }

    /**
     * @return the column index of the entry.
     */
    public int getColumn() {
        return this.column;
    }

    /**
     * @return the relative symmetry threshold.
     */
    public double getThreshold() {
        return this.threshold;
    }
}
