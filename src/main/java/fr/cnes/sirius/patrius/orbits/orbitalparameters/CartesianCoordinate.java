/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * 
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.orbits.OrbitType;

/**
 * Enumerates the different coordinates associated with the cartesian parameters.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* END-HISTORY
 */
public enum CartesianCoordinate implements OrbitalCoordinate {

    /** First component of the position vector. */
    X(0),

    /** Second component of the position vector. */
    Y(1),

    /** Third component of the position vector. */
    Z(2),

    /** First component of the velocity vector. */
    VX(3),

    /** Second component of the velocity vector. */
    VY(4),

    /** Third component of the velocity vector. */
    VZ(5);

    /** Store all the enumerates in an array. */
    private static final CartesianCoordinate[] VALUES = values();

    /** Index of the coordinate in the state vector array. */
    private final int stateVectorIndex;

    /**
     * Creates a new enumeration value with the specified state vector index.
     *
     * @param index
     *        the index of the coordinate in the state vector array
     */
    private CartesianCoordinate(final int index) {
        this.stateVectorIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    public int getStateVectorIndex() {
        return this.stateVectorIndex;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return OrbitType.CARTESIAN;
    }

    /**
     * Gets the coordinate type associated with a given state vector index.
     *
     * @param stateVectorIndex
     *        the state vector index
     * @return the coordinate type associated with the provided state vector index
     * @throws IllegalArgumentException
     *         if the provided state vector index is not between 0 and 5 (included)
     */
    public static CartesianCoordinate valueOf(final int stateVectorIndex) {
        // Check if the state vector index is valid
        OrbitalCoordinate.checkStateVectorIndex(stateVectorIndex);

        return VALUES[stateVectorIndex];
    }
}
