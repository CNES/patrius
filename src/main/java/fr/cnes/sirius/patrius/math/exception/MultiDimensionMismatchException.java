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
package fr.cnes.sirius.patrius.math.exception;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Exception to be thrown when two sets of dimensions differ.
 * 
 * @since 3.0
 * @version $Id: MultiDimensionMismatchException.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MultiDimensionMismatchException extends MathIllegalArgumentException {
     /** Serializable UID. */
    private static final long serialVersionUID = -8415396756375798143L;

    /** Wrong dimensions. */
    private final Integer[] wrong;
    /** Correct dimensions. */
    private final Integer[] expected;

    /**
     * Construct an exception from the mismatched dimensions.
     * 
     * @param wrongIn
     *        Wrong dimensions.
     * @param expectedIn
     *        Expected dimensions.
     */
    public MultiDimensionMismatchException(final Integer[] wrongIn,
        final Integer[] expectedIn) {
        this(PatriusMessages.DIMENSIONS_MISMATCH, wrongIn, expectedIn);
    }

    /**
     * Construct an exception from the mismatched dimensions.
     * 
     * @param specific
     *        Message pattern providing the specific context of
     *        the error.
     * @param wrongIn
     *        Wrong dimensions.
     * @param expectedIn
     *        Expected dimensions.
     */
    public MultiDimensionMismatchException(final Localizable specific,
        final Integer[] wrongIn,
        final Integer[] expectedIn) {
        super(specific, wrongIn, expectedIn);
        this.wrong = wrongIn.clone();
        this.expected = expectedIn.clone();
    }

    /**
     * @return an array containing the wrong dimensions.
     */
    public Integer[] getWrongDimensions() {
        return this.wrong.clone();
    }

    /**
     * @return an array containing the expected dimensions.
     */
    public Integer[] getExpectedDimensions() {
        return this.expected.clone();
    }

    /**
     * @param index
     *        Dimension index.
     * @return the wrong dimension stored at {@code index}.
     */
    public int getWrongDimension(final int index) {
        return this.wrong[index];
    }

    /**
     * @param index
     *        Dimension index.
     * @return the expected dimension stored at {@code index}.
     */
    public int getExpectedDimension(final int index) {
        return this.expected[index];
    }
}
