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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Interface for classes listing the coordinates associated with a type of orbital parameters.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
 */
public interface OrbitalCoordinate {

    /**
     * Gets the orbit type to which this orbital coordinate is related.
     *
     * @return the orbit type to which this orbital coordinate is related
     */
    public OrbitType getOrbitType();

    /**
     * Gets the index of the coordinate in the state vector array.
     *
     * @return the index of the coordinate in the state vector array.
     */
    public int getStateVectorIndex();

    /**
     * Gets the coordinate type associated with the same state vector index in a given orbit type
     * and position angle type.
     *
     * @param orbitType
     *        the target orbit type
     * @param positionAngle
     *        the target position angle type
     * @return the coordinate type associated with the same state vector index in the specified
     *         orbit and position angle types
     */
    public default OrbitalCoordinate convertTo(final OrbitType orbitType,
            final PositionAngle positionAngle) {
        return orbitType.getCoordinateType(getStateVectorIndex(), positionAngle);
    }

    /**
     * Static method to check if the state vector index is valid (between 0 and 5 (included)).
     * 
     * @param stateVectorIndex
     *        the state vector index
     * @throws IllegalArgumentException
     *         if the provided state vector index is not between 0 and 5 (included)
     */
    public static void checkStateVectorIndex(final int stateVectorIndex) {
        if ((stateVectorIndex < 0) || (stateVectorIndex > 5)) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.INVALID_STATE_VECTOR_INDEX, stateVectorIndex);
        }
    }
}
