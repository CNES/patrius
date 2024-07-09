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
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.MultiDimensionMismatchException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when either the number of rows or the number of
 * columns of a matrix do not match the expected values.
 * 
 * @since 3.0
 * @version $Id: MatrixDimensionMismatchException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MatrixDimensionMismatchException extends MultiDimensionMismatchException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -8415396756375798143L;

    /**
     * Construct an exception from the mismatched dimensions.
     * 
     * @param wrongRowDim
     *        Wrong row dimension.
     * @param wrongColDim
     *        Wrong column dimension.
     * @param expectedRowDim
     *        Expected row dimension.
     * @param expectedColDim
     *        Expected column dimension.
     */
    public MatrixDimensionMismatchException(final int wrongRowDim,
        final int wrongColDim,
        final int expectedRowDim,
        final int expectedColDim) {
        super(PatriusMessages.DIMENSIONS_MISMATCH_2x2,
            new Integer[] { wrongRowDim, wrongColDim },
            new Integer[] { expectedRowDim, expectedColDim });
    }

    /**
     * @return the expected row dimension.
     */
    public int getWrongRowDimension() {
        return this.getWrongDimension(0);
    }

    /**
     * @return the expected row dimension.
     */
    public int getExpectedRowDimension() {
        return this.getExpectedDimension(0);
    }

    /**
     * @return the wrong column dimension.
     */
    public int getWrongColumnDimension() {
        return this.getWrongDimension(1);
    }

    /**
     * @return the expected column dimension.
     */
    public int getExpectedColumnDimension() {
        return this.getExpectedDimension(1);
    }
}
