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
package fr.cnes.sirius.patrius.math.ode;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * Class mapping the part of a complete state or derivative that pertains
 * to a specific differential equation.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see SecondaryEquations
 * @version $Id: EquationsMapper.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class EquationsMapper implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20110925L;

    /** Index of the first equation element in complete state arrays. */
    private final int firstIndex;

    /** Dimension of the secondary state parameters. */
    private final int dimension;

    /**
     * simple constructor.
     * 
     * @param firstIndexIn
     *        index of the first equation element in complete state arrays
     * @param dimensionIn
     *        dimension of the secondary state parameters
     */
    public EquationsMapper(final int firstIndexIn, final int dimensionIn) {
        this.firstIndex = firstIndexIn;
        this.dimension = dimensionIn;
    }

    /**
     * Get the index of the first equation element in complete state arrays.
     * 
     * @return index of the first equation element in complete state arrays
     */
    public int getFirstIndex() {
        return this.firstIndex;
    }

    /**
     * Get the dimension of the secondary state parameters.
     * 
     * @return dimension of the secondary state parameters
     */
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Extract equation data from a complete state or derivative array.
     * 
     * @param complete
     *        complete state or derivative array from which
     *        equation data should be retrieved
     * @param equationData
     *        placeholder where to put equation data
     * @throws DimensionMismatchException
     *         if the dimension of the equation data does not
     *         match the mapper dimension
     */
    public void extractEquationData(final double[] complete, final double[] equationData) {
        if (equationData.length != this.dimension) {
            throw new DimensionMismatchException(equationData.length, this.dimension);
        }
        System.arraycopy(complete, this.firstIndex, equationData, 0, this.dimension);
    }

    /**
     * Insert equation data into a complete state or derivative array.
     * 
     * @param equationData
     *        equation data to be inserted into the complete array
     * @param complete
     *        placeholder where to put equation data (only the
     *        part corresponding to the equation will be overwritten)
     * @throws DimensionMismatchException
     *         if the dimension of the equation data does not
     *         match the mapper dimension
     */
    public void insertEquationData(final double[] equationData, final double[] complete) {
        if (equationData.length != this.dimension) {
            throw new DimensionMismatchException(equationData.length, this.dimension);
        }
        System.arraycopy(equationData, 0, complete, this.firstIndex, this.dimension);
    }

}
