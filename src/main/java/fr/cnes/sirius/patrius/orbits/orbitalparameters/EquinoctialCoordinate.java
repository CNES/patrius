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
import fr.cnes.sirius.patrius.orbits.PositionAngle;

/**
 * Enumerates the different types of coordinate associated with the equinoctial parameters.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* END-HISTORY
 */
public enum EquinoctialCoordinate implements OrbitalCoordinate {

    /** Semi-major axis. */
    SEMI_MAJOR_AXIS(0),

    /** First component of the eccentricity vector. */
    E_X(1),

    /** Second component of the eccentricity vector. */
    E_Y(2),

    /** First component of the inclination vector. */
    H_X(3),

    /** Second component of the inclination vector. */
    H_Y(4),

    /** True longitude argument. */
    TRUE_LONGITUDE_ARGUMENT(5),

    /** Mean longitude argument. */
    MEAN_LONGITUDE_ARGUMENT(5),

    /** Eccentric longitude argument. */
    ECCENTRIC_LONGITUDE_ARGUMENT(5);

    /** Store all the enumerates in an array. */
    private static final EquinoctialCoordinate[] VALUES = values();

    /** Index of the coordinate in the state vector array. */
    private final int stateVectorIndex;

    /**
     * Creates a new enumeration value with the specified state vector index.
     *
     * @param index
     *        the index of the coordinate in the state vector array
     */
    private EquinoctialCoordinate(final int index) {
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
        return OrbitType.EQUINOCTIAL;
    }

    /**
     * Gets the coordinate type associated with a given state vector index.
     *
     * @param stateVectorIndex
     *        the state vector index
     * @param positionAngle
     *        the position angle type (only used for the longitude argument)
     * @return the coordinate type associated with the provided state vector index
     * @throws IllegalArgumentException
     *         if the provided state vector index is not between 0 and 5 (included)
     */
    public static EquinoctialCoordinate valueOf(final int stateVectorIndex,
            final PositionAngle positionAngle) {
        final EquinoctialCoordinate coordinateType;

        // Check if the state vector index is valid
        OrbitalCoordinate.checkStateVectorIndex(stateVectorIndex);

        if (stateVectorIndex < 5) {
            // Extract directly the value at the given index
            coordinateType = VALUES[stateVectorIndex];
        } else {
            // Depends on the position angle
            switch (positionAngle) {
                case TRUE:
                    coordinateType = TRUE_LONGITUDE_ARGUMENT;
                    break;
                case MEAN:
                    coordinateType = MEAN_LONGITUDE_ARGUMENT;
                    break;
                case ECCENTRIC:
                    coordinateType = ECCENTRIC_LONGITUDE_ARGUMENT;
                    break;
                default:
                    // Should never happen, kept for safety
                    throw new EnumConstantNotPresentException(PositionAngle.class,
                            positionAngle.name());
            }
        }

        return coordinateType;
    }
}